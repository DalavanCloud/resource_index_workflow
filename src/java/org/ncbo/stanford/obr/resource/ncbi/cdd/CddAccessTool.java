package org.ncbo.stanford.obr.resource.ncbi.cdd;

import gov.nih.nlm.ncbi.www.soap.eutils.esummary.DocSumType;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ESummaryRequest;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ESummaryResult;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ItemType;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;

import org.ncbo.stanford.obr.resource.ncbi.AbstractNcbiResourceAccessTool;

import obs.obr.populate.Element;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

/**
 * 
 * AccessTool for the resource CDD @ NCBI
 * 
 * use eUtils
 * 
 * @author Adrien Coulet
 * @version OBS v_1
 * @date   04-Mar-09 
 *
 */
public class CddAccessTool extends AbstractNcbiResourceAccessTool{
	
	private static final String CDD_URL			= "http://www.ncbi.nlm.nih.gov/Structure/cdd/cdd.shtml";
	private static final String CDD_NAME 		= "Conserved Domain Database (CDD)";
	private static final String CDD_RESOURCEID 	= "CDD";
	private static final String CDD_DESCRIPTION = "The Conserved Domain Database (CDD) contains protein domain models imported from outside sources, such as Pfam and SMART, and curated at NCBI. CDD contains over 12,000 such models and is linked to other NCBI databases, including protein sequences, bibliographic citations, and taxonomy.";
	private static final String CDD_LOGO 		= "http://www.ncbi.nlm.nih.gov/Structure/cdd/cdd2.gif";
	private static final String CDD_ELT_URL 	= "http://www.ncbi.nlm.nih.gov/Structure/cdd/cddsrv.cgi?uid=";
		
	// two static attributs specific of eUtils
	private static final String CDD_EUTILS_DB   = "cdd";
	private static final String CDD_EUTILS_TERM = "all[filter]";
	
	// domain expert provide this following informations
	private static final String[] CDD_ITEMKEYS = {UID_COLUMN,					"title",							"abstract"}; // see http://www.ncbi.nlm.nih.gov/entrez/query/static/docsum_fields.html#cdd
	private static final Double[] CDD_WEIGHTS  = {0.0, 							1.0, 								0.6};
	// OntoID associated for reported annotations
	private static final String[] CDD_ONTOIDS  = {Structure.NOT_FOR_ANNOTATION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION};
	
	private static Structure CDD_STRUCTURE = new Structure(CDD_ITEMKEYS, CDD_RESOURCEID, CDD_WEIGHTS, CDD_ONTOIDS);
	private static String CDD_MAIN_ITEMKEY = "title";
	
	public CddAccessTool(){
		super(CDD_NAME, CDD_RESOURCEID, CDD_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(CDD_URL));
			this.getToolResource().setResourceLogo(new URL(CDD_LOGO));
			this.getToolResource().setResourceElementURL(CDD_ELT_URL);
			this.getToolResource().setResourceDescription(CDD_DESCRIPTION);
		} catch (MalformedURLException e) {
			logger.error("", e);
		}		
	}
	
	@Override
	protected String getEutilsDB() {
		return CDD_EUTILS_DB;
	}
	
	@Override
	protected String getEutilsTerm() {
		return CDD_EUTILS_TERM;
	}
	
	@Override
	public void updateResourceInformation() {
		// TODO See if it can be implemented for this resource.
	}
	
	@Override
	public int updateResourceContent(){
		return super.eutilsUpdateAll(UID_COLUMN);
	}
	
	@Override
	protected int updateElementTableWithUIDs(HashSet<String> UIDs) throws BadElementStructureException{
		int nbElement = 0;
		ESummaryRequest esummaryRequest = new ESummaryRequest();
		esummaryRequest.setEmail(EUTILS_EMAIL);
		esummaryRequest.setTool(EUTILS_TOOL);
		esummaryRequest.setDb(this.getEutilsDB());

		ESummaryResult esummaryResult;
		StringBuffer UIDlist;
		DocSumType[] resultDocSums;
		ItemType[] docSumItems;
		ArrayList<String> contextNames = this.getToolResource().getResourceStructure().getContextNames(); 
		Element element;
		Structure eltStructure = new Structure(contextNames);

		String[] UIDsTab = new String[UIDs.size()];
		UIDsTab = UIDs.toArray(UIDsTab);
		int max;
		
		for(int step=0; step<UIDsTab.length; step+=EUTILS_MAX){
			max = step+EUTILS_MAX; 
			UIDlist = new StringBuffer();
			if(max>UIDsTab.length) {max = UIDsTab.length;}
			for(int u=step; u<max; u++){
				UIDlist.append(UIDsTab[u]);
				if(u<max-1) {UIDlist.append(COMMA_STRING);}
			}
			esummaryRequest.setId(UIDlist.toString());
			try {
				esummaryResult = this.getToolEutils().run_eSummary(esummaryRequest);
				resultDocSums = esummaryResult.getDocSum();
				for(int i=0; i<resultDocSums.length; i++){
					docSumItems = resultDocSums[i].getItem();
					
					// resultDocSums[i].getID contains the UID
					eltStructure.putContext(Structure.generateContextName(CDD_RESOURCEID, CDD_ITEMKEYS[0]), resultDocSums[i].getId());
					//logger.info("result UID: " + resultDocSums[i].getId());
					// localElementID
					//logger.info("get data of "+docSumItems[0].get_any()[0].toString()+"...");	
					
					// get "title"
					String titleString=EMPTY_STRING;
					org.apache.axis.message.MessageElement[] title = docSumItems[1].get_any();
					if(title != null){
						for (int k=0; k<title.length; k++){
							titleString+=title[k].toString();
							if (k!=(title.length-1)){
								titleString+=GT_SEPARATOR_STRING;
							}
						 }
						 eltStructure.putContext(Structure.generateContextName(CDD_RESOURCEID, CDD_ITEMKEYS[1]), titleString);
					}else{
						eltStructure.putContext(Structure.generateContextName(CDD_RESOURCEID, CDD_ITEMKEYS[1]), EMPTY_STRING);
					}	
					
					// get "abstract"
					String abstractString=EMPTY_STRING;
					org.apache.axis.message.MessageElement[] abstractMsg = docSumItems[2].get_any();
					if(abstractMsg != null){
						for (int k=0; k<abstractMsg.length; k++){
							abstractString+=abstractMsg[k].toString();
							if (k!=(abstractMsg.length-1)){
								abstractString+=EMPTY_STRING;
							}
						 }
						 eltStructure.putContext(Structure.generateContextName(CDD_RESOURCEID, CDD_ITEMKEYS[2]), abstractString);
					}else{
						eltStructure.putContext(Structure.generateContextName(CDD_RESOURCEID, CDD_ITEMKEYS[2]), EMPTY_STRING);
					}
				
					// localElementID and structure into a new element
					element = new Element(docSumItems[0].get_any()[0].toString(), eltStructure);
					
					if (resourceUpdateService.addElement(element)){
							nbElement ++;
					}
				}
			} catch (RemoteException e) {
				logger.error("** PROBLEM ** Cannot get information using ESummary.", e);
			}
		}
		return nbElement;
	}
		
	@Override
	public String elementURLString(String elementLocalID) {
		return CDD_ELT_URL + elementLocalID;
	}
	
	@Override
	public String mainContextDescriptor() {
		return CDD_MAIN_ITEMKEY;
	}	
}