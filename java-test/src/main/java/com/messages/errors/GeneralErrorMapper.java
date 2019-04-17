package com.messages.errors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GeneralErrorMapper implements ExceptionMapper {

	private final String GENERIC_ERROR = "There was a problem processing your request please try again";

	public Response toResponse(Throwable throwable) {
		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(GENERIC_ERROR).build();
	}
}
