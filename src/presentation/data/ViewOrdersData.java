package presentation.data;

import java.util.List;

import java.util.logging.Logger;
import java.util.stream.Collectors;

import business.usecasecontrol.ViewOrdersController;
import presentation.util.CacheReader;

public enum ViewOrdersData {
	INSTANCE;
	private static final Logger LOG = 
		Logger.getLogger(ViewOrdersData.class.getSimpleName());
	private OrderPres selectedOrder;
	
	//implementation july 6
	private ViewOrdersController controller = new ViewOrdersController();
	
	public OrderPres getSelectedOrder() {
		return selectedOrder;
	}
	public void setSelectedOrder(OrderPres so) {
		selectedOrder = so;
	}
	
	public List<OrderPres> getOrders() {
		return controller.getOrderHistory(CacheReader.readCustomer()).stream().map(s -> {return (OrderPres)s;}).collect(Collectors.toList());
//		LOG.warning("ViewOrdersData method getOrders() has not been implemented.");
  //  return DefaultData.ALL_ORDERS;
	}
}
