package usersService;

import org.springframework.data.jpa.repository.JpaRepository;

import usersService.model.Role;
import usersService.model.Users;

public interface UserRepository extends JpaRepository<Users, Long> {
    Users findByEmail(String email);
    Users findByRole(Role role);
}