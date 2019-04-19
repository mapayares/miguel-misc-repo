package com.messages.utilities;

import java.io.IOException;

import org.bson.Document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.messages.errors.ObjectMapperException;

public class ObjectMapperUtil {

	/**
	 * Helper method to convert JSON to model objects. Returns null if the
	 * given JSON is null or empty. Throws ObjectMapperException if error
	 * mapping object.
	 *
	 * @param objectMapper
	 *            the object mapper to use
	 * @param jsonObject
	 *            the json object to convert from
	 * @param clazz
	 *            the class of the object to convert to
	 * @return
	 * @throws IOException
	 */
	public static <T> T toObject(ObjectMapper objectMapper, Document jsonObject, Class<T> clazz) {
		if (jsonObject == null  || jsonObject.isEmpty())
			return null;
		try {
			return objectMapper.readValue(toString(objectMapper, jsonObject), clazz);
		} catch (IOException e) {
			throw new ObjectMapperException("Failed to parse JSON object", e);
		}
	}

	/**
	 * Helper method to convert object to a JSON String
	 *
	 * @param objectMapper
	 *            the object mapper to use
	 * @param object
	 *            the object to convert from
	 * @return
	 */
	public static String toString(ObjectMapper objectMapper, Object object) {
		FilterProvider filterProvider = new SimpleFilterProvider().setFailOnUnknownId(false);
		try {
			return objectMapper.writer(filterProvider).writeValueAsString(object);
		} catch (IOException e) {
			throw new ObjectMapperException("Failed to parse JSON object", e);
		}
	}

	/**
	 * Helper method to convert object to JSON object
	 * @param objectMapper
	 * 			the object mapper to user to convert
	 * @param object
	 * 			the object to convert from
	 * @return
	 */
	public static Document toDocument(ObjectMapper objectMapper, Object object) {
		FilterProvider filterProvider = new SimpleFilterProvider().setFailOnUnknownId(false);
		try {
			return Document.parse(objectMapper.writer(filterProvider).writeValueAsString(object));
		} catch (IOException e) {
			throw new ObjectMapperException("Failed to turn object to json object", e);
		}
	}
}
