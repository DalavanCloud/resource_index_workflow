/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.stanford.obr.resource.nif.ctdchemgoenriched;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import obs.obr.populate.Element;
import obs.obr.populate.Structure;

import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.ncbo.stanford.obr.resource.nif.AbstractNifResourceAccessTool;

import au.com.bytecode.opencsv.CSVReader;

/**
 * AccessTool for CTD Pathways (via NIF).
 * @author r.malviya
 */
public class CTDCGEAccessTool extends AbstractNifResourceAccessTool {
	
	
	private static final String URL = "http://ctdbase.org/";
	private static final String NAME = "CTD Chem Go Enriched (via NIF)";
	private static final String RESOURCEID = "CTDCGE";
	private static final String DESCRIPTION = "A public database that enhances understanding about the effects of environmental chemicals on human health. "
			+ "In detail, it contains information about gene/protein-disease associations, chemical-disease associations, interactions between chemicals and genes/proteins, "
			+ "as well as the related pathways.";
	private static final String LOGO = "http://neurolex.org/w/images/b/bb/CTD.PNG";
	private static final String ELT_URL = "http://ctdbase.org/detail.go?type=pathway&acc=";
	private static final String[] ITEMKEYS = { "ChemicalName", "ChemicalID",
			"CasRN", "Ontology", "GOTermName", "HighestGOLevel", "PValue",
			"CorrectedPValue", "TargetMatchQty", "TargetTotalQty",
			"BackgroundMatchQty", "BackgroundTotalQty" };
	private static final Double[] WEIGHTS = { 1.0, 0.9, 0.8, 0.7, 0.6, 0.5,
			0.4, 0.3, 0.2, 0.1, 0.0, 0.0 };
	private static final String[] ONTOIDS = {
			Structure.FOR_CONCEPT_RECOGNITION,
			Structure.FOR_CONCEPT_RECOGNITION,
			Structure.FOR_CONCEPT_RECOGNITION,
			Structure.FOR_CONCEPT_RECOGNITION,
			Structure.FOR_CONCEPT_RECOGNITION,
			Structure.FOR_CONCEPT_RECOGNITION,
			Structure.FOR_CONCEPT_RECOGNITION,
			Structure.FOR_CONCEPT_RECOGNITION,
			Structure.FOR_CONCEPT_RECOGNITION,
			Structure.FOR_CONCEPT_RECOGNITION,
			Structure.FOR_CONCEPT_RECOGNITION,
			Structure.FOR_CONCEPT_RECOGNITION };
	private static Structure STRUCTURE = new Structure(ITEMKEYS, RESOURCEID,
			WEIGHTS, ONTOIDS);
	private static String MAIN_ITEMKEY = "Chemical";
	private Map<String, String> localOntologyIDMap;
	private static char seprator = '	';
	private double MAX_ROW = 5000;

