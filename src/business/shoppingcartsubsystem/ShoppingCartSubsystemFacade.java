package business.shoppingcartsubsystem;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import business.customersubsystem.AddressImpl;
import business.customersubsystem.CreditCardImpl;
import business.exceptions.BackendException;
import business.exceptions.BusinessException;
import business.exceptions.RuleException;
import business.externalinterfaces.Address;
import business.externalinterfaces.CartItem;
import business.externalinterfaces.CreditCard;
import business.externalinterfaces.CustomerProfile;
import business.externalinterfaces.CustomerSubsystem;
import business.externalinterfaces.DbClassShoppingCartForTest;
import business.externalinterfaces.ShoppingCart;
import business.externalinterfaces.ShoppingCartSubsystem;
import business.usecasecontrol.CheckoutController;
import middleware.exceptions.DatabaseException;
import presentation.data.SessionCache;
import presentation.util.CacheReader;

public class ShoppingCartSubsystemFacade implements ShoppingCartSubsystem {
	
	private static final Logger LOG = Logger.getLogger(DbClassShoppingCart.class
			.getPackage().getName());
	ShoppingCartImpl liveCart = new ShoppingCartImpl(new LinkedList<CartItem>());
	ShoppingCartImpl savedCart;
	Integer shopCartId;
	CustomerProfile customerProfile;
//	= CacheReader.readCustomer().getCustomerProfile();
	
	// interface methods
	public void setCustomerProfile(CustomerProfile customerProfile) {
		this.customerProfile = customerProfile;
	}
	
	public void makeSavedCartLive() {
		liveCart = savedCart;
		
		//Bandeshor //7/12/2016
		//Also set the cart with the default values of address and payment 
		/*CustomerSubsystem css= CacheReader.readCustomer();
		setBillingAddress(css.getDefaultBillingAddress());
		setShippingAddress(css.getDefaultShippingAddress());
		setPaymentInfo(css.getDefaultPaymentInfo());*/
		
	}
	
	public ShoppingCart getLiveCart() {
		return liveCart;
	}

	public void retrieveSavedCart() throws BackendException {
		try {
			DbClassShoppingCart dbClass = new DbClassShoppingCart();
			ShoppingCartImpl cartFound = dbClass.retrieveSavedCart(customerProfile);
			if(cartFound == null) {
				savedCart = new ShoppingCartImpl(new ArrayList<CartItem>());
			} else {
				savedCart = cartFound;
			}
		} catch(DatabaseException e) {
			LOG.log(Level.SEVERE, "Database Exception occurred retrieving Saved Cart", e);
			throw new BackendException(e);
		}

	}
	
	@Override
	public void setShippingAddress(Address addr) {
		liveCart.setShipAddress(addr);
		
	}

	@Override
	public void setBillingAddress(Address addr) {
		liveCart.setBillAddress(addr);
		
	}

	@Override
	public void setPaymentInfo(CreditCard cc) {
		liveCart.setPaymentInfo(cc);
		
	}
	
	public void setCartItems(List<CartItem> list) {
		liveCart.setCartItems(list);
	}
	
	public List<CartItem> getCartItems() {
		return liveCart.getCartItems();
	}
	
	//static methods
	public static CartItem createCartItem(String productName, String quantity,
            String totalprice) {
		try {
			return new CartItemImpl(productName, quantity, totalprice);
		} catch(BackendException e) {
			LOG.log(Level.SEVERE, "Backend Exception occurred creating Cart Item", e);
			throw new RuntimeException("Can't create a cartitem because of productid lookup: " + e.getMessage());
		}
	}


	
	//interface methods for testing
	
	public ShoppingCart getEmptyCartForTest() {
		return new ShoppingCartImpl();
	}

	
	public CartItem getEmptyCartItemForTest() {
		return new CartItemImpl();
	}

	@Override
	public void clearLiveCart() {
		//implemented//Bandeshor 7/8/2016
		liveCart.clearCart();
	}

	@Override
	public List<CartItem> getLiveCartItems() {
		//implemented//Bandeshor 7/8/2016
		return liveCart.getCartItems();
	}

	@Override
	public void saveLiveCart() throws BackendException {
		// TODO Auto-generated method stub
		//implement
		//System.out.println("testing..."+customerProfile.getCustId());
		customerProfile = CacheReader.readCustomer().getCustomerProfile();
		CustomerSubsystem cust = CacheReader.readCustomer();
		liveCart.setBillAddress(cust.getDefaultBillingAddress());
		liveCart.setShipAddress(cust.getDefaultShippingAddress());
		liveCart.setPaymentInfo(cust.getDefaultPaymentInfo());
		DbClassShoppingCart dbShoppingCart = new DbClassShoppingCart();
		try {
			dbShoppingCart.saveCart(customerProfile, liveCart);
		} catch (DatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new BackendException(e);
		}
	}

	@Override
	public void runShoppingCartRules() throws RuleException, BusinessException {
		//implemented//Bandeshor 7/8/2016
		CheckoutController controller = new CheckoutController();
		controller.runShoppingCartRules(this);
	}

	@Override
	public void runFinalOrderRules() throws RuleException, BusinessException {
		//implemented//Bandeshor 7/8/2016
		CheckoutController controller = new CheckoutController();
		controller.runFinalOrderRules(this);
	}

	@Override
	public DbClassShoppingCartForTest getGenericShoppingCart() {
		// TODO Auto-generated method stub
		return new DbClassShoppingCart();
	}

	@Override
	public ShoppingCart getGenericShoppingCartForTest() {
		// TODO Auto-generated method stub
		ShoppingCartImpl cart = new ShoppingCartImpl();
		cart.setBillAddress(new AddressImpl("test","test","test","test",false,false));
		cart.setShipAddress(new AddressImpl("test","test","test","test",false,false));
		cart.setPaymentInfo(new CreditCardImpl("test", "test", "test", "test"));
		return cart;
	}

	//Bereket //7/12/2016
	@Override
	public void deleteSavedCartAfterSubmit(CustomerProfile cusProfile) throws BackendException {
		// TODO Auto-generated method stub
		try{
			////check if retrived from the database, if not do not delete
			if(SessionCache.getInstance().get(SessionCache.RETRIVED_SHOPPING_CART) != null
					&&
					(boolean)SessionCache.getInstance().get(SessionCache.RETRIVED_SHOPPING_CART)
					){
				DbClassShoppingCart dbshoppingcart = new DbClassShoppingCart();
				dbshoppingcart.deleteCartAfterSubmissions(cusProfile);
			}
		}
		catch(DatabaseException e){
			e.printStackTrace();
			throw new BackendException(e);
		}
		
	}

	

}
