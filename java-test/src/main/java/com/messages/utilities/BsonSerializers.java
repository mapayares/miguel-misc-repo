package com.messages.utilities;

import java.util.Date;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.module.SimpleSerializers;

public class BsonSerializers extends SimpleSerializers {

	public BsonSerializers() {
		addSerializer(ObjectId.class, new ObjectIdSerializer());
		addSerializer(Date.class, new DateSerializer());
	}
}
