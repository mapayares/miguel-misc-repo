package com.messages.app;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import com.messages.errors.GeneralErrorMapper;
import com.messages.errors.MongodbExceptionMapper;
import com.messages.errors.ObjectMapperErrorMapper;
import com.messages.errors.UserNotFoundExceptionMapper;

@Component
public class JerseyConfig extends ResourceConfig
{
	public JerseyConfig()
	{
		register(MessagesRestEndpoints.class);
		register(UserNotFoundExceptionMapper.class);
		register(MongodbExceptionMapper.class);
		register(ObjectMapperErrorMapper.class);
		register(GeneralErrorMapper.class);
	}
}