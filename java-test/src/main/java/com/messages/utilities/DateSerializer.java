package com.messages.utilities;

import java.io.IOException;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class DateSerializer extends JsonSerializer<Date> {

	@Override
	public void serialize(Date date, JsonGenerator generator, SerializerProvider provider) throws IOException {
		DateTime dateTime = new DateTime(date).withZone(DateTimeZone.UTC);
		DateTimeFormatter ISO_FORMATTER_UTC = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZoneUTC();


		generator.writeStartObject();
		generator.writeFieldName("$date");
		generator.writeString(ISO_FORMATTER_UTC.print(dateTime));
		generator.writeEndObject();
	}
}
