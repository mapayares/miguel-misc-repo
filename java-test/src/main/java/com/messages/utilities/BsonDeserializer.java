package com.messages.utilities;

import java.util.Date;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.module.SimpleDeserializers;

public class BsonDeserializer extends SimpleDeserializers {

	public BsonDeserializer() {
		addDeserializer(ObjectId.class, new ObjectIdDeserializer());
		addDeserializer(Date.class, new DateDeserializer());
	}
}
