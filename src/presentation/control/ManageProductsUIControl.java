package presentation.control;

import presentation.data.CatalogPres;
import presentation.data.DefaultData;
import presentation.data.ManageProductsData;
import presentation.data.ProductPres;
import presentation.gui.AddCatalogPopup;
import presentation.gui.AddProductPopup;
import presentation.gui.MaintainCatalogsWindow;
import presentation.gui.MaintainProductsWindow;
import presentation.util.CacheReader;
import presentation.util.TableUtil;
import business.exceptions.BackendException;
import business.exceptions.UnauthorizedException;
import business.externalinterfaces.Catalog;
import business.externalinterfaces.Product;
import business.productsubsystem.ProductImpl;
import business.productsubsystem.ProductSubsystemFacade;
import business.usecasecontrol.ManageProductsController;
import business.util.Convert;
import business.util.DataUtil;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;


public enum ManageProductsUIControl {
	INSTANCE;
	private static final Logger LOG 
		= Logger.getLogger(ManageProductsUIControl.class.getSimpleName());
	private ManageProductsController controller = new ManageProductsController();
	private Stage primaryStage;
	private Callback startScreenCallback;

	public void setPrimaryStage(Stage ps, Callback returnMessage) {
		primaryStage = ps;
		startScreenCallback = returnMessage;
	}

	// windows managed by this class
	MaintainCatalogsWindow maintainCatalogsWindow;
	MaintainProductsWindow maintainProductsWindow;
	AddCatalogPopup addCatalogPopup;
	AddProductPopup addProductPopup;

