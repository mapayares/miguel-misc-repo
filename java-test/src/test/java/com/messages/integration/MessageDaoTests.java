
package com.messages.integration;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyString;

import static com.messages.utilities.StringConstants.EMAIL;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.messages.models.User;
import com.messages.models.UserMessage;
import com.messages.services.MessageDaoImpl;
import com.messages.services.MongoDBConnection;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

@RunWith(MockitoJUnitRunner.class)
public class MessageDaoTests {

	private static final String USER = "intuit@test.com";

	@Mock
	private MongoDBConnection mockDBConn;

	@Mock
	private MongoCollection<Document> mockColl;

	@Mock
	private MongoDatabase mockMongoDatebase;

	@Mock
	private FindIterable mockIterable;

	@InjectMocks
	private MessageDaoImpl messageDao;

	@Before
	public void before() {
		messageDao = new MessageDaoImpl(mockDBConn);
	}

	@Test
	public void testUser() {

		List<String> followers = Arrays.asList("tester2.com", "tester3@test.com", "tester1@tealium.com");
		Document mockResult = new Document(EMAIL, USER).append("followers", followers);

		when(mockDBConn.getCollection(anyString())).thenReturn(mockColl);
		when(mockColl.find(Mockito.any(Bson.class))).thenReturn(mockIterable);
		when(mockIterable.first()).thenReturn(mockResult);

		User user = messageDao.getUser("miguel@test.com");
		Assert.assertEquals(USER, user.getUserName());
		Assert.assertEquals(3, user.getFollowers().size());

	}

	@Test
	public void testFollowUser() {
		when(mockDBConn.getCollection(anyString())).thenReturn(mockColl);

		messageDao.followUser(USER, "fan@test.com", true);
		verify(mockColl, times(1)).updateOne(any(Bson.class), any(Bson.class));
	}

	@Test
	public void testUnFollowUser() {
		when(mockDBConn.getCollection(anyString())).thenReturn(mockColl);
		messageDao.followUser(USER, "fan@test.com", false);

		verify(mockColl, times(1)).updateOne(any(Bson.class), any(Bson.class));
	}

	@Test
	public void testSendMessage() {
		List<String> followers = Arrays.asList("fun@test.com", "test@test.com", "intuit@test.com");
		UserMessage message = new UserMessage();
		message.setDate(new Date());
		message.setMessageContent("this is a test");
		message.setMessageId("345");
		message.setSender(USER);

		when(mockDBConn.getCollection(anyString())).thenReturn(mockColl);

		messageDao.sendMessage(message, followers);

		verify(mockColl, times(3)).updateOne(any(Bson.class), any(Bson.class), any(UpdateOptions.class));
	}

	@Test
	public void testReplyMessage() {
		UpdateResult result = Mockito.mock(UpdateResult.class);
		String messageId = "3dgd42l";
		String follower = "test@test.com";
		List<String> replyContent = Arrays.asList("Testing on db", "This is another test");

		when(mockDBConn.getCollection(anyString())).thenReturn(mockColl);
		when(mockColl.updateOne(any(Bson.class), any(Bson.class))).thenReturn(result);
		when(result.getModifiedCount()).thenReturn(1l);

		messageDao.replyMessage(messageId, follower, USER, replyContent);

		verify(mockColl, times(1)).updateOne(any(Bson.class), any(Bson.class));

	}
}


