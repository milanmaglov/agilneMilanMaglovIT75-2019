package currencyConversion;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(name = "bank-account")
public interface BankAccountProxy {

    @GetMapping("/bank-account/get/{email}")
    public ResponseEntity<BankAccountDto> getBankAccount(@PathVariable String email);
    @PutMapping("/bank-account/update/user/{email}/subtract/{quantityS}from/{currS}/add/{quantityA}to/{currA}")
    public ResponseEntity<BankAccountDto> updateBankAccountBalance(@PathVariable String email, @PathVariable BigDecimal quantityS,
                                                                   @PathVariable String currS, @PathVariable BigDecimal quantityA,
                                                                   @PathVariable String currA);

}
