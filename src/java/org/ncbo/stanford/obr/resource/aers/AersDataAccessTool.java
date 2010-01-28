package org.ncbo.stanford.obr.resource.aers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import obs.common.utils.UnzipUtils;
import obs.obr.populate.Element;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

import org.ncbo.stanford.obr.resource.AbstractXmlResourceAccessTool;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
/**
 * AersDataAccessTool is responsible for getting data elements from zip.
 * Gets the safety report data to populate _ET table 
 * using zip file.
 * zip file found at location
 * http://www.fda.gov/downloads/Drugs/GuidanceComplianceRegulatoryInformation/Surveillance/AdverseDrugEffects/
 * @author palanisamy
 *
 */
//Note: before start workflow execution on AERS RAT, check value of 'aersZip' and 'aersExtract' variables (It needs to be changed each time)
public class AersDataAccessTool extends AbstractXmlResourceAccessTool {
	
	private static final String AERS_URL = "http://www.aersdata.com/";	
	private static final String AERS_NAME = "Adverse Event Reporting System Data";	
	private static final String AERS_RESOURCEID = "AERS";	
	private static final String AERS_DESCRIPTION = "Adverse Event Reporting System (AERS) Databases are validated and normalized to provide consistent results to our users that depend on data such as this for research into Adverse Events reported to the FDA by Consumers, Doctors and many other health care related professionals.";
	private static final String AERS_LOGO = "http://www.aersdata.com/images/newidex.jpg";
	private static final String AERS_ELT_URL = "http://www.aersdata.com/Quicksearch/QuickAersdata_list.php?a=search&value=1&SearchField=1&SearchOption=Equals&SearchFor=?";
	private static final String[] AERS_ITEMKEYS	= {"REAC" , "DRUG_CHAR" , "DRUG_NAMES", "DRUG_ADMIN_ROUTE" , "DRUG_INDI"};
	private static final Double[] AERS_WEIGHTS = {1.0, 0.9, 1.0, 0.9, 1.0};
	private static final String[] AERS_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION,Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION};
	private static final Structure AERS_STRUCTURE = new Structure(AERS_ITEMKEYS, AERS_RESOURCEID, AERS_WEIGHTS, AERS_ONTOIDS);
	private static final String AERS_MAIN_ITEMKEY = "DRUG_NAMES";
		
	// AERS data file down load location (URL)  
	private static final String AERS_URL_ZIP = "http://www.fda.gov/downloads/Drugs/GuidanceComplianceRegulatoryInformation/Surveillance/AdverseDrugEffects/";
	protected static final String HYPEN_STRING = "-";
	private static Properties safetyReportsSpecification;
	private static final String FILE_EXTENSION_	= ".SGM";
	
	// Safety report test file name
	private static final String TEST_FILE = "ADR_TEST.SGM";
	
	//sub directories under directory for Extracts Data Files  
	private static final String SUB_G_DIR = "sgml";
	
	//sub directories under directory for Extracts Data Files  
	private static final String SUB_Q_DIR = "sqml";
	
	// Constant for 'safety report' string
	private static final String AERS_SAFETYREPORT 		= "safetyreport";	
	private static final String AERS_SAFETY_ID_			= "safetyreportid";
	private static final String AERS_PATIENT_			= "patient";
	private static final String AERS_REACTION_			= "reaction";
	private static final String AERS_REACTION_DES_DRAFT_= "reactionmeddrapt";
	private static final String AERS_DRUG_				= "drug";
	private static final String AERS_DRUG_CHAR_			= "drugcharacterization";
	private static final String AERS_DRUG_NAME_			= "medicinalproduct";
	private static final String AERS_DRUG_ADMIN_		= "drugadministrationroute";
	private static final String AERS_DRUG_INDI_			= "drugindication";
	
	//All Zip Files name
	private static final String[] arraersZip  = new String[]{"ucm084155.zip", "ucm083998.zip", "ucm083854.zip", "ucm085815.zip",
															"ucm085799.zip", "ucm085789.zip", "ucm085782.zip", "UCM150386.zip",
															"UCM173889.zip","UCM186489.zip","UCM197919.zip"};
	//execute workflow for AERS RAT by passing 'arraersZip' array index one by one
	private static final String[] aersZip	  =	new String[]{arraersZip[3]};// value for this variable varies by execution 
	
	//All extracts Data Files name
	private static final String[] arraersExtract =	new String[]{"aers_sgml_2007q1","aers_sgml_2007q2","aers_sgml_2007q3","aers_sgml_2007q4",
		 														"aers_sgml_2008q1","aers_sgml_2008q2","aers_sgml_2008q3","aers_sgml_2008q4",
		 														"aers_sgml_2009_q1","aers_sgml_2009_q2","aers_sgml_2009_q3"};
	//execute workflow for AERS RAT by passing 'arraersExtract' array index one by one
	private static final String[] aersExtract =	new String[]{arraersExtract[3]};// value for this variable varies by execution
	
	//drugcharacterization key values appends with this string in safety reports specification properties 
	private static final String SPEC_CHAR_="DRUG_CHAR_";
	
	//drugadministrationroute key values appends with this string in safety reports specification properties
	private static final String SPEC_ADMIN_="DRUG_ADMIN_";
	
	/**
	 * Constructor for AersDataAccessTool
	 * @param obsConnectionInfo
	 */
	public AersDataAccessTool(){
		super(AERS_NAME, AERS_RESOURCEID, AERS_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(AERS_URL));
			this.getToolResource().setResourceLogo(new URL(AERS_LOGO));
			this.getToolResource().setResourceElementURL(AERS_ELT_URL);
			//Loads the properties file to get all the drugcharacterization and drugadministrationroute key value pares
			loadSafetyReportsSpecificatonProperities();			
			
		}catch (MalformedURLException e) {
			logger.error("", e);			 
		}
		this.getToolResource().setResourceDescription(AERS_DESCRIPTION);
	}

	@Override
	public void updateResourceInformation() {
		// TODO See if it can be implemented for this resource.
	}

	@Override
	public int updateResourceContent() {		
		return updateAllElements();
	}
	/**
	 * Update all the elements to database	 
	 * @return
	 */
	public int updateAllElements(){		
		logger.info("Updating " + this.getToolResource().getResourceName() + " elements...");
		int nbElement = 0;
		try {			
			//Extracts all data Files
			for(int i=0;i<aersZip.length;i++){				
				nbElement += extractZipFile( AERS_URL_ZIP + aersZip[i], aersExtract[i]);
			}
		} catch (FileNotFoundException e) {
			logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e); 
		} catch (BadElementStructureException e) {
			logger.error("** PROBLEM ** Cannot update " + this.getToolResource().getResourceName() +" because of a Structure problem.", e);
		}
		return nbElement;
	}
	/**
	 * Extracting zip files to local path using UnzipUtils.java class.	  
	 * Removing file from local path after finish the data parsing.
	 * @param unzipUtils
	 * @param fileName
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws FileNotFoundException 
	 */
	
	private int extractZipFile(String fileName, String outFileName) throws FileNotFoundException,BadElementStructureException{
		// Download and extracts zip file
		int nbElement = 0;		
		try {
			
			//Extracting ZIP file
			UnzipUtils.unzip(fileName, outFileName);
			//Parsing the file using Bio parser
			nbElement=parseXmlFile(UnzipUtils.getDataFile());
			
		} catch(FileNotFoundException ex){
			throw ex;
		} catch(BadElementStructureException ex1){
			throw ex1;
		} finally{
//			UnzipUtils.deleteDir(UnzipUtils.getDataFile());			
		}
		return nbElement;
	}
	/**
	 * Parse XML files using DOM parser
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 * @throws BadElementStructureException
	 */
	private int parseXmlFile(File file)throws	FileNotFoundException, BadElementStructureException{
		int nbElement = 0;
		AersDataElement aersElement;
		Element element;
		//parse using builder to get DOM representation of the XML file
		String subDir=SUB_G_DIR;
		String[] children = new File(file.getName()+SLASH_STRING+subDir).list();
		if(children==null){
			subDir=SUB_Q_DIR;
			children = new File(file.getName()+SLASH_STRING+subDir).list();
		}
		
		Document dom=null;
		//parsing all the files from the directory
		for(String fileName:children){
			//check file extension .SGM and not equals to ADS_TEST.SGM
			if(fileName.endsWith(FILE_EXTENSION_) && ! fileName.equals(TEST_FILE)){
				aersElement=null;
				element=null;
				//Add file path with main directory, '/', sgml, '/' and file name
				String filePath=file.getName()+SLASH_STRING+subDir+SLASH_STRING+fileName;
				
				//Replacing file content which contains '&' instead of '&amp;' 
				logger.info("Replacing the '&' instead of '&amp;' from "+filePath +" file...");
				replaceAmp(file.getName()+SLASH_STRING+subDir+SLASH_STRING,fileName);
				
				//Parsing the file
				logger.info("Parsing "+filePath +" file...");
				dom = AbstractXmlResourceAccessTool.parseXML(filePath);
				
				//get the root element
				org.w3c.dom.Element domRoot = dom.getDocumentElement();
				//get the node list of 'safetyreport' XML elements
				NodeList experimentList = domRoot.getElementsByTagName(AERS_SAFETYREPORT);
				if(experimentList != null && experimentList.getLength() > 0) {
					int listSize = experimentList.getLength();
					logger.info("Total number of elements on " + this.getToolResource().getResourceName() + ": " + listSize);
					// for each 'safetyreport' XML element
					for(int i = 0 ; i <listSize; i++) {
						aersElement = new AersDataElement((org.w3c.dom.Element)experimentList.item(i), this);
						element = aersElement.getElement();
						if (resourceUpdateService.addElement(element)){
							nbElement ++;
						}
					}
				}				
			} 
		}
		return nbElement;
	}	
	/**
	 * Replacing file content which contains '&' instead of '&amp;' 
	 * @param filePath
	 * @param fileName
	 * @throws FileNotFoundException
	 * @throws BadElementStructureException
	 */
	private void replaceAmp(String filePath, String fileName)throws	FileNotFoundException, BadElementStructureException{
		
		try{
		    // Open the file that is the first 
		    // command line parameter			
		    FileInputStream fstream = new FileInputStream(filePath+fileName);		    
		    // Get the object of DataInputStream
		    DataInputStream in = new DataInputStream(fstream);
		        BufferedReader br = new BufferedReader(new InputStreamReader(in));
		    String tempFile=filePath+"temp"+FILE_EXTENSION_;
	        BufferedWriter bWriter=new BufferedWriter(new FileWriter(tempFile));
		        
		    String strLine;		    
		    //Read File Line By Line
		    while ((strLine = br.readLine()) != null)   {
		      // Print the content on the console
		    	if(strLine.contains("&")){
			    	strLine=strLine.replace("&", "&amp;");
		    	}
		    	bWriter.append(strLine);
		    	bWriter.append("\n");
		    }
		    
		    //Close the input stream
		    in.close();
		    br.close();
		    bWriter.close();
		    
		    //Deletes original file
		    if(new File(filePath+fileName).exists()){
		    	new File(filePath+fileName).delete();		    	
		    }	
		 
		    // File (or directory) to be moved
		    File file = new File(tempFile);
		    
		    // Move file to new directory
		    file.renameTo(new File(new File(filePath), fileName));
		    
		    }catch (Exception e){//Catch exception if any
		      logger.error("Error replacing file content '&' to '&amp;' : " + e.getMessage());
		    }		
	}
	
	@Override
	public String elementURLString(String elementLocalID) {
		return AERS_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return AERS_MAIN_ITEMKEY;
	}

	@Override
	/**
	 * This function don't use ArrayExpressElement for optimization reasons.
	 */
	public HashSet<String> queryOnlineResource(String query) {
		HashSet<String> answerIDs = new HashSet<String>();

		// do not execute queryOnline for phrase with space
		String regexp = "\\S+\\s.+";
		if (!query.matches(regexp)){
			String accnum = EMPTY_STRING;
			//parse using builder to get DOM representation of the XML file done with the query
			Document dom = AbstractXmlResourceAccessTool.parseXML(query);
			//get the root element
			org.w3c.dom.Element domRoot = dom.getDocumentElement();
			//get a nodelist of 'experiment' XML elements
			NodeList experimentList = domRoot.getElementsByTagName(AERS_SAFETYREPORT);
			org.w3c.dom.Element experimentElt;

			if(experimentList != null && experimentList.getLength() > 0) {
				int listSize1 = experimentList.getLength();
				// for each 'experiment' XML element
				for(int i = 0 ; i <listSize1; i++) {
					experimentElt = (org.w3c.dom.Element)experimentList.item(i);
					accnum = experimentElt.getAttribute(AERS_SAFETY_ID_);
					answerIDs.add(accnum);
				}
			}
		}
		return answerIDs;
	}

	/**
	 * AersDataElement is responsible for getting data elements from parsed XML files
	 * @author palanisamy
	 *
	 */
	private class AersDataElement {

		AersDataAccessTool eltAETool;
		HashMap<String,String> eltInfo;		
		final String REAC = Structure.generateContextName(AERS_RESOURCEID, AERS_ITEMKEYS[0]);
		final String DRUG_CHAR = Structure.generateContextName(AERS_RESOURCEID, AERS_ITEMKEYS[1]);
		final String DRUG_NAMES = Structure.generateContextName(AERS_RESOURCEID, AERS_ITEMKEYS[2]);
		final String DRUG_ADMIN_ROUTE = Structure.generateContextName(AERS_RESOURCEID, AERS_ITEMKEYS[3]);
		final String DRUG_INDICATION = Structure.generateContextName(AERS_RESOURCEID, AERS_ITEMKEYS[4]);
				
		AersDataElement(org.w3c.dom.Element experimentElt, AersDataAccessTool aeTool){
			this.eltAETool = aeTool;
			this.eltInfo = new HashMap<String, String>(6);
			String isrNo=EMPTY_STRING;
			String reactionMedDrapt=EMPTY_STRING;
			String drugCharacterization=EMPTY_STRING;
			String medicinalProduct=EMPTY_STRING;
			String drugAdministrationRoute=EMPTY_STRING;
			String drugIndication=EMPTY_STRING;
			NodeList nodeList=experimentElt.getChildNodes();					
			
			for(int i=0;i<nodeList.getLength();i++){				
				if(nodeList.item(i).getNodeName().equals(AERS_SAFETY_ID_)){
					isrNo=nodeList.item(i).getTextContent().split(HYPEN_STRING)[0];
				} else if(nodeList.item(i).getNodeName().equals(AERS_PATIENT_)){
					NodeList patientList=nodeList.item(i).getChildNodes();					
					for(int j=0;j<patientList.getLength();j++){
						if(patientList.item(j).getNodeName().equals(AERS_REACTION_)){							
							NodeList reactionList=patientList.item(j).getChildNodes();
							for(int k=0;k<reactionList.getLength();k++){
								if(reactionList.item(k).getNodeName().equals(AERS_REACTION_DES_DRAFT_)){
									reactionMedDrapt+=reactionList.item(k).getTextContent()+COMMA_SEPARATOR;
								}//if
							}//for
						} else if(patientList.item(j).getNodeName().equals(AERS_DRUG_)){
							NodeList drugList=patientList.item(j).getChildNodes();								
							for(int k=0;k<drugList.getLength();k++){
								if(drugList.item(k).getNodeName().equals(AERS_DRUG_CHAR_)){
									drugCharacterization+=safetyReportsSpecification.getProperty(SPEC_CHAR_+drugList.item(k).getTextContent().trim()).toString().trim()+COMMA_SEPARATOR;
								} else if(drugList.item(k).getNodeName().equals(AERS_DRUG_NAME_)){
									medicinalProduct+=drugList.item(k).getTextContent()+COMMA_SEPARATOR;
								} else if(drugList.item(k).getNodeName().equals(AERS_DRUG_ADMIN_)){
									drugAdministrationRoute+=safetyReportsSpecification.getProperty(SPEC_ADMIN_+drugList.item(k).getTextContent().trim()).toString().trim()+COMMA_SEPARATOR;
								} else if(drugList.item(k).getNodeName().equals(AERS_DRUG_INDI_)){
									drugIndication+=drugList.item(k).getTextContent()+COMMA_SEPARATOR;
								}
							}
						}
					}					
				}
			}
			this.eltInfo.put(AERS_SAFETY_ID_, isrNo.substring(0,isrNo.length()));
			
			if(reactionMedDrapt.length()>2){
				this.eltInfo.put(REAC, reactionMedDrapt.substring(0,reactionMedDrapt.length()-2));
			}else{
				this.eltInfo.put(REAC, reactionMedDrapt.substring(0,reactionMedDrapt.length()));
			}
			if(drugCharacterization.length()>2){
				this.eltInfo.put(DRUG_CHAR, drugCharacterization.substring(0,drugCharacterization.length()-2));
			}else{
				this.eltInfo.put(DRUG_CHAR, drugCharacterization.substring(0,drugCharacterization.length()));
			}	
			if(medicinalProduct.length()>2){
				this.eltInfo.put(DRUG_NAMES, medicinalProduct.substring(0,medicinalProduct.length()-2));
			}else{
				this.eltInfo.put(DRUG_NAMES, medicinalProduct.substring(0,medicinalProduct.length()));
			}
			if(drugAdministrationRoute.length()>2){
				this.eltInfo.put(DRUG_ADMIN_ROUTE, drugAdministrationRoute.substring(0,drugAdministrationRoute.length()-2));
			}else{
				this.eltInfo.put(DRUG_ADMIN_ROUTE, drugAdministrationRoute.substring(0,drugAdministrationRoute.length()));
			}
			if(drugIndication.length()>2){
				this.eltInfo.put(DRUG_INDICATION, drugIndication.substring(0,drugIndication.length()-2));
			}else{
				this.eltInfo.put(DRUG_INDICATION, drugIndication.substring(0,drugIndication.length()));
			}		
		}
		
		/**
		 * Gets all the elements with structure of context names
		 * @return elements
		 */
		Element getElement(){
			Element element = null;
			ArrayList<String> contextNames = this.eltAETool.getToolResource().getResourceStructure().getContextNames(); 
			Structure eltStructure = new Structure(contextNames);

			for(String contextName: contextNames){
				eltStructure.putContext(contextName, this.eltInfo.get(contextName));
			}//for
			try {
				element = new Element(this.eltInfo.get(AERS_SAFETY_ID_), eltStructure);
			} catch (BadElementStructureException e) {			 
				logger.error("** PROBLEM ** Cannot create Element for AersDataElement with accnum: " + this.eltInfo.get(AERS_SAFETY_ID_) + ". Null have been returned.", e);
			}
			return element;
		}
	}
	
	/**
	 * This method is used to load the SafetyReportsSpecification.properties file
	 */
	public static void loadSafetyReportsSpecificatonProperities(){
		try{			
			safetyReportsSpecification=new Properties();
			//Initialize the InputStream
			//InputStream inputStream = ClassLoader.getSystemResourceAsStream("org/ncbo/stanford/obr/resource/aers/SafetyReportsSpecification.properties");
			InputStream inputStream = AersDataAccessTool.class.getResourceAsStream("/org/ncbo/stanford/obr/resource/aers/SafetyReportsSpecification.properties");
			//InputStream inputStream = AersDataAccessTool.class.getResourceAsStream("/SafetyReportsSpecification.properties");
			safetyReportsSpecification.load(inputStream);
		}catch(Exception ex){	
			logger.error("Problem in loading safety reports specification properties", ex);
		}
	}
}
