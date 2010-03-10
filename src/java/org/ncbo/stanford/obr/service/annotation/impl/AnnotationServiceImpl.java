package org.ncbo.stanford.obr.service.annotation.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import obs.common.beans.DictionaryBean;
import obs.common.utils.ExecutionTimer;
import obs.obr.populate.Structure;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.dao.annoation.DirectAnnotationDao.DirectAnnotationEntry;
import org.ncbo.stanford.obr.dao.dictionary.DictionaryDao;
import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.ncbo.stanford.obr.resource.ResourceAccessTool;
import org.ncbo.stanford.obr.service.AbstractResourceService;
import org.ncbo.stanford.obr.service.annotation.AnnotationService;
import org.ncbo.stanford.obr.util.FileResourceParameters;
import org.ncbo.stanford.obr.util.mgrep.ConceptRecognitionTools;

public class AnnotationServiceImpl extends AbstractResourceService implements
		AnnotationService {

	// Logger for AnnotationServiceImpl
	protected static Logger logger = Logger
			.getLogger(AnnotationServiceImpl.class);

	public AnnotationServiceImpl(ResourceAccessTool resourceAccessTool) {
		super(resourceAccessTool);
	}
	 
	/** 
	 * Processes the resource with Mgrep and populates the the corresponding _DAT.
	 * The given boolean specifies if the complete dictionary must be used, or not (only the delta dictionary).
	 * The annotation done with termName that are in the specified stopword list are removed from _DAT.
	 * This function implements the step 2 of the OBR workflow.
	 *  
	 * @param withCompleteDictionary if true uses complete dictionary for annotation otherwise uses delta dictionary
	 * @param dictionary Latest dictionary bean
	 * @param stopwords {@code Set} of string used as stopwords
	 * @return {@code int} the number of direct annotations created. 
	 */
	public int resourceAnnotation(boolean withCompleteDictionary, DictionaryBean dictionary, 
			HashSet<String> stopwords) {
		int nbAnnotation;
		boolean useTemporaryElementTable;	
		 
		if(resourceAccessTool.getResourceType() == ResourceType.SMALL){
			useTemporaryElementTable =false; 
		}else{
			useTemporaryElementTable =true; 
		} 

		// processes direct mgrep annotations
		nbAnnotation = this.conceptRecognitionWithMgrep(dictionary,
		 		withCompleteDictionary, stopwords);

		// processes direct reported annotations
		nbAnnotation += this.reportExistingAnnotations(dictionary);

		// updates the dictionary column in _ET
		logger.info("Updating the dictionary field in ElementTable...");
		  
		// Update dictionary id for element table.
		try{
			elementTableDao.updateDictionary(dictionary.getDictionaryID(), useTemporaryElementTable);	
		}finally{
			if(useTemporaryElementTable){
				elementTableDao.deleteTemporaryTable();
			} 
		}
			
		return nbAnnotation;
	}

	/**
	 * Applies Mgrep on the corresponding resource. Only the elements in _ET
	 * with a dictionaryID < to the latest one are selected (or the one with
	 * null); Returns the number of annotations added to _DAT.
	 */
	private int conceptRecognitionWithMgrep(DictionaryBean dictionary,
			boolean withCompleteDictionary, HashSet<String> stopwords) {
		int nbDirectAnnotation = 0;
		ExecutionTimer timer = new ExecutionTimer();

		logger.info("** Concept recognition with Mgrep:");
		// Checks if the dictionary file exists
		File dictionaryFile;
		try {
			if (withCompleteDictionary) {
				dictionaryFile = new File(DictionaryDao
						.completeDictionaryFileName(dictionary));
			} else {
				dictionaryFile = new File(DictionaryDao
						.dictionaryFileName(dictionary));
			}
			if (dictionaryFile.createNewFile()) {
				logger.info("Re-creation of the dictionaryFile...");
				HashSet<String> localOntologyIDs = null;
				
				// For big resources get list of ontologies used for annotation.
				if(resourceAccessTool.getResourceType() == ResourceType.BIG){
					localOntologyIDs= resourceAccessTool.getOntolgiesForAnnotation();
				}
				
				if (withCompleteDictionary) {
					dictionaryDao.writeDictionaryFile(dictionaryFile,localOntologyIDs);
				} else {
					dictionaryDao.writeDictionaryFile(dictionaryFile, dictionary
							.getDictionaryID(),localOntologyIDs);
				}
			}
		} catch (IOException e) {
			dictionaryFile = null;
			logger
					.error(
							"** PROBLEM ** Cannot create the dictionaryFile. null returned.",
							e);
		}

		// Writes the resource file with the elements not processed with the
		// latest dictionary
		timer.start();
		File resourceFile = this.writeMgrepResourceFile(dictionary
				.getDictionaryID());
		timer.end();
		logger.info("ResourceFile created in: "
				+ timer.millisecondsToTimeString(timer.duration()) + "\n");

		// Calls Mgrep
		timer.reset();
		timer.start();
		File mgrepFile = this.mgrepCall(dictionaryFile, resourceFile);
		timer.end();
		logger.info("Mgrep executed in: "
				+ timer.millisecondsToTimeString(timer.duration()));

		// Process the Mgrep result file
		timer.reset();
		timer.start();
		nbDirectAnnotation = this.processMgrepFile(mgrepFile, dictionary
				.getDictionaryID());
		timer.end();
		logger.info("MgrepFile processed in: "
				+ timer.millisecondsToTimeString(timer.duration()));

		// Deletes the files created for Mgrep and generated by Mgrep
		resourceFile.delete();
		mgrepFile.delete();

		// Removes Mgrep annotations done with the given list of stopwords
		timer.reset();
		timer.start();
		int nbDelete = directAnnotationTableDao
				.deleteEntriesFromStopWords(stopwords);
		timer.end();
		logger.info(nbDelete + " annotations removed with stopword list in: "
				+ timer.millisecondsToTimeString(timer.duration()));

		return nbDirectAnnotation - nbDelete;
	}

	private File mgrepCall(File dictionaryFile, File resourceFile) {
		logger.info("Call to Mgrep...");
		File mgrepFile = null;
		try {
			mgrepFile = ConceptRecognitionTools.mgrepLocal(dictionaryFile,
					resourceFile);
		} catch (IOException e) {
			logger.error("** PROBLEM ** Cannot create MgrepFile.", e);
		} catch (Exception e) {
			logger.error("** PROBLEM ** Cannot execute Mgrep.", e);
		}

		return mgrepFile;

	}

	private int processMgrepFile(File mgrepFile, int dictionaryID) {
		int nbAnnotation = -1;
		logger.info("Processing of the result file...");
		nbAnnotation = directAnnotationTableDao.loadMgrepFile(mgrepFile,
				dictionaryID);
		logger.info(nbAnnotation + " annotations done with Mgrep.");
		return nbAnnotation;
	}

	/********************************* EXPORT CONTENT FUNCTIONS *****************************************************/

	/**
	 * Returns a file that respects the Mgrep resource file requirements. This
	 * text file has 3 columns: [integer | integer | text] they are respectively
	 * [elementID | contextID | text]. The file contains only the element that
	 * have been already annotated with a previous version of the given
	 * dictionary (or never annotated).
	 */
	public File writeMgrepResourceFile(int dictionaryID) {
		logger
				.info("Exporting the resource content to a file to be annotated with Mgrep...");
		String name = FileResourceParameters.mgrepInputFolder()
				+ ResourceAccessTool.RESOURCE_NAME_PREFIX
				+ resourceAccessTool.getToolResource().getResourceID() + "_V"
				+ dictionaryID + "_MGREP.txt";
		File mgrepResourceFile = new File(name);
		try {
			mgrepResourceFile.createNewFile();
			
			boolean useTemporaryElementTable;			
			if(resourceAccessTool.getResourceType() == ResourceType.SMALL){
				useTemporaryElementTable =false; 
			}else{
				useTemporaryElementTable =true; 
			}
			
			elementTableDao.writeNonAnnotatedElements(mgrepResourceFile,
					dictionaryID, resourceAccessTool.getToolResource()
							.getResourceStructure(), useTemporaryElementTable);
		} catch (IOException e) {
			logger.error(
					"** PROBLEM ** Cannot create Mgrep file for exporting resource "
							+ resourceAccessTool.getToolResource()
									.getResourceName(), e);
		}
		return mgrepResourceFile;
	}

	/**
	 * For annotations with concepts from ontologies that already exist in the
	 * resource, annotations are reported to the _ET table in the form of
	 * localConceptIDs separated by '> '. This function transfers the reported
	 * annotation into the corresponding _DAT table in order for them to be
	 * available in the same format and to be processed by the rest of the
	 * workflow (semantic expansion). It use the dictionaryID of the given
	 * dictionary. Returns the number of reported annotations added to _DAT.
	 */
	private int reportExistingAnnotations(DictionaryBean dictionary) {
		int nbReported;
		ExecutionTimer timer = new ExecutionTimer();

		logger.info("Processing of existing reported annotations...");
		timer.start();
		nbReported = directAnnotationTableDao.addEntries(getExistingAnnotations(dictionary.getDictionaryID(),
						resourceAccessTool.getToolResource()
								.getResourceStructure()));

		timer.end();
		logger.info(nbReported + " reported annotations processed in: "
				+ timer.millisecondsToTimeString(timer.duration()));

		return nbReported;
	}
	
	/**
	 * 
	 * @param dictionaryID
	 * @param structure
	 * @return
	 */
	public HashSet<DirectAnnotationEntry> getExistingAnnotations(int dictionaryID, Structure structure){
		
		HashSet<DirectAnnotationEntry> reportedAnnotations = new HashSet<DirectAnnotationEntry>();
		
		boolean useTemporaryElementTable;			
		if(resourceAccessTool.getResourceType() == ResourceType.SMALL){
			useTemporaryElementTable =false; 
		}else{
			useTemporaryElementTable =true; 
		}
		
		for(String contextName: structure.getContextNames()){
			// we must exclude contexts NOT_FOR_ANNOTATION and contexts FOR_CONCEPT_RECOGNITION 
			if(!structure.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION) &&
					!structure.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)){
				boolean isNewVersionOntlogy = ontologyDao.hasNewVersionOfOntology(structure.getOntoID(contextName), structure.getResourceID());
				String localOntologyID = ontologyDao.getLatestLocalOntologyID(structure.getOntoID(contextName));
				reportedAnnotations.addAll(elementTableDao.getExistingAnnotations(dictionaryID, structure, contextName, localOntologyID, isNewVersionOntlogy, useTemporaryElementTable));				
			}
			
		}
		return reportedAnnotations;
	}

	/**
	 * Method removes annotations for given ontology versions.
	 * 
	 * <p>For big resources, it remove local ontology id one by one
	 * <p>For other resources remove all local ontology ids
	 * 
	 * @param {@code List} of localOntologyIDs String containing ontology versions.
	 */
	public void removeAnnotations(List<String> localOntologyIDs) {
		
		if(resourceAccessTool.getResourceType()!= ResourceType.BIG){
			 directAnnotationTableDao.deleteEntriesFromOntologies(localOntologyIDs);	 
		 }else{
			 for (String localOntologyID : localOntologyIDs) {
				 directAnnotationTableDao.deleteEntriesFromOntology(localOntologyID);	 
			}
			 
		 }
	}

	/**
	 * This method creates temporary element table used for annotation for non annotated
	 * element for given dictionary id.
	 * 
	 * @param dictionaryID 
	 * @return Number of rows containing in temporary table
	 */ 
	public int createTemporaryElementTable(int dictionaryID) {
		 return elementTableDao.createTemporaryTable(dictionaryID, resourceAccessTool.getMaxNumberOfElementsToProcess());		
	}
}
