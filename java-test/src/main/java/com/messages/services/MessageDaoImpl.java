package com.messages.services;

import static com.messages.utilities.StringConstants.$ADD_TO_SET;
import static com.messages.utilities.StringConstants.$EACH;
import static com.messages.utilities.StringConstants.$SET;
import static com.messages.utilities.StringConstants.EMAIL;
import static com.messages.utilities.StringConstants.FOLLOWER;
import static com.messages.utilities.StringConstants.MESSAGE_REPLY_CONTENTS;
import static com.messages.utilities.StringConstants.USERS_COLLECTION;
import static com.messages.utilities.StringConstants.USERS_MESSAGES;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Singleton;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messages.errors.MongoDbException;
import com.messages.models.User;
import com.messages.models.UserMessage;
import com.messages.utilities.BsonModule;
import com.messages.utilities.ObjectMapperUtil;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

@Singleton
@Component
public class MessageDaoImpl implements MessageDao {

	private final Logger logger = LoggerFactory.getLogger(MessageDao.class);
	private final MongoDBConnection mongoDB;
	private final ObjectMapper objectMapper;

	@Autowired
	public MessageDaoImpl(MongoDBConnection mongoDB) {
		this.mongoDB = mongoDB;

		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new BsonModule());
	}

	@Override
	public List<UserMessage> getMessages(String userName) {
		List<UserMessage> messages = new ArrayList<>();
		Document sort = new Document("date", 1);
		Bson query = Filters.eq("sender", userName);
		MongoCollection collection = mongoDB.getCollection(USERS_MESSAGES);
		try {
			MongoCursor<Document> docs = collection.find(query).limit(100).sort(sort).iterator();
			while (docs.hasNext()) {
				Document doc = docs.next();
				UserMessage messg = ObjectMapperUtil.toObject(objectMapper, doc, UserMessage.class);
				messages.add(messg);
			}
		} catch (MongoException e) {
			throw new MongoDbException("Failed to get messages for User: " + userName, e);
		}
		return messages;
	}

	@Override
	public void sendMessage(UserMessage message, List<String> followers) {
		logger.debug("User: {} sending message to it's user", message.getSender());

		Document jsonMessage = ObjectMapperUtil.toDocument(objectMapper, message);
		Document update = new Document("$set", jsonMessage);
		UpdateOptions options = new UpdateOptions().upsert(true);
		MongoCollection coll = mongoDB.getCollection(USERS_MESSAGES);
		for(String follower : followers) {

			Bson query = Filters.and(Filters.eq(FOLLOWER, follower), Filters.eq("date", message.getDate()));
			try {
				coll.updateOne(query, update, options);
			} catch (MongoException e) {
				logger.debug("Failed to send message to User: {} from User: {}", follower, message.getSender());
			}
		}
	}

	@Override
	public User getUser(String userName) {
		Document doc = null;

		Bson query = Filters.eq(EMAIL, userName);
		logger.debug("Querying DB for User: {}", userName);
		MongoCollection<Document> coll = mongoDB.getCollection(USERS_COLLECTION);
		try {
			doc = coll.find(query).first();
		} catch (MongoException e) {
			throw new MongoDbException("Failed to get User: " + userName + " in the database");
		}

		if (doc == null) {
			return null;
		}

		logger.debug("Found user: {} in db", userName);
		User user = ObjectMapperUtil.toObject(objectMapper, doc, User.class);
		return user;
	}

	@Override
	public void followUser(String userName, String follower, boolean follow) {
		logger.debug("User: {} is now following User: {}", follower, userName);

		Bson query = Filters.eq(EMAIL, userName);
		Document update = new Document($ADD_TO_SET, new Document("followers", follower));

		if (!follow) {
			update = new Document("$pull", new Document("followers", follower));
		}

		MongoCollection coll = mongoDB.getCollection(USERS_COLLECTION);
		try {
			coll.updateOne(query, update);
		} catch (MongoException e) {
			throw new MongoDbException("Failed to add a new follower for User: " + userName);
		}

	}

	@Override
	public boolean isMessageValid(String messageId, String follower, String sender) {
		logger.debug("User: {} is replying to message id: {}", follower, messageId);

		Bson query = getMessageQuery(messageId, sender, follower);
		MongoCollection<Document> coll = mongoDB.getCollection(USERS_MESSAGES);
		try {
			long record = coll.countDocuments(query);
			if (record != 1) {
				return false;
			}
			return true;
		} catch (MongoException e) {
			throw new MongoDbException("Failed to find message id: " + messageId);
		}
	}

	@Override
	public void replyMessage(String messageId, String follower, String sender, List<String> replyContent) {
		logger.debug("User: {} is reply back to message id: {}", follower, messageId);

		Bson query = getMessageQuery(messageId, sender, follower);
		Document messageReplyUpdate = new Document($ADD_TO_SET, new Document(MESSAGE_REPLY_CONTENTS, new Document($EACH, replyContent)));
		Document dateReplyDate = new Document($SET, new Document("reply_date", new Date()));
		Document update = new Document();

		update.putAll(messageReplyUpdate);
		update.putAll(dateReplyDate);

		try {
			UpdateResult result = mongoDB.getCollection(USERS_MESSAGES).updateOne(query, update);
			if (result.getModifiedCount() != 1l) {
				throw new MongoDbException("Failed to reply back to message id:" + messageId);
			}
		} catch (MongoException e) {
			throw new MongoDbException("Failed to reply back to message id: " + messageId);
		}
	}

	private Bson getMessageQuery(String messageId, String sender, String follower) {
		Bson query = Filters.and(Filters.eq("message_id", messageId), Filters.eq(FOLLOWER, follower), Filters.eq("sender", sender));
		return query;
	}

}