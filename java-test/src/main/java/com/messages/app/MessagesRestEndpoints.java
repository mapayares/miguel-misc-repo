package com.messages.app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.messages.models.UserMessage;
import com.messages.services.MessageService;

@Component
@Path("/application")
public class MessagesRestEndpoints {

	private final MessageService messageService;

	@Autowired
	public MessagesRestEndpoints(final MessageService messageService) {
		this.messageService = messageService;

	}

	@POST
	@Path("/login")
	public Response login(@FormParam("username") String userName, @FormParam("password") String password) {




		return Response.ok().build();
	}

	@POST
	@Path("/logout")
	public Response logout() {

		return Response.ok().build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{username}/messages")
	public Response getMessages(@PathParam("username") String userName) {
		if (Strings.isNullOrEmpty(userName)) {
			return Response.status(Status.BAD_REQUEST).entity("User name cannot be empty").build();
		}

		List<UserMessage> messages = messageService.getMessage(userName);
		return Response.ok().entity(messages).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/messages")
	public Response sendMessages(UserMessage message) {
		if (message == null || Strings.isNullOrEmpty(message.getSender())) {
			return Response.status(Status.BAD_REQUEST).entity("JSON object is incomplete").build();
		}

		long messageId = messageService.sendMessage(message);
		Map<String, Long> entity = new HashMap<>();
		entity.put("message_id", messageId);
		return Response.ok().entity(entity).build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/message/reply")
	public Response replyMessage(UserMessage message) {
		if (message == null || message.getMessageId() == 0L || Strings.isNullOrEmpty(message.getFollower())) {
			return Response.status(Status.BAD_REQUEST).entity("JSON object is incomplete").build();
		}

		messageService.replyMessage(message);

		return Response.ok().build();
	}

	@PUT
	@Path("/{user}/messages/follow")
	public Response followUser(@PathParam("user") String userToFollow, @QueryParam("follower") String follower,
			@DefaultValue("false") boolean follow) {
		if (Strings.isNullOrEmpty(userToFollow) || Strings.isNullOrEmpty(follower)) {
			return Response.status(Status.BAD_REQUEST).entity("User to follow or follower is empty").build();
		}

		messageService.followUser(userToFollow, follower, follow);

		return Response.ok().build();
	}




}
