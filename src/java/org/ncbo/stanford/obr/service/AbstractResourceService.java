package org.ncbo.stanford.obr.service;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.dao.DaoFactory;
import org.ncbo.stanford.obr.dao.aggregation.AggregationDao;
import org.ncbo.stanford.obr.dao.aggregation.ConceptFrequencyDao;
import org.ncbo.stanford.obr.dao.annotation.DirectAnnotationDao;
import org.ncbo.stanford.obr.dao.annotation.expanded.IsaExpandedAnnotationDao;
import org.ncbo.stanford.obr.dao.annotation.expanded.MapExpandedAnnotationDao;
import org.ncbo.stanford.obr.dao.element.ElementDao;
import org.ncbo.stanford.obr.resource.ResourceAccessTool;
import org.ncbo.stanford.obr.util.helper.StringHelper;

public abstract class AbstractResourceService implements DaoFactory, StringHelper{	
	
	// Logger for AbstractResourceService 
	protected static Logger logger = Logger.getLogger(AbstractResourceService.class);
	
	protected static ResourceAccessTool resourceAccessTool;	
	
	protected static ElementDao elementTableDao;
	protected static DirectAnnotationDao directAnnotationTableDao;
	protected static IsaExpandedAnnotationDao isaExpandedAnnotationTableDao;
	protected static MapExpandedAnnotationDao mapExpandedAnnotationTableDao;
	protected static AggregationDao aggregationTableDao;
	protected static ConceptFrequencyDao conceptFrequencyDao;
	  
	public AbstractResourceService(ResourceAccessTool resourceAccessTool) {
		super();
		
		if(AbstractResourceService.resourceAccessTool== null 
				|| AbstractResourceService.resourceAccessTool.getToolResource().getResourceId()!=resourceAccessTool.getToolResource().getResourceId()){
			 AbstractResourceService.resourceAccessTool = resourceAccessTool;
				
			// Creating Element Table Dao for given resource access tool
			 elementTableDao= new ElementDao(resourceAccessTool.getToolResource().getResourceId() 
					, resourceAccessTool.getToolResource().getResourceStructure()) ;
					 
			 directAnnotationTableDao= new DirectAnnotationDao(resourceAccessTool.getToolResource().getResourceId()) ;
			
			 isaExpandedAnnotationTableDao= new IsaExpandedAnnotationDao(resourceAccessTool.getToolResource().getResourceId());
			 
			 mapExpandedAnnotationTableDao= new MapExpandedAnnotationDao(resourceAccessTool.getToolResource().getResourceId());
			
			 aggregationTableDao = new AggregationDao(resourceAccessTool.getToolResource().getResourceId());
			 
			 conceptFrequencyDao = new ConceptFrequencyDao(resourceAccessTool.getToolResource().getResourceId());			
		}
		
		
	}
	
	
	

}
