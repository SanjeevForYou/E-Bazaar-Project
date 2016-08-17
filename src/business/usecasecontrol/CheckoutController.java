package business.usecasecontrol;

import java.util.List;
import java.util.logging.Logger;

import business.customersubsystem.RulesPayment;
import business.exceptions.BackendException;
import business.exceptions.BusinessException;
import business.exceptions.RuleException;
import business.externalinterfaces.Address;
import business.externalinterfaces.CreditCard;
import business.externalinterfaces.CustomerSubsystem;
import business.externalinterfaces.Rules;
import business.externalinterfaces.ShoppingCartSubsystem;
import business.shoppingcartsubsystem.RulesFinalOrder;
import business.shoppingcartsubsystem.RulesShoppingCart;

public class CheckoutController  {
		
	private static final Logger LOG = Logger.getLogger(CheckoutController.class
			.getPackage().getName());
	
	
	public void runShoppingCartRules(ShoppingCartSubsystem shopCart) throws RuleException, BusinessException {
		//implemented//Bandeshor 7/8/2016
		Rules shoppingRuleObject = new RulesShoppingCart(shopCart.getLiveCart());
		shoppingRuleObject.runRules();
	}
	
	public void runPaymentRules(Address addr, CreditCard cc) throws RuleException, BusinessException {
		//implemented//Bandeshor 7/8/2016
		Rules paymentRuleObject = new RulesPayment(addr, cc);
		paymentRuleObject.runRules();
	}
	
	public Address runAddressRules(CustomerSubsystem cust, Address addr) throws RuleException, BusinessException {
		return cust.runAddressRules(addr);
	}
	
	public List<Address> getShippingAddresses(CustomerSubsystem cust) throws BackendException {
		return cust.getAllShipAddresses();
	}
	
	public List<Address> getBillingAddresses(CustomerSubsystem cust) throws BackendException {
		return cust.getAllShipAddresses();
	}
	
	/** Asks the ShoppingCart Subsystem to run final order rules */
	public void runFinalOrderRules(ShoppingCartSubsystem scss) throws RuleException, BusinessException {
		//implemented//Bandeshor 7/8/2016
		new RulesFinalOrder(scss.getLiveCart()).runRules();
	}
	
	/** Asks Customer Subsystem to check credit card against 
	 *  Credit Verification System 
	 */
	public void verifyCreditCard(CustomerSubsystem cust) throws BusinessException {
		//implemented//Bandeshor 7/8/2016
		cust.checkCreditCard();
	}
	
	public void saveNewAddress(CustomerSubsystem cust, Address addr) throws BackendException {		
		cust.saveNewAddress(addr);
	}
	
	/** Asks Customer Subsystem to submit final order */
	public void submitFinalOrder(CustomerSubsystem cust) throws BackendException {
		//implemented//Bandeshor 7/8/2016
		cust.submitOrder();
		cust.refreshAfterSubmit();
	}


}