	// Manage catalogs
	private class MaintainCatalogsHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			LOG.warning("Login is not being checked in MaintainCatalogsHandler.");
			maintainCatalogsWindow = new MaintainCatalogsWindow(primaryStage);
			ObservableList<CatalogPres> list = ManageProductsData.INSTANCE.getCatalogList();
			maintainCatalogsWindow.setData(list);
			primaryStage.hide();
			try {
				Authorization.checkAuthorization(maintainCatalogsWindow, CacheReader.custIsAdmin());
				//show this screen if user is authorized
				maintainCatalogsWindow.show();
			} catch(UnauthorizedException ex) {
				LOG.log(Level.WARNING, "Unauthorize Exception maintaing Catalog", e);
            	startScreenCallback.displayError(ex.getMessage());
            	maintainCatalogsWindow.hide();
            	primaryStage.show();           	
            }
		}
	}
	
	public MaintainCatalogsHandler getMaintainCatalogsHandler() {
		return new MaintainCatalogsHandler();
	}
	
	private class MaintainProductsHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			LOG.warning("Login is not being checked in MaintainProductsHandler.");
			maintainProductsWindow = new MaintainProductsWindow(primaryStage);
			CatalogPres selectedCatalog = ManageProductsData.INSTANCE.getSelectedCatalog();
			if(selectedCatalog != null) {
				ObservableList<ProductPres> list = ManageProductsData.INSTANCE.getProductsList(selectedCatalog);
				maintainProductsWindow.setData(ManageProductsData.INSTANCE.getCatalogList(), list);
			}
			maintainProductsWindow.show();  
	        primaryStage.hide();
	        LOG.warning("Authorization has not been implemented for Maintain Products");
		}
	}
	public MaintainProductsHandler getMaintainProductsHandler() {
		return new MaintainProductsHandler();
	}
	
	private class BackButtonHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent evt) {		
			maintainCatalogsWindow.clearMessages();		
			maintainCatalogsWindow.hide();
			startScreenCallback.clearMessages();
			primaryStage.show();
		}
			
	}
	public BackButtonHandler getBackButtonHandler() {
		return new BackButtonHandler();
	}
	
	private class BackFromProdsButtonHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent evt) {		
			maintainProductsWindow.clearMessages();		
			maintainProductsWindow.hide();
			startScreenCallback.clearMessages();
			primaryStage.show();
		}			
	}
	
	public BackFromProdsButtonHandler getBackFromProdsButtonHandler() {
		return new BackFromProdsButtonHandler();
	}
	
	//////new
	/* Handles the request to delete selected row in catalogs table */
	private class DeleteCatalogHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent evt) {
			TableView<CatalogPres> table = maintainCatalogsWindow.getTable();
			ObservableList<CatalogPres> tableItems = table.getItems();
		    ObservableList<Integer> selectedIndices = table.getSelectionModel().getSelectedIndices();
		    ObservableList<CatalogPres> selectedItems = table.getSelectionModel()
					.getSelectedItems();
		    List<CatalogPres> selectTempCatalogPres = new ArrayList<>(selectedItems);
		    for(CatalogPres cPres:selectedItems){
		    	selectTempCatalogPres.add(cPres);
    		}
		    if(tableItems.isEmpty()) {
		    	maintainCatalogsWindow.displayError("Nothing to delete!");
		    } else if (selectedIndices == null || selectedIndices.isEmpty()) {
		    	maintainCatalogsWindow.displayError("Please select a row.");
		    } else {
		    	try {
		    		for(CatalogPres cPres:selectTempCatalogPres)
		    			controller.deleteCatalog(cPres.getCatalog());
				} catch (BackendException e) {
					e.printStackTrace();
					LOG.log(Level.SEVERE, "Backend Exception deleting a Product to DB", e);
				}
		    	boolean result =  ManageProductsData.INSTANCE.removeFromCatalogList(selectedItems);
			    if(result) {
			    	table.setItems(ManageProductsData.INSTANCE.getCatalogList());
			    	//maintainCatalogsWindow.displayError(
			    		//"Selected catalog still needs to be deleted from database!");  	
			    } else {
			    	maintainCatalogsWindow.displayInfo("No items deleted.");
			    }
		    }
		}			
	}
	
	public DeleteCatalogHandler getDeleteCatalogHandler() {
		return new DeleteCatalogHandler();
	}
	
	/* Produces an AddCatalog popup in which user can add new catalog data */
	private class AddCatalogHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent evt) {
			addCatalogPopup = new AddCatalogPopup();
			addCatalogPopup.show(maintainCatalogsWindow);	
		}
	}
	public AddCatalogHandler getAddCatalogHandler() {
		return new AddCatalogHandler();
	} 
	
	/* Invoked by AddCatalogPopup - reads user input for a new catalog to be added to db */
	private class AddNewCatalogHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent evt) {
			//validate input
			TextField nameField = addCatalogPopup.getNameField();
			String catName = nameField.getText();
			if(catName.equals("")) 
				addCatalogPopup.displayError("Name field must be nonempty!");
			else {
				try {
					int newCatId = controller.saveNewCatalog(catName);
					CatalogPres catPres = ManageProductsData.INSTANCE.catalogPresFromData(newCatId, catName);
					ManageProductsData.INSTANCE.addToCatalogList(catPres);
					maintainCatalogsWindow.setData(ManageProductsData.INSTANCE.getCatalogList());
					TableUtil.refreshTable(maintainCatalogsWindow.getTable(), 
							ManageProductsData.INSTANCE.getManageCatalogsSynchronizer());
					addCatalogPopup.clearMessages();
					addCatalogPopup.hide();
				} catch(BackendException e) {
					LOG.log(Level.SEVERE, "Backend Exception adding New Catalog to DB", e);
					addCatalogPopup.displayError("A database error has occurred. Check logs and try again later.");
				}
			}	
		}
	}
	
	public AddNewCatalogHandler getAddNewCatalogHandler() {
		return new AddNewCatalogHandler();
	} 

	/* Handles the request to delete selected row in catalogs table */
	private class DeleteProductHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent evt) {
			CatalogPres selectedCatalog = ManageProductsData.INSTANCE.getSelectedCatalog();
		    ObservableList<ProductPres> tableItems = ManageProductsData.INSTANCE.getProductsList(selectedCatalog);
			TableView<ProductPres> table = maintainProductsWindow.getTable();
			ObservableList<Integer> selectedIndices = table.getSelectionModel().getSelectedIndices();
		    ObservableList<ProductPres> selectedItems = table.getSelectionModel().getSelectedItems();
		    List<ProductPres> selectTempProductPres = new ArrayList<>(selectedItems);
		    for(ProductPres pPres:selectedItems){
		    	selectTempProductPres.add(pPres);
    		}
		    if(tableItems.isEmpty()) {
		    	maintainProductsWindow.displayError("Nothing to delete!");
		    } else if (selectedIndices == null || selectedIndices.isEmpty()) {
		    	maintainProductsWindow.displayError("Please select a row.");
		    } else {
		    	try {
		    		for(ProductPres pPres:selectTempProductPres)
		    			controller.deleteProduct(pPres.getProduct());
				} catch (BackendException e) {
					e.printStackTrace();
					LOG.log(Level.SEVERE, "Backend Exception deleting a Product to DB", e);
				}
		    	boolean result 
		    	    =  ManageProductsData.INSTANCE.removeFromProductList(selectedCatalog, selectedItems);
			    if(result) {
			    	table.setItems(ManageProductsData.INSTANCE.getProductsList(selectedCatalog));
			    	TableUtil.refreshTable(maintainProductsWindow.getTable(), 
							ManageProductsData.INSTANCE.getManageProductsSynchronizer());
			    	//maintainProductsWindow.displayError("Product still needs to be deleted from db!");
			    } else {
			    	maintainProductsWindow.displayInfo("No items deleted.");
			    }
				
		    }			
		}			
	}
	
	public DeleteProductHandler getDeleteProductHandler() {
		return new DeleteProductHandler();
	}
	
	/* Produces an AddProduct popup in which user can add new product data */
	private class AddProductHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent evt) {
			addProductPopup = new AddProductPopup();
			String catNameSelected 
			   = ManageProductsData.INSTANCE.getSelectedCatalog().getCatalog().getName();
			addProductPopup.setCatalog(catNameSelected);
			addProductPopup.show(maintainProductsWindow);
		}
	}
	public AddProductHandler getAddProductHandler() {
		return new AddProductHandler();
	} 
	
	/* Invoked by AddCatalogPopup - reads user input for a new catalog to be added to db */
	private class AddNewProductHandler implements EventHandler<ActionEvent> {
		public void handle(ActionEvent evt) {
			String name = addProductPopup.getName().getText();
			String date = addProductPopup.getManufactureDate().getText();
			Integer numAvail = Integer.parseInt(addProductPopup.getNumAvail().getText());
			Double price = Double.parseDouble(addProductPopup.getUnitPrice().getText());
			String description = addProductPopup.getDescription().getText();
			//validate input (better to implement in rules engine
			if(name.trim().equals("")) 
				addProductPopup.displayError("Product Name field must be nonempty!");
			else if(date.trim().equals("")) 
				addProductPopup.displayError("Manufacture Date field must be nonempty!");
			else if(!date.matches("\\d{4}-\\d{2}-\\d{2}"))
				addProductPopup.displayError("Please use correct Manufacture Date format (mm/dd/yyyy)");
			else if(numAvail.equals("")) 
				addProductPopup.displayError("Number in Stock field must be nonempty!");
			else if(price.equals("")) 
				addProductPopup.displayError("Unit Price field must be nonempty!");
			else if(description.trim().equals("")) 
				addProductPopup.displayError("Description field must be nonempty!");
			else {
				//code this as in AddNewCatalogHandler (above)
				Catalog selectedCatalog = ManageProductsData.INSTANCE.getSelectedCatalog().getCatalog();
				Product prod = ManageProductsData.INSTANCE.productPresFromData(selectedCatalog, name, date, numAvail, price).getProduct();
				CatalogPres catPres = ManageProductsData.INSTANCE.catalogPresFromData(selectedCatalog.getId(), selectedCatalog.getName());
				try {
					int newProdId = controller.saveNewProduct(prod, selectedCatalog);
					
					prod = new ProductImpl(selectedCatalog, newProdId,
							name,
							numAvail,
							price,
							Convert.localDateForString(date),
							description);
					ProductPres prodPres = new ProductPres();
					prodPres.setProduct(prod);
					
					ManageProductsData.INSTANCE.addToProdList(catPres, prodPres);
					maintainProductsWindow.setData(ManageProductsData.INSTANCE.getProductsList(catPres));
					TableUtil.refreshTable(maintainProductsWindow.getTable(),
							ManageProductsData.INSTANCE.getManageProductsSynchronizer());
					addProductPopup.clearMessages();
					addProductPopup.hide();
				} catch (BackendException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					LOG.log(Level.SEVERE, "Backend Exception adding New Product to DB", e);
					addProductPopup.displayError("A database error has occurred. Check logs and try again later.");
				}
				//addProductPopup.displayInfo("You need to implement this!");
			}	
		}
		
	}
	public AddNewProductHandler getAddNewProductHandler() {
		return new AddNewProductHandler();
	} 

}