    // constructors
    public CTDCGEAccessTool() {
        super(NAME, RESOURCEID, STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(URL));
            this.getToolResource().setResourceDescription(DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(LOGO));
            this.getToolResource().setResourceElementURL(ELT_URL);
        } catch (MalformedURLException e) {
            logger.error(EMPTY_STRING, e);
        }
        localOntologyIDMap = createLocalOntologyIDMap(STRUCTURE);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.BIG;
    }

    @Override
    public void updateResourceInformation() {
        // TODO 
        // can be used to update resource name, description, logo, elt_url.
    }

    @Override
    public HashSet<String> queryOnlineResource(String query) {
        // TODO 
        // not used for caArray 
        return new HashSet<String>();
    }

    @Override
    public String elementURLString(String elementLocalID) {
        return ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        return MAIN_ITEMKEY;
    }

    /**
     * This method creates map of latest version of ontology with contexts as key.
     * It uses virtual ontology ids associated with contexts. 
     * 
     * @param structure {@code Structure} for given resource
     * @return {@code HashMap} of latest local ontology id with context as key.
     */
    public HashMap<String, String> createLocalOntologyIDMap(Structure structure) {
        HashMap<String, String> localOntologyIDMap = new HashMap<String, String>();
        String virtualOntologyID;
        for (String contextName : structure.getOntoIds().keySet()) {
            virtualOntologyID = structure.getOntoIds().get(contextName);
            if (!virtualOntologyID.equals(Structure.FOR_CONCEPT_RECOGNITION)
                    && !virtualOntologyID.equals(Structure.NOT_FOR_ANNOTATION)) {
                localOntologyIDMap.put(contextName, ontlogyService.getLatestLocalOntologyID(virtualOntologyID));
            }
        }
        return localOntologyIDMap;
    }
    
    @Override
    public int updateResourceContent() {
        int nbElement = 0;
        this.getAllElements();
  
        return nbElement;
    }

    /** This method is used to get all elements from resource site.
     *  @return HashSet<Element>
     */
    @SuppressWarnings("resource")
	public void getAllElements() {
     
        try{
        	HashSet<String> allElementsInET = this.resourceUpdateService.getAllLocalElementIDs();
        	
        	Map<String, Map<String, String>> allRowsData = new HashMap<String, Map<String, String>>();
        	
        	URL csvFile=new URL("http://ctdbase.org/reports/CTD_chem_go_enriched.tsv.gz");
        	CSVReader csvReader=null;
        	
        	csvReader = new CSVReader(new BufferedReader(new InputStreamReader(new GZIPInputStream(csvFile.openStream()))), seprator);
        	
        	String[] headerRow;
        	
        	
        	
 
			while ((headerRow = csvReader.readNext()) != null) {
				if (headerRow[0].contains("Fields:")) {
					headerRow = csvReader.readNext();
					headerRow = csvReader.readNext();
					break;
				}
			}    	
				
				while(headerRow!=null){
					int rowCount=0;
        	while((headerRow=csvReader.readNext())!=null){
    			
    			for(int i=0;i<headerRow.length;i++){
    				String localElementId=EMPTY_STRING;
    				Map<String, String> elementAttributes = new HashMap<String, String>();
    				while(i<headerRow.length){
    				if (i==0)
    				     elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[0]), headerRow[i]);
    				else if(i==1)
    					elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[1]), headerRow[i]);           				     
    				else if(i==2)        					
    				     elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[2]), headerRow[i]);        				
    				else if(i==3)
   					     elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[3]), headerRow[i]);
    				else if(i==4)
   					     elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[4]), headerRow[i]);
    				else if(i==5)
    					localElementId =headerRow[i] ;   					     
    				else if(i==6)
    					elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[5]), headerRow[i]);
    				else if(i==7)
      					 elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[6]), headerRow[i]);
    				else if(i==8)
      					 elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[7]), headerRow[i]);
    				else if(i==9)
      					 elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[8]), headerRow[i]);
    				else if(i==10)
      					 elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[9]), headerRow[i]);
    				else if(i==11)
      					 elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[10]), headerRow[i]);
    				else if(i==12)
      					 elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[11]), headerRow[i]);
    				i++;
    				}
    				
    				if (allElementsInET.contains(localElementId)) {
                        continue;
                    } else {
                    	rowCount++;
                        allRowsData.put(localElementId, elementAttributes);
                    }
    				
                       
    			}
    			if(rowCount==MAX_ROW){
    				break;
    			}
    			
    	}//parsing ends

            // Second phase: creation of elements 
        	if(rowCount>1){
        		HashSet<Element> elementSet = new HashSet<Element>();
            for (String localElementID : allRowsData.keySet()) {
                Map<String, String> elementAttributes = new HashMap<String, String>();
                elementAttributes = allRowsData.get(localElementID);  
                
                // PUT DATA INTO A STRUCTURE++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                Structure elementStructure = new Structure(STRUCTURE.getContextNames());
                for (String contextName : STRUCTURE.getContextNames()) {
                    boolean attributeHasValue = false;

                    for (String att : elementAttributes.keySet()) {
                        if (contextName.equals(att)) {
                            // not an existing annotation
                            if (STRUCTURE.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)
                                    || STRUCTURE.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
                                elementStructure.putContext(contextName, elementAttributes.get(att));
                                attributeHasValue = true;

                            }
                        }
                    }

                    // to avoid null value in the structure
                    if (!attributeHasValue) {
                        elementStructure.putContext(contextName, EMPTY_STRING);
                    }
                }
                // put the element structure in a new element
                try {
                    Element exp = new Element(localElementID, elementStructure);
                    elementSet.add(exp);
                } catch (Element.BadElementStructureException e) {
                    logger.error(EMPTY_STRING, e);
                }
            }
            allRowsData.clear();
            Element myExp;
            int nbElement = 0;
            Iterator<Element> i = elementSet.iterator();
            while (i.hasNext()) {
            	rowCount++;
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_CTDCGE_ET table.", e);
                }
                
            }
            logger.info(nbElement + " elements added to the OBR_CTDCGE_ET table.");
        }
				}
         
        } catch (Exception e) {
            logger.error("** PROBLEM ** Problem in getting rows.", e);
        }
     
        
    }
}    