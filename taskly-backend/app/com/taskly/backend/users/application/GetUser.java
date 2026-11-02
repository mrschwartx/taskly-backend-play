package com.taskly.backend.users.application;

import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.shared.domain.NotFoundException;
import com.taskly.backend.users.domain.UserRepository;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GetUser {

	private final UserRepository userRepository;

	@Inject
	public GetUser(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserDto execute(String userId) {
		UUID id = EntityIds.parse(userId, "user");
		return userRepository.findById(id)
				.map(UserDto::from)
				.orElseThrow(() -> new NotFoundException("User not found!"));
	}
}
