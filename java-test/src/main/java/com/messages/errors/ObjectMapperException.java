package com.messages.errors;

public class ObjectMapperException extends RuntimeException {

	private static final long serialVersionUID = -490273965910545145L;

	public ObjectMapperException(String error, Throwable e) {
		super(error, e);
	}

	public ObjectMapperException(String error) {
		super(error);
	}

}
