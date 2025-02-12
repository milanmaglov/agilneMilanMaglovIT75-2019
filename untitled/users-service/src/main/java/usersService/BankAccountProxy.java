package usersService;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import usersService.model.BankAccountDto;

@FeignClient(name = "bank-account")
public interface BankAccountProxy {
    @PostMapping("bank-account/create/{email}")
    public ResponseEntity<BankAccountDto> createBankAccount(@PathVariable String email);

    @PutMapping("/bank-account/update/{oldEmail}/for/{newEmail}")
    public ResponseEntity<BankAccountDto> updateBankAccountEmail(@PathVariable String oldEmail, @PathVariable String newEmail);

    @DeleteMapping("/bank-account/delete/{email}")
    void deleteBankAccount(@PathVariable String email);
}
