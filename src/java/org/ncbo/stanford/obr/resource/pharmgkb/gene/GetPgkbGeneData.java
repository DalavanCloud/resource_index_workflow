package org.ncbo.stanford.obr.resource.pharmgkb.gene;

import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import obs.common.utils.StreamGobbler;
import obs.obr.populate.Element;
import obs.obr.populate.Resource;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.resource.pharmgkb.disease.GetPgkbDiseaseData;
import org.ncbo.stanford.obr.resource.pharmgkb.drug.GetPgkbDrugData;

public class GetPgkbGeneData {

	// Logger for this class
	private static Logger logger = Logger.getLogger(GetPgkbGeneData.class);

//	attributes
	private static String PERL_SCRIPT_PATH =new File(ClassLoader.getSystemResource("org/ncbo/stanford/obr/resource/pharmgkb/gene/genes.pl" ).getFile()).getAbsolutePath();
	private static String COMMAND                = "perl "  + PERL_SCRIPT_PATH;
	Hashtable<String, Hashtable<String, Hashtable<Integer, String>>> geneData        = new Hashtable<String, Hashtable<String, Hashtable<Integer, String>>>();	//<geneAccession, Hashtable of attribut-value couple>
	Hashtable<String, Hashtable<Integer, String>> geneAttribute   = new Hashtable<String, Hashtable<Integer, String>>();	//<attributName, value> (a value could be a map)
	Hashtable<Integer, String>   attributeValues = new Hashtable<Integer, String>();
	Structure basicStructure = null;
	String resourceID="";
	
	//constructor
	public GetPgkbGeneData(Resource myResource){	
		this.basicStructure = myResource.getResourceStructure();
		this.resourceID     = myResource.getResourceID();		 
	}
	public GetPgkbGeneData(){
		
	}
	
