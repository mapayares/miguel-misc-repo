package com.messages.errors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Provider
public class UserNotFoundExceptionMapper implements ExceptionMapper<UserNotFoundException> {

	private final static Logger logger = LoggerFactory.getLogger(UserNotFoundExceptionMapper.class);
	private final static String genericErrorMessage = "please check your username and try again";

	public Response toResponse(UserNotFoundException userNotFoundException) {
		logger.error("User: {} does not exists in the app", userNotFoundException.getMessage());

		return Response.status(Status.BAD_REQUEST).entity(genericErrorMessage).build();
	}
}
