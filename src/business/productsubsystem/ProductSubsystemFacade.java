package business.productsubsystem;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import business.exceptions.BackendException;
import business.externalinterfaces.Catalog;
import business.externalinterfaces.CatalogTypes;
import business.externalinterfaces.DbClassCatalogTypesForTest;
import business.externalinterfaces.Product;
import business.externalinterfaces.ProductSubsystem;
import business.util.TwoKeyHashMap;
import middleware.exceptions.DatabaseException;

public class ProductSubsystemFacade implements ProductSubsystem {
	private static final Logger LOG = 
			Logger.getLogger(ProductSubsystemFacade.class.getPackage().getName());
	public static Catalog createCatalog(int id, String name) {
		return new CatalogImpl(id, name);
	}
	public static Product createProduct(Catalog c, String name, 
			LocalDate date, int numAvail, double price) {
		return new ProductImpl(c, name, date, numAvail, price);
	}
	public static Product createProduct(Catalog c, Integer pi, String pn, int qa, 
			double up, LocalDate md, String desc) {
		return new ProductImpl(c, pi, pn, qa, up, md, desc);
	}
	
	/** obtains product for a given product name */
    public Product getProductFromName(String prodName) throws BackendException {
    	try {
			DbClassProduct dbclass = new DbClassProduct();
			return dbclass.readProduct(getProductIdFromName(prodName));
		} catch(DatabaseException e) {
			LOG.log(Level.SEVERE, "DB Exception occurred while getting product by name", e);
			throw new BackendException(e);
		}	
    }
    public Integer getProductIdFromName(String prodName) throws BackendException {
		try {
			DbClassProduct dbclass = new DbClassProduct();
			TwoKeyHashMap<Integer,String,Product> table = dbclass.readProductTable();
			return table.getFirstKey(prodName);
		} catch(DatabaseException e) {
			LOG.log(Level.SEVERE, "DB Exception occurred while getting product by ID from name", e);
			throw new BackendException(e);
		}
		
	}
    public Product getProductFromId(Integer prodId) throws BackendException {
		try {
			DbClassProduct dbclass = new DbClassProduct();
			return dbclass.readProduct(prodId);
		} catch(DatabaseException e) {
			LOG.log(Level.SEVERE, "DB Exception occured while getting product by ID from name", e);
			throw new BackendException(e);
		}
	}
    public CatalogTypes getCatalogTypes() throws BackendException {
    	try {
			DbClassCatalogTypes dbClass = new DbClassCatalogTypes();
			return dbClass.getCatalogTypes();
		} catch(DatabaseException e) {
			LOG.log(Level.SEVERE, "DB Exception occurred while getting catalog types", e);
			throw new BackendException(e);
		}
    }
    
    public List<Catalog> getCatalogList() throws BackendException {
    	try {
			DbClassCatalogTypes dbClass = new DbClassCatalogTypes();
			return dbClass.getCatalogTypes().getCatalogs();
		} catch(DatabaseException e) {
			LOG.log(Level.SEVERE, "DB Exception occured while getting catalog list", e);
			throw new BackendException(e);
		}
    }
    
    public List<Product> getProductList(Catalog catalog) throws BackendException {
    	try {
    		DbClassProduct dbclass = new DbClassProduct();
    		return dbclass.readProductList(catalog);
    	} catch(DatabaseException e) {
    		LOG.log(Level.SEVERE, "DB Exception occurred while getting product list", e);
    		throw new BackendException(e);
    	}
    }
    
    /*  @added by Claudio  */
	public int readQuantityAvailable(Product product) throws BackendException {
		try {
			DbClassProduct dbclass = new DbClassProduct();
			return dbclass.readProduct(product.getProductId()).getQuantityAvail();
		} catch (DatabaseException e) {
			LOG.log(Level.SEVERE, "DB Exception occurred while getting product quantity available", e);
			throw new BackendException(e);
		}
	}
	
	public int saveNewCatalog(String catalogName) throws BackendException {
		try {
			DbClassCatalog dbclass = new DbClassCatalog();
			return dbclass.saveNewCatalog(catalogName);
		} catch(DatabaseException e) {
			LOG.log(Level.SEVERE, "DB Exception occurred while saving new catalog", e);
    		throw new BackendException(e);
    	}
	}
	@Override
	public Catalog getCatalogFromName(String catName) throws BackendException {  //add try-catch
		CatalogTypesImpl cat = new CatalogTypesImpl();
		int catId = cat.getCatalogId(catName);
		return cat.getCatalogs().get(catId);
	}
	
	@Override
	public int saveNewProduct(Product product, Catalog catalog) throws BackendException {
		try {
			DbClassProduct dbclass = new DbClassProduct();
			return dbclass.saveNewProduct(product, catalog);
		} catch (DatabaseException e) {
			LOG.log(Level.SEVERE, "Database Exception occurred while saving New Product", e);
			throw new BackendException(e);
		}
	}
	
	@Override
	public void deleteProduct(Product product) throws BackendException {
		try {
			DbClassProduct dbclass = new DbClassProduct();
			CatalogImpl cat = new CatalogImpl();
			dbclass.readProductList(cat).remove(product);
			dbclass.deleteProduct(product);
		} catch(DatabaseException e) {
			LOG.log(Level.SEVERE, "Database Exception occurred while deleting Product", e);
		  	throw new BackendException(e);
		}
	}
	
	@Override
	public void deleteCatalog(Catalog catalog) throws BackendException {
		try {
			DbClassCatalog dbclass = new DbClassCatalog();
			dbclass.deleteCatalog(catalog.getName());
		} catch(DatabaseException e) {
			LOG.log(Level.SEVERE, "Database Exception occurred while deleting catalog", e);
		  	throw new BackendException(e);
		}
	}
	
	/** Testing */
	@Override
	public DbClassCatalogTypesForTest getGenericDbClassCatalogs() {
		return new DbClassCatalogTypes();
	};
	
	@Override
	public Catalog getGenericCatalogTypes() {
		return new CatalogImpl(1,"CatalogTest");
	}
}
