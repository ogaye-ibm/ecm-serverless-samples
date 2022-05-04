package org.ibm.ecm.sample.weekhook;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class WebhookLifeCycle {

    private static final Logger LOGGER = Logger.getLogger(WebhookLifeCycle.class);

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("The application Webhook Receiver is startining..." + ProfileManager.getActiveProfile());
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("The application Webhook Receiver is stopping...");
    }

}
