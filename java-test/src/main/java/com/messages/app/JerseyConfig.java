package com.messages.app;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig
{
	public JerseyConfig()
	{
		register(MessagesRestEndpoints.class);
	}
}