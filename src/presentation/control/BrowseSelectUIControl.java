package presentation.control;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import middleware.dataaccess.StatementPrep;
import presentation.data.BrowseSelectData;
import presentation.data.CartItemPres;
import presentation.data.CatalogPres;
import presentation.data.ProductPres;
import presentation.data.SessionCache;
import presentation.gui.CatalogListWindow;
import presentation.gui.OrdersWindow;
import presentation.gui.ProductDetailsWindow;
import presentation.gui.ProductListWindow;
import presentation.gui.ShoppingCartWindow;
import presentation.util.CacheReader;
import presentation.util.TableUtil;
import business.exceptions.BackendException;
//import rulesengine.OperatingException;
//import rulesengine.ReteWrapper;
//import rulesengine.ValidationException;
//import system.rulescore.rulesengine.*;
//import system.rulescore.rulesupport.*;
//import system.rulescore.util.*;
import business.exceptions.BusinessException;
import business.exceptions.RuleException;
import business.exceptions.UnauthorizedException;
import business.externalinterfaces.CartItem;
import business.externalinterfaces.Product;
import business.externalinterfaces.ShoppingCartSubsystem;
import business.shoppingcartsubsystem.CartItemImpl;
import business.shoppingcartsubsystem.ShoppingCartSubsystemFacade;
import business.usecasecontrol.BrowseAndSelectController;
import business.util.DataUtil;

public enum BrowseSelectUIControl {
	// Singleton
	INSTANCE;
	private static final Logger LOG = Logger.getLogger(BrowseSelectUIControl.class.getPackage().getName());
	private BrowseAndSelectController controller = new BrowseAndSelectController();
	// Windows that this controller manages
	// It's difficult to manage CatalogListWindow, so instead
	// we access CatalogListWindow statically
	// private CatalogListWindow catalogListWindow;
	private ProductListWindow productListWindow;
	private ProductDetailsWindow productDetailsWindow;
	private ShoppingCartWindow shoppingCartWindow;
	private OrdersWindow ordersWindow;
	private Stage primaryStage;
	private Callback startScreenCallback;

	public void setPrimaryStage(Stage ps, Callback callback) {
		primaryStage = ps;
		startScreenCallback = callback;
	}

	// Number of units requested when item is first added to cart
	private static final int INIT_QUANT_REQUESTED = 1;

