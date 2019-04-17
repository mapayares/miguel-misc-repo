package com.messages.services;

import java.util.List;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.messages.MessageDao;
import com.messages.models.Messages;
import com.messages.models.UserMessages;

@Singleton
@Service
public class MessageService {

	private final MessageDao messageDao;

	@Autowired
	public MessageService(MessageDao messageDao) {
		this.messageDao = messageDao;

	}

	private final Logger logger = LoggerFactory.getLogger(MessageService.class);

	public List<UserMessages> getMessage(String userName) {
		logger.debug("Getting messages for User: {}", userName);

		List<UserMessages> messages = messageDao.getMessages(userName);

		return messages;
	}

	public void sendMessage(String userName, Messages message) {
		if (Strings.isNullOrEmpty(message.getContent())) {
			throw new RuntimeException("Message content cannot be empty");
		}


	}

	public void replyMessage(Messages message) {




	}

}
