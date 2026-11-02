package com.taskly.backend.shared.infrastructure.http;

import java.util.Map;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class RootController extends Controller {

	public Result welcome() {
		return ok(Json.toJson(Map.of("message", "Welcome to the Taskly API")));
	}
}
