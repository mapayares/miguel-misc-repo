package com.messages.errors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class SQLDBErrorMapper implements ExceptionMapper<SQLDBError> {

	private final static Logger logger = LoggerFactory.getLogger(SQLDBErrorMapper.class);
	private final static String genericErrorMessage = "application is having intermidiate database issue";

	public Response toResponse(SQLDBError sqldbError) {
		logger.error("Threw a MySQL error message: {} Throw error: {}", sqldbError.getMessage(), sqldbError.getCause());

		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(genericErrorMessage).build();
	}
}
