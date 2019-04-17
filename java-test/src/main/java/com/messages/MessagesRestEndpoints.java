package com.messages;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.messages.models.Messages;
import com.messages.models.UserMessages;
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
	public Response getCategory(@PathParam("username") String userName) {

		List<UserMessages> messages = messageService.getMessage(userName);

		return Response.ok().entity(messages).build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{username}/messages")
	public Response sendMessages(@PathParam("username") String userName, Messages messages) {



		return Response.ok().build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{username}/message/reply")
	public Response replyMessage(@PathParam("userName") String userName, Messages message) {


		return null;
	}

}
