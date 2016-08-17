
package business.ordersubsystem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import business.externalinterfaces.*;
import business.util.Convert;
import middleware.DbConfigProperties;
import middleware.dataaccess.DataAccessSubsystemFacade;
import middleware.exceptions.DatabaseException;
import middleware.externalinterfaces.DataAccessSubsystem;
import middleware.externalinterfaces.DbClass;
import middleware.externalinterfaces.DbConfigKey;


class DbClassOrder implements DbClass, DbClassOrderForTest {
	enum Type {GET_ORDER_ITEMS, GET_ORDER_IDS, GET_ORDER_DATA, SUBMIT_ORDER, SUBMIT_ORDER_ITEM};
	
	private static final Logger LOG = 
		Logger.getLogger(DbClassOrder.class.getPackage().getName());
	private DataAccessSubsystem dataAccessSS = 
    	new DataAccessSubsystemFacade();
    
    private Type queryType;
    
    private String orderItemsQuery = "SELECT * FROM OrderItem WHERE orderid = ?";
    private String orderIdsQuery = "SELECT orderid FROM Ord WHERE custid = ?";
    private String orderDataQuery = "SELECT orderid, orderdate, totalpriceamount FROM Ord WHERE orderid = ?";
    private String submitOrderQuery = "INSERT into Ord "+
        "(custid, shipaddress1, shipcity, shipstate, shipzipcode, billaddress1, billcity, billstate,"+
           "billzipcode, nameoncard,  cardnum,cardtype, expdate, orderdate, totalpriceamount) " +
        "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"; 
    
    //altered by sanjeev on july 7
    private String submitOrderItemQuery = "INSERT into orderitem "+
        "(orderitemid, orderid, productid, quantity, totalprice)" + " VALUES(?,?,?,?,?)";
    Object[] orderItemsParams, orderIdsParams, orderDataParams, submitOrderParams, submitOrderItemParams;
    int[] orderItemsTypes, orderIdsTypes, orderDataTypes, submitOrderTypes, submitOrderItemTypes;
    
    //This is set by submitOrder and then used by submitOrderData
    private CustomerProfile custProfile;
    private List<Integer> orderIds;
    private List<OrderItem> orderItems;
    private OrderImpl orderData;
    private Order order;  
    
    DbClassOrder(){}
    
    List<Integer> getAllOrderIds(CustomerProfile custProfile) throws DatabaseException {
        queryType = Type.GET_ORDER_IDS;
        orderIdsParams = new Object[]{custProfile.getCustId()};
        orderIdsTypes = new int[]{Types.INTEGER};
        dataAccessSS.atomicRead(this);
        return Collections.unmodifiableList(orderIds);      
    }
    
    OrderImpl getOrderData(Integer orderId) throws DatabaseException {
    	queryType = Type.GET_ORDER_DATA;
    	orderDataParams = new Object[]{orderId};
    	orderDataTypes = new int[]{Types.INTEGER};
    	dataAccessSS.atomicRead(this);      	
        return orderData;
    }
    
    /**
	 *  This method submits top-level data in Order to the Ord table (this is
	 *  executed within the helper method submitOrderData)
	 *  and then, after it gets the order id, it submits each OrderItem from
	 *  Order to the OrderItem table (items are submitted one at a time
	 *  using submitOrderItem). All this is done within a transaction.
	 *  Separate methods are provided 
	 *  implemented by sanjeev on july 7
     */
    void submitOrder(CustomerProfile custProfile, Order order) throws DatabaseException {
       
    	//set the value to global objects
    	this.custProfile = custProfile;
    	this.order = order;
    
    	//database connection
    	dataAccessSS.establishConnection(this);
    	dataAccessSS.startTransaction();
    	
    	//submit order and get order id
    	int orderId = submitOrderData();
    	
    	//set order id on every order item
    	List<OrderItem> orderItems = order.getOrderItems();
    	orderItems.stream().forEach(ord -> ord.setOrderId(orderId));
        
    	//submit orderitems to database
    	for(OrderItem orditem : orderItems)
    	{
    		submitOrderItem(orditem);
    	}
    	
        dataAccessSS.commit();
        dataAccessSS.releaseConnection();
    }
	    
    /** This is part of the general submitOrder method */
	private Integer submitOrderData() throws DatabaseException {	
		queryType = Type.SUBMIT_ORDER;
		Address shipAddr = order.getShipAddress();
        Address billAddr = order.getBillAddress();
        CreditCard cc = order.getPaymentInfo();
    	submitOrderParams = new Object[]{custProfile.getCustId(),
    	                  shipAddr.getStreet(),
    	                  shipAddr.getCity(),
    	                  shipAddr.getState(),
    	                  shipAddr.getZip(),
    	                  billAddr.getStreet(),
    	                  billAddr.getCity(),
    	                  billAddr.getState(),
    	                  billAddr.getZip(),
    	                  cc.getNameOnCard(),
    	                  cc.getCardNum(),
    	                  cc.getCardType(),
    	                  cc.getExpirationDate(),
    	                  Convert.localDateAsString(order.getOrderDate()),
    	                  order.getTotalPrice()};
    	
        
    	submitOrderTypes = new int[]{Types.INTEGER, Types.VARCHAR, Types.VARCHAR,Types.VARCHAR,Types.VARCHAR,//shipping
    			Types.VARCHAR, Types.VARCHAR,Types.VARCHAR,Types.VARCHAR,//billing
    			Types.VARCHAR, Types.VARCHAR,Types.VARCHAR,Types.VARCHAR,//cc
    			Types.VARCHAR, Types.DOUBLE};
		return dataAccessSS.insert();    
	}
	
