package io.vertx.workshop.portfolio.impl;

import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.workshop.common.MicroServiceVerticle;
import io.vertx.workshop.portfolio.PortfolioService;
import static io.vertx.workshop.portfolio.PortfolioService.ADDRESS;
import static io.vertx.workshop.portfolio.PortfolioService.EVENT_ADDRESS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A verticle publishing the portfolio service.
 */
public class PortfolioVerticle extends MicroServiceVerticle {

    private static final Logger log = LoggerFactory.getLogger(PortfolioVerticle.class);

    @Override
    public void start() {
        super.start();

        // Create the service object
        PortfolioServiceImpl service = new PortfolioServiceImpl(vertx, discovery, config().getDouble("money", 10000.00));

        // Register the service proxy on the event bus
        ProxyHelper.registerService(PortfolioService.class, vertx, service, ADDRESS);

        // Publish it in the discovery infrastructure
        this.publishEventBusService("portfolio", ADDRESS, PortfolioService.class, ar -> {
            if (ar.failed()) {
                log.error("Publish portfolio service error.", ar.cause());
            } else {
                log.info("Publish portfolio service : {}", ar.succeeded());
            }
        });

        this.publishMessageSource("portfolio-events", EVENT_ADDRESS, ar -> {
            if (ar.failed()) {
                log.error("Publish portfolio events service error.", ar.cause());
            } else {
                log.info("Publish portfolio events service : {}", ar.succeeded());
            }
        });
    }
}
