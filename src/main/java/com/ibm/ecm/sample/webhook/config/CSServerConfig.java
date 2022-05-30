package com.ibm.ecm.sample.webhook.config;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import org.eclipse.microprofile.config.inject.ConfigProperties;

import javax.decorator.Decorator;

@StaticInitSafe
@ConfigMapping(prefix = "cs.server")
public interface CSServerConfig {
    String username();
    String password();
    @WithName("objectstore")
    String objectStore();
    @WithName("graphql.url")
    String graphqlUrl();
    @WithName("webhook-receiver-url")
    String webhookReceiverUrl();
}