	/** This is part of the general submitOrder method  -implementing by sanjeev on july 7*/
	private void submitOrderItem(OrderItem item) throws DatabaseException {
   

		queryType=Type.SUBMIT_ORDER_ITEM;
        submitOrderItemParams = new Object[]{item.getOrderItemId(),item.getOrderId(), item.getProductId(), item.getQuantity(), item.getTotalPrice()};
        submitOrderItemTypes = new int[]{Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.DOUBLE};
        
        dataAccessSS.insert(); 
               
	}
   
	//implemented
    public List<OrderItem> getOrderItems(int orderId) throws DatabaseException {	
    	queryType = Type.GET_ORDER_ITEMS;
    	orderItemsParams = new Object[]{orderId};
        orderItemsTypes = new int[]{Types.INTEGER};
        dataAccessSS.atomicRead(this);
        return Collections.unmodifiableList(orderItems); 
        
    }
   
    private void populateOrderItems(ResultSet rs) throws DatabaseException {
   	
    	orderItems = new LinkedList<OrderItem>();
    	try {
            while(rs.next()){
            	OrderItem oItem = new OrderItemImpl("",rs.getInt("quantity"),rs.getDouble("totalprice")); 
            	oItem.setProductId(rs.getInt("orderitemid"));
                oItem.setOrderId(rs.getInt("orderid"));
                oItem.setProductId(rs.getInt("productid"));
                
               orderItems.add(oItem);
 
            }
        }
        catch(SQLException e){
            throw new DatabaseException(e);
        }
    }
    
    private void populateOrderIds(ResultSet resultSet) throws DatabaseException {
        orderIds = new LinkedList<Integer>();
        try {
            while(resultSet.next()){    
                orderIds.add(resultSet.getInt("orderid"));
            }
        }
        catch(SQLException e){
        	LOG.log(Level.SEVERE, "Database Exception occurred populating Order Ids", e);
            throw new DatabaseException(e);
        }
    }
    
    private void populateOrderData(ResultSet resultSet) throws DatabaseException { 
    	
    	try {
   		orderData = new OrderImpl();
   		
   		if(resultSet.next())
   		{
   			orderData.setOrderId(resultSet.getInt("orderid"));
   		 	orderData.setTotalPrice(resultSet.getDouble("totalpriceamount"));
   	       	orderData.setDate(Convert.localDateForString(resultSet.getString("orderdate")));       	
   		}
     
		} catch (SQLException e) {
			LOG.log(Level.SEVERE, "Database Exception occurred populating Order Ids", e);
            throw new DatabaseException(e);
		}
    	
    }    
    
    //used
    boolean checkStatus(String status)
    {
    	if(status == "delivered")
    	{
    		return true;
    	}
    	else
    	{
    		return false;
    	}
    }
 
    public void populateEntity(ResultSet resultSet) throws DatabaseException {
    	switch(queryType) {
	    	case GET_ORDER_ITEMS:
	    		populateOrderItems(resultSet);
	    		break;
	    	case GET_ORDER_IDS:
	    		populateOrderIds(resultSet);
	    		break;
	    	case GET_ORDER_DATA :
	        	populateOrderData(resultSet);
	        	break;
	        default :
	        	//do nothing
    	}
    }
    
    public String getDbUrl() {
    	DbConfigProperties props = new DbConfigProperties();	
    	return props.getProperty(DbConfigKey.ACCOUNT_DB_URL.getVal());
    }
    
    public String getQuery() {
    	switch(queryType) {
	    	case GET_ORDER_ITEMS:
	    		return orderItemsQuery;
	    	case GET_ORDER_IDS:
	    		return orderIdsQuery;
	    	case GET_ORDER_DATA :
	        	return orderDataQuery;
	    	case SUBMIT_ORDER:
	    		return submitOrderQuery;
	    	case SUBMIT_ORDER_ITEM:
	    		return submitOrderItemQuery;
	        default :
	        	return null;
		}
    }

	@Override
	public Object[] getQueryParams() {
		switch(queryType) {
	    	case GET_ORDER_ITEMS:
	    		return orderItemsParams;
	    	case GET_ORDER_IDS:
	    		return orderIdsParams;
	    	case GET_ORDER_DATA :
	        	return orderDataParams;
	    	case SUBMIT_ORDER:
	    		return submitOrderParams;
	    	case SUBMIT_ORDER_ITEM:
	    		return submitOrderItemParams;
	        default :
	        	return null;
		}
	}

	@Override
	public int[] getParamTypes() {
		switch(queryType) {
	    	case GET_ORDER_ITEMS:
	    		return orderItemsTypes;
	    	case GET_ORDER_IDS:
	    		return orderIdsTypes;
	    	case GET_ORDER_DATA :
	        	return orderDataTypes;
	    	case SUBMIT_ORDER:
	    		return submitOrderTypes;
	    	case SUBMIT_ORDER_ITEM:
	    		return submitOrderItemTypes;
	        default :
	        	return null;
		}
	}

	/*
	 * For testing only
	 * @see business.externalinterfaces.DbClassOrderForTest#getAllOrderIdsForTest(business.externalinterfaces.CustomerProfile)
	 */
	@Override
	public List<Integer> getAllOrderIdsForTest(CustomerProfile cust) throws DatabaseException {
		return getAllOrderIds(cust);
	}
    
}