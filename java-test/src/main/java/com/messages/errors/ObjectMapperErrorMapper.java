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
public class ObjectMapperErrorMapper implements ExceptionMapper<ObjectMapperException> {

	private final static Logger logger = LoggerFactory.getLogger(MongodbExceptionMapper.class);
	private final static String genericErrorMessage = "Failed to parse JSON object";

	@Override
	public Response toResponse(ObjectMapperException objectException) {
		logger.error("Threw a object mapper exception error message: {} Throw error: {}", objectException.getMessage(), objectException.getCause());

		Map<String, String> error = new HashMap<>();
		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(error.put(ERROR, genericErrorMessage)).build();
	}
}
