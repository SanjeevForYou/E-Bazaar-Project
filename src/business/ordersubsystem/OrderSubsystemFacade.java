package business.ordersubsystem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import middleware.exceptions.DatabaseException;
import business.exceptions.BackendException;
import business.externalinterfaces.*;
import business.productsubsystem.ProductSubsystemFacade;
import business.util.*;

public class OrderSubsystemFacade implements OrderSubsystem {
	private static final Logger LOG = 
			Logger.getLogger(OrderSubsystemFacade.class.getPackage().getName());
	CustomerProfile custProfile;
	    
    public OrderSubsystemFacade(CustomerProfile custProfile){
        this.custProfile = custProfile;
    }
	
	/** 
     *  Used by customer subsystem at login to obtain this customer's order history from the database.
	 *  Assumes cust id has already been stored into the order subsystem facade 
	 *  This is created by using auxiliary methods at the bottom of this class file.
	 *  First get all order ids for this customer. For each such id, get order data
	 *  and form an order, and with that order id, get all order items and insert
	 *  into the order.
	 */
    public List<Order> getOrderHistory() throws BackendException {

		try {
			List<Order> orders = new ArrayList<Order>();
			List<Integer> orderIds = getAllOrderIds();
			for (Integer ordid : orderIds) {
				try {
					orders.add(getOrderByOrderID(ordid.intValue()));
				} catch (DatabaseException e) {
					LOG.log(Level.SEVERE, "Exception occured when geting order history", e);
				}
			}

			orders.stream().forEach(ord -> {
				try {
					ord.setOrderItems(getOrderItems(ord.getOrderId()));
				} catch (DatabaseException e) {
					LOG.log(Level.SEVERE, "Exception occured when geting order history", e);
				}
			});
			return orders;

		} catch (DatabaseException e) {
			LOG.log(Level.SEVERE, "Exception occured when geting order history", e);
			throw new BackendException(e);
		}

	}
    
    Order getOrderByOrderID(int orderID) throws DatabaseException
    {
    	DbClassOrder dbClass = new DbClassOrder();
    	return dbClass.getOrderData(orderID);
    }
    
    public void submitOrder(ShoppingCart cart) throws BackendException {

    	List<CartItem> cartItems = cart.getCartItems();
    	List<OrderItem> orderItems = cartItems.stream().map(cartitem -> Convert.createOrderItemFromCartItem(cartitem)).collect(Collectors.toList());
    	
    	Order order = new OrderImpl();
    	
    	order.setDate(LocalDate.now());
    	order.setOrderItems(orderItems);
    	
    	Address shippingAddress = cart.getShippingAddress();
    	Address billingAddress = cart.getBillingAddress();
    	CreditCard creditCard = cart.getPaymentInfo();
    	
    	order.setShipAddress(shippingAddress);
    	order.setBillAddress(billingAddress);
    	order.setPaymentInfo(creditCard);
    	
    	DbClassOrder dbClassOrder = new DbClassOrder();
    	try {
    		dbClassOrder.submitOrder(custProfile, order);
		} catch (DatabaseException e) {
			LOG.log(Level.SEVERE, "Backend exception occured on ordersubsystem order submission", e);
			throw new BackendException(e);
		}	 	
    }
	
	/** Used whenever an order item needs to be created from outside the order subsystem */
    public static OrderItem createOrderItem(
    		Integer prodId, Integer orderId, String quantityReq, String totalPrice) {   
    	OrderItem orderitem = new OrderItemImpl("", Integer.parseInt(quantityReq),Double.parseDouble(totalPrice));
    	orderitem.setProductId(prodId);
    	orderitem.setOrderId(orderId);
    	return orderitem;
    }
    
    /** to create an Order object from outside the subsystem */
    public static Order createOrder(Integer orderId, String orderDate, String totalPrice) {

    	Order order = new OrderImpl();
    	order.setOrderId(orderId);
    	order.setDate(Convert.localDateForString(orderDate));
    	order.setTotalPrice(Double.parseDouble(totalPrice));
    	return order;
    }
    
    ///////////// Methods internal to the Order Subsystem -- NOT public
    List<Integer> getAllOrderIds() throws DatabaseException {
        DbClassOrder dbClass = new DbClassOrder();
        return dbClass.getAllOrderIds(custProfile);
        
    }
    
    /** Part of getOrderHistory */
    List<OrderItem> getOrderItems(int orderId) throws DatabaseException {
        DbClassOrder dbClass = new DbClassOrder();
        List<OrderItem> ordItems = dbClass.getOrderItems(orderId);
        
        ProductSubsystemFacade product = new ProductSubsystemFacade();
        ordItems.stream().forEach(item ->{
        	try{item.setProductName(product.getProductFromId(item.getProductId()).getProductName());
        	}catch(BackendException e){
        		LOG.log(Level.SEVERE, "Backend exception occured on ordersubsystem", e);
        	} 	
        });
        return ordItems;
    }
    
    /** Uses cust id to locate top-level order data for customer -- part of getOrderHistory */
    OrderImpl getOrderData(Integer custId) throws DatabaseException {
    	DbClassOrder dbClass = new DbClassOrder();
    	return dbClass.getOrderData(custId);
    }
    
    //
    public DbClassOrderForTest getGenericDbClassOrder()
    {
    	return new DbClassOrder();
    }
}
