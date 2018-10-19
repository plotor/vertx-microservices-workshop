package io.vertx.workshop.common;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Launcher extends io.vertx.core.Launcher {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
        new Launcher().dispatch(args);
    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {
        options.setClustered(true).setClusterHost("127.0.0.1");
    }

    @Override
    public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
        super.beforeDeployingVerticle(deploymentOptions);

        if (deploymentOptions.getConfig() == null) {
            deploymentOptions.setConfig(new JsonObject());
        }

        File conf = new File("src/conf/config.json");
        deploymentOptions.getConfig().mergeIn(this.getConfiguration(conf));
    }

    /**
     * 加载指定配置文件为 {@link JsonObject}
     *
     * @param config
     * @return
     */
    private JsonObject getConfiguration(File config) {
        JsonObject conf = new JsonObject();
        if (config.isFile()) {
            log.info("Reading config file : {}", config.getAbsolutePath());
            try (Scanner scanner = new Scanner(config).useDelimiter("\\A")) {
                String sconf = scanner.next();
                try {
                    conf = new JsonObject(sconf);
                } catch (DecodeException e) {
                    log.error("Configuration file {} does not contain a valid JSON object", sconf);
                }
            } catch (FileNotFoundException e) {
                // Ignore it.
            }
        } else {
            log.error("Config file not found : {}", config.getAbsolutePath());
        }
        return conf;
    }
}
