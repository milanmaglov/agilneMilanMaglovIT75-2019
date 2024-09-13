package cryptowallet.cryptowallet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class CryptoWalletController{

    @Autowired
    private Environment environment;

    @Autowired
    private CryptoWalletRepository repo;

    @Autowired
    private UserProxy userProxy;

    @GetMapping("/crypto-wallet/user")
    public List<CryptoWallet> getCryptoWallets() throws Exception {
        System.out.println("Request reached /crypto-wallet/user endpoint.");

        try {
            String port = environment.getProperty("local.server.port");
            System.out.println("Port: " + port);

            List<CryptoWallet> cryptoWallets = repo.findAll();
            System.out.println("Crypto Wallets size: " + cryptoWallets.size());

            for (CryptoWallet cryptoWallet : cryptoWallets) {
                cryptoWallet.setEnvironment(port);
            }

            if (cryptoWallets == null) {
                throw new Exception("Crypto wallets not found!");
            }

            return cryptoWallets;
        } catch (Exception ex) {
            System.err.println("Exception in /crypto-wallet/user: " + ex.getMessage());
            throw new Exception(ex.getMessage());
        }
    }


    @GetMapping("/crypto-wallet/user/{email}")
    public CryptoWallet getCryptoWallet(@PathVariable String email) throws Exception{

        try {
            String port = environment.getProperty("local.server.port");

            CryptoWallet cryptoWallet = repo.findByEmail(email);

            if(cryptoWallet == null) {
                throw new CustomExceptions.EntityDoesntExistException("Crypto wallet for " + email + " not found!");
            }

            return new CryptoWallet(email, cryptoWallet.getBTC_amount(),
                    cryptoWallet.getETH_amount(), cryptoWallet.getLTC_amount(), cryptoWallet.getXRP_amount(),
                    port);
        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
    }

    @PostMapping("/crypto-wallet/create/{email}")
    public ResponseEntity<CryptoWallet> createCryptoWallet(@PathVariable String email) throws Exception{

        try {
            String port = environment.getProperty("local.server.port");
            CryptoWallet existingCryptoWallet = repo.findByEmail(email);

            if(existingCryptoWallet != null) {
                throw new CustomExceptions.EntiyWithEmailAlreadyExistsException("Crypto wallet for " + email + " already exists!");
            }

            CryptoWallet newCryptoWallet = new CryptoWallet(email, port);

            repo.save(newCryptoWallet);

            return ResponseEntity.status(201).body(newCryptoWallet);

        } catch (CustomExceptions.EntiyWithEmailAlreadyExistsException e) {
            throw e;
        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
    }

    @PutMapping("/crypto-wallet/update/{email}")
    public ResponseEntity<CryptoWallet> updateBankAccountEntity(@PathVariable String email, @RequestBody CryptoWallet updatedCryptoWallet) throws Exception{

        try {
            CryptoWallet existingCryptoWallet = repo.findByEmail(email);

            String port = environment.getProperty("local.server.port");

            if(existingCryptoWallet == null) {
                throw new CustomExceptions.EntityDoesntExistException("Crypto wallet for " + email + " not found!");
            }

            existingCryptoWallet.setBTC_amount(updatedCryptoWallet.getBTC_amount());
            existingCryptoWallet.setETH_amount(updatedCryptoWallet.getETH_amount());
            existingCryptoWallet.setLTC_amount(updatedCryptoWallet.getLTC_amount());
            existingCryptoWallet.setXRP_amount(updatedCryptoWallet.getXRP_amount());

            existingCryptoWallet.setEnvironment(port);

            repo.save(existingCryptoWallet);
            return ResponseEntity.status(200).body(existingCryptoWallet);
        } catch (CustomExceptions.EntityDoesntExistException e) {
            throw e;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    @PutMapping("/crypto-wallet/update/{oldEmail}/for/{newEmail}")
    public ResponseEntity<CryptoWallet> updateCryptoWalletEmail(@PathVariable String oldEmail, @PathVariable String newEmail) throws Exception{

        try {
            CryptoWallet existingCryptoWallet = repo.findByEmail(oldEmail);

            if(existingCryptoWallet == null) {
                throw new CustomExceptions.EntityDoesntExistException("Crypto wallet for " + oldEmail + " not found!");
            }

            UserDto user = userProxy.getUserByEmail(newEmail).getBody();

            if(user == null){
                throw new CustomExceptions.EntityDoesntExistException("No existing user with email : " + oldEmail);
            }

            existingCryptoWallet.setEmail(newEmail);

            repo.save(existingCryptoWallet);
            return ResponseEntity.status(200).body(existingCryptoWallet);
        } catch (CustomExceptions.EntityDoesntExistException e) {
            throw e;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    @PutMapping("/crypto-wallet/update/user/{email}/subtract/{quantityS}/from/{currS}/add/{quantityA}to/{currA}")
    public ResponseEntity<CryptoWallet> updateCryptoWalletBalance(@PathVariable String email, @PathVariable BigDecimal quantityS,
                                                                  @PathVariable String currS, @PathVariable BigDecimal quantityA, @PathVariable String currA) throws Exception{
        try {
            CryptoWallet userCryptoWallet = repo.findByEmail(email);

            if(userCryptoWallet == null){
                throw new CustomExceptions.EntityDoesntExistException("There is no bank account for user with email " + email);
            }

            subtract(currS, quantityS, userCryptoWallet);
            add(currA, quantityA, userCryptoWallet);

            repo.save(userCryptoWallet);
            return ResponseEntity.status(201).body(userCryptoWallet);
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }
    }

    @PutMapping("/crypto-wallet/update/user/{email}/change_by/{quantity}/from/{curr}/increase/or/decrease/{in_de_crease}")
    public ResponseEntity<CryptoWallet> changeCryptoWalletBalance(@PathVariable String email, @PathVariable BigDecimal quantity,
                                                                  @PathVariable String curr, @PathVariable Boolean in_de_crease) throws Exception{
        try {
            CryptoWallet userCryptoWallet = repo.findByEmail(email);

            if(userCryptoWallet == null){
                throw new CustomExceptions.EntityDoesntExistException("There is no crypto wallet for user with email " + email);
            }

            if(in_de_crease){
                add(curr, quantity, userCryptoWallet);
            } else {
                subtract(curr, quantity, userCryptoWallet);
            }

            repo.save(userCryptoWallet);
            return ResponseEntity.status(201).body(userCryptoWallet);
        } catch (CustomExceptions.EntityDoesntExistException e){
            throw e;
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }
    }

    @DeleteMapping("/crypto-wallet/delete/{email}")
    public ResponseEntity<Boolean> deleteCryptoWallet(@PathVariable String email) throws Exception{
        try {
            CryptoWallet existingCryptoWallet = repo.findByEmail(email);

            if(existingCryptoWallet == null) {
                throw new CustomExceptions.EntityDoesntExistException("Crypto wallet  for " + email + " not found!");
            }

            repo.delete(existingCryptoWallet);

            return ResponseEntity.status(204).body(true);
        } catch (CustomExceptions.EntityDoesntExistException e) {
            throw e;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    private static void subtract(String curr, BigDecimal quantity, CryptoWallet wallet){

        switch (curr) {
            case "BTC" -> wallet.setBTC_amount(wallet.getBTC_amount().subtract(quantity));
            case "ETH" -> wallet.setETH_amount(wallet.getETH_amount().subtract(quantity));
            case "LTC" -> wallet.setLTC_amount(wallet.getLTC_amount().subtract(quantity));
            case "XRP" -> wallet.setXRP_amount(wallet.getXRP_amount().subtract(quantity));
        }
    }

    private static void add(String curr, BigDecimal quantity, CryptoWallet wallet){

        switch (curr) {
            case "BTC" -> wallet.setBTC_amount(wallet.getBTC_amount().add(quantity));
            case "ETH" -> wallet.setETH_amount(wallet.getETH_amount().add(quantity));
            case "LTC" -> wallet.setLTC_amount(wallet.getLTC_amount().add(quantity));
            case "XRP" -> wallet.setXRP_amount(wallet.getXRP_amount().add(quantity));
        }
    }
}
