package com.messages.utilities;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;

public class BsonModule extends Module {

	@Override
	public String getModuleName() {
		return "bson-module";
	}

	@Override
	public Version version() {
		return new Version(1, 0, 0, "");
	}

	@Override
	public void setupModule(SetupContext context) {
		context.addSerializers(new BsonSerializers());
		context.addDeserializers(new BsonDeserializer());

	}
}
