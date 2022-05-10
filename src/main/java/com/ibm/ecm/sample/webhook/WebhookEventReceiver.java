package com.ibm.ecm.sample.webhook;

import com.ibm.ecm.sample.webhook.util.Constants;
import com.ibm.ecm.sample.webhook.util.GraphQLAPIUtil;
import com.ibm.ecm.sample.webhook.util.GraphQLCallTemplate;
import com.ibm.ecm.sample.webhook.util.WebhookReceiverLogger;
import org.jboss.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Path("/receiver")
public class WebhookEventReceiver {

    private static final Logger LOGGER = Logger.getLogger(WebhookEventReceiver.class);
    @Context
    ServletContext context;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listener(String eventJson) {
        LOGGER.info("Receiver called with payload: " + eventJson);

        String methodName = "listener";
        WebhookReceiverLogger.info(this.getClass().getName(), methodName);

        // Get Application wide data
        @SuppressWarnings("unchecked")
        List<String> eventsList = (List<String>) context.getAttribute(Constants.EVENTS_JSON_ATTR_KEY);
        if (eventsList == null) {
            eventsList = Collections.synchronizedList(new ArrayList<String>());
            context.setAttribute(Constants.EVENTS_JSON_ATTR_KEY, eventsList);
        }
        String eevExternalEventActionId = (String) context.getAttribute(Constants.EEV_EVENT_ACTION_ID_ATTR_KEY);

        @SuppressWarnings("unchecked")
        List<String> subscriptionIdList = (List<String>) context
                .getAttribute(Constants.SUBSCRIPTION_ID_LIST_ATTR_KEY);
        if (eventsList == null) {
            subscriptionIdList = Collections.synchronizedList(new ArrayList<String>());
            context.setAttribute(Constants.SUBSCRIPTION_ID_LIST_ATTR_KEY, subscriptionIdList);
        }

        @SuppressWarnings("unchecked")
        List<String> docIdList = (List<String>) context.getAttribute(Constants.DOCUMENT_ID_LIST_ATTR_KEY);
        if (docIdList == null) {
            docIdList = Collections.synchronizedList(new ArrayList<String>());
            context.setAttribute(Constants.DOCUMENT_ID_LIST_ATTR_KEY, docIdList);
        }

        WebhookReceiverLogger.info("WebhookReceiver listener webhook JSON response is -");
        WebhookReceiverLogger.info("~~~~~~~~~~~~~~~~~~~~>>>>>>");
        WebhookReceiverLogger.info(eventJson);
        WebhookReceiverLogger.info("~~~~~~~~~~~~~~~~~~~~<<<<<<");
        eventsList.add(eventJson);
        WebhookReceiverLogger
                .info("WebhookReceiver (2) listener webhook EventList size =" + eventsList.size());

        // Process event JSON and call back CPE to get the source document
        JSONObject jsonPayload = null;
        try {
            jsonPayload = new JSONObject(eventJson);
        } catch (Exception e) {
            // Problem with parsing the event JSON
            WebhookReceiverLogger.error("Invalid JSON payload is " + eventJson, e);
            WebhookReceiverLogger.info(this.getClass().getName(), methodName);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }


        // Get event information from the JSON Payload
        String eventType = jsonPayload.get("eventType").toString();
        String objectStoreId = jsonPayload.get("objectStoreId").toString();
        String sourceObjectId = null;
        try {
            sourceObjectId = jsonPayload.get("sourceObjectId").toString();
        } catch (Exception e) {
        }
        String sourceClassId = null;
        try {
            sourceClassId = jsonPayload.get("sourceClassId").toString();
        } catch (Exception e) {
        }
        String receiverRegistrationId = jsonPayload.get("receiverRegistrationId").toString();
        String subscriptionId = (String) jsonPayload.get("subscriptionId");
        WebhookReceiverLogger.info(" =====+++++=====!!!!!", " eventType=" + eventType,
                " objectStoreId=" + objectStoreId, " sourceObjectId=" + sourceObjectId,
                " sourceClassId=" + sourceClassId, " receiverRegistrationId=" + receiverRegistrationId,
                " subscriptionId=" + subscriptionId);

        if (eventType.equals("CreationEvent")) {
            WebhookReceiverLogger.info("WebhookClaim document created with ID=" + sourceObjectId);
        } else if (eventType.equals("UpdateEvent")) {
            WebhookReceiverLogger.info("WebhookClaim document updated with ID=" + sourceObjectId);
        }

        try {
            // Ping GraphQL server first before continuing
            WebhookReceiverLogger.info("  =====+++++=====!!!!! "
                    + "callGraphQLAPI ping with objectStoreId=" + objectStoreId);
            String graphQLSchema = String.format(GraphQLCallTemplate.PING_CONTENTSERVICE_SERVER,
                    objectStoreId);
            JSONObject jsonGraphQLResponse = GraphQLAPIUtil.callGraphQLAPI(graphQLSchema);

            // Handle errors in JSON, if any
            if (jsonGraphQLResponse.has("errors")) {
                JSONArray jsonResponseErrors = jsonGraphQLResponse.getJSONArray("errors");

                // If there is an error, return response based on the first one
                if (jsonResponseErrors.length() > 0) {
                    Object graphQLErrorObj = jsonResponseErrors.get(0);
                    JSONObject graphQLError = new JSONObject(graphQLErrorObj.toString());

                    /*
                     * Return with the error (400 by default if there is no error code in the
                     * extensons)
                     */
                    String errorMessage = graphQLError.getString("message");
                    int status = 400;

                    // Check for statusCode in extensions
                    JSONObject jsonExtentions = graphQLError.getJSONObject("extensions");
                    if (jsonExtentions != null) {
                        status = jsonExtentions.getInt("statusCode");
                    }

                    // Return with the exception
                    WebhookReceiverLogger.error("Error contacting CPE", errorMessage);
                    WebhookReceiverLogger.info(this.getClass().getName(), methodName);
                    return Response.status(status).build();
                }
            }
        } catch (Exception e) {
            WebhookReceiverLogger.error("Error contacting CPE", e);
        }



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
    public String hello(String test) {
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