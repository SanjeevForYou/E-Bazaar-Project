package presentation.control;

import java.util.logging.Logger;

import business.exceptions.BackendException;
import business.exceptions.UnauthorizedException;
import business.externalinterfaces.CustomerSubsystem;
import business.ordersubsystem.OrderSubsystemFacade;
import business.usecasecontrol.BrowseAndSelectController;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import presentation.data.*;
import presentation.gui.LoginWindow;
import presentation.gui.OrderDetailWindow;
import presentation.gui.OrdersWindow;
import presentation.gui.ShoppingCartWindow;
import presentation.util.CacheReader;

public enum ViewOrdersUIControl {
	INSTANCE;
	
	private static final Logger LOG = 
			Logger.getLogger(ViewOrdersUIControl.class.getName());
	
	private OrdersWindow ordersWindow;
	private OrderDetailWindow orderDetailWindow;
    private Stage primaryStage;
    private Callback startScreenCallback;
    
    //added
	
	public void setPrimaryStage(Stage ps, Callback returnMessage) {
		primaryStage = ps;
		startScreenCallback = returnMessage;
	}
	
	private class ViewOrdersHandler implements EventHandler<ActionEvent>, Callback {
	
		@Override
		public void handle(ActionEvent evt) {
			CustomerSubsystem cust = CacheReader.readCustomer();
			ordersWindow = new OrdersWindow(primaryStage);
			if(cust!=null)
			{
				doUpdate();
			}
			else
			{
				LoginUIControl login = new LoginUIControl(ordersWindow, primaryStage, this);
				login.startLogin();
			}
				
		}

		@Override
		public Text getMessageBar() {
			Text messageBar = new Text();
			return messageBar;
		}

		@Override
		public void doUpdate() {
		System.out.println("Do update review orders sanjeev");
			try {
				ordersWindow.setData(FXCollections.observableList(ViewOrdersData.INSTANCE.getOrders()));
			} catch (BackendException e) {
				
				LOG.warning("Couldn't get OrderHistory..");
				e.printStackTrace();
			}
			ordersWindow.show();
	        primaryStage.hide();
			
		}	
	}
	public ViewOrdersHandler getViewOrdersHandler() {
		return new ViewOrdersHandler();
	}
	
	//OrdersWindow
	private class ViewOrderDetailsHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent evt) {
			TableView<OrderPres> table = ordersWindow.getTable();
			OrderPres selectedOrder = table.getSelectionModel().getSelectedItem();
			if(selectedOrder == null) {
				ordersWindow.displayError("Please select a row.");
			} else {
				ordersWindow.clearMessages();
				orderDetailWindow = new OrderDetailWindow();
				orderDetailWindow.setData(FXCollections.observableList(selectedOrder.getOrderItemsPres()));
				ordersWindow.hide();
				orderDetailWindow.show();
			}
		}
	}
	
	public ViewOrderDetailsHandler getViewOrderDetailsHandler() {
		return new ViewOrderDetailsHandler();
	}
	
	
	//OrderDetailWindow
	private class OrderDetailsOkHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent evt) {
			ordersWindow.show();
			orderDetailWindow.hide();
		}
	}
	public OrderDetailsOkHandler getOrderDetailsOkHandler() {
		return new OrderDetailsOkHandler();
	}
	
	private class CancelHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent evt) {
			ordersWindow.hide();
			startScreenCallback.clearMessages();
			primaryStage.show();
			
		}
	}
	public CancelHandler getCancelHandler() {
		return new CancelHandler();
	}
}
