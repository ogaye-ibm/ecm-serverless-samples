package com.ibm.ecm.sample.webhook;

import com.ibm.ecm.sample.webhook.config.CSServerConfig;
import com.ibm.ecm.sample.webhook.util.*;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;
import org.jboss.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class WebhookLifeCycle {

    @Inject
    CSServerConfig csServerConfig;

    @Inject
    GraphQLAPIUtil graphQLAPIUtil;

    private static final Logger LOGGER = Logger.getLogger(WebhookLifeCycle.class);

    //@Context ServletContext context;

    void onStart(@Observes StartupEvent ev) {

        LOGGER.info("The application Webhook Receiver is starting..." + ProfileManager.getActiveProfile());

        String methodName = "contextInitialized";
        WebhookReceiverLogger.entering(this.getClass().getName(),
                methodName);
        //ServletContext context = ev.getServletContext();

        // Ping the GraphQL server
        String objectStoreId = csServerConfig.objectStore();
        //CSServerInfo.CS_SERVER_OBJECTSTORE;

        WebhookReceiverLogger.debug(
                "WebhookReceiverServletContextListener starting up-");
        WebhookReceiverLogger.debug("~~~~~~~~~~~~~~~~~~~~");

        WebhookReceiverLogger.debug("  =====+++++=====!!!!! "
                + "callGraphQLAPI ping with objectStoreId="
                + objectStoreId);
        String graphQLSchema = String.format(
                GraphQLCallTemplate.PING_CONTENTSERVICE_SERVER, objectStoreId);
        JSONObject jsonGraphQLResponse =
                graphQLAPIUtil.callGraphQLAPI(graphQLSchema);

        // Handle errors in JSON, if any
        if (jsonGraphQLResponse.has("errors")) {
            JSONArray jsonResponseErrors = jsonGraphQLResponse
                    .getJSONArray("errors");

            // If there is an error, log response based on the first one
            if (jsonResponseErrors.length() > 0) {
                Object graphQLErrorObj = jsonResponseErrors.get(0);
                JSONObject graphQLError = new JSONObject(
                        graphQLErrorObj.toString());

                // Log the error
                String errorMessage = graphQLError.getString("message");
                WebhookReceiverLogger.error("Error contacting CPE",
                        errorMessage);
            }
        }

        // Create the event action and subscription
        String eventActionName = Constants.EVENTACTION_NAME;
        String eventActionDesc = Constants.EVENTACTION_DESCRIPTION;
        String webhookSecret = csServerConfig.webhookReceiver().hmac();
        //.Constants.HMAC_CREDENTIAL_SECRET;
        String webhookReceiverURL = csServerConfig.webhookReceiver().url();
        //.webhookReceiverUrl(); //CSServerInfo.WEBHOOK_RECEIVER_URL;
        String webhookReceiverId = csServerConfig.webhookReceiver().registrationId();
        //Constants.WEBHOOK_RECEIVER_REGISTRATION_ID;
        String subscriptionName = Constants.CREATE_EVENTSUBSCRIPTION_NAME;
        String subscriptionDesc = Constants.CREATE_EVENTSUBSCRIPTION_DESCRIPTION;
        WebhookReceiverLogger.debug("  =====+++++=====!!!!! "
                        + "callGraphQLAPI eevCreateExternalEventAction "
                        + "mutation with objectStoreId=" + objectStoreId,
                " eventActionName=" + eventActionName,
                " eventActionDesc=" + eventActionDesc,
                " webhookSecret=" + webhookSecret,
                " webhookReceiverURL=" + webhookReceiverURL,
                " webhookReceiverId=" + webhookReceiverId,
                " subscriptionName=" + subscriptionName,
                " subscriptionDesc=" + subscriptionDesc);
        graphQLSchema = String.format(
                GraphQLCallTemplate.CREATE_EVENTACTION_WITH_CLASSSUBSCRIPTION,
                objectStoreId, eventActionName, eventActionDesc, webhookSecret,
                webhookReceiverURL, webhookReceiverId, subscriptionName,
                subscriptionDesc);
        jsonGraphQLResponse = graphQLAPIUtil.callGraphQLAPI(graphQLSchema);

        String eevExternalEventActionId = null;
        try {
            // Handle errors in JSON, if any
            if (jsonGraphQLResponse.has("errors")) {
                JSONArray jsonResponseErrors = jsonGraphQLResponse
                        .getJSONArray("errors");

                // If there is an error, log response based on the first one
                if (jsonResponseErrors.length() > 0) {
                    Object graphQLErrorObj = jsonResponseErrors.get(0);
                    JSONObject graphQLError = new JSONObject(
                            graphQLErrorObj.toString());

                    // Log the error
                    String errorMessage = graphQLError.getString("message");
                    WebhookReceiverLogger.error(
                            "Error creating Webhook External Event Action",
                            errorMessage);
                }
            }

            // Get EEV External Event Action ID
            JSONObject jsonResponseData =
                    jsonGraphQLResponse.getJSONObject("data");
            JSONObject jsonResponseEEVAction =
                    jsonResponseData.getJSONObject("eevCreateExternalEventAction");
            eevExternalEventActionId =
                    jsonResponseEEVAction.getString("id");

            WebhookReceiverLogger.info(
                    "Successfully created the Webhook External Event Action "
                            + "with ID=" + eevExternalEventActionId);
           // context.setAttribute(Constants.EEV_EVENT_ACTION_ID_ATTR_KEY, eevExternalEventActionId);

        } catch (JSONException je) {
            String errorMessage = "Unable to parse response JSON to get Webhook"
                    + " external event action ID";
            WebhookReceiverLogger.error(errorMessage, je);
        } catch (Exception e) {
            WebhookReceiverLogger.error(
                    "Unable to get ID for eevCreateExternalEventAction", e);
        }

        // Get the created subscriptions for later deletion
        if (eevExternalEventActionId != null) {
            // Retrieve the event action and associated subscription
            WebhookReceiverLogger.debug("  =====+++++=====!!!!! "
                            + "callGraphQLAPI fetch eevExternalEventAction "
                            + "query with objectStoreId=" + objectStoreId,
                    " eevExternalEventActionId=" + eevExternalEventActionId);
            graphQLSchema = String.format(
                    GraphQLCallTemplate.FETCH_EVENTACTION_WITH_CLASSSUBSCRIPTION,
                    objectStoreId, eevExternalEventActionId);
            jsonGraphQLResponse = graphQLAPIUtil.callGraphQLAPI(graphQLSchema);

            List<String> subscriptionIdList =
                    Collections.synchronizedList(new ArrayList<String>());
            try {
                // Handle errors in JSON, if any
                if (jsonGraphQLResponse.has("errors")) {
                    JSONArray jsonResponseErrors = jsonGraphQLResponse
                            .getJSONArray("errors");

                    // If there is an error, log response based on the first one
                    if (jsonResponseErrors.length() > 0) {
                        Object graphQLErrorObj = jsonResponseErrors.get(0);
                        JSONObject graphQLError = new JSONObject(
                                graphQLErrorObj.toString());

                        // Log the error
                        String errorMessage = graphQLError.getString("message");
                        WebhookReceiverLogger.error(
                                "Error retrieving Webhook External Event subscriptions",
                                errorMessage);
                    }
                }

                // Get IDs of subscriptions for associated event action
                JSONObject jsonResponseData =
                        jsonGraphQLResponse.getJSONObject("data");
                JSONObject jsonResponseEEVAction =
                        jsonResponseData.getJSONObject("eevExternalEventAction");
                JSONObject jsonResponseSubscriptions =
                        jsonResponseEEVAction.getJSONObject("subscriptions");
                JSONArray jsonSubscriptionsArray =
                        jsonResponseSubscriptions.getJSONArray("subscriptions");

                // Add the IDs for each subscription to the array for later
                for (Object sub : jsonSubscriptionsArray) {
                    JSONObject s = new JSONObject(sub.toString());
                    String sId = s.getString("id");
                    subscriptionIdList.add(sId.toString());
                }

                WebhookReceiverLogger.info("List of Webhook subscription IDs="
                        + Arrays.toString(subscriptionIdList.toArray()));

                // Save External Event Action ID for update
                //context.setAttribute(Constants.SUBSCRIPTION_ID_LIST_ATTR_KEY, subscriptionIdList);

            } catch (JSONException je) {
                String errorMessage = "Unable to parse response JSON to get "
                        + "Webhook external event subscription IDs";
                WebhookReceiverLogger.error(errorMessage, je);
            } catch (Exception e) {
                WebhookReceiverLogger.error(
                        "Unable to get IDs from eevExternalEventAction", e);
            }
        }

        WebhookReceiverLogger.exiting(this.getClass().getName(),
                methodName);

    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("The application Webhook Receiver is stopping...");
    }

}
