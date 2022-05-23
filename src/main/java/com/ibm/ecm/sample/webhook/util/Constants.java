
package com.ibm.ecm.sample.webhook.util;
public class Constants {

    // Event action and subscription names and descriptions
    public static final String CREATE_EVENTSUBSCRIPTION_NAME =
            "WebhookReceiverDocumentCreationSub";
    public static final String CREATE_EVENTSUBSCRIPTION_DESCRIPTION =
            "Content Event Sample Webhook Receiver Document Creation Subscription";
    public static final String UPDATE_EVENTSUBSCRIPTION_NAME =
            "WebhookReceiverDocumentUpdateSub";
    public static final String UPDATE_EVENTSUBSCRIPTION_DESCRIPTION =
            "Content Event Sample Webhook Receiver Document Update Subscription";
    public static final String EVENTACTION_NAME =
            "WebhookReceiverDocumentEventAction";
    public static final String EVENTACTION_DESCRIPTION =
            "Content Event Sample Webhook Receiver Document Event Action";

    // Webhook Event Action properties
    public static final String WEBHOOK_RECEIVER_REGISTRATION_ID = "webhook";
    public static final String HMAC_CREDENTIAL_SECRET = "4aafcb1d-cc3d-4e31-b85f-26e39dba2b73";
    
    /*
     * ServletContext attribute key constants, which are used to share data
     * across classes
     */
    public static final String EVENTS_JSON_ATTR_KEY = "EVENTS_JSON";
    public static final String EEV_EVENT_ACTION_ID_ATTR_KEY = "EEV_ACTION_ID";
    public static final String SUBSCRIPTION_ID_LIST_ATTR_KEY = "SUB_ID_LIST";
    public static final String DOCUMENT_ID_LIST_ATTR_KEY = "DOC_ID_LIST";
    
    // Logging constants
    public static final String LOGGER_RECEIVER = "com.ibm.ecm.sample.webhook";

}
