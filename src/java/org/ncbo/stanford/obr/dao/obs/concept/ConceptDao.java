package org.ncbo.stanford.obr.dao.obs.concept;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.ncbo.stanford.obr.dao.obs.AbstractObsDao;
import org.ncbo.stanford.obr.dao.obs.ontology.OntologyDao;
import org.ncbo.stanford.obr.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
/**
 * This class is a representation for the OBS(slave) DB obs_concept table. The table contains 
 * the following columns:
 * <ul>
 * <li>id INT(11) NOT NULL PRIMARY KEY
   <li>local_concept_id VARCHAR(246) NOT NULL UNIQUE
   <li>ontology_id INT(11) NOT NULL
   <li>is_toplevel TINY NOT NULL
 * </ul>
 * 
 */
public class ConceptDao extends AbstractObsDao {
	
	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obs.concept.table.suffix");
		
	private PreparedStatement addEntryStatement;
	private static PreparedStatement deleteEntriesFromOntologyStatement;
	
	private ConceptDao() {
		super(TABLE_SUFFIX);

	}
	public static String name(String resourceID){		
		return OBS_PREFIX + TABLE_SUFFIX;
	}
	
	private static class ConceptDaoHolder {
		private final static ConceptDao CONCEPT_DAO_INSTANCE = new ConceptDao();
	}

	/**
	 * Returns a ConceptTable object by creating one if a singleton not already exists.
	 */
	public static ConceptDao getInstance(){
		return ConceptDaoHolder.CONCEPT_DAO_INSTANCE;
	}
	
	@Override
	protected String creationQuery() {
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
		"id INT(11) NOT NULL PRIMARY KEY, " +		
		"local_concept_id VARCHAR(246) NOT NULL UNIQUE, " +
		"ontology_id INT(11) NOT NULL, " +
		"is_toplevel BOOL NOT NULL, " +
		"FOREIGN KEY (ontology_id) REFERENCES " + ontologyDao.getTableSQLName() + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
		"INDEX X_" + this.getTableSQLName() +"_isTopLevel (is_toplevel)" +
	");";
}
	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();	
		this.openDeleteEntriesFromOntologyStatement();
	}
	
	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		this.addEntryStatement.close();
		deleteEntriesFromOntologyStatement.close();
	}
	
	@Override
	protected void openAddEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (id, local_concept_id, ontology_id, is_toplevel) VALUES (?,?,?,?);");
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
		
	public boolean addEntry(ConceptEntry entry){
		boolean inserted = false;
		try {
			addEntryStatement.setInt(1, entry.getId());
			addEntryStatement.setString(2, entry.getLocalConceptID());
			addEntryStatement.setInt(3, entry.getOntologyID());
			addEntryStatement.setBoolean(4, entry.isTopLevel());
			this.executeSQLUpdate(addEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddEntryStatement();
			return this.addEntry(entry);
		}
		catch (MySQLIntegrityConstraintViolationException e){
			//logger.error("Table " + this.getTableSQLName() + " already contains an entry for the concept: " + entry.getLocalConceptID() +".");
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot add an entry on table " + this.getTableSQLName(), e);
			logger.error(entry.toString());
		}
		return inserted;	
	} 	 
	
	/**
	 * Method loads the data entries from given file to concept table
	 * 
	 * @param conceptEntryFile File containing concept table entries.
	 * @return Number of entries populated in concept table.
	 */
	public int populateSlaveConceptTableFromFile(File conceptEntryFile) {
		int nbInserted =0 ;
		
		StringBuffer queryb = new StringBuffer();
		queryb.append("LOAD DATA INFILE '");
		queryb.append(conceptEntryFile.getAbsolutePath());
		queryb.append("' IGNORE INTO TABLE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FIELDS TERMINATED BY '\t' IGNORE 1 LINES"); 
		try{
			 nbInserted = this.executeSQLUpdate(queryb.toString());			
		} catch (SQLException e) {			 
			logger.error("Problem in populating concept table from file : " + conceptEntryFile.getAbsolutePath(), e);
		} 	
		return nbInserted;
	}
	/**
	 * 
	 */
	private void openDeleteEntriesFromOntologyStatement(){
		/*DELETE obs_concept FROM obs_concept
		  	WHERE obs_concept.ontology_id = select obs_ontology.id FROM obs_ontology 
		  		WHERE obs_ontology.local_ontology_id = ?; */
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" WHERE ");
		queryb.append(this.getTableSQLName());		
		queryb.append(".ontology_id =(" );
		queryb.append(" SELECT id FROM ");
		queryb.append(OntologyDao.name(""));
		queryb.append(" WHERE ");
		queryb.append("local_ontology_id=?)");		
		deleteEntriesFromOntologyStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * Deletes the rows for the given local_ontology_id.
	 * @return True if the rows were successfully removed. 
	 */
	public boolean deleteEntriesFromOntology(String localOntologyID){
		boolean deleted = false;
		try{
			deleteEntriesFromOntologyStatement.setString(1, localOntologyID);
			executeSQLUpdate(deleteEntriesFromOntologyStatement);
			deleted = true;
		}		
		catch (MySQLNonTransientConnectionException e) {
			this.openDeleteEntriesFromOntologyStatement();
			return this.deleteEntriesFromOntology(localOntologyID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete entries from "+this.getTableSQLName()+" for local_ontology_id: "+ localOntologyID+". False returned.", e);
		}
		return deleted;
	}
	public static class ConceptEntry{
		
		private int id;
		private String localConceptID;
		private int ontologyID;
		private boolean isTopLevel;
				
		public ConceptEntry(int id, String localConceptID, int ontologyID,
				boolean isToplevel) {
			this.id = id;
			this.localConceptID = localConceptID;
			this.ontologyID = ontologyID;
			this.isTopLevel = isToplevel;
		}
		
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getLocalConceptID() {
			return localConceptID;
		}
		public void setLocalConceptID(String localConceptID) {
			this.localConceptID = localConceptID;
		}
		public int getOntologyID() {
			return ontologyID;
		}
		public void setOntologyID(int ontologyID) {
			this.ontologyID = ontologyID;
		}
		
		public boolean isTopLevel() {
			return isTopLevel;
		}
		public void setTopLevel(boolean isTopLevel) {
			this.isTopLevel = isTopLevel;
		}
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("ConceptEntry: [");
			sb.append(this.localConceptID + ",");
			sb.append(this.localConceptID);
			sb.append(", ");
			sb.append(this.ontologyID);
			sb.append(", ");
			sb.append(this.isTopLevel);
			sb.append("]");
			return sb.toString();
		}		
	}	 
}
