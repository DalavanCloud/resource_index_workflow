package org.ncbo.stanford.obr.resource.uniprotkb;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import obs.obr.populate.Element;
import obs.obr.populate.Structure;

import org.ncbo.stanford.obr.resource.ResourceAccessTool;
 
/**
 * extract GO annotation on protein entry of uniprot. This data are in the following file
 * ftp://ftp.geneontology.org/pub/go/gene-associations/gene_association.goa_human.gz
 * @author  Adrien Coulet
 * @version OBR v1
 * @date    21-Nov-2008
 */
public class UpkbAnnotAccessTool extends ResourceAccessTool  {

	//	private static final String UPKB_FILE    = "ftp://ftp.geneontology.org/pub/go/gene-associations/gene_association.goa_human.gz";
	private static final String UPKB_URL         = "http://www.uniprot.org/";
	private static final String UPKB_NAME        = "UniProt KB";
	private static final String UPKB_RESOURCEID  = "UPKB";
	private static final String UPKB_DESCRIPTION = "The mission of UniProt is to provide the scientific community with a comprehensive, high-quality and freely accessible resource of protein sequence and functional information.";
	private static final String UPKB_LOGO        = "http://www.uniprot.org/images/logo.gif";
	private static final String UPKB_ELT_URL     = "http://www.uniprot.org/uniprot/";
	
	private static final String[] UPKB_ITEMKEYS  = {"geneSymbol",						"goAnnotationList",	"proteinName"};
	private static final Double[] UPKB_WEIGHTS 	 = {1.0,                				1.0,           		0.7};
	// Virtual ontlogy id for gene ontology (GO)- 1070
	private static final String[] UPKB_ONTOIDS 	 = {Structure.FOR_CONCEPT_RECOGNITION, 	"1070",        		Structure.FOR_CONCEPT_RECOGNITION}; // "" when the itemkey is not an annotation, the static id (internal) of the ontology which is used for annotations
	
	// XX_ITEMKEYS_ONTOID: "null" if the corresponding itemkey is not an annotation; if the corresponding itemkey is an existing annotation, then it is the OBS static id (internal) of the ontology used for annotations 
	// OBS static ontology id examples are: "GO", "NCI" for UMLS ontologies or 1061 (SO-Pharm), 1032 (NCI) for bioportal ontologies  
	private static Structure UPKB_STRUCTURE      = new Structure(UPKB_ITEMKEYS, UPKB_RESOURCEID, UPKB_WEIGHTS, UPKB_ONTOIDS);
	private static String UPKB_MAIN_ITEMKEY      = "geneSymbol";	
	
	private Map<String, String> localOntologyIDMap;
	
	public UpkbAnnotAccessTool(){
		super(UPKB_NAME, UPKB_RESOURCEID, UPKB_STRUCTURE );
		try {
			this.getToolResource().setResourceURL(new URL(UPKB_URL));
			this.getToolResource().setResourceLogo(new URL(UPKB_LOGO));
			this.getToolResource().setResourceElementURL(UPKB_ELT_URL);
		} catch (MalformedURLException e) {
			logger.error("", e);
		}
		this.getToolResource().setResourceDescription(UPKB_DESCRIPTION);		
		localOntologyIDMap = createLocalOntologyIDMap(UPKB_STRUCTURE);
	}

	@Override
	public void updateResourceInformation() {
		// TODO See if it can be implemented for this resource.
	}
	
	@Override
	public HashSet<String> queryOnlineResource(String query) {
		// TODO See if it can be implemented for this resource.
		return new HashSet<String>();
	}
	

	@Override
	public String elementURLString(String elementLocalID) {
		return UPKB_ELT_URL + elementLocalID;
	}
	
	@Override
	public String mainContextDescriptor() {
		return UPKB_MAIN_ITEMKEY;
	}
	
	@Override
	public int updateResourceContent() {
		int nbElement = 0;	
		try {		
			Element  myProt;		
			
			HashSet<Element> annotList = this.getAllAnnotations();
			
			// gets the elements already in the corresponding _ET
			HashSet<String> allElementsInET = this.resourceUpdateService.getAllLocalElementIDs();
			// traverses the set of elements returned by Upkb to prune it with the element in ET
			HashSet<Element> elementsToRemove = new HashSet<Element>(); 
			for (Element annot: annotList){
				if (allElementsInET.contains(annot.getLocalElementID())){
					elementsToRemove.add(annot);
				}
			}
			annotList.removeAll(elementsToRemove);
			logger.info("Number of new elements to dump: " + annotList.size());
			
			// for each protein annotation accessed by the tool
			Iterator<Element> i = annotList.iterator();
			while(i.hasNext()){
				// populates OBR_UPKB_ET with each of these protein annotation
				myProt = i.next();
				try{
					if(!myProt.getElementStructure().hasNullValues()){				
						if(this.resourceUpdateService.addElement(myProt)){
							nbElement++;
						}
					}				
				} catch (Exception e) {
					logger.error("** PROBLEM ** Problem with disease "+ myProt.getLocalElementID() +" when populating the OBR_UPKB_ET table.", e);
				}
			}
		} catch (Exception e) {
			logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName() + " with the file ftp://ftp.geneontology.org/pub/go/gene-associations/gene_association.goa_human.gz", e); 
		}
		logger.info(nbElement+" annotated proteins added to the OBR_UPKB_ET table.");
		return nbElement;
	}

	/**
	 * get all Uniprot protein in the file ftp://ftp.geneontology.org/pub/go/gene-associations/gene_association.goa_human.gz
	 * This access tool get all the data with only one function (no getList then getOneElementData).
	 * This choice is due to the format of the resource: an file downloaded by ftp and parsed once. 
	 */
	public HashSet<Element> getAllAnnotations(){
		logger.info("* Get All GO annotations of human proteins ... ");		
		HashSet<Element> annotList = new HashSet<Element>();
		int nbAdded = 0;
		try{
			GetUniprotGOAnnotations myExtractor = new GetUniprotGOAnnotations(this.getToolResource()); 
			annotList = myExtractor.getElements(localOntologyIDMap);
		}catch(Exception e){
			logger.error("** PROBLEM ** Problem with extracting annotation from distant file. Maybe check the script 'getGoUniprotAnnot.sh'", e);
		}
		nbAdded = annotList.size();
		logger.info((nbAdded)+" annotated proteins found.");
		return annotList;
	}	
	
	public HashMap<String, String>  createLocalOntologyIDMap(Structure structure){
		HashMap<String, String> localOntologyIDMap = new HashMap<String, String>();
		String virtualOntologyID;
		for (String contextName: structure.getOntoIDs().keySet()){
			virtualOntologyID = structure.getOntoIDs().get(contextName);
			if(!virtualOntologyID.equals(Structure.FOR_CONCEPT_RECOGNITION) && 
					!virtualOntologyID.equals(Structure.NOT_FOR_ANNOTATION)){
				localOntologyIDMap.put(contextName, ontlogyService.getLatestLocalOntologyID(virtualOntologyID));
			} 
		}
		return localOntologyIDMap;
	}
	 
}
