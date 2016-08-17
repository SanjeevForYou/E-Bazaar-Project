package business.customersubsystem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Logger;

import middleware.DbConfigProperties;
import middleware.dataaccess.DataAccessSubsystemFacade;
import middleware.exceptions.DatabaseException;
import middleware.externalinterfaces.DataAccessSubsystem;
import middleware.externalinterfaces.DbClass;
import middleware.externalinterfaces.DbConfigKey;
//Bandeshor 7/8/2016
public class DbClassCreditCard implements DbClass{
	enum Type {READCARD};
	@SuppressWarnings("unused")
	private static final Logger LOG = 
		Logger.getLogger(DbClassCreditCard.class.getPackage().getName());
	private DataAccessSubsystem dataAccessSS = 
    	new DataAccessSubsystemFacade();
    
    private Type queryType;
    
    private String readCardQuery 
        = "SELECT nameoncard,expdate,cardtype,cardnum FROM Customer WHERE custid = ?";
    private Object[] readCardParams;
    private int[] readCardTypes;
    
    /** Used for reading in values from the database */
    private CreditCardImpl creditCard;
      
    public CreditCardImpl readDefaultCreditCard(Integer custId) throws DatabaseException {
        queryType = Type.READCARD;
        readCardParams = new Object[]{custId};
        readCardTypes = new int[]{Types.INTEGER};
        dataAccessSS.atomicRead(this); 
        return creditCard;
    }
 
    public void populateEntity(ResultSet resultSet) throws DatabaseException {
        try {   
            //we take the first returned row
            if(resultSet.next()){
            	creditCard = new CreditCardImpl(resultSet.getString("nameoncard"),
            			resultSet.getString("expdate"),
            			resultSet.getString("cardnum"),
            			resultSet.getString("cardtype"));
                
            }
        }
        catch(SQLException e){
            throw new DatabaseException(e);
        }
    }

    public String getDbUrl() {
    	DbConfigProperties props = new DbConfigProperties();	
    	return props.getProperty(DbConfigKey.ACCOUNT_DB_URL.getVal());
    }

    @Override
    public String getQuery() {
    	switch(queryType) {
	    	case READCARD :
	    		return readCardQuery;
	    	default :
	    		return null;
    	}
    }

	@Override
	public Object[] getQueryParams() {
		switch(queryType) {
	    	case READCARD :
	    		return readCardParams;
	    	default :
	    		return null;
		}
	}
	@Override
	public int[] getParamTypes() {
		switch(queryType) {
	    	case READCARD :
	    		return readCardTypes;
	    	default :
	    		return null;
		}
	}

}
