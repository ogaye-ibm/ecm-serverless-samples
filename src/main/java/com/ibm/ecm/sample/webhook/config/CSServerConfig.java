package com.ibm.ecm.sample.webhook.config;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@StaticInitSafe
@ConfigMapping(prefix = "cs.server")
public interface CSServerConfig {
    String username();
    String password();
    @WithName("objectstore")
    String objectStore();
    @WithName("graphql.url")
    String graphqlUrl();

    WebhookReceiver webhookReceiver();
    interface WebhookReceiver {
        @WithName("url")
        String url();
        @WithName("registration-id")
        String registrationId();
        @WithName("hmac")
        String hmac();

    }
}
