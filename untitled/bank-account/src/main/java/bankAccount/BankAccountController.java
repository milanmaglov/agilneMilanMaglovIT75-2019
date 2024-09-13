package bankAccount;

import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLDecoder;

@RestController
public class BankAccountController {

    @Autowired
    private Environment environment;

    @Autowired
    private BankAccountRepository repo;

    @Autowired
    private UserProxy userProxy;

    @GetMapping("/bank-account/get/{email}")
    public BankAccount getBankAccount(@PathVariable String email) throws Exception{
        try {
            String port = environment.getProperty("local.server.port");

            String decodedEmail = URLDecoder.decode(email);

            BankAccount bankAccount = repo.findByEmail(decodedEmail);

            if (bankAccount == null){
                throw new CustomExceptions.EntityDoesntExistException("Bank account for " + decodedEmail + " not found!");
            }
            return new BankAccount(bankAccount.getId(), decodedEmail, bankAccount.getRSD_amount(), bankAccount.getEUR_amount(),
                    bankAccount.getUSD_amount(), bankAccount.getGBP_amount(), bankAccount.getCHF_amount(), port);
        }catch (Exception ex){
            throw new Exception(ex.getMessage());
        }
    }

    @PostMapping("/bank-account/create/{email}")
    public ResponseEntity<BankAccount> createBankAccount(@PathVariable String email) throws Exception{
        try{
            String port = environment.getProperty("local.server.port");
            BankAccount existingAccount = repo.findByEmail(email);

            if (existingAccount != null){
                throw new CustomExceptions.EntiyWithEmailAlreadyExistsException("Bank account already exists for email: " + email);
            }

            BankAccount newAccount = new BankAccount(email, port);

            repo.save(newAccount);

            return ResponseEntity.status(201).body(newAccount);

        }catch (CustomExceptions.EntiyWithEmailAlreadyExistsException e) {
            throw e;
        } catch (Exception ex){
            throw new Exception(ex.getMessage());
        }
    }

    @PutMapping("/bank-account/update/{email}")
    public ResponseEntity<BankAccount> updateBankAccountEntity(@PathVariable String email, @RequestBody BankAccount updatedBankAccount) throws Exception{

        try {
            BankAccount existingBankAccount = repo.findByEmail(email);

            String port = environment.getProperty("local.server.port");

            if(existingBankAccount == null) {
                throw new Exception("Bank account for " + email + " not found!");
            }

            existingBankAccount.setCHF_amount(updatedBankAccount.getCHF_amount());
            existingBankAccount.setEUR_amount(updatedBankAccount.getEUR_amount());
            existingBankAccount.setGBP_amount(updatedBankAccount.getGBP_amount());
            existingBankAccount.setRSD_amount(updatedBankAccount.getRSD_amount());
            existingBankAccount.setUSD_amount(updatedBankAccount.getUSD_amount());

            existingBankAccount.setEnvironment(port);

            repo.save(existingBankAccount);
            return ResponseEntity.status(200).body(existingBankAccount);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    @PutMapping("/bank-account/update/{oldEmail}/for/{newEmail}")
    public ResponseEntity<BankAccount> updateBankAccountEmail(@PathVariable String oldEmail, @PathVariable String newEmail) throws Exception{

        try {
            BankAccount existingBankAccount = repo.findByEmail(oldEmail);

            if(existingBankAccount == null) {
                throw new CustomExceptions.EntityDoesntExistException("Bank account for " + oldEmail + " not found!");
            }

            UserDto user = userProxy.getUserByEmail(newEmail).getBody();

            if(user == null){
                throw new CustomExceptions.EntiyWithEmailAlreadyExistsException("No existing user with email : " + oldEmail);
            }

            existingBankAccount.setEmail(newEmail);

            repo.save(existingBankAccount);
            return ResponseEntity.status(200).body(existingBankAccount);
        }
        catch (CustomExceptions.EntityDoesntExistException | CustomExceptions.EntiyWithEmailAlreadyExistsException e){
            throw e;
        }
        catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }
    @PutMapping("/bank-account/update/user/{email}/subtract/{quantityS}from/{currS}/add/{quantityA}to/{currA}")
    public ResponseEntity<BankAccount> updateBankAccountBalance(@PathVariable String email, @PathVariable BigDecimal quantityS,
                                                                @PathVariable String currS, @PathVariable BigDecimal quantityA, @PathVariable String currA) throws Exception{
        try {
            BankAccount userBankAccount = repo.findByEmail(email);

            if(userBankAccount == null){
                throw new Exception("There is no bank account for user with email " + email);
            }

            subtract(currS, quantityS, userBankAccount);
            add(currA, quantityA, userBankAccount);

            repo.save(userBankAccount);
            return ResponseEntity.status(201).body(userBankAccount);
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }
    }

    @PutMapping("/bank-account/update/user/{email}/change_by/{quantity}/from/{curr}/increase/or/decrease/{in_de_crease}")
    public ResponseEntity<BankAccount> changeBankAccountBalance(@PathVariable String email, @PathVariable BigDecimal quantity,
                                                                @PathVariable String curr, @PathVariable  Boolean in_de_crease) throws Exception{
        try {
            BankAccount userBankAccount = repo.findByEmail(email);

            if(userBankAccount == null){
                throw new Exception("There is no bank account for user with email " + email);
            }
            if(in_de_crease){
                add(curr, quantity, userBankAccount);
            } else{
                subtract(curr, quantity, userBankAccount);
            }

            repo.save(userBankAccount);
            return ResponseEntity.status(201).body(userBankAccount);
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }
    }

    @DeleteMapping("/bank-account/delete/{email}")
    public ResponseEntity<Boolean> deleteBankAccount(@PathVariable String email) throws Exception{
        try {
            BankAccount account = repo.findByEmail(email);
            if (account == null){
                throw new CustomExceptions.EntityDoesntExistException("Account doesn't exist with email: " + email);
            }

            repo.delete(account);

            return ResponseEntity.status(204).body(true);
        }catch (CustomExceptions.EntityDoesntExistException e) {
            throw e;
        }
        catch (Exception ex){
            throw new Exception(ex.getMessage());
        }
    }
    private static void subtract(String curr, BigDecimal quantity, BankAccount account){

        switch (curr) {
            case "RSD" -> account.setRSD_amount(account.getRSD_amount().subtract(quantity));
            case "USD" -> account.setUSD_amount(account.getUSD_amount().subtract(quantity));
            case "GBP" -> account.setGBP_amount(account.getGBP_amount().subtract(quantity));
            case "EUR" -> account.setEUR_amount(account.getEUR_amount().subtract(quantity));
            case "CHF" -> account.setCHF_amount(account.getCHF_amount().subtract(quantity));
        }
    }

    private static void add(String curr, BigDecimal quantity, BankAccount account){

        switch (curr) {
            case "RSD" -> account.setRSD_amount(account.getRSD_amount().add(quantity));
            case "USD" -> account.setUSD_amount(account.getUSD_amount().add(quantity));
            case "GBP" -> account.setGBP_amount(account.getGBP_amount().add(quantity));
            case "EUR" -> account.setEUR_amount(account.getEUR_amount().add(quantity));
            case "CHF" -> account.setCHF_amount(account.getCHF_amount().add(quantity));
        }
    }
}