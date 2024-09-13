package usersService;

import java.util.List;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import usersService.model.Role;
import usersService.model.Users;

import javax.swing.*;

@RestController
public class UserController {

	@Autowired
	private UserRepository repo;

	@Autowired
	private BankAccountProxy bankAccountProxy;

	@Autowired
	private CryptoWalletProxy cryptoWalletProxy;

	@Autowired
	private Environment environment;
	
	@GetMapping("/users-service/all")
	public ResponseEntity<List<Users>> getAllUsers() throws Exception{
		try {
			List<Users> allUsers = repo.findAll();

			String port = environment.getProperty("local.server.port");

			for (Users user: allUsers
				 ) {
				user.setEnvironment(port);
			}
			return ResponseEntity.status(200).body(allUsers);
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	@GetMapping("/users-service/get/{email}")
	public Users GetUser(@PathVariable String email) throws Exception{
		try{
			String port = environment.getProperty("local.server.port");
			Users user = repo.findByEmail(email);

			return new Users(user.getId(), email, user.getPassword(), user.getRole(), port);
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}
	
	@PostMapping("/users-service/create")
	public ResponseEntity<Users> createUser(@RequestBody Users user, HttpServletRequest request) throws Exception{
		try {
			String role = request.getHeader("User-Role");
			Users existingUser = repo.findByEmail(user.getEmail());
			Users owner = repo.findByRole(Role.OWNER);

			if(existingUser != null) {
				throw new CustomExceptions.EntiyWithEmailAlreadyExistsException("User with given email already exists in database ");
			}
			if(owner != null && user.getRole().equals(Role.OWNER)){
				throw new CustomExceptions.OwnerAlreadyExistsException("Owner already exists in this database. ");
			}
			if(role.endsWith("ADMIN") && (user.getRole().equals(Role.ADMIN) || user.getRole().equals(Role.OWNER))) {
				throw new CustomExceptions.MethodExecutionPermissionDeniedException("Method not allowed for following role: " + role);
			}

			String port = environment.getProperty("local.server.port");
			user.setEnvironment(port);

			Users createdUser = repo.save(user);

			if (createdUser.getRole().equals(Role.USER)){
				bankAccountProxy.createBankAccount(createdUser.getEmail());
				cryptoWalletProxy.createCryptoWallet(createdUser.getEmail());
			}
			return ResponseEntity.status(201).body(createdUser);
		}catch (CustomExceptions.EntiyWithEmailAlreadyExistsException | CustomExceptions.OwnerAlreadyExistsException
				| CustomExceptions.MethodExecutionPermissionDeniedException e){
			throw e;
		} catch (Exception ex){
			throw new Exception(ex.getMessage());
		}
	}

	@PutMapping("/users-service/update/{email}")
	public ResponseEntity<Users> updateUser(@PathVariable("email") String email, @RequestBody Users updateUser) throws Exception{

		try {
			Users existingUser = repo.findByEmail(email);
			Users checkNewEmail = repo.findByEmail(updateUser.getEmail());
			if(existingUser == null) {
				throw new CustomExceptions.EntityDoesntExistException("User with " + email + "not found");
			}
			if(checkNewEmail != null && checkNewEmail!=existingUser) {
				throw new CustomExceptions.EntiyWithEmailAlreadyExistsException("User with given email already exists in database");
			}

			String port = environment.getProperty("local.server.port");

			existingUser.setEmail(updateUser.getEmail());
			existingUser.setPassword(updateUser.getPassword());
			existingUser.setRole(updateUser.getRole());
			existingUser.setEnvironment(port);

			Users updatedUser = repo.save(existingUser);
			if(updatedUser.getRole().equals(Role.USER) && !Objects.equals(updatedUser.getEmail(), email)) {
				bankAccountProxy.updateBankAccountEmail(email, updatedUser.getEmail());
				cryptoWalletProxy.updateCryptoWalletEmail(email, updatedUser.getEmail());
			}
			return ResponseEntity.status(201).body(updatedUser);
		} catch (CustomExceptions.EntityDoesntExistException | CustomExceptions.EntiyWithEmailAlreadyExistsException e) {
			throw e;
		} catch (Exception ex) {
			throw new Exception(ex.getMessage());
		}
	}

	@DeleteMapping("users-service/delete/{email}")
	public ResponseEntity<Boolean> deleteUser(@PathVariable String email) throws Exception{
		try{
			Users user = repo.findByEmail(email);
			if (user == null){
				throw new CustomExceptions.EntityDoesntExistException("User doesn't exist with email: " + email);
			}

			if (user.getRole().equals(Role.USER)){
				bankAccountProxy.deleteBankAccount(email);
				cryptoWalletProxy.deleteCryptoWallet(email);
			}

			repo.delete(user);
			return ResponseEntity.status(204).body(true);
		}catch (CustomExceptions.EntityDoesntExistException e){
			throw e;
		}
		catch (Exception ex){
			throw new Exception(ex.getMessage());
		}
	}
 }
