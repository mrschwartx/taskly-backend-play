package com.taskly.backend.users.application;

import com.taskly.backend.shared.domain.AuthenticationFailedException;
import com.taskly.backend.shared.domain.NotFoundException;
import com.taskly.backend.shared.domain.TextRules;
import com.taskly.backend.shared.domain.ValidationException;
import com.taskly.backend.users.domain.PasswordHasher;
import com.taskly.backend.users.domain.TokenService;
import com.taskly.backend.users.domain.User;
import com.taskly.backend.users.domain.UserRepository;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AuthenticateUser {

	private final UserRepository userRepository;

	private final PasswordHasher passwordHasher;

	private final TokenService tokenService;

	@Inject
	public AuthenticateUser(UserRepository userRepository, PasswordHasher passwordHasher, TokenService tokenService) {
		this.userRepository = userRepository;
		this.passwordHasher = passwordHasher;
		this.tokenService = tokenService;
	}

	public AuthResult execute(Map<String, Object> input) {
		if (!(input.get("email") instanceof String email) || !(input.get("password") instanceof String password)) {
			throw new ValidationException("Email and password are required");
		}

		User user = userRepository.findByEmail(TextRules.trim(email))
				.orElseThrow(() -> new NotFoundException("User not found!"));

		if (!passwordHasher.matches(password, user.getPasswordHash())) {
			throw new AuthenticationFailedException("Wrong password!");
		}

		return new AuthResult(UserDto.from(user), tokenService.issue(user.getId()));
	}
}
