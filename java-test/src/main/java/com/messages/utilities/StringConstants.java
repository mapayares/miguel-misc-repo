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

	public static final String ERROR = "error";

	public static final String MESSAGE_REPLY_CONTENTS = "message_reply_contents";

	public static final String FOLLOWER = "follower";

	public static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

}
