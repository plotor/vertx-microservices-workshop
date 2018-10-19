package io.vertx.workshop.quote;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This verticle exposes a HTTP endpoint to retrieve the current / last values of the maker data (quotes).
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class RestQuoteAPIVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(RestQuoteAPIVerticle.class);

    private Map<String, JsonObject> quotes = new HashMap<>();

    @Override
    public void start() throws Exception {
        vertx.eventBus().<JsonObject>consumer(GeneratorConfigVerticle.ADDRESS).handler(message -> {
            JsonObject quote = message.body();
            if (null != quote && quote.containsKey("name")) {
                quotes.put(quote.getString("name"), quote);
            }
        });

        vertx.createHttpServer()
                .requestHandler(request -> {
                    HttpServerResponse response = request.response().putHeader("content-type", "application/json");
                    String name = request.getParam("name");
                    if (StringUtils.isNotBlank(name)) {
                        JsonObject quote = quotes.get(name);
                        if (null == quote) {
                            log.info("No quote found for name[{}] and response 404.", name);
                            response.setStatusCode(404);
                        } else {
                            log.info("Found quote for name[{}] and return data.", name);
                            response.end(Json.encodePrettily(quote));
                        }
                    } else {
                        log.info("Missing param 'name' and return all quotes.");
                        response.end(Json.encodePrettily(quotes));
                    }
                }).listen(this.config().getInteger("http.port"), ar -> {
            if (ar.succeeded()) {
                log.info("Start http server success, listen on port : {}", this.config().getInteger("http.port"));
            } else {
                log.error("Start http server error", ar.cause());
            }
        });
    }
}
