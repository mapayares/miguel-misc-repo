package com.messages.errors;

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

	public Response toResponse(MongoDbException mongoDbException) {
		logger.error("Threw a MongoDB error message: {} error: {}", mongoDbException.getMessage(), mongoDbException.getCause());

		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(genericErrorMessage).build();
	}
}
