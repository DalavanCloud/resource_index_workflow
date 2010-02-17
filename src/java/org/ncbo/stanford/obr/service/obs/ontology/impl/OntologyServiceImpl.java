/**
 * 
 */
package org.ncbo.stanford.obr.service.obs.ontology.impl;

import org.ncbo.stanford.obr.dao.DaoFactory;
import org.ncbo.stanford.obr.service.obs.ontology.OntologyService;

/**
 * @author Kuladip Yadav
 *
 */
public class OntologyServiceImpl implements OntologyService, DaoFactory {

	/**
	 * private constructor to follow singleton pattern
	 * 
	 */
	private OntologyServiceImpl() {		
		 super();
	} 
	
	private static class OntologyServiceHolder {
		private final static OntologyServiceImpl ONTOLOGY_SERVICE_INSTANCE = new OntologyServiceImpl();
	}
	
	/**
	 * Returns a OntologyServiceImpl object by creating one if a singleton not already exists.
	 */
	public static OntologyServiceImpl getInstance(){
		return OntologyServiceHolder.ONTOLOGY_SERVICE_INSTANCE;
	}
	
	/**
	 * This method gets latest version of ontology for given virtual ontology id
	 * 
	 * @param virtualOntologyID 
	 * @return String of latest version of ontology.
	 */
	public String getLatestLocalOntologyID(String virtualOntologyID) {
		 
		return ontologyDao.getLatestLocalOntologyID(virtualOntologyID);
	}

}
