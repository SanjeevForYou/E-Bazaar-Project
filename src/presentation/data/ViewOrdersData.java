package presentation.data;

import java.util.List;

import java.util.logging.Logger;

import business.exceptions.BackendException;
import business.externalinterfaces.CustomerSubsystem;
import business.ordersubsystem.OrderSubsystemFacade;
import javafx.collections.FXCollections;
import presentation.util.CacheReader;
import presentation.util.UtilForUIClasses;

public enum ViewOrdersData {
	INSTANCE;
	private static final Logger LOG = 
		Logger.getLogger(ViewOrdersData.class.getSimpleName());
	private OrderPres selectedOrder;
	public OrderPres getSelectedOrder() {
		return selectedOrder;
	}
	public void setSelectedOrder(OrderPres so) {
		selectedOrder = so;
	}
	
	public List<OrderPres> getOrders() throws BackendException {
		//implemented
		CustomerSubsystem cust = CacheReader.readCustomer(); 
		return UtilForUIClasses.orderListToOrderPresList(cust.getOrderHistory());
		
	}
}
