package com.ibm.ecm.sample.webhook.health;

import com.ibm.ecm.sample.webhook.WebhookEventReceiver;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@Readiness
@ApplicationScoped
public class WebhookHealthCheck implements HealthCheck {

    @Inject
    WebhookEventReceiver webhookEventReceiver;

    @Override
    public HealthCheckResponse call() {
        webhookEventReceiver.ready();
        return HealthCheckResponse.named("Webhook Event Receiver REST Endpoint Readiness").up().build();
    }

}
