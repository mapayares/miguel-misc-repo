package com.messages;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;

import com.messages.models.User;
import com.messages.services.MessageDao;
import com.messages.services.MessageService;

@RunWith(MockitoJUnitRunner.class)
public class MessageServiceTests {

	private static final String USER = "user@test.com";

	@Autowired
	private MessageService messageService;

	@Mock
	private MessageDao messageDao;



	@Test
	public void testGetUser() {
		User user = new User();
		List<String> followers = Arrays.asList("tester@test.com", "intuit@test.com");
		user.setFollowers(followers);
		user.setUserName(USER);

		Mockito.when(messageDao.getUser(USER)).thenReturn(user);

	}



}
