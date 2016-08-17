package presentation.control;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.text.Text;
import launch.Start;
import presentation.data.BrowseSelectData;
import presentation.data.CheckoutData;
import presentation.data.CustomerPres;
import presentation.data.ErrorMessages;
import presentation.data.SessionCache;
import presentation.gui.CatalogListWindow;
import presentation.gui.FinalOrderWindow;
import presentation.gui.OrderCompleteWindow;
import presentation.gui.PaymentWindow;
import presentation.gui.ShippingBillingWindow;
import presentation.gui.ShoppingCartWindow;
import presentation.gui.TermsWindow;
import presentation.util.CacheReader;
//import rulesengine.OperatingException;
//import rulesengine.ReteWrapper;
//import rulesengine.ValidationException;
//import system.rulescore.rulesengine.*;
//import system.rulescore.rulesupport.*;
//import system.rulescore.util.*;
import business.exceptions.BackendException;
import business.exceptions.BusinessException;
import business.exceptions.RuleException;
import business.externalinterfaces.Address;
import business.externalinterfaces.CustomerProfile;
import business.externalinterfaces.CustomerSubsystem;
import business.usecasecontrol.CheckoutController;
import business.util.DataUtil;

public enum CheckoutUIControl {
	INSTANCE;
	private CheckoutController controller = new CheckoutController();
	private static final Logger LOG = Logger.getLogger(CheckoutUIControl.class
			.getPackage().getName());
	// Windows managed by CheckoutUIControl
	ShippingBillingWindow shippingBillingWindow;
	PaymentWindow paymentWindow;
	TermsWindow termsWindow;
	FinalOrderWindow finalOrderWindow;
	OrderCompleteWindow orderCompleteWindow;

	public ShippingBillingWindow getShippingBillingWindow() {
		return shippingBillingWindow;
	}

	// handler for ShoppingCartWindow proceeding to checkout
	private class ProceedFromCartToShipBill implements
			EventHandler<ActionEvent>, Callback {
		CheckoutData data = CheckoutData.INSTANCE;
		public void doUpdate() {
			shippingBillingWindow = new ShippingBillingWindow();
			CustomerProfile custProfile = data.getCustomerProfile(CacheReader.readCustomer());
			Address defaultShipAddress = data.getDefaultShippingData();
			Address defaultBillAddress = data.getDefaultBillingData();
			
			shippingBillingWindow.setShippingAddress(custProfile.getFirstName()
					+ " " + custProfile.getLastName(),
					defaultShipAddress.getStreet(),
					defaultShipAddress.getCity(),
					defaultShipAddress.getState(), defaultShipAddress.getZip());
			shippingBillingWindow.setBillingAddress(custProfile.getFirstName()
					+ " " + custProfile.getLastName(),
					defaultBillAddress.getStreet(),
					defaultBillAddress.getCity(),
					defaultBillAddress.getState(), defaultBillAddress.getZip());
			shippingBillingWindow.show();
		}
		@Override
		public void handle(ActionEvent evt) {
			ShoppingCartWindow.INSTANCE.clearMessages();
			ShoppingCartWindow.INSTANCE.setTableAccessByRow();
			
			
			boolean rulesOk = true;
			
			//Bandeshor //7/8/2016
			/* check that cart is not empty before going to next screen */	
			
			try {
				controller.runShoppingCartRules(CacheReader.readCustomer().getShoppingCart());
			} catch (RuleException e) {
				//handle
				getMessageBar().setText(e.getLocalizedMessage());
				e.printStackTrace();
				rulesOk= false;
			} catch (BusinessException e) {
				//handle
				getMessageBar().setText(e.getLocalizedMessage());
				e.printStackTrace();
				rulesOk = false;
			}
	
			
			if (rulesOk) {
				ShoppingCartWindow.INSTANCE.hide();
				boolean isLoggedIn = CacheReader.readLoggedIn();
				//do no create instace of ShoppingBillWindow here
				if (!isLoggedIn) {
					LoginUIControl loginControl 
					  = new LoginUIControl(shippingBillingWindow, ShoppingCartWindow.INSTANCE, this);
					loginControl.startLogin();
				} else {
					doUpdate();
				}			
			}

		}
		@Override
		public Text getMessageBar() {
			return ShoppingCartWindow.INSTANCE.getMessageBar();
		}
	}
	
	

	
	public ProceedFromCartToShipBill getProceedFromCartToShipBill() {
		return new ProceedFromCartToShipBill();
	}

