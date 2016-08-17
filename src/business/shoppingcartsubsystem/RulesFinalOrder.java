package business.shoppingcartsubsystem;

import java.util.HashMap;
import java.util.List;

import business.exceptions.BusinessException;
import business.exceptions.RuleException;
import business.externalinterfaces.DynamicBean;
import business.externalinterfaces.Rules;
import business.externalinterfaces.RulesSubsystem;
import business.externalinterfaces.ShoppingCart;
import business.externalinterfaces.RulesConfigKey;
import business.externalinterfaces.RulesConfigProperties;
import business.rulesbeans.FinalOrderBean;
import business.rulesbeans.ShopCartBean;
import business.rulesubsystem.RulesSubsystemFacade;

/**
 * This Rules class is different from RulesShoppingCart
 * (even though it checks aspects of a Shopping Cart)
 * because it needs to execute at a different time during
 * execution of the application (namely, when final order
 * is submitted)
 */
public class RulesFinalOrder implements Rules{
//Bandeshor 7/8/2016
	private HashMap<String,DynamicBean> table;
	private DynamicBean bean;	
	private RulesConfigProperties config = new RulesConfigProperties();
	
	public RulesFinalOrder(ShoppingCart shoppingcart){
		bean = new FinalOrderBean(shoppingcart);
	}
	
	@Override
	public String getModuleName() {
		return config.getProperty(RulesConfigKey.FINAL_ORDER_MODULE.getVal());
	}

	@Override
	public String getRulesFile() {
		return config.getProperty(RulesConfigKey.FINAL_ORDER_RULES_FILE.getVal());
	}

	@Override
	public void prepareData() {
		
		table = new HashMap<String,DynamicBean>();		
		String deftemplate = config.getProperty(RulesConfigKey.FINAL_ORDER_DEFTEMPLATE.getVal());
		table.put(deftemplate, bean);
	}

	@Override
	public HashMap<String, DynamicBean> getTable() {
		
		return table;
	}

	@Override
	public void runRules() throws BusinessException, RuleException {
		
		RulesSubsystem rules = new RulesSubsystemFacade();
    	rules.runRules(this);
	}

	@Override
	public void populateEntities(List<String> updates) {
		
		//do nothing according to other Rules classes
	}

	@Override
	public List<?> getUpdates() {
		
		return null;
	}

	
}
