package com.messages.utilities;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class DateDeserializer extends JsonDeserializer<Date> {

	@Override
	public Date deserialize(JsonParser parser, DeserializationContext context) throws IOException {
		SimpleDateFormat format = new SimpleDateFormat(StringConstants.ISO_DATE_FORMAT);

		JsonNode tree = parser.readValueAsTree();
		String dateString = tree.get("$date").textValue();
		if (dateString == null) {
			Long longDate = tree.get("$date").longValue();
			if (longDate == null) {
				return null;
			}
			return new DateTime(longDate).withZone(DateTimeZone.UTC).toDate();
		}
		try {
			return new Date(format.parse(dateString).getTime());
		} catch (ParseException e) {
			throw new RuntimeException("Failed to parse string date to Date Object: " + dateString, e);
		}
	}

}