	// method
	public Element getGeneElement(String geneAccession) {

		Structure elementStructure = basicStructure;
		Element myGene = null;
		
		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		try {
			
			logger.info("get the data for "+geneAccession+"... ");
			
			if (geneAccession!=null){				
				process = runtime.exec(COMMAND+" "+geneAccession);
			}
			
			// error message and script output management
	        StreamGobbler errorGobbler  = new StreamGobbler(process.getErrorStream(), "ERROR");            
	        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT");	            
            
	        errorGobbler.start();
	        outputGobbler.start();
	        
            int exitValue = process.waitFor();
            logger.info("ExitValue: " + exitValue);        

	        HashMap<Integer, String> lines = StreamGobbler.lines;         	        

			try {
				geneData = new Hashtable<String, Hashtable<String, Hashtable<Integer, String>>>();				
				Integer attributeNumber = 0;
				Pattern setPattern  = Pattern.compile("^\\t(.*)$");
				Pattern dataPattern = Pattern.compile("^(.+):(.*)$");
				String attributeName = null;
				
				if(!lines.keySet().isEmpty()){
					for(int i=0; i<lines.keySet().size();i++) {
						String resultLine=lines.get(i);
	
						// process the line	
						Matcher setMatcher = setPattern.matcher(resultLine);		
						// line with an attribute name =====================
						if (!setMatcher.matches()){ 
							//new attribute	
							Matcher dataMatcher = dataPattern.matcher(resultLine);
							if(dataMatcher.matches()){
								//first we put in the hashtable last things							
								if (attributeName!=null && attributeValues!=null){
									geneAttribute.put(attributeName, attributeValues);
								}							
								// then initialization
								attributeName   = dataMatcher.group(1);
								attributeValues = new Hashtable<Integer, String>();
								
								if(!dataMatcher.group(2).equals("")){ // simple case in which we have atributeName: value on one line
									String value = null;
									value = dataMatcher.group(2).replaceFirst(" ", "");
									attributeValues.put(1, value);	
								}else{
									attributeNumber = 0;
								}							
							}
						// non header line => value ========================		
						}else{											
							if (attributeName!=null){
								attributeNumber++;
								String value = null;
								value = setMatcher.group(1);
								attributeValues.put(attributeNumber, value);
							}
						}
					}
				}
				if(attributeName!=null  && attributeValues!=null){
					geneAttribute.put(attributeName, attributeValues); //update of geneAttribute content				
					//update the geneData
					geneData.put(geneAccession, geneAttribute);//update of geneData content
				}else{
					logger.info("PROBLEM when getting data with the web service");
				}
				
				// PUT DATA INTO AN ELEMENT++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
				//System.out.println(geneData.get(geneAccession).toString());		
				// for each attribute
				String attInString = "";
				GetPgkbDrugData    myDrugExtractor    = new GetPgkbDrugData();
				GetPgkbDiseaseData myDiseaseExtractor = new GetPgkbDiseaseData();
				
				for (String contextName: elementStructure.getContextNames()){
					boolean attributeHasValue = false;
					for (String att : geneAttribute.keySet()){
						// TODO Adrien check ici
						if (contextName.equals(this.resourceID+"_"+att)){
							attributeHasValue = true;
							// transform repetitive element (hashtables) in a string with > as a separator.
							attInString = "";
							Hashtable<Integer, String> valueTable = geneAttribute.get(att);
							for (Integer valueNb :valueTable.keySet()){
								if (!attInString.equals("")){
									// specific case of gene => we want to store gene symbol and not the PharmGKB localElementID
									if(att.equals("geneRelatedDrugs")){
										attInString = attInString+"> "+myDrugExtractor.getDrugNameByDrugLocalID(valueTable.get(valueNb));
									}else if(att.equals("geneRelatedDiseases")){
										attInString = attInString+"> "+myDiseaseExtractor.getDiseaseNameByDiseaseLocalID(valueTable.get(valueNb));
									}else{
										attInString = attInString+"> "+valueTable.get(valueNb);
									}
								}else{
									if(att.equals("geneRelatedDrugs")){
										attInString=myDrugExtractor.getDrugNameByDrugLocalID(valueTable.get(valueNb));
									}else if(att.equals("geneRelatedDiseases")){
										attInString = myDiseaseExtractor.getDiseaseNameByDiseaseLocalID(valueTable.get(valueNb));
									}else{
										attInString=valueTable.get(valueNb);
									}									
								}				
							}
							elementStructure.putContext(contextName,attInString);
						}
					}
					// to avoid null value in the structure
					if (!attributeHasValue){
						elementStructure.putContext(contextName,"");
					}				
				}
				//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
				
			} finally {
			}
		}catch(Throwable t){            
            logger.error("Problem in processing element", t);
        }
		try{
			myGene = new Element(geneAccession, elementStructure);
		}catch(BadElementStructureException e){
			logger.error("", e);
		}
		return myGene;
	}
	public String getGeneSymbolByGenePgkbLocalID(String genePgkbLocalID) {
		String geneSymbol = "";
		
		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		try {
			//geneAccession="PA447230";
			if (genePgkbLocalID!=null){				
				process = runtime.exec(COMMAND+" "+genePgkbLocalID);
				//InputStream results = process.getInputStream();
			}


			// error message and script output management
	        StreamGobbler errorGobbler  = new StreamGobbler(process.getErrorStream(), "ERROR");            
	        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT");	            
            
	        errorGobbler.start();
	        outputGobbler.start();
	        
            int exitValue = process.waitFor();
            System.out.println("ExitValue: " + exitValue);        

	        HashMap<Integer, String> lines = StreamGobbler.lines;         	        

			try {
				geneSymbol = ""; 				
				Pattern dataPattern  = Pattern.compile("^geneSymbol: (.*)$");
				//String attributeName = null;
				
				if(!lines.keySet().isEmpty()){
					for(int i=0; i<lines.keySet().size();i++) {
						String resultLine=lines.get(i);
						// process the line	
						Matcher dataMatcher = dataPattern.matcher(resultLine);		
						// line with the geneSymbol ===========================
						if (dataMatcher.matches()){ 
								geneSymbol   = dataMatcher.group(1);	
								//System.out.println(genePgkbLocalID+" => "+geneSymbol);
						}
					}
				}
			}finally{
			}

		}catch(Throwable t){
			logger.error("Problem in getting gene symbol for genePgkbLocalID : " + genePgkbLocalID, t);            
        }
		return geneSymbol;
	}
}