	// handlers for ShippingBillingWindow
	private class BackToShoppingCartHandler implements
			EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent evt) {
			ShoppingCartWindow.INSTANCE.show();
			shippingBillingWindow.clearMessages();
			shippingBillingWindow.hide();
		}
	}

	public BackToShoppingCartHandler getBackToShoppingCartHandler() {
		return new BackToShoppingCartHandler();
	}

	private class ProceedToPaymentHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent evt) {
			shippingBillingWindow.clearMessages();
			boolean rulesOk = true;
			Address cleansedShipAddress = null;
			Address cleansedBillAddress = null;
			CustomerSubsystem cust = CacheReader.readCustomer(); 
			//Bandeshor //7/12/2016
			//check even if not saving the Addresses,but if ticked save, save it
				try {				
					Address tempAddr 
					   = controller.runAddressRules(cust, shippingBillingWindow
							.getShippingAddress());
					if(shippingBillingWindow.getSaveShipAddr())
						cleansedShipAddress = tempAddr;
				} catch (RuleException e) {
					LOG.log(Level.FINER, "Rule Exception: Shipping address error", e);
					rulesOk = false;
					shippingBillingWindow
							.displayError("Shipping address error: "
									+ e.getMessage());
				} catch (BusinessException e) {
					LOG.log(Level.SEVERE, "Business Exception getting Save Shipping Address", e);
					rulesOk = false;
					shippingBillingWindow.displayError(
							ErrorMessages.GENERAL_ERR_MSG + ": Message: " + e.getMessage());
				}
			
			if (rulesOk) {
					try {
						Address tempAddr= controller.runAddressRules(cust, shippingBillingWindow
								.getBillingAddress());
						if(shippingBillingWindow.getSaveBillAddr())
							cleansedBillAddress = tempAddr;
					} catch (RuleException e) {
						LOG.log(Level.FINER, "Rule Exception: Billing address error", e);
						rulesOk = false;
						shippingBillingWindow
								.displayError("Billing address error: "
										+ e.getMessage());
					} catch (BusinessException e) {
						LOG.log(Level.SEVERE, "Business Exception getting Save Billing Address", e);
						rulesOk = false;
						shippingBillingWindow.displayError(
								ErrorMessages.GENERAL_ERR_MSG + ": Message: " + e.getMessage());
					}
			}
			if (rulesOk) {
				
				LOG.info("Address Rules passed!");
				if (cleansedShipAddress != null) {
					try {
						controller.saveNewAddress(cust, cleansedShipAddress);
					} catch(BackendException e) {
						LOG.log(Level.SEVERE, "Backend Exception saving New Shipping Address", e);
						shippingBillingWindow.displayError("New shipping address not saved. Message: " 
							+ e.getMessage());
					}
				}
				if (cleansedBillAddress != null) {		
					try {
						controller.saveNewAddress(cust, cleansedBillAddress);
					} catch(BackendException e) {
						LOG.log(Level.SEVERE, "Backend Exception saving New Billing Address", e);
						shippingBillingWindow.displayError("New billing address not saved. Message: " 
							+ e.getMessage());
					}
				}
				paymentWindow = new PaymentWindow();
				paymentWindow.show();
				shippingBillingWindow.hide();
			}
		}
	}
	

	public ProceedToPaymentHandler getProceedToPaymentHandler() {
		return new ProceedToPaymentHandler();
	}

	public class SaveShipChangeListener implements ChangeListener<Boolean> {
		@Override
		public void changed(ObservableValue<? extends Boolean> observed,
				Boolean oldval, Boolean newval) {
			shippingBillingWindow.displayInfo("");
		}
	}

	public class SaveBillChangeListener implements ChangeListener<Boolean> {
		@Override
		public void changed(ObservableValue<? extends Boolean> observed,
				Boolean oldval, Boolean newval) {
			shippingBillingWindow.displayInfo("");

		}
	}

	public SaveShipChangeListener getSaveShipChangeListener() {
		return new SaveShipChangeListener();
	}

	public SaveBillChangeListener getSaveBillChangeListener() {
		return new SaveBillChangeListener();
	}

	// handlers for PaymentWindow
	private class BackToShipBillWindow implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent evt) {
			paymentWindow.clearMessages();
			shippingBillingWindow.show();
			paymentWindow.hide();
		}
	}

	public BackToShipBillWindow getBackToShipBillWindow() {
		return new BackToShipBillWindow();
	}

	private class BackToCartHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent evt) {
			paymentWindow.clearMessages();
			ShoppingCartWindow.INSTANCE.show();
			paymentWindow.hide();
		}
	}

	public BackToCartHandler getBackToCartHandler() {
		return new BackToCartHandler();
	}

	private class ProceedToTermsHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent evt) {
			try {
				controller.runPaymentRules(shippingBillingWindow.getBillingAddress(),
					paymentWindow.getCreditCardFromWindow());
				paymentWindow.clearMessages();
				paymentWindow.hide();
				termsWindow = new TermsWindow();
				termsWindow.show();
			} catch(RuleException e) {
				LOG.log(Level.FINER, "Rule Exception showing Terms Window", e);
				paymentWindow.displayError(e.getMessage());
			} catch(BusinessException e) {
				LOG.log(Level.SEVERE, "Business Exception getting Billing Address", e);
				paymentWindow.displayError(ErrorMessages.DATABASE_ERROR);
			}
		}
	}

	public ProceedToTermsHandler getProceedToTermsHandler() {
		return new ProceedToTermsHandler();
	}


	// handlers for TermsWindow

	private class ToCartFromTermsHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent evt) {
			termsWindow.hide();
			ShoppingCartWindow.INSTANCE.show();
		}
	}

	public ToCartFromTermsHandler getToCartFromTermsHandler() {
		return new ToCartFromTermsHandler();
	}

	private class AcceptTermsHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent evt) {
			finalOrderWindow = new FinalOrderWindow();
			finalOrderWindow.setData(BrowseSelectData.INSTANCE.getCartData());
			finalOrderWindow.show();
			termsWindow.hide();
		}
	}

	public AcceptTermsHandler getAcceptTermsHandler() {
		return new AcceptTermsHandler();
	}

	// handlers for FinalOrderWindow
		private class SubmitHandler implements EventHandler<ActionEvent> {
			@Override
			public void handle(ActionEvent evt) {
				//changes
				boolean isRuleOk = false;
					try {
						controller.runFinalOrderRules(CacheReader.readCustomer().getShoppingCart());
						isRuleOk =true;
					} catch (RuleException e) {
						LOG.log(Level.FINER, "Rule Exception when submitting final order", e);
						finalOrderWindow.displayError(e.getMessage());
					}catch (BusinessException e) {
						LOG.log(Level.SEVERE, "Rules based Exception while submitting  order", e);
						finalOrderWindow.displayError(e.getMessage());
				}
					
					if(isRuleOk)
					{
						try {
							controller.submitFinalOrder(CacheReader.readCustomer());
							orderCompleteWindow = new OrderCompleteWindow();
							orderCompleteWindow.show();
							finalOrderWindow.clearMessages();
							finalOrderWindow.hide();
						} catch (BackendException e) {
							LOG.log(Level.SEVERE, "Backend Exception while submitting  order", e);
							finalOrderWindow.displayError("Order submission failed!!");
						}
					}
			
			}

		}
		
	public SubmitHandler getSubmitHandler() {
		return new SubmitHandler();
	}

	private class CancelOrderHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent evt) {
			finalOrderWindow.displayInfo("Order Cancelled");
		}
	}

	public CancelOrderHandler getCancelOrderHandler() {
		return new CancelOrderHandler();
	}

	private class ToShoppingCartFromFinalOrderHandler implements
			EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent evt) {
			ShoppingCartWindow.INSTANCE.show();
			finalOrderWindow.hide();
			finalOrderWindow.clearMessages();
		}
	}

	public ToShoppingCartFromFinalOrderHandler getToShoppingCartFromFinalOrderHandler() {
		return new ToShoppingCartFromFinalOrderHandler();
	}

	// handlers for OrderCompleteWindow

	private class ContinueFromOrderCompleteHandler implements
			EventHandler<ActionEvent> {
		CatalogListWindow catWindow;
		@Override
		public void handle(ActionEvent evt) {
			try{
				catWindow = CatalogListWindow.getInstance(ShoppingCartWindow.INSTANCE.getPrimaryStage(),FXCollections.observableList(BrowseSelectData.INSTANCE.getCatalogList()));
			}
			catch(Exception e){
				e.printStackTrace();
			}
			if(catWindow != null)
				{
				catWindow.show();
				}
			orderCompleteWindow.hide();
		}
	}

	public ContinueFromOrderCompleteHandler getContinueFromOrderCompleteHandler() {
		return new ContinueFromOrderCompleteHandler();
	}
	
	
	//Patch3
	public class SetShippingInSelect implements Consumer<CustomerPres> {

		@Override
		public void accept(CustomerPres cust) {
			shippingBillingWindow.setShippingAddress(
					cust.fullNameProperty().get(), 
					cust.streetProperty().get(), 
					cust.cityProperty().get(), 
					cust.stateProperty().get(), 
					cust.zipProperty().get());
			
		}	
	}
	public SetShippingInSelect getSetShippingInSelect() {
		return new SetShippingInSelect();
	}
	
	public class SetBillingInSelect implements Consumer<CustomerPres> {
		@Override
		public void accept(CustomerPres cust) {
			shippingBillingWindow.setBillingAddress(
					cust.fullNameProperty().get(), 
					cust.streetProperty().get(), 
					cust.cityProperty().get(), 
					cust.stateProperty().get(), 
					cust.zipProperty().get());
			
		}	
	}
	
	public SetBillingInSelect getSetBillingInSelect() {
		return new SetBillingInSelect();
	}
}
