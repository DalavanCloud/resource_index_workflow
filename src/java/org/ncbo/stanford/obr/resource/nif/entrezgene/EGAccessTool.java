/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.stanford.obr.resource.nif.entrezgene;

import org.ncbo.stanford.obr.resource.nif.AbstractNifResourceAccessTool;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import obs.obr.populate.Element;
import obs.obr.populate.Structure;
import org.jsoup.Jsoup;
import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * AccessTool for EntrezGene (via NIF).
 * @author s.kharat
 */
public class EGAccessTool extends AbstractNifResourceAccessTool {

    private static final String URL = "http://www.ncbi.nlm.nih.gov/gene";
    private static final String NAME = "Entrez Gene (via NIF)";
    private static final String RESOURCEID = "EG";
    private static final String DESCRIPTION = "EntrezGene (NCBI Gene) is a searchable database of genes, from RefSeq genomes, and defined by sequence "
            + "and/or located in the NCBI Map Viewer. The content of EntrezGene represents the result of curation and automated integration of data from "
            + "NCBI's Reference Sequence project (RefSeq), from collaborating model organism databases, and from many other databases available from NCBI.";
    private static final String LOGO = "http://neurolex.org/w/images/f/f9/Ncbi.png";
    private static final String ELT_URL = "http://www.ncbi.nlm.nih.gov/gene?term=";
    private static final String[] ITEMKEYS = {"Gene_Symbol", "Synonyms", "Description", "Organism", "Gene_ID", "Type"};
    private static final Double[] WEIGHTS = {1.0, 0.9, 0.9, 0.7, 0.0, 0.0};
    private static final String[] ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION,
        Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION};
    private static Structure STRUCTURE = new Structure(ITEMKEYS, RESOURCEID, WEIGHTS, ONTOIDS);
    private static String MAIN_ITEMKEY = "Gene_Symbol";
    // Constant 
    private static final String nifId = "nif-0000-02801-1";
    private static final String Gene_Symbol = "Gene Symbol";
    private static final String Synonyms = "Synonyms";
    private static final String Description = "Description";
    private static final String Organism = "Organism";
    private static final String Gene_ID = "Gene ID";
    private static final String Type = "Type";
    private Map<String, String> localOntologyIDMap;

    // constructors
    public EGAccessTool() {
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
        try {
            Element myExp;
            //Get all elements from resource site
            HashSet<Element> allElementList = this.getAllElements();
            logger.info("Number of new elements to dump: " + allElementList.size());

            // for each experiments accessed by the tool
            Iterator<Element> i = allElementList.iterator();
            while (i.hasNext()) {
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_EG_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_EG_ET table.");
        return nbElement;
    }

    /** This method is used to get all elements from resource site.
     *  @return HashSet<Element>
     */
    public HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for NIF Entrez Gene  ... ");
        HashSet<Element> elementSet = new HashSet<Element>();
        int nbAdded = 0;
        int offset = 0;
        int totalCount = 0;

        try {
            //get all elements from _ET table            
            HashSet<String> allElementsInET = this.resourceUpdateService.getAllLocalElementIDs();

            Map<String, Map<String, String>> allRowsData = new HashMap<String, Map<String, String>>();

            //parsing data
            do {
                Document dom = queryFederation(nifId, query, offset, rowCount);
                if (dom != null) {
                    Node tableData = dom.getFirstChild().getChildNodes().item(1);
                    //get total records
                    totalCount = Integer.parseInt(tableData.getAttributes().getNamedItem(resultCount).getNodeValue());
                    offset += rowCount;

                    Node results = tableData.getChildNodes().item(1);

                    // Iterate over the returned structure 
                    NodeList rows = results.getChildNodes();
                    for (int i = 0; i < rows.getLength(); i++) {
                        String localElementId = EMPTY_STRING;
                        Map<String, String> elementAttributes = new HashMap<String, String>();

                        Node row = rows.item(i);
                        for (int j = 0; j < row.getChildNodes().getLength(); j++) {
                            NodeList vals = row.getChildNodes().item(j).getChildNodes();
                            String name = null;
                            String value = null;

                            for (int k = 0; k < vals.getLength(); k++) {
                                if (nodeName.equals(vals.item(k).getNodeName())) {
                                    name = vals.item(k).getTextContent();
                                } else if (nodeValue.equals(vals.item(k).getNodeName())) {
                                    value = vals.item(k).getTextContent();
                                }
                            }

                            if (name.equalsIgnoreCase(Gene_Symbol)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[0]), value);
                            } else if (name.equalsIgnoreCase(Synonyms)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[1]), value);
                            } else if (name.equalsIgnoreCase(Description)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[2]), value);
                            } else if (name.equalsIgnoreCase(Organism)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[3]), value);
                            } else if (name.equalsIgnoreCase(Gene_ID)) {
                                localElementId = value.substring(value.indexOf(ELT_URL) + ELT_URL.length(), value.indexOf(endTag));
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[4]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(Type)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[5]), value);
                            }
                        }

                        //Check if elementId is present in database.
                        if (allElementsInET.contains(localElementId)) {
                            continue;
                        } else {
                            allRowsData.put(localElementId, elementAttributes);
                        }
                    }
                } else {
                    offset += rowCount;
                }
            } while (offset < totalCount);

            //parsing ends

            // Second phase: creation of elements           
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

        } catch (Exception e) {
            logger.error("** PROBLEM ** Problem in getting rows.", e);
        }
        nbAdded = elementSet.size();
        logger.info((nbAdded) + " rows found.");
        return elementSet;
    }
}
