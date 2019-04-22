package com.messages.unit;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;

import com.messages.models.User;
import com.messages.models.UserMessage;
import com.messages.services.MessageDao;
import com.messages.services.MessageService;

@RunWith(MockitoJUnitRunner.class)
public class MessageServiceTests {

	private static final String USER = "miguel@test.com";

	@Autowired
	private MessageService messageService;

	@Mock
	private MessageDao messageDao;

	@Before
	public void before() {
		messageService = new MessageService(messageDao);
	}



	@Test
	public void testGetMessages() {
		User user = getUser();

		UserMessage mockMessage = new UserMessage();
		mockMessage.setSender(USER);
		mockMessage.setMessageId("123hgfdj");
		mockMessage.setMessageContent("test");
		mockMessage.setFollower("test@test.com");

		UserMessage mockMessage2 = new UserMessage();
		mockMessage2.setSender(USER);
		mockMessage2.setMessageId("12465loikujhgfd");
		mockMessage2.setMessageContent("test");
		mockMessage2.setFollower("test@test.com");

		List<UserMessage> expected = Arrays.asList(mockMessage, mockMessage2);

		Mockito.when(messageDao.getUser(USER)).thenReturn(user);
		Mockito.when(messageDao.getMessages(Mockito.anyString())).thenReturn(expected);

		List<UserMessage> actual = messageService.getMessage(USER);

		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testSendMessage() {
		User user = getUser();

		UserMessage mockMessage = new UserMessage();
		mockMessage.setSender(USER);
		mockMessage.setMessageContent("test");
		mockMessage.setFollower("test@test.com");

		Mockito.when(messageDao.getUser(USER)).thenReturn(user);

		String expected = messageService.sendMessage(mockMessage);
		String length = String.valueOf(expected);

		Mockito.verify(messageDao, Mockito.times(1))
				.sendMessage(Mockito.any(UserMessage.class), Mockito.anyList());

		Assert.assertEquals(12, expected.length());
	}

	@Test (expected = RuntimeException.class)
	public void testBadSendMessage() {
		User user = getUser();

		UserMessage mockMessage = new UserMessage();
		mockMessage.setMessageContent("test");
		mockMessage.setFollower("test@test.com");

		String expected = messageService.sendMessage(mockMessage);

		Mockito.verify(messageDao, Mockito.times(1))
				.sendMessage(Mockito.any(UserMessage.class), Mockito.anyList());

	}

	@Test
	public void testFollowUser() {
		User user = getUser();
		Mockito.when(messageDao.getUser(USER)).thenReturn(user);

		messageService.followUser(user.getUserName(), user.getUserName(), true);

		Mockito.verify(messageDao, Mockito.times(1)).followUser(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
	}

	@Test
	public void testUnFollowUser() {
		User user = getUser();
		Mockito.when(messageDao.getUser(USER)).thenReturn(user);

		messageService.followUser(user.getUserName(), user.getUserName(), false);

		Mockito.verify(messageDao, Mockito.times(1)).followUser(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
	}

	private User getUser() {
		User user = new User();
		List<String> followers = Arrays.asList("tester@test.com", "intuit@test.com");
		user.setFollowers(followers);
		user.setUserName(USER);

		return user;
	}

}
