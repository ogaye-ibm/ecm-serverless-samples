package org.ibm.ecm.sample.weekhook.health;

import org.ibm.ecm.sample.weekhook.WebhookEventReceiver;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@Readiness
@ApplicationScoped
public class WebhookHealthCheck implements HealthCheck {

    @Inject
    WebhookEventReceiver exampleResource;

    @Override
    public HealthCheckResponse call() {
        exampleResource.ready();
        return HealthCheckResponse.named("Webhook Event Receiver REST Endpoint").up().build();
    }

}
