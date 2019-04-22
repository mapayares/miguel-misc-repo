package com.messages.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@Component
public class MongoDBConnection {

	private MongoDatabase mongoDB;

	@Autowired
	public MongoDBConnection(@Value("${mongodb.host}") String host, @Value("${mongodb.port}") int mongodbPort, @Value("${mongodb.db}") String db) {

		MongoClient client = new MongoClient(host, mongodbPort);
		mongoDB = client.getDatabase(db);

	}

	public MongoCollection getCollection(String collName) {
		MongoCollection coll =mongoDB.getCollection(collName);
		return coll;
	}

}
