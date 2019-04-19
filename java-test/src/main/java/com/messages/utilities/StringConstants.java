package com.messages.utilities;

import com.amazonaws.regions.Regions;

public class StringConstants {

	private static final String awsRegion = Regions.US_WEST_1.getName();

	public static final String USERS_COLLECTION = "users";

	public static final String USERS_MESSAGES = "users_messages";

	public static final String $ADD_TO_SET = "$addToSet";

	public static final String $EACH = "$each";

	public static final String $SET = "$set";

	public static final String EMAIL = "email";

	public static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

}
