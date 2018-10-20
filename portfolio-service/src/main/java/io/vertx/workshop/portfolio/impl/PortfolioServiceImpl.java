package io.vertx.workshop.portfolio.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.workshop.portfolio.Portfolio;
import io.vertx.workshop.portfolio.PortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The portfolio service implementation.
 */
public class PortfolioServiceImpl implements PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioServiceImpl.class);

    private final Vertx vertx;
    private final Portfolio portfolio;
    private final ServiceDiscovery discovery;

    public PortfolioServiceImpl(Vertx vertx, ServiceDiscovery discovery, double initialCash) {
        this.vertx = vertx;
        this.portfolio = new Portfolio().setCash(initialCash);
        this.discovery = discovery;
    }

    @Override
    public void getPortfolio(Handler<AsyncResult<Portfolio>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(portfolio));
    }

    private void sendActionOnTheEventBus(String action, int amount, JsonObject quote, int newAmount) {
        JsonObject object = new JsonObject()
                .put("action", action)
                .put("quote", quote)
                .put("date", System.currentTimeMillis())
                .put("amount", amount)
                .put("owned", newAmount);
        log.info("Publish action to the event bus, address[{}]", ADDRESS);
        vertx.eventBus().publish(ADDRESS, object);
    }

    @Override
    public void evaluate(Handler<AsyncResult<Double>> resultHandler) {
        HttpEndpoint.getWebClient(discovery, new JsonObject().put("name", "quotes"), client -> {
            if (client.succeeded()) {
                this.computeEvaluation(client.result(), resultHandler);
            } else {
                log.info("Get http service exception", client.cause());
                resultHandler.handle(Future.failedFuture(client.cause()));
            }
        });
    }

    private void computeEvaluation(WebClient webClient, Handler<AsyncResult<Double>> resultHandler) {
        // We need to call the service for each company we own shares
        List<Future> results = portfolio.getShares().entrySet().stream()
                .map(entry -> this.getValueForCompany(webClient, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        // We need to return only when we have all results, for this we create a composite future. The set handler
        // is called when all the futures has been assigned.
        CompositeFuture.all(results).setHandler(
                ar -> {
                    double sum = results.stream().mapToDouble(fut -> (double) fut.result()).sum();
                    resultHandler.handle(Future.succeededFuture(sum));
                });
    }

    private Future<Double> getValueForCompany(WebClient client, String company, int numberOfShares) {
        // Create the future object that will  get the value once the value have been retrieved
        Future<Double> future = Future.future();

        client.get("/?name=" + encode(company)).send(event -> {
            if (event.succeeded()) {
                HttpResponse<Buffer> response = event.result();
                if (200 == response.statusCode()) {
                    future.complete(numberOfShares * response.bodyAsJsonObject().getDouble("bid"));
                } else {
                    future.complete(0.0);
                }
            } else {
                future.fail(event.cause());
            }
        });

        return future;
    }

    @Override
    public void buy(int amount, JsonObject quote, Handler<AsyncResult<Portfolio>> resultHandler) {
        if (amount <= 0) {
            resultHandler.handle(Future.failedFuture(
                    "Cannot buy " + quote.getString("name") + " - the amount must be greater than 0"));
            return;
        }

        if (quote.getInteger("shares") < amount) {
            resultHandler.handle(Future.failedFuture(
                    "Cannot buy " + amount + " - not enough stocks on the market (" + quote.getInteger("shares") + ")"));
            return;
        }

        double price = amount * quote.getDouble("ask");
        String name = quote.getString("name");
        // 1) do we have enough money
        if (portfolio.getCash() >= price) {
            // Yes, buy it
            portfolio.setCash(portfolio.getCash() - price);
            int current = portfolio.getAmount(name);
            int newAmount = current + amount;
            portfolio.getShares().put(name, newAmount);
            this.sendActionOnTheEventBus("BUY", amount, quote, newAmount);
            resultHandler.handle(Future.succeededFuture(portfolio));
        } else {
            resultHandler.handle(Future.failedFuture(
                    "Cannot buy " + amount + " of " + name + " - " + "not enough money, need " + price + ", has " + portfolio.getCash()));
        }
    }

    @Override
    public void sell(int amount, JsonObject quote, Handler<AsyncResult<Portfolio>> resultHandler) {
        if (amount <= 0) {
            resultHandler.handle(Future.failedFuture(
                    "Cannot sell " + quote.getString("name") + " - the amount must be greater than 0"));
            return;
        }

        double price = amount * quote.getDouble("bid");
        String name = quote.getString("name");
        int current = portfolio.getAmount(name);
        // 1) do we have enough stocks
        if (current >= amount) {
            // Yes, sell it
            int newAmount = current - amount;
            if (newAmount == 0) {
                portfolio.getShares().remove(name);
            } else {
                portfolio.getShares().put(name, newAmount);
            }
            portfolio.setCash(portfolio.getCash() + price);
            this.sendActionOnTheEventBus("SELL", amount, quote, newAmount);
            resultHandler.handle(Future.succeededFuture(portfolio));
        } else {
            resultHandler.handle(Future.failedFuture(
                    "Cannot sell " + amount + " of " + name + " - " + "not enough stocks in portfolio"));
        }

    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding");
        }
    }

}
