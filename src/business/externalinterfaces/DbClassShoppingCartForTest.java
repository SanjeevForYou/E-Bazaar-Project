package business.externalinterfaces;

import middleware.exceptions.DatabaseException;
import middleware.externalinterfaces.DbClass;

public interface DbClassShoppingCartForTest extends DbClass {

	Integer saveLiveCart(CustomerProfile custProfile, ShoppingCart liveCart) throws DatabaseException;
}
