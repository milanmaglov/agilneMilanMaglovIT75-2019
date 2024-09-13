package apiGateway.authentication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import authentication.dtos.CustomUserDto;
import org.springframework.web.server.ServerWebExchange;

@Configuration
@EnableWebFluxSecurity
public class ApiGatewayAuthentication {


	/*@Bean
	public MapReactiveUserDetailsService userDetailsService(BCryptPasswordEncoder encoder) {
		List<UserDetails> users = new ArrayList<>();
		users.add(User.withUsername("user")
				.password(encoder.encode("password1"))
				.roles("USER")
				.build());

		users.add(User.withUsername("admin")
				.password(encoder.encode("password2"))
				.roles("ADMIN")
				.build());

		return new MapReactiveUserDetailsService(users);
	}*/

	@Bean
	public MapReactiveUserDetailsService userDetailsService(BCryptPasswordEncoder encoder) {
	    List<UserDetails> users = new ArrayList<>();
	    List<CustomUserDto> usersFromDatabase;

	    try {
	        ResponseEntity<CustomUserDto[]> response =
	            new RestTemplate().getForEntity("http://localhost:8770/users-service/all", CustomUserDto[].class);
	        usersFromDatabase = Arrays.asList(response.getBody());

	        for (CustomUserDto userDto : usersFromDatabase) {
	            users.add(User.withUsername(userDto.getEmail())
	                    .password(encoder.encode(userDto.getPassword()))
	                    .roles(userDto.getRole())
	                    .build());
	        }
	    } catch (RestClientException e) {
	        e.printStackTrace();
	    }

	    return new MapReactiveUserDetailsService(users);
	}


	@Bean
	public BCryptPasswordEncoder getEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityWebFilterChain filterChain(ServerHttpSecurity http) throws Exception{
		http.csrf().disable()
		.authorizeExchange()
				.pathMatchers("/currency-exchange/from/{from}/to/{to}").permitAll()

				.pathMatchers("/crypto-exchange/from/{from}/to/{to}").permitAll()

				.pathMatchers("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}").hasRole("USER")

				.pathMatchers("/crypto-conversion/from/{from}/to/{to}/quantity/{quantity}").hasRole("USER")

				.pathMatchers("/users-service/all").hasAnyRole("ADMIN", "OWNER")
				.pathMatchers("/users-service/get/{email}").permitAll()
				.pathMatchers("/users-service/create").hasAnyRole("ADMIN", "OWNER")
				.pathMatchers("/users-service/update/{email}").hasAnyRole("ADMIN", "OWNER")
				.pathMatchers("/users-service/delete/{email}").hasRole("OWNER")

				.pathMatchers(HttpMethod.GET,"/bank-account/get/{email}").permitAll()
				.pathMatchers("/bank-account/create/{email}").hasRole("ADMIN")
				.pathMatchers("/bank-account/update/{email}").hasRole("ADMIN")
				.pathMatchers("/bank-account/update/{oldEmail}/for/{newEmail}").hasRole("ADMIN")
				.pathMatchers("/bank-account/update/user/{email}/subtract/{quantityS}from/{currS}/add/{quantityA}to/{currA}").permitAll()
				.pathMatchers("/bank-account/delete/{email}").hasRole("ADMIN")

				.pathMatchers(HttpMethod.GET,"/crypto-wallet/user").permitAll()
				.pathMatchers("/crypto-wallet/user/{email}").permitAll()
				.pathMatchers("/crypto-wallet/create/{email}").hasRole("ADMIN")
				.pathMatchers("/crypto-wallet/update/{email}").hasRole("ADMIN")
				.pathMatchers("/crypto-wallet/update/{oldEmail}/for/{newEmail}").hasRole("ADMIN")
				.pathMatchers("/crypto-wallet/delete/{email}").hasRole("ADMIN")

				.pathMatchers("/transfer-service/currency/{currency}/amount/{amount}/to/user/{email}").hasRole("USER")
				.pathMatchers("/trade-service/from/{from}/to/{to}/quantity/{quantity}").hasRole("USER")

				.and().httpBasic().and()

				.addFilterBefore((exchange, chain) -> {
					if (exchange.getRequest().getURI().getPath().startsWith("/currency-exchange/") ||
							exchange.getRequest().getURI().getPath().startsWith("/crypto-exchange/") ||
							exchange.getRequest().getURI().getPath().startsWith("/bank-account/get/") ||
							exchange.getRequest().getURI().getPath().startsWith("/crypto-wallet/user")) {
						return chain.filter(exchange);
					}
					return ReactiveSecurityContextHolder.getContext()
							.map(context -> context.getAuthentication())
							.flatMap(authentication -> {
								String role = authentication.getAuthorities().iterator().next().getAuthority();
								String email = authentication.getName();

								ServerWebExchange modifiedExchange = exchange.mutate()
										.request(builder -> builder.header("User-Role", role))
										.request(builder -> builder.header("User-Email", email))
										.build();
								return chain.filter(modifiedExchange);
							});
				}, SecurityWebFiltersOrder.AUTHORIZATION);

		return http.build();
	}
}
