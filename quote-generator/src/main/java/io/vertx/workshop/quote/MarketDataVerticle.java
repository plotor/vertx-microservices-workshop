package io.vertx.workshop.quote;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Random;

/**
 * A verticle simulating the evaluation of a company evaluation in a very unrealistic and irrational way.
 * It emits the new data on the `market` address on the event bus.
 */
public class MarketDataVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(MarketDataVerticle.class);

    private String name;
    private int variation;
    private long period;
    private String symbol;
    int stocks;
    private double price;

    double bid;
    double ask;

    int share;
    private double value;

    private final Random random = new Random();

    /**
     * Method called when the verticle is deployed.
     * 初始化当前类实例参数，同时会启动一个定时任务
     */
    @Override
    public void start() {
        // Retrieve the configuration, and initialize the verticle.
        JsonObject config = this.config();
        this.init(config); // 基于当前配置初始化类实例参数

        // Every `period` ms, the given Handler is called.
        vertx.setPeriodic(period, l -> {
            this.compute();
            this.send();
        });
    }

    /**
     * 基于当前配置初始化类实例参数
     *
     * @param config the configuration
     */
    void init(JsonObject config) {
        this.period = config.getLong("period", 3000L);
        this.variation = config.getInteger("variation", 100);
        this.name = config.getString("name");
        Objects.requireNonNull(this.name);
        this.symbol = config.getString("symbol", name);
        this.stocks = config.getInteger("volume", 10000);
        this.price = config.getDouble("price", 100.0);

        this.value = price;
        this.ask = price + random.nextInt(variation / 2);
        this.bid = price + random.nextInt(variation / 2);

        this.share = stocks / 2;
    }

    /**
     * Sends the market data on the event bus.
     */
    private void send() {
        log.debug("Timing send the market data to the event bus, address[{}]", GeneratorConfigVerticle.ADDRESS);
        vertx.eventBus().publish(GeneratorConfigVerticle.ADDRESS, this.toJson());
    }

    /**
     * Compute the new evaluation...
     */
    void compute() {
        log.debug("Timing compute the new evaluation.");
        if (random.nextBoolean()) {
            value = value + random.nextInt(variation);
            ask = value + random.nextInt(variation / 2);
            bid = value + random.nextInt(variation / 2);
        } else {
            value = value - random.nextInt(variation);
            ask = value - random.nextInt(variation / 2);
            bid = value - random.nextInt(variation / 2);
        }

        if (value <= 0) {
            value = 1.0;
        }
        if (ask <= 0) {
            ask = 1.0;
        }
        if (bid <= 0) {
            bid = 1.0;
        }

        if (random.nextBoolean()) {
            // Adjust share
            int shareVariation = random.nextInt(100);
            if (shareVariation > 0 && share + shareVariation < stocks) {
                share += shareVariation;
            } else if (shareVariation < 0 && share + shareVariation > 0) {
                share += shareVariation;
            }
        }
    }

    /**
     * @return a json representation of the market data (quote). The structure is close to
     * <a href="https://en.wikipedia.org/wiki/Market_data">https://en.wikipedia.org/wiki/Market_data</a>.
     */
    private JsonObject toJson() {
        return new JsonObject()
                .put("exchange", "vert.x stock exchange")
                .put("symbol", symbol)
                .put("name", name)
                .put("bid", bid)
                .put("ask", ask)
                .put("volume", stocks)
                .put("open", price)
                .put("shares", share);

    }
}