	//// Handlers for browse and select portion of Start page
	private class OnlinePurchaseHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent evt) {
			try {
				CatalogListWindow catList = CatalogListWindow.getInstance(primaryStage,
						FXCollections.observableList(BrowseSelectData.INSTANCE.getCatalogList()));
				catList.show();
				primaryStage.hide();
			} catch (BackendException e) {
				LOG.log(Level.SEVERE, "Backend Exception occurred handling Browse&Select window", e);
				startScreenCallback.displayError("Database error. Message: " + e.getMessage());
				primaryStage.show();
			}
		}
	}

	private class RetrieveSavedCartHandler implements EventHandler<ActionEvent>, Callback {
		public void doUpdate() {
			try {
				Authorization.checkAuthorization(shoppingCartWindow, CacheReader.custIsAdmin());
			} catch (UnauthorizedException e) {
				LOG.log(Level.SEVERE, "Unauthorized Exception occurred retrieving saved cart", e);
				displayError(e.getMessage());
				return;
			}
			controller.retrieveSavedCart(BrowseSelectData.INSTANCE.obtainCurrentShoppingCartSubsystem(),
					CacheReader.readLoggedIn());
			BrowseSelectData.INSTANCE.updateCartData();
			primaryStage.hide();
			
			//Team //7/12/2016
			//change the retrivedFromShoppingCart in sessionChacahe to True so that we can delete it from databse
			//when the user submit order
			SessionCache.getInstance().add(SessionCache.RETRIVED_SHOPPING_CART, true);
			
			shoppingCartWindow.show();
		}

		public Text getMessageBar() {
			return startScreenCallback.getMessageBar();
		}

		@Override
		public void handle(ActionEvent evt) {
			shoppingCartWindow = ShoppingCartWindow.INSTANCE;
			boolean isLoggedIn = CacheReader.readLoggedIn();
			if (!isLoggedIn) {
				LoginUIControl loginControl = new LoginUIControl(shoppingCartWindow, primaryStage, this);
				loginControl.startLogin();
			} else {
				doUpdate();
			}
		}

	}

	public OnlinePurchaseHandler getOnlinePurchaseHandler() {
		return new OnlinePurchaseHandler();
	}

	public RetrieveSavedCartHandler getRetrieveSavedCartHandler() {
		return new RetrieveSavedCartHandler();
	}

	////////// Handlers for CatalogListWindow
	private class ViewProductsHandler implements EventHandler<ActionEvent> {

		@Override
		public void handle(ActionEvent evt) {

			TableView<CatalogPres> table = CatalogListWindow.getInstance().getTable();
			CatalogPres cat = table.getSelectionModel().getSelectedItem();
			if (cat == null) {
				CatalogListWindow.getInstance().displayError("Please select a row.");
			} else {
				try {
					BrowseSelectData.INSTANCE.setSelectedCatalog(cat);
					CatalogListWindow.getInstance().clearMessages();
					productListWindow = new ProductListWindow(cat);
					List<ProductPres> prods = BrowseSelectData.INSTANCE.getProductList(cat);
					productListWindow.setData(FXCollections.observableList(prods));
					CatalogListWindow.getInstance().hide();
					productListWindow.show();
				} catch (BackendException e) {
					LOG.log(Level.SEVERE, "Backend Exception occurred viewing products", e);
					CatalogListWindow.getInstance()
							.displayError("Unable to display list of products: " + e.getMessage());
				}
			}
		}
	}

	private class BackToPrimaryHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent evt) {
			startScreenCallback.clearMessages();
			primaryStage.show();
			CatalogListWindow.getInstance().hide();
		}
	}

	private class CatalogsToCartHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent evt) {
			ShoppingCartWindow.INSTANCE.show();
			CatalogListWindow.getInstance().hide();
		}
	}

	public ViewProductsHandler getViewProductsHandler() {
		return new ViewProductsHandler();
	}

	public BackToPrimaryHandler getBackToPrimaryHandler() {
		return new BackToPrimaryHandler();
	}

	public CatalogsToCartHandler getCatalogsToCartHandler() {
		return new CatalogsToCartHandler();
	}

	///////// Handlers for Product List Window
	private class BackToCatalogListHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent evt) {
			CatalogListWindow.getInstance().show();
			productListWindow.hide();
		}
	}

	private class ViewProductDetailsHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent evt) {
			TableView<ProductPres> table = productListWindow.getTable();
			ProductPres prod = table.getSelectionModel().getSelectedItem();
			if (prod == null) {
				productListWindow.displayError("Please select a row.");
			} else {
				BrowseSelectData.INSTANCE.setSelectedProduct(prod);
				productListWindow.clearMessages();
				productDetailsWindow = new ProductDetailsWindow(prod);
				productListWindow.hide();
				productDetailsWindow.show();
			}
		}
	}

	public BackToCatalogListHandler getBackToCatalogListHandler() {
		return new BackToCatalogListHandler();
	}

	public ViewProductDetailsHandler getViewProductDetailsHandler() {
		return new ViewProductDetailsHandler();
	}

	//////////// ProductDetailsWindow handlers
	private class BackToProductListHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent evt) {
			productListWindow.show();
			productDetailsWindow.hide();
		}
	}

	/**
	 * Converts new cart item data, which is input by user, into a CartItemPres
	 * object (data is put into a CartItemData which is inserted into
	 * CartItemPres) and this CartItemPres object is added to the top of the UI
	 * table of items. The cart in the ShoppingCartSubsystem is also updated
	 * with a new CartItem, obtained from the CartItemPres
	 */
	
	
	/**
	 * @author Bereket
	 *added a call back function to this handler
	 */
	private class AddToCartHandler implements EventHandler<ActionEvent>, Callback {

		boolean isLoggedIn = CacheReader.readLoggedIn();

		public void handle(ActionEvent evt) {
									
			if (!isLoggedIn) {
				LoginUIControl loginControl = new LoginUIControl(ShoppingCartWindow.INSTANCE,
						productDetailsWindow, this);
				loginControl.startLogin();
			} else {
				doUpdate();
			}

		}

		@Override
		public Text getMessageBar() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void doUpdate() {
			/* First create a CartItemPres from the user input */
			
			int quant = INIT_QUANT_REQUESTED; // = 1
			double unitPrice = Double
					.parseDouble(BrowseSelectData.INSTANCE.getSelectedProduct().unitPriceProperty().get());
			String name = BrowseSelectData.INSTANCE.getSelectedProduct().nameProperty().get();
			CartItemPres cartPres = BrowseSelectData.INSTANCE.cartItemPresFromData(name, unitPrice, quant);

			/* Then add it to the cart in the ShoppingCartSubsystem */
			BrowseSelectData.INSTANCE.addToCart(cartPres);
			shoppingCartWindow = ShoppingCartWindow.INSTANCE;
			shoppingCartWindow.setData(BrowseSelectData.INSTANCE.getCartData());
			shoppingCartWindow.setPrimaryStage(primaryStage);
			shoppingCartWindow.show();
			shoppingCartWindow.clearMessages();
			productDetailsWindow.hide();

		}
	}

	public void runShoppingCartRules() {

	}

	public void runQuantityRules(Product product, String quantityRequested) throws RuleException, BusinessException {
		controller.runQuantityRules(product, quantityRequested);
	}

	public BackToProductListHandler getBackToProductListHandler() {
		return new BackToProductListHandler();
	}

	public AddToCartHandler getAddToCartHandler() {
		return new AddToCartHandler();
	}

	//////////// Handlers for ShoppingCartWindow

	private class CartContinueHandler implements EventHandler<ActionEvent> {

		@Override
		public void handle(ActionEvent arg0) {
			try {
				CatalogListWindow window = CatalogListWindow.getInstance(primaryStage,
						FXCollections.observableList(BrowseSelectData.INSTANCE.getCatalogList()));
				window.clearMessages();
				shoppingCartWindow.hide();
				window.setTableAccessByRow();
				window.show();
			} catch (BackendException e) {
				LOG.log(Level.SEVERE, "Backend Exception occurred getting catalog information from DB", e);
				shoppingCartWindow.displayError("Database is unavailable. Please try again later.");
			}
		}
	}

	public CartContinueHandler getCartContinueHandler() {
		return new CartContinueHandler();
	}

	private class SaveCartHandler implements EventHandler<ActionEvent> {

		@Override
		public void handle(ActionEvent evt) {
			// shoppingCartWindow.displayInfo("You need to implement this
			// handler.");
			// List<CartItem> cartItems = new ArrayList<>();
			// //ObservableList<CartItemPres> cartData
			// //CartItemImpl(String productName,
			// //String quantity,
			// //String totalprice)
			// ShoppingCartSubsystemFacade shoppingCart = new
			// ShoppingCartSubsystemFacade();
			//
			// for(CartItemPres itemPres :
			// BrowseSelectData.INSTANCE.getCartData()){
			// try {
			// cartItems.add(new
			// CartItemImpl(itemPres.getCartItem().getItemName(),
			// itemPres.getCartItem().getQuantity()+"",
			// itemPres.getCartItem().getTotalPrice()+""));
			// } catch (BackendException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// }
			// shoppingCart.setCartItems(cartItems);
			//
			// try {
			// shoppingCart.saveLiveCart();
			// } catch (BackendException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// //System.out.println("testing..."+shoppingCart);

			BrowseSelectData.INSTANCE.updateShoppingCart();
			ShoppingCartSubsystem shopCartSs = BrowseSelectData.INSTANCE.obtainCurrentShoppingCartSubsystem();

			controller.updateShoppingCartItems(shopCartSs, shopCartSs.getCartItems());

			try {
				//shoppingCartWindow.clearMessages();
				shopCartSs.saveLiveCart();
				int numbItems = shoppingCartWindow.getCartItems().size();
				shoppingCartWindow.getMessageBar().setText(numbItems + " Items successfully saved");
			} catch (BackendException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public SaveCartHandler getSaveCartHandler() {
		return new SaveCartHandler();
	}

	/**
	 * Updates the view of cart items in the UI
	 */
	public void updateCartItems(ObservableList<CartItemPres> items) {
		if (shoppingCartWindow != null) {
			shoppingCartWindow.setData(items);
			TableUtil.refreshTable(shoppingCartWindow.getTable(),
					BrowseSelectData.INSTANCE.getShoppingCartSynchronizer());
		}
	}

}
