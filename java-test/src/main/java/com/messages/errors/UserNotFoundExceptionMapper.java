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
import org.springframework.stereotype.Component;

@Provider
@Component
public class UserNotFoundExceptionMapper extends Throwable implements ExceptionMapper<UserNotFoundException> {

	private final static Logger logger = LoggerFactory.getLogger(UserNotFoundExceptionMapper.class);
	private final static String genericErrorMessage = "please check your username and try again";

	@Override
	public Response toResponse(UserNotFoundException userError) {
		logger.error("{} does not exists in the app Stack trace: {}", userError.getMessage(), userError.getStackTrace());

		Map<String, String> error = new HashMap<>();
		return Response.status(Status.BAD_REQUEST).entity(error.put(ERROR, genericErrorMessage)).build();
	}
}
