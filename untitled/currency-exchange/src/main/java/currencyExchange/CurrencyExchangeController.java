package currencyExchange;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@RestController
public class CurrencyExchangeController {
	
	@Autowired
	private CurrencyExchangeRepository repo;
	
	@Autowired 
	private Environment environment;

	@GetMapping("/currency-exchange")
	public List<CurrencyExchange> getAll(){
		return repo.findAll();
	}

	//localhost:8000/currency-exchange/from/EUR/to/RSD

	@GetMapping("/currency-exchange/from/{from}/to/{to}")
	public ResponseEntity<CurrencyExchange> getExchange(@PathVariable("from") String from, @PathVariable("to") String to) throws Exception {
		try {
			String port = environment.getProperty("local.server.port");

			CurrencyExchange currencyExchangeData = repo.findByFromAndToIgnoreCase(from, to);

			if (currencyExchangeData == null) {
				throw new CustomExceptions.EntityDoesntExistException("Data not found!");
			}

			CurrencyExchange exchange = new CurrencyExchange(currencyExchangeData.getId(), from, to, currencyExchangeData.getConversionMultiple(), port);
			return ResponseEntity.ok().body(exchange);
		} catch (CustomExceptions.EntityDoesntExistException e){
			throw e;
		} catch (Exception e){
			throw new Exception(e.getMessage());
		}
	}
}
