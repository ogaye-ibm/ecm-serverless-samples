package org.ibm.ecm.sample.weekhook;

import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/receiver")
public class WebhookEventReceiver {

    private static final Logger LOGGER = Logger.getLogger(WebhookEventReceiver.class);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listener(String eventJson) {
        System.out.println("Receiver called with payload: " + eventJson);
        LOGGER.info("Receiver called with payload: " + eventJson);
        return Response.ok("All good!").build();
    }

    @GET
    @Path("/live")
    @Produces(MediaType.TEXT_PLAIN)
    public String live() {
        return "live";
    }

    @GET
    @Path("/ready")
    @Produces(MediaType.TEXT_PLAIN)
    public String ready() {
        return "ready";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/hello")
    public String hello() {
        return "Hello RESTEasy";
    }

    @PostConstruct
    public void init() {
        LOGGER.info("... init");
    }

    @PreDestroy
    public void cleanup() {
        LOGGER.info("... cleaned up");
    }

}