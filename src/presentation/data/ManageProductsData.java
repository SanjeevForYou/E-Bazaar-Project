package presentation.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import presentation.util.UtilForUIClasses;
import business.customersubsystem.CustomerSubsystemFacade;
import business.exceptions.BackendException;
import business.externalinterfaces.Catalog;
import business.externalinterfaces.Product;
import business.externalinterfaces.ProductSubsystem;
import business.util.Convert;
import business.productsubsystem.ProductSubsystemFacade;

public enum ManageProductsData {
	INSTANCE;
	private static final Logger LOG = Logger.getLogger(ManageProductsData.class.getName());
	private CatalogPres defaultCatalog = readDefaultCatalogFromDataSource();
	private CatalogPres readDefaultCatalogFromDataSource() {
		
		//Bandeshor// 7/7/2016
		ProductSubsystem productSub = new ProductSubsystemFacade();
		try {
			return UtilForUIClasses.catalogToCatalogPres(productSub.getCatalogList().get(0));
		} catch (BackendException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return DefaultData.CATALOG_LIST_DATA.get(0);
	}
	public CatalogPres getDefaultCatalog() {
		return defaultCatalog;
	}
	
	private CatalogPres selectedCatalog = defaultCatalog;
	public void setSelectedCatalog(CatalogPres selCatalog) {
		selectedCatalog = selCatalog;
	}
	public CatalogPres getSelectedCatalog() {
		return selectedCatalog;
	}
	//////////// Products List model
	private ObservableMap<CatalogPres, List<ProductPres>> productsMap
	   = readProductsFromDataSource();
	
	/** Initializes the productsMap */
	private ObservableMap<CatalogPres, List<ProductPres>> readProductsFromDataSource() {
		
		//Bandeshor// 7/7/2016
		ProductSubsystem productSub = new ProductSubsystemFacade();
		try {
			ObservableMap<CatalogPres,List<ProductPres>> retMap = FXCollections.observableHashMap();
			List<Catalog> catalogList = productSub.getCatalogList();
			for(Catalog catalog:catalogList){
				
				List<Product> productList=productSub.getProductList(catalog);
				List<ProductPres> productPresList = productList.stream().map(product -> {
					ProductPres pres= new ProductPres();
					pres.setProduct(product);
					return pres;
				}).collect(Collectors.toList());
				
				CatalogPres catPres = UtilForUIClasses.catalogToCatalogPres(catalog);
				retMap.put(catPres, productPresList);	
			}
			return retMap;
		} catch (BackendException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return DefaultData.PRODUCT_LIST_DATA;
	}
	
	/** Delivers the requested products list to the UI */
	public ObservableList<ProductPres> getProductsList(CatalogPres catPres) {
		return FXCollections.observableList(productsMap.get(catPres));
	}
	
	public ProductPres productPresFromData(Catalog c, String name, String date,  //MM/dd/yyyy 
			int numAvail, double price) {
		
		Product product = ProductSubsystemFacade.createProduct(c, name, 
				Convert.localDateForString(date), numAvail, price);
		ProductPres prodPres = new ProductPres();
		prodPres.setProduct(product);
		return prodPres;
	}
	
	public void addToProdList(CatalogPres catPres, ProductPres prodPres) {
		ObservableList<ProductPres> newProducts =
		           FXCollections.observableArrayList(prodPres);
		List<ProductPres> specifiedProds = productsMap.get(catPres);
		
		//Place the new item at the bottom of the list
		specifiedProds.addAll(newProducts);
		
	}
	
	/** This method looks for the 0th element of the toBeRemoved list 
	 *  and if found, removes it. In this app, removing more than one product at a time
	 *  is  not supported.
	 */
	public boolean removeFromProductList(CatalogPres cat, ObservableList<ProductPres> toBeRemoved) {
		if(toBeRemoved != null && !toBeRemoved.isEmpty()) {
			boolean result = productsMap.get(cat).remove(toBeRemoved.get(0));
			return result;
		}
		return false;
	}
		
	//////// Catalogs List model
	private ObservableList<CatalogPres> catalogList = readCatalogsFromDataSource();

	/** Initializes the catalogList */
	private ObservableList<CatalogPres> readCatalogsFromDataSource() {
		
		//Bandeshor// 7/7/2016 
		ProductSubsystem productSub = new ProductSubsystemFacade();
		try {
			List<Catalog> catlogs = productSub.getCatalogList();
			List<CatalogPres> catlogPres = catlogs.stream()
					.map(ctg ->{ return UtilForUIClasses.catalogToCatalogPres(ctg);})
					.collect(Collectors.toList());
			return FXCollections.observableList(catlogPres);
		} catch (BackendException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return FXCollections.observableList(DefaultData.CATALOG_LIST_DATA);
		}
		
		
	}

	/** Delivers the already-populated catalogList to the UI */
	public ObservableList<CatalogPres> getCatalogList() {
		return catalogList;
	}

	public CatalogPres catalogPresFromData(int id, String name) {
		Catalog cat = ProductSubsystemFacade.createCatalog(id, name);
		CatalogPres catPres = new CatalogPres();
		catPres.setCatalog(cat);
		return catPres;
	}

	public void addToCatalogList(CatalogPres catPres) {
		ObservableList<CatalogPres> newCatalogs = FXCollections
				.observableArrayList(catPres);

		// Place the new item at the bottom of the list
		// catalogList is guaranteed to be non-null
		boolean result = catalogList.addAll(newCatalogs);
		if(result) { //must make this catalog accessible in productsMap
			productsMap.put(catPres, FXCollections.observableList(new ArrayList<ProductPres>()));
		}
	}

	/**
	 * This method looks for the 0th element of the toBeRemoved list in
	 * catalogList and if found, removes it. In this app, removing more than one
	 * catalog at a time is not supported.
	 * 
	 * This method also updates the productList by removing the products that
	 * belong to the Catalog that is being removed.
	 * 
	 * Also: If the removed catalog was being stored as the selectedCatalog,
	 * the next item in the catalog list is set as "selected"
	 */
	public boolean removeFromCatalogList(ObservableList<CatalogPres> toBeRemoved) {
		boolean result = false;
		CatalogPres item = toBeRemoved.get(0);
		if (toBeRemoved != null && !toBeRemoved.isEmpty()) {
			result = catalogList.remove(item);
		}
		if(item.equals(selectedCatalog)) {
			if(!catalogList.isEmpty()) {
				selectedCatalog = catalogList.get(0);
			} else {
				selectedCatalog = null;
			}
		}
		if(result) {//update productsMap
			productsMap.remove(item);
		}
		return result;
	}
	
	//Synchronizers
	public class ManageProductsSynchronizer implements Synchronizer {
		@SuppressWarnings("rawtypes")
		@Override
		public void refresh(ObservableList list) {
			productsMap.put(selectedCatalog, list);
		}
	}
	public ManageProductsSynchronizer getManageProductsSynchronizer() {
		return new ManageProductsSynchronizer();
	}
	
	private class ManageCatalogsSynchronizer implements Synchronizer {
		@SuppressWarnings("rawtypes")
		@Override
		public void refresh(ObservableList list) {
			catalogList = list;
		}
	}
	public ManageCatalogsSynchronizer getManageCatalogsSynchronizer() {
		return new ManageCatalogsSynchronizer();
	}
}
