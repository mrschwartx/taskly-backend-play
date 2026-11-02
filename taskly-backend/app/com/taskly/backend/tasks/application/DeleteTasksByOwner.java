package com.taskly.backend.tasks.application;

import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.tasks.domain.TaskRepository;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DeleteTasksByOwner {

	private final TaskRepository taskRepository;

	@Inject
	public DeleteTasksByOwner(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	public int execute(Object ownerId) {
		return taskRepository.deleteByOwner(EntityIds.parse(ownerId, "user"));
	}
}
