package com.messages.errors;

public class UserNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -8902739659105451506L;

	public UserNotFoundException(String error) {
		super(error);
	}

}
