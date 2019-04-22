package com.messages.services;

import java.util.List;

import com.messages.models.User;
import com.messages.models.UserMessage;

public interface MessageDao {
	List<UserMessage> getMessages(String userName);

	void sendMessage(UserMessage message, List<String> followers);

	User getUser(String userName);

	void followUser(String userName, String follower, boolean follow);

	boolean isMessageValid(String messageId, String follower, String sender);

	void replyMessage(String messageId, String follower, String sender, List<String> replyContent);
}


