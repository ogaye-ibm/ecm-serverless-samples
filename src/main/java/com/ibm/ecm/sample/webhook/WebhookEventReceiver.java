package com.ibm.ecm.sample.webhook;

import com.ibm.ecm.sample.webhook.util.Constants;
import com.ibm.ecm.sample.webhook.util.GraphQLAPIUtil;
import com.ibm.ecm.sample.webhook.util.GraphQLCallTemplate;
import com.ibm.ecm.sample.webhook.util.WebhookReceiverLogger;
import org.jboss.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
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

    @PostConstruct
    public void init() {
        LOGGER.info("... init");
    }

    @PreDestroy
    public void cleanup() {
        LOGGER.info("... cleaned up");
    }

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
            if (jsonGraphQLResponse != null && jsonGraphQLResponse.has("errors")) {
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

        /*
         * Check the created event action and its associated subscription. Update the
         * subscription from being subscribed to Creation Events on the Claim document
         * subclass to being subscribed to Update Events only. If the change was already
         * made, skip the change to the subscription. Also, only do this after the first
         * creation event triggers, such that if the triggering event is not a creation
         * event, skip the update of the subscription
         */
        try {
            if (eventType.equals("CreationEvent") && (eevExternalEventActionId != null)) {
                // Retrieve the event action and associated subscription
                WebhookReceiverLogger.debug(
                        "  =====+++++=====!!!!! "
                                + "callGraphQLAPI eevExternalEventAction query "
                                + "with objectStoreId=" + objectStoreId,
                        " eevExternalEventActionId=" + eevExternalEventActionId);
                String graphQLSchema = String.format(
                        GraphQLCallTemplate.FETCH_EVENTACTION_WITH_CLASSSUBSCRIPTION,
                        objectStoreId, eevExternalEventActionId);
                JSONObject jsonGraphQLResponse = GraphQLAPIUtil.callGraphQLAPI(graphQLSchema);

                // Check if we should update the associated subscription
                boolean updateSubscription = false;
                try {
                    // Handle errors in JSON, if any
                    if (jsonGraphQLResponse.has("errors")) {
                        JSONArray jsonResponseErrors = jsonGraphQLResponse
                                .getJSONArray("errors");

                        // If there is an error, return response based on the first one
                        if (jsonResponseErrors.length() > 0) {
                            Object graphQLErrorObj = jsonResponseErrors.get(0);
                            JSONObject graphQLError = new JSONObject(
                                    graphQLErrorObj.toString());

                            /*
                             * Return with the error (400 by default if there is no error
                             * code in the extensons)
                             */
                            String errorMessage = graphQLError.getString("message");
                            int status = 400;

                            // Check for statusCode in extensions
                            JSONObject jsonExtentions = graphQLError
                                    .getJSONObject("extensions");
                            if (jsonExtentions != null) {
                                status = jsonExtentions.getInt("statusCode");
                            }

                            // Return with the exception
                            WebhookReceiverLogger.error(
                                    "Error retrieving Webhook External Event "
                                            + "Action and subscriptions",
                                    errorMessage);
                            WebhookReceiverLogger.exiting(this.getClass().getName(),
                                    methodName);
                            return Response.status(status).build();
                        }
                    }

                    // Get IDs of subscriptions for associated event action
                    JSONObject jsonResponseData = jsonGraphQLResponse.getJSONObject("data");
                    JSONObject jsonResponseEEVAction = jsonResponseData
                            .getJSONObject("eevExternalEventAction");
                    JSONObject jsonResponseSubscriptions = jsonResponseEEVAction
                            .getJSONObject("subscriptions");
                    JSONArray jsonSubscriptionsArray = jsonResponseSubscriptions
                            .getJSONArray("subscriptions");

                    /*
                     * Check if the subscription that the event triggered is associated with the
                     * original event action. If it is, update the subscription.
                     */
                    for (Object sub : jsonSubscriptionsArray) {
                        JSONObject s = new JSONObject(sub.toString());
                        String subId = s.getString("id");

                        // Find the subscription for the triggering event
                        if (subId.equals(subscriptionId)) {
                            updateSubscription = true;
                        }
                    }

                } catch (JSONException je) {
                    String errorMessage = "Unable to parse response JSON to get Webhook "
                            + "external event subscription IDs";
                    WebhookReceiverLogger.error(errorMessage, je);
                } catch (Exception e) {
                    WebhookReceiverLogger.error("Unable to get IDs from eevExternalEventAction", e);
                }

                // Update the event action and associated subscription if the
                // subscription is still tied to Creation events
                if (updateSubscription) {
                    String eventActionName = Constants.EVENTACTION_NAME;
                    String newSubscriptionName = Constants.UPDATE_EVENTSUBSCRIPTION_NAME;
                    String newSubscriptionDesc = Constants.UPDATE_EVENTSUBSCRIPTION_DESCRIPTION;

                    WebhookReceiverLogger.debug("  =====+++++=====!!!!! "
                                    + "callGraphQLAPI eevUpdateExternalEventAction "
                                    + "mutation with objectStoreId==" + objectStoreId,
                            " eevExternalEventActionId=" + eevExternalEventActionId,
                            " eventActionName=" + eventActionName,
                            " newSubscriptionName=" + newSubscriptionName,
                            " newScriptionDesc=" + newSubscriptionDesc,
                            " subscriptionId=" + subscriptionId);
                    graphQLSchema = String.format(
                            GraphQLCallTemplate.UPDATE_EVENTACTION_WITH_CLASSSUBSCRIPTION,
                            objectStoreId, eevExternalEventActionId, eventActionName,
                            newSubscriptionName, newSubscriptionDesc, subscriptionId);
                    jsonGraphQLResponse = GraphQLAPIUtil.callGraphQLAPI(graphQLSchema);

                    // Handle errors in JSON, if any
                    if (jsonGraphQLResponse.has("errors")) {
                        JSONArray jsonResponseErrors = jsonGraphQLResponse
                                .getJSONArray("errors");

                        // If there is an error, return response based on the first one
                        if (jsonResponseErrors.length() > 0) {
                            Object graphQLErrorObj = jsonResponseErrors.get(0);
                            JSONObject graphQLError = new JSONObject(
                                    graphQLErrorObj.toString());

                            /*
                             * Return with the error (400 by default if there is no error
                             * code in the extensons)
                             */
                            String errorMessage = graphQLError.getString("message");
                            int status = 400;

                            // Check for statusCode in extensions
                            JSONObject jsonExtentions = graphQLError
                                    .getJSONObject("extensions");
                            if (jsonExtentions != null) {
                                status = jsonExtentions.getInt("statusCode");
                            }

                            // Return with the exception
                            WebhookReceiverLogger.error(
                                    "Error updating Webhook Exernal Event Action",
                                    errorMessage);
                            WebhookReceiverLogger.exiting(this.getClass().getName(),
                                    methodName);
                            return Response.status(status).build();
                        }
                    }

                    WebhookReceiverLogger.info("Changed Webhook external event " + "action with ID "
                            + eevExternalEventActionId
                            + " from being subscribed to Creation events to "
                            + "being subscribed to Update events.");
                }
            }
        } catch (Exception e) {
            WebhookReceiverLogger.error("Error updating subscription", e);
        }

        /*
         * Retrieve the event source object from CPE using GraphQL calls
         *
         * Note: The source object information is also in the event JSON in SourceObject
         * in properties. However, the code below for using GraphQL to retrieve the
         * document is used more as a demonstration. One advantage of using the GraphQL
         * call though is that we can get the most up to date version of the document.
         * Given that the Webhook event action is an asynchronous event action, some
         * time may have passed between the triggering event and when the Webhook call
         * is processed.
         */
        try {
            if (sourceObjectId != null) {
                WebhookReceiverLogger.debug("  =====+++++=====!!!!! "
                                + "callGraphQLAPI document query with objectStoreId==" + objectStoreId,
                        " sourceObjectId=" + sourceObjectId);
                String graphQLSchema = String.format(GraphQLCallTemplate.GET_DOCUMENT, objectStoreId,
                        sourceObjectId);
                JSONObject jsonGraphQLResponse = GraphQLAPIUtil.callGraphQLAPI(graphQLSchema);

                // Handle errors in JSON, if any
                if (jsonGraphQLResponse.has("errors")) {
                    JSONArray jsonResponseErrors = jsonGraphQLResponse.getJSONArray("errors");

                    // If there is an error, return response based on the first one
                    if (jsonResponseErrors.length() > 0) {
                        Object graphQLErrorObj = jsonResponseErrors.get(0);
                        JSONObject graphQLError = new JSONObject(graphQLErrorObj.toString());

                        /*
                         * Return with the error (400 by default if there is no error code in
                         * the extensons)
                         */
                        String errorMessage = graphQLError.getString("message");
                        int status = 400;

                        // Check for statusCode in extensions
                        JSONObject jsonExtentions = graphQLError.getJSONObject("extensions");
                        if (jsonExtentions != null) {
                            status = jsonExtentions.getInt("statusCode");
                        }

                        // Return with the exception
                        WebhookReceiverLogger.error("Error retrieving WebhookClaim document",
                                errorMessage);
                        WebhookReceiverLogger.exiting(this.getClass().getName(), methodName);
                        return Response.status(status).build();
                    }
                }

                if (eventType.equals("CreationEvent")) {
                    // Keep track of document for later deletion
                    docIdList.add(sourceObjectId);
                }

                try {
                    // Get Properties from Claim document JSON
                    JSONObject jsonResponseData = jsonGraphQLResponse.getJSONObject("data");
                    JSONObject jsonDocument = jsonResponseData.getJSONObject("document");
                    JSONArray jsonProperties = jsonDocument.getJSONArray("properties");

                    /*
                     * Iterate through the properties object for the custom properties on the claim
                     * document.
                     */
                    Double dollarAmount = null;
                    String priority = null;
                    Integer riskFactor = null;
                    for (Object propertyObj : jsonProperties) {
                        JSONObject prop = new JSONObject(propertyObj.toString());
                        String propId = prop.getString("id");

                        /*
                         * Handle property based on the ID, which is the symbolic name. If there
                         * is no value field on the property, skip it.
                         */
                        if (propId.equals("WebhookDollarAmount") && prop.has("value")) {
                            Object dollarAmountObj = prop.get("value");
                            if (dollarAmountObj != null) {
                                /*
                                 * Check if the object is null before converting to a
                                 * double
                                 */
                                dollarAmount = Double.parseDouble(
                                        dollarAmountObj.toString());
                            }
                        } else if (propId.equals("WebhookPriority") && prop.has("value")) {
                            priority = prop.getString("value");
                        } else if (propId.equals("WebhookRiskFactor") && prop.has("value")) {
                            Object riskFactorObj = prop.get("value");
                            if ((riskFactorObj != null)
                                    && (!riskFactorObj.toString().equals("null"))) {
                                /*
                                 * Check if the object is null before converting to an
                                 * integer
                                 */
                                riskFactor = Integer.parseInt(riskFactorObj.toString());
                            }

                        }
                    }

                    WebhookReceiverLogger
                            .debug("WebhookClaim document Dollar Amount=" + dollarAmount);
                    WebhookReceiverLogger.debug("WebhookClaim document Claim Priority=" + priority);
                    WebhookReceiverLogger.debug("WebhookClaim document Risk Factor=" + riskFactor);

                    /*
                     * In a real application, the Webhook Receiver listener code would then take in
                     * the dollar amount, priority, and then do processing on those values.
                     * Potentially, this could involve calling out to other applications to handle
                     * the processing of the claim. In this example, we will leave that processing
                     * as an exercise for the user and just set the risk factor to a specific
                     * number.
                     */

                    // ***Your processing of the Claim document here.***

                    /*
                     * Make sure to not attempt to update the Priority if it has already been set.
                     * Otherwise, we will get into an infinite loop.
                     */
                    if (riskFactor == null) {
                        // Set Risk Factor to hard-coded value
                        riskFactor = 15;

                        /*
                         * Build the Array of properties to update in GraphQL Note: GraphQL is
                         * subtly different from JSON in that we do not want to wrap the
                         * property name in double quotes. Thus, we cannot use a JSON array or
                         * JSON object to build the propsArray string.
                         */
                        String propsArray = "[{ WebhookRiskFactor: " + riskFactor + " }]";

                        WebhookReceiverLogger.debug(
                                "  =====+++++=====!!!!! callGraphQLAPI "
                                        + "updateDocument mutation with "
                                        + "objectStoreId=" + objectStoreId,
                                " sourceObjectId=" + sourceObjectId,
                                " and properties=" + propsArray.toString());
                        graphQLSchema = String.format(GraphQLCallTemplate.UPDATE_DOCUMENT,
                                objectStoreId, sourceObjectId, propsArray.toString());
                        jsonGraphQLResponse = GraphQLAPIUtil.callGraphQLAPI(graphQLSchema);

                        // Handle errors in JSON, if any
                        if (jsonGraphQLResponse.has("errors")) {
                            JSONArray jsonResponseErrors = jsonGraphQLResponse
                                    .getJSONArray("errors");

                            // If there is an error, return response based on the first one
                            if (jsonResponseErrors.length() > 0) {
                                Object graphQLErrorObj = jsonResponseErrors.get(0);
                                JSONObject graphQLError = new JSONObject(
                                        graphQLErrorObj.toString());

                                /*
                                 * Return with the error (400 by default if there is no
                                 * error code in the extensons)
                                 */
                                String errorMessage = graphQLError.getString("message");
                                int status = 400;

                                // Check for statusCode in extensions
                                JSONObject jsonExtentions = graphQLError
                                        .getJSONObject("extensions");
                                if (jsonExtentions != null) {
                                    status = jsonExtentions.getInt("statusCode");
                                }

                                // Return with the exception
                                WebhookReceiverLogger.error(
                                        "Error updating WebhookClaim document",
                                        errorMessage);
                                WebhookReceiverLogger.exiting(this.getClass().getName(),
                                        methodName);
                                return Response.status(status).build();
                            }
                        }

                        WebhookReceiverLogger
                                .info("Updated WebhookClaim document Risk Factor to "
                                        + riskFactor);
                    } else {
                        WebhookReceiverLogger.info("No change made to WebhookClaim document.");
                    }

                } catch (JSONException je) {
                    String errorMessage = "Problem with parsing or building JSON "
                            + "for retrieving or updating document";
                    WebhookReceiverLogger.error(errorMessage, je);
                } catch (Exception e) {
                    WebhookReceiverLogger.error("Problem with retrieving or updating document", e);
                }
            }

        } catch (Exception e) {
            WebhookReceiverLogger.error("Error updating document", e);
        }

        return Response.ok("All good!").build();
    }

}