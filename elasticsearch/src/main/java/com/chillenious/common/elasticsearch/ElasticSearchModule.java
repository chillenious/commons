package com.chillenious.common.elasticsearch;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.chillenious.common.ShutdownHooks;
import com.chillenious.common.WithShutdown;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Bootstraps and binds ElasticSearch related objects.
 * <p/>
 * ElasticSearch comes with a smart configuration loading mechanism itself, which
 * includes polling for elasticsearch.yml to load a specific configuration for the
 * instance. This project deliberately doesn't ship with one, which leaves the door
 * open to include one in the project you use this without the need to change the
 * bootstrapping code itself.
 * See http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/setup-configuration.html#settings
 */
public class ElasticSearchModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchModule.class);

    static final class RegisterElasticSearchShutDown implements WithShutdown {

        private final Node node;

        @Inject
        RegisterElasticSearchShutDown(Node node,
                                      ShutdownHooks shutdownHooks) {
            this.node = node;
            shutdownHooks.add(this);
        }

        @Override
        public void shutdown() {
            log.info("shutting down elasticsearch node");
            try {
                node.close();
            } catch (Exception e) {
                log.error("unable to close node: " + e.getMessage());
            }
        }
    }

    public ElasticSearchModule() {
    }

    @Override
    protected void configure() {
        final Node node = nodeBuilder().node();
        bind(Node.class).toInstance(node);
        Client client = node.client();
        bind(Client.class).toInstance(client);
        bind(RegisterElasticSearchShutDown.class).asEagerSingleton();
    }
}
