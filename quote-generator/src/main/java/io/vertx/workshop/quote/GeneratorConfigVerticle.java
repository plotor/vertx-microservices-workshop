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
            // MarketDataVerticle 会基于配置项初始化类实例参数，同时启动一个定时任务定时计算并将信息发送到 event bus
            vertx.deployVerticle(MarketDataVerticle.class.getName(), new DeploymentOptions().setConfig(company));
        }

        // Deploy the verticle with a configuration.
        // 启动一个 HTTP 服务，监听 35000 端口用于接收 HTTP 请求
        vertx.deployVerticle(RestQuoteAPIVerticle.class.getName(), new DeploymentOptions().setConfig(this.config()));

        // Publish the services in the discovery infrastructure.
        this.publishMessageSource("market-data", ADDRESS, result -> {
            if (!result.succeeded()) {
                log.info("MARKET-DATA service publish error", result.cause());
            }
            log.info("MARKET-DATA service published : {}", result.succeeded());
        });

        this.publishHttpEndpoint("quotes", "localhost", this.config().getInteger("http.port", 8080), result -> {
            if (result.failed()) {
                log.error("QUOTES (REST ENDPOINT) service publish error", result.cause());
            } else {
                log.info("QUOTES (REST ENDPOINT) service publish finish : {}", result.succeeded());
            }
        });
    }
}
