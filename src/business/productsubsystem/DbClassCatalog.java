package business.productsubsystem;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;
import java.util.logging.Logger;

import middleware.DbConfigProperties;
import middleware.dataaccess.DataAccessSubsystemFacade;
import middleware.exceptions.DatabaseException;
import middleware.externalinterfaces.DataAccessSubsystem;
import middleware.externalinterfaces.DbClass;
import middleware.externalinterfaces.DbConfigKey;
import business.externalinterfaces.Catalog;
import business.externalinterfaces.DbClassCatalogTypesForTest;

/**
 * This class is concerned with managing data for a single
 * catalog. To read or update the entire list of catalogs in
 * the database, see DbClassCatalogs
 *
 */
public class DbClassCatalog implements DbClass {
	enum Type {INSERT, DELETE};
	@SuppressWarnings("unused")
	private static final Logger LOG = 
		Logger.getLogger(DbClassCatalog.class.getPackage().getName());
	private DataAccessSubsystem dataAccessSS = 
    	new DataAccessSubsystemFacade();
	
	private Type queryType;
	private List<Catalog> catalogList;
	
	private String insertQuery = "INSERT into CatalogType (catalogname) VALUES(?)";
	private String deleteQuery = "DELETE FROM CatalogType WHERE catalogname = ?";
	private Object[] insertParams, deleteParams;
	private int[] insertTypes, deleteTypes;
    
    public int saveNewCatalog(String catalogName) throws DatabaseException {
    	queryType = Type.INSERT;
    	insertParams = new Object[]{catalogName};
    	insertTypes = new int[]{Types.VARCHAR};
    	return dataAccessSS.insertWithinTransaction(this);  	
    }
    
    public int deleteCatalog(String catalogName) throws DatabaseException {
    	queryType = Type.DELETE;
    	deleteParams = new Object[]{catalogName};
    	deleteTypes = new int[]{Types.VARCHAR};
    	return dataAccessSS.insertWithinTransaction(this);  	
    }
    
    @Override
	public String getDbUrl() {
		DbConfigProperties props = new DbConfigProperties();	
    	return props.getProperty(DbConfigKey.PRODUCT_DB_URL.getVal());
	}
    
    @Override
	public String getQuery() {
		switch(queryType) {
			case INSERT:
				return insertQuery;
			case DELETE:
				return deleteQuery;
			default:
				return null;
		}
	}
    @Override
   	public Object[] getQueryParams() {
   		switch(queryType) {
   			case INSERT:
   				return insertParams;
   			case DELETE:
   				return deleteParams;
   			default:
   				return null;
   		}
    }		
	 @Override
	public int[] getParamTypes() {
		 switch(queryType) {
			case INSERT:
				return insertTypes;
			case DELETE:
				return deleteTypes;
			default:
				return null;
		}
	 }
    @Override
	public void populateEntity(ResultSet resultSet) throws DatabaseException {
		// do nothing
	}
	
    
}
