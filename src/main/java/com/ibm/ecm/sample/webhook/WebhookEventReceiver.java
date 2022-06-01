package com.ibm.ecm.sample.webhook;

import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/receiver")
public class WebhookEventReceiver {

    private static final Logger LOGGER = Logger.getLogger(WebhookEventReceiver.class);
    @Context
    ServletContext context;

    @Inject
    WebhookEventReceiverDelegate delegate;

    @PostConstruct
    public void init() {
        LOGGER.info("... init");
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
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello RESTEasy";
    }
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listener(String eventJson) {
        return delegate.listenerDelegate(eventJson, context);
    }

    @PreDestroy
    public void cleanup() {
        LOGGER.info("... clean up");
    }

}