
package com.messages;

import static com.messages.utilities.StringConstants.EMAIL;
import static com.messages.utilities.StringConstants.USERS_COLLECTION;
import static com.messages.utilities.StringConstants.USERS_MESSAGES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
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
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

@RunWith(MockitoJUnitRunner.class)
public class MessageDaoTests {

	private static final String USER = "miguel@test.com";

	@Autowired
	private MessageDao messageDao;

	@Mock
	private MongoDatabase mongoDb;

	@Mock
	private MongoCollection mongoCollection;

	@Mock
	private FindIterable findIterable;

	@Before
	public void before() {
		//ReflectionTestUtils.setField(MessageDao.class, "mongodb.host", "fake");
		messageDao = new MessageDao("localhost", 27017, "core");
	}

	@Test
	public void testUser() {

		List<String> followers = Arrays.asList("tester2.com", "tester3@test.com");
		Document mockResult = new Document(EMAIL, USER).append("followers", followers);
		when(mongoDb.getCollection(anyString())).thenReturn(mongoCollection);
		when(mongoCollection.find(any(Bson.class))).thenReturn(findIterable);
		when(findIterable.first()).thenReturn(mockResult);

		User user = messageDao.getUser("miguel@test.com");

	}

	@Test
	public void testFollowUser() {
		messageDao.followUser(USER, "fan@test.com", true);
		verify(mongoDb, times(1)).getCollection(Mockito.eq(USERS_COLLECTION)).updateOne(any(Bson.class), any(Bson.class));

	}

	@Test
	public void testUnFollowUser() {
		messageDao.followUser(USER, "fan@test.com", false);
		verify(mongoDb, times(1)).getCollection(Mockito.eq(USERS_COLLECTION)).updateOne(any(Document.class), any(Document.class));

	}

	@Test
	public void testSendMessage() {
		List<String> followers = Arrays.asList("fun@test.com", "test@test.com", "intuit@test.com");
		UserMessage message = new UserMessage();
		message.setDate(new Date());
		message.setMessageContent("this is a test");
		message.setMessageId(342l);
		message.setSender(USER);

		messageDao.sendMessage(message, followers);

		verify(mongoDb, times(3)).getCollection(Mockito.eq(USERS_MESSAGES)).updateOne(any(Document.class), any(Document.class), any(UpdateOptions.class));
	}

	@Test
	public void testReplyMessage() {
		long messageId = 342l;
		String follower = "test@test.com";
		List<String> replyContent = Arrays.asList("Testing for the bug", "This is another test");

		messageDao.replyMessage(messageId, follower, USER, replyContent);

	}

	@Test
	public void testGetMessages() {

		messageDao.getMessages(USER);

	}


}


