package com.messages.errors;

import static com.messages.utilities.StringConstants.ERROR;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class MongodbExceptionMapper implements ExceptionMapper<MongoDbException> {

	private final static Logger logger = LoggerFactory.getLogger(MongodbExceptionMapper.class);
	private final static String genericErrorMessage = "application is having intermidiate database issue";

	@Override
	public Response toResponse(MongoDbException mongoDbException) {
		logger.error("Threw a MongoDB error message: {} cause: {}", mongoDbException.getMessage(), mongoDbException.getCause());

		Map<String, String> error = new HashMap<>();
		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(error.put(ERROR, genericErrorMessage)).build();
	}
}
