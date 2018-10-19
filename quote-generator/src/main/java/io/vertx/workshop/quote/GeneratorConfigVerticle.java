package io.vertx.workshop.quote;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.workshop.common.MicroServiceVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a verticle generating "fake" quotes based on the configuration.
 */
public class GeneratorConfigVerticle extends MicroServiceVerticle {

    private static final Logger log = LoggerFactory.getLogger(GeneratorConfigVerticle.class);

    /**
     * The address on which the data are sent.
     */
    public static final String ADDRESS = "market";

    /**
     * This method is called when the verticle is deployed.
     */
    @Override
    public void start() {
        super.start();

        // Read the configuration, and deploy a MarketDataVerticle for each company listed in the configuration.
        JsonArray quotes = this.config().getJsonArray("companies");
        for (Object quote : quotes) {
            JsonObject company = (JsonObject) quote;
            // Deploy another verticle without configuration.
            vertx.deployVerticle(MarketDataVerticle.class.getName(), new DeploymentOptions().setConfig(company));
        }

        // Deploy the verticle with a configuration.
        vertx.deployVerticle(RestQuoteAPIVerticle.class.getName(), new DeploymentOptions().setConfig(this.config()));

        // Publish the services in the discovery infrastructure.
        this.publishMessageSource("market-data", ADDRESS, result -> {
            if (!result.succeeded()) {
                log.info("MARKET-DATA SERVICE PUBLISH ERROR", result.cause());
            }
            log.info("MARKET-DATA SERVICE PUBLISHED : {}", result.succeeded());
        });

        this.publishHttpEndpoint("quotes", "localhost", this.config().getInteger("http.port", 8080), result -> {
            if (result.failed()) {
                log.error("QUOTES (REST ENDPOINT) SERVICE PUBLISH ERROR", result.cause());
            } else {
                log.info("QUOTES (REST ENDPOINT) SERVICE PUBLISHED : {}", result.succeeded());
            }
        });
    }
}
