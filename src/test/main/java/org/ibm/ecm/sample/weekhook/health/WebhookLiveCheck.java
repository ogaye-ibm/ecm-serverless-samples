package org.ibm.ecm.sample.weekhook.health;

import org.ibm.ecm.sample.weekhook.WebhookEventReceiver;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@Liveness
@ApplicationScoped
public class WebhookLiveCheck implements HealthCheck {

    @Inject
    WebhookEventReceiver exampleResource;

    @Override
    public HealthCheckResponse call() {
        exampleResource.live();
        return HealthCheckResponse.named("Webhook Event Receiver REST Endpoint").up().build();
    }
}
