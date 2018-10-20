package io.vertx.workshop.trader.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.servicediscovery.types.MessageSource;
import io.vertx.workshop.common.MicroServiceVerticle;
import io.vertx.workshop.portfolio.PortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A compulsive trader...
 */
public class JavaCompulsiveTraderVerticle extends MicroServiceVerticle {

    private static final Logger log = LoggerFactory.getLogger(JavaCompulsiveTraderVerticle.class);

    @Override
    public void start(Future<Void> future) {
        super.start();

        // 1. Initialize the trader
        String company = TraderUtils.pickACompany();
        int numberOfShares = TraderUtils.pickANumber();
        log.info("Initialize company[{}] and numberOfShares[{}]", company, numberOfShares);

        // 2. Retrieve the 2 services we use
        Future<MessageConsumer<JsonObject>> marketFuture = Future.future();
        MessageSource.getConsumer(discovery, new JsonObject().put("name", "market-data"), marketFuture.completer());

        Future<PortfolioService> portfolioFuture = Future.future();
        EventBusService.getProxy(discovery, PortfolioService.class, portfolioFuture.completer());

        // 3. When both have been retrieve, apply the trading logic on every new market data.
        CompositeFuture.all(marketFuture, portfolioFuture).setHandler(event -> {
            if (event.failed()) {
                future.fail(event.cause());
            } else {
                marketFuture.result().handler(message ->
                        TraderUtils.dumbTradingLogic(company, numberOfShares, portfolioFuture.result(), message.body()));
            }
        });

        future.complete();
    }

}
