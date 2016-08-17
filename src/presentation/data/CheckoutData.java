package presentation.data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import business.customersubsystem.CustomerSubsystemFacade;
import business.exceptions.BackendException;
import business.externalinterfaces.Address;
import business.externalinterfaces.CreditCard;
import business.externalinterfaces.CustomerProfile;
import business.externalinterfaces.CustomerSubsystem;
import business.usecasecontrol.CheckoutController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import presentation.gui.GuiConstants;
import presentation.util.CacheReader;

public enum CheckoutData {
	INSTANCE;
	private CheckoutController controller = new CheckoutController();
	public Address createAddress(String street, String city, String state,
			String zip, boolean isShip, boolean isBill) {
		return CustomerSubsystemFacade.createAddress(street, city, state, zip, isShip, isBill);
	}
	
	public CreditCard createCreditCard(String nameOnCard,String expirationDate,
               String cardNum, String cardType) {
		return CustomerSubsystemFacade.createCreditCard(nameOnCard, expirationDate, 
				cardNum, cardType);
	}
	
	//Customer Ship Address Data
	private ObservableList<CustomerPres> shipAddresses; 
	
	//Customer Bill Address Data
	private ObservableList<CustomerPres> billAddresses; 
	
//	private List<CustomerPres> shipInfoForCust()  {
//		CustomerProfile custProf = getCustomerProfile();
//		List<CustomerPres> retval = new ArrayList<>();
//		try {
//			List<Address> shipAddresses =
//				CheckoutController.INSTANCE.getShippingAddresses(custProf);
//			for(Address a : shipAddresses) {
//				CustomerPres cp = new CustomerPres(custProf, a);
//				retval.add(cp);
//			}
//		} catch(BackendException e) {
//			CheckoutUIControl.INSTANCE.
//			  getShippingBillingWindow().displayError("Error retrieving addresses. Try again later.");
//		}
//		
//		
//		return retval;
//		//go to use case controller
//		//get saved ship addresses for customer
//		//get cust profile
//		//assemble into a List<CustomerPres> and return
//	}
//	private void loadShipAddresses() {		
//	    List<CustomerPres> list = shipInfoForCust()
//						   .stream()
//						   .filter(cust -> cust.getAddress().isShippingAddress())
//						   .collect(Collectors.toList());
//		shipAddresses = FXCollections.observableList(list);				   									   
//	}
	
	private void loadShipAddresses() throws BackendException {
		CustomerSubsystem custSS
		  = (CustomerSubsystem)SessionCache.getInstance().get(SessionCache.CUSTOMER);
		List<Address> shippingAddresses = controller.getShippingAddresses(custSS);
		List<CustomerPres> displayableCustList =
				shippingAddresses.stream()
				                 .map(addr -> new CustomerPres(custSS.getCustomerProfile(), addr))
				                 .collect(Collectors.toList());
		shipAddresses =  FXCollections.observableList(displayableCustList);				   
										   
	}
	private void loadBillAddresses() throws BackendException {
		//Bandeshor// 7/7/2016
		CustomerSubsystem custSS = CacheReader.readCustomer();
		List<Address> billingAddresses = controller.getBillingAddresses(custSS);
		List<CustomerPres> displayableCustList =
				billingAddresses.stream()
				                 .map(addr -> new CustomerPres(custSS.getCustomerProfile(), addr))
				                 .collect(Collectors.toList());
		billAddresses =  FXCollections.observableList(displayableCustList);	
		/*List list = DefaultData.CUSTS_ON_FILE
				   .stream()
				   .filter(cust -> cust.getAddress().isBillingAddress())
				   .collect(Collectors.toList());
		billAddresses = FXCollections.observableList(list);*/
	}
	public ObservableList<CustomerPres> getCustomerShipAddresses() throws BackendException {
		if(shipAddresses == null) loadShipAddresses();
		return shipAddresses;
	}
	public ObservableList<CustomerPres> getCustomerBillAddresses() throws BackendException {
		if(billAddresses == null) loadBillAddresses();
		return billAddresses;
	}

	public List<String> getDisplayAddressFields() {
		return GuiConstants.DISPLAY_ADDRESS_FIELDS;
	}
	public List<String> getDisplayCredCardFields() {
		return GuiConstants.DISPLAY_CREDIT_CARD_FIELDS;
	}
	public List<String> getCredCardTypes() {
		return GuiConstants.CREDIT_CARD_TYPES;
	}
	public Address getDefaultShippingData() {
		//implemented //Bandeshor//7/7/2016
		/*List<String> add = DefaultData.DEFAULT_SHIP_DATA;
		return CustomerSubsystemFacade.createAddress(add.get(0), add.get(1), 
				add.get(2), add.get(3), true, false);*/
		CustomerSubsystem customerSubSystem = CacheReader.readCustomer();
		return customerSubSystem.getDefaultShippingAddress();
		
	}
	
	public Address getDefaultBillingData() {
		//implemented //Bandeshor//7/7/2016
		/*List<String> add =  DefaultData.DEFAULT_BILLING_DATA;
		return CustomerSubsystemFacade.createAddress(add.get(0), add.get(1), 
				add.get(2), add.get(3), false, true);*/
		CustomerSubsystem customerSubSystem = CacheReader.readCustomer();
		return customerSubSystem.getDefaultBillingAddress();
	}
	
	public List<String> getDefaultPaymentInfo() {
		//implemented //Bandeshor//7/7/2016
		/*return DefaultData.DEFAULT_PAYMENT_INFO;*/
		CustomerSubsystem customerSubSystem = CacheReader.readCustomer();
		CreditCard creditCard = customerSubSystem.getDefaultPaymentInfo();
		return Arrays.asList(creditCard.getNameOnCard(), creditCard.getCardNum(), creditCard.getCardType(), creditCard.getExpirationDate());
	}
	
	
	public CustomerProfile getCustomerProfile(CustomerSubsystem cust) {
		return cust.getCustomerProfile();
	}
	
		
	
	private class ShipAddressSynchronizer implements Synchronizer {
		public void refresh(ObservableList list) {
			shipAddresses = list;
		}
	}
	public ShipAddressSynchronizer getShipAddressSynchronizer() {
		return new ShipAddressSynchronizer();
	}
	
	private class BillAddressSynchronizer implements Synchronizer {
		public void refresh(ObservableList list) {
			billAddresses = list;
		}
	}
	public BillAddressSynchronizer getBillAddressSynchronizer() {
		return new BillAddressSynchronizer();
	}
	
	public static class ShipBill {
		public boolean isShipping;
		public String label;
		public Synchronizer synch;
		public ShipBill(boolean shipOrBill, String label, Synchronizer synch) {
			this.isShipping = shipOrBill;
			this.label = label;
			this.synch = synch;
		}
		
	}
	
}
