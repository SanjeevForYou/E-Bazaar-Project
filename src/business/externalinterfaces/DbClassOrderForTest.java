package business.externalinterfaces;

import java.util.List;

import middleware.exceptions.DatabaseException;

public interface DbClassOrderForTest{

	List<Integer> getAllOrderIdsForTest(CustomerProfile cust) throws DatabaseException;
}
