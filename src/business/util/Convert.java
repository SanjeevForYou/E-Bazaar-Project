package business.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import business.externalinterfaces.CartItem;
import business.externalinterfaces.OrderItem;
import business.externalinterfaces.OrderSubsystem;
import business.ordersubsystem.OrderSubsystemFacade;

public class Convert {
	public static final String DATE_PATTERN = "MM/dd/yyyy";
	public static final String DATE_PATTERN_ALT = "MM/dd/yy";

	public static LocalDate localDateForString(String date) {  //pattern: "MM/dd/yyyy"
		LocalDate retval = null;
		try {
			retval = LocalDate.parse(date, DateTimeFormatter.ofPattern(DATE_PATTERN));
			return retval;
		} catch (Exception e) {
			return LocalDate.parse(date, DateTimeFormatter.ofPattern(DATE_PATTERN_ALT));
		}
			
	}
	
	public static String localDateAsString(LocalDate date) {  //pattern: "MM/dd/yyyy"
		return date.format(DateTimeFormatter.ofPattern(DATE_PATTERN));
	}
	
	/**Convert OrderItem object to CartItem object
	 * Added by sanjeev on july 7
	 * @param cartitem
	 * @return OrderItem
	 */
	public static OrderItem createOrderItemFromCartItem(CartItem cartitem)
	{
		return OrderSubsystemFacade.createOrderItem(cartitem.getProductid(), 1, cartitem.getQuantity(), cartitem.getTotalprice()); //orderid , is not known in cartitem
	}
}
