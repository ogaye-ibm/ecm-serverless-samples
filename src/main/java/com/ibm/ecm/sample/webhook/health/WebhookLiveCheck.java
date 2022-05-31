package com.ibm.ecm.sample.webhook.health;

import com.ibm.ecm.sample.webhook.WebhookEventReceiver;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@Liveness
@ApplicationScoped
public class WebhookLiveCheck implements HealthCheck {

    @Inject
    WebhookEventReceiver webhookEventReceiver;

    @Override
    public HealthCheckResponse call() {
        webhookEventReceiver.live();
        return HealthCheckResponse.named("Webhook Event Receiver REST Endpoint Liveness").up().build();
    }
}
