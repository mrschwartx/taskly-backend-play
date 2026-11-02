package com.taskly.backend.tasks.presentation;

import com.taskly.backend.config.Authenticated;
import com.taskly.backend.config.AuthenticatedAction;
import com.taskly.backend.shared.infrastructure.http.QueryParameters;
import com.taskly.backend.shared.infrastructure.http.RequestBodies;
import com.taskly.backend.tasks.application.CreateTask;
import com.taskly.backend.tasks.application.DeleteTask;
import com.taskly.backend.tasks.application.GetTask;
import com.taskly.backend.tasks.application.ListTasksByUser;
import com.taskly.backend.tasks.application.UpdateTask;
import java.util.Map;
import javax.inject.Inject;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

@Authenticated
public class TasksController extends Controller {

	private final CreateTask createTask;

	private final GetTask getTask;

	private final ListTasksByUser listTasksByUser;

	private final UpdateTask updateTask;

	private final DeleteTask deleteTask;

	@Inject
	public TasksController(CreateTask createTask, GetTask getTask, ListTasksByUser listTasksByUser,
			UpdateTask updateTask, DeleteTask deleteTask) {
		this.createTask = createTask;
		this.getTask = getTask;
		this.listTasksByUser = listTasksByUser;
		this.updateTask = updateTask;
		this.deleteTask = deleteTask;
	}

	public Result listByUser(Http.Request request, String id) {
		Map<String, String[]> parameters = request.queryString();
		return ok(Json.toJson(listTasksByUser.execute(AuthenticatedAction.requester(request).userId(), id,
				QueryParameters.value(parameters, "status"), QueryParameters.value(parameters, "orderBy"),
				QueryParameters.value(parameters, "page"))));
	}

	public Result get(Http.Request request, String id) {
		return ok(Json.toJson(getTask.execute(AuthenticatedAction.requester(request).userId(), id)));
	}

	@BodyParser.Of(BodyParser.TolerantText.class)
	public Result create(Http.Request request) {
		return ok(Json.toJson(createTask.execute(AuthenticatedAction.requester(request).userId(),
				RequestBodies.json(request))));
	}

	@BodyParser.Of(BodyParser.TolerantText.class)
	public Result update(Http.Request request, String id) {
		return ok(Json.toJson(updateTask.execute(AuthenticatedAction.requester(request).userId(), id,
				RequestBodies.json(request))));
	}

	public Result delete(Http.Request request, String id) {
		deleteTask.execute(AuthenticatedAction.requester(request).userId(), id);
		return ok(Json.toJson(Map.of("message", "Task has been deleted")));
	}
}
