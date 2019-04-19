package com.messages.errors;

public class MongoDbException extends RuntimeException {

	private static final long serialVersionUID = -4902739659105451506L;

	public MongoDbException(String message, Throwable e) {
		super(message, e);

	}

	public MongoDbException(String message) {
		super(message);
	}
}
