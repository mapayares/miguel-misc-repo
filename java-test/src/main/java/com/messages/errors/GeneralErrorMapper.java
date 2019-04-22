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
public class GeneralErrorMapper implements ExceptionMapper<RuntimeException> {

	private final String GENERIC_ERROR = "There was a problem processing your request please try again";
	private final static Logger logger = LoggerFactory.getLogger(GeneralErrorMapper.class);

	@Override
	public Response toResponse(RuntimeException e) {
		logger.error("Failed with unknown error: {} trace stack: {}", e.getMessage(), e.getStackTrace());

		Map<String, String> error = new HashMap<>();
		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(error.put(ERROR, GENERIC_ERROR)).build();
	}
}
