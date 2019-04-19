package com.messages.services;

import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.messages.errors.UserNotFoundException;
import com.messages.models.User;
import com.messages.models.UserMessage;

@Singleton
@Service
public class MessageService {

	private final MessageDao messageDao;

	@Autowired
	public MessageService(MessageDao messageDao) {
		this.messageDao = messageDao;

	}

	private final Logger logger = LoggerFactory.getLogger(MessageService.class);

	public List<UserMessage> getMessage(String userName) {
		logger.debug("Getting messages for User: ", userName);

		getUser(userName);

		List<UserMessage> messages = messageDao.getMessages(userName);
		return messages;
	}

	public long sendMessage(UserMessage message) {
		if (Strings.isNullOrEmpty(message.getMessageContent()) || Strings.isNullOrEmpty(message.getSender()))  {
			throw new RuntimeException("Message content cannot be empty");
		}

		long messageId = new Random().nextLong();

		User user = getUser(message.getSender());
		List<String> followers = user.getFollowers();
		message.setDate(new Date());
		message.setMessageId(messageId);

		messageDao.sendMessage(message, followers);

		return messageId;
	}

	public void replyMessage(UserMessage messageReply) {

		boolean originalMessage = messageDao.isMessageValid(messageReply.getMessageId(), messageReply.getFollower(), messageReply.getSender());
		if (!originalMessage) {
			throw new RuntimeException("Reply message sender does not match original message sender");
		}

		messageDao.replyMessage(messageReply.getMessageId(), messageReply.getFollower(), messageReply.getSender(), messageReply.getMessageReply());
	}

	public void followUser(String userToFollow, String follower, boolean follow) {
		getUser(userToFollow);
		getUser(follower);

		messageDao.followUser(userToFollow, follower, follow);

	}

	private User getUser(String userName) {

		User user = messageDao.getUser(userName);
		if (user == null) {
			throw new UserNotFoundException("User: " + userName + " does not exists");
		}
		return user;
	}

}
