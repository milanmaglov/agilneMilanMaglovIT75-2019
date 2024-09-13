package currencyConversion;

import java.math.BigDecimal;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrencyConversionController {
	
	@Autowired
	private CurrencyExchangeProxy currencyExchangeProxy;

	@Autowired
	private BankAccountProxy bankAccountProxy;

	//localhost:8100/currency-conversion/from/EUR/to/RSD/quantity/100

	/*
	@GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
	public CurrencyConversion getConversion
		(@PathVariable String from, @PathVariable String to, @PathVariable double quantity) {
		
		HashMap<String,String> uriVariables = new HashMap<String,String>();
		uriVariables.put("from", from);
		uriVariables.put("to", to);
		
		ResponseEntity<CurrencyConversion> response = 
				new RestTemplate().
				getForEntity("http://localhost:8000/currency-exchange/from/{from}/to/{to}",
						CurrencyConversion.class, uriVariables);
		
		CurrencyConversion cc = response.getBody();
		
		return new CurrencyConversion(from,to,cc.getConversionMultiple(), cc.getEnvironment(), quantity,
				cc.getConversionMultiple().multiply(BigDecimal.valueOf(quantity)));
	}
	
	//localhost:8100/currency-conversion?from=EUR&to=RSD&quantity=50
	@GetMapping("/currency-conversion")
	public ResponseEntity<?> getConversionParams(@RequestParam String from, @RequestParam String to, @RequestParam double quantity) {
		
		HashMap<String,String> uriVariable = new HashMap<String, String>();
		uriVariable.put("from", from);
		uriVariable.put("to", to);
		
		try {
		ResponseEntity<CurrencyConversion> response = new RestTemplate().
				getForEntity("http://localhost:8000/currency-exchange/from/{from}/to/{to}", CurrencyConversion.class, uriVariable);
		CurrencyConversion responseBody = response.getBody();
		return ResponseEntity.status(HttpStatus.OK).body(new CurrencyConversion(from,to,responseBody.getConversionMultiple(),responseBody.getEnvironment(),
				quantity, responseBody.getConversionMultiple().multiply(BigDecimal.valueOf(quantity))));
		}
		catch(HttpClientErrorException e) {
			return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
		}
	}
	
	//localhost:8100/currency-conversion-feign?from=EUR&to=RSD&quantity=50
	@GetMapping("/currency-conversion-feign")
	public ResponseEntity<?> getConversionFeign(@RequestParam String from, @RequestParam String to, @RequestParam double quantity){
		
		try {
			ResponseEntity<CurrencyConversion> response = currencyExchangeProxy.getExchange(from, to);
			CurrencyConversion responseBody = response.getBody();
			return ResponseEntity.ok(new CurrencyConversion(from,to,responseBody.getConversionMultiple(),responseBody.getEnvironment()+" feign",
				quantity, responseBody.getConversionMultiple().multiply(BigDecimal.valueOf(quantity))));
		}catch(FeignException e) {
			return ResponseEntity.status(e.status()).body(e.getMessage());
		}
	}
	
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<String> handleMissingParams(MissingServletRequestParameterException ex) {
	    String parameter = ex.getParameterName();
	    //return ResponseEntity.status(ex.getStatusCode()).body(ex.getMessage());
	    return ResponseEntity.status(ex.getStatusCode()).body("Value [" + ex.getParameterType() + "] of parameter [" + parameter + "] has been ommited");
	}
	*/

	@GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
	@RateLimiter(name = "default")
	public ResponseEntity<?> getConversionFeign(@PathVariable String from, @PathVariable String to, @PathVariable double quantity, HttpServletRequest request) throws Exception {
		try {
			String email = request.getHeader("User-Email");

			BankAccountDto userBankAccount = bankAccountProxy.getBankAccount(email).getBody();

			BigDecimal availableAmount = switch (from) {
                case "RSD" -> userBankAccount.getRSD_amount();
                case "USD" -> userBankAccount.getUSD_amount();
                case "EUR" -> userBankAccount.getEUR_amount();
                case "CHF" -> userBankAccount.getCHF_amount();
                case "GBP" -> userBankAccount.getGBP_amount();
                default -> throw new CustomExceptions.InvalidRequestParameterValueException("Currency not supported.");
            };

            if (availableAmount.compareTo(BigDecimal.valueOf(quantity)) >= 0) {
				return getResponseEntity(from, to, quantity, email, currencyExchangeProxy, bankAccountProxy);
			} else {
				throw new CustomExceptions.InsufficientFundsException("User doesn't have the given amount of " + from + " currency on their account. Account amount is " + availableAmount + " and the specified amount is " + quantity);
			}
		} catch (CustomExceptions.EntityDoesntExistException | CustomExceptions.InvalidRequestParameterValueException e) {
			throw e;
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	private static ResponseEntity<?> getResponseEntity(@PathVariable String from, @PathVariable String to, @PathVariable double quantity, String email, CurrencyExchangeProxy currencyExchangeProxy, BankAccountProxy bankAccountProxy) throws CustomExceptions.EntityDoesntExistException {
		ResponseEntity<CurrencyConversion> response = currencyExchangeProxy.getExchange(from, to);
		if (response == null) {
			throw new CustomExceptions.EntityDoesntExistException("Currency exchange data not found!");
		}
		CurrencyConversion responseBody = response.getBody();
		CurrencyConversion newConversion = new CurrencyConversion(from, to, responseBody.getConversionMultiple(), responseBody.getEnvironment() + " feign",
				quantity, responseBody.getConversionMultiple().multiply(BigDecimal.valueOf(quantity)));
		BankAccountDto updatedBalance = bankAccountProxy.updateBankAccountBalance(email, BigDecimal.valueOf(quantity), from, newConversion.getConversionTotal(), to).getBody();
		return ResponseEntity.ok().body(
				"User : " + email + System.lineSeparator() +
						"RSD amount : " + updatedBalance.getRSD_amount().toString() + System.lineSeparator() +
						"USD amount : " + updatedBalance.getUSD_amount().toString() + System.lineSeparator() +
						"GBP amount : " + updatedBalance.getGBP_amount().toString() + System.lineSeparator() +
						"CHF amount : " + updatedBalance.getCHF_amount().toString() + System.lineSeparator() +
						"EUR amount : " + updatedBalance.getEUR_amount().toString() + System.lineSeparator() +
						"User successfully converted " + quantity
						+ " of " + from + " to " + newConversion.getConversionTotal() + " of " + to);
	}
	
}
