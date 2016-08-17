package business.externalinterfaces;

import middleware.exceptions.DatabaseException;

public interface DbClassCustomerProfileForTest {
	public String[] getUserInfoForTest(CustomerProfile customerProfile) throws DatabaseException;
}
