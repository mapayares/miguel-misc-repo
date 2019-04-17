package com.messages.errors;

public class SQLDBError extends RuntimeException {

	private static final long serialVersionUID = -4902739659105451506L;

	public SQLDBError(String message, Throwable e) {
		super(message, e);

	}

	public SQLDBError(String message) {
		super(message);
	}
}
