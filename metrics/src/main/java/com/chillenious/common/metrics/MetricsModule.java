package com.chillenious.common.metrics;

import com.chillenious.common.Settings;
import com.chillenious.common.ShutdownHooks;
import com.chillenious.common.WithShutdown;
import com.chillenious.common.util.Strings;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Sets up Metrics. We're keeping it simple for now and assume projects
 * use a single configuration.
 * <p>
 * Use setting <code>metrics.graphite.instanceName</code> to configure a base name to create
 * a registry for, which is then used for binding and possibly a few other things.
 */
public class MetricsModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(MetricsModule.class);

    static final class ShutdownInitializer implements WithShutdown {

        private final GraphiteReporter graphiteReporter;

        @Inject
        ShutdownInitializer(final ShutdownHooks shutdownHooks,
                            final GraphiteReporter graphiteReporter) {
            this.graphiteReporter = graphiteReporter;
            shutdownHooks.add(this);
        }

        @Override
        public void shutdown() {
            graphiteReporter.close();
        }
    }

    private final Settings settings;

    public MetricsModule(final Settings settings) {
        Preconditions.checkNotNull(settings);
        this.settings = settings;
    }

    @Override
    protected void configure() {

        if (settings.getBoolean("metrics.graphite.enabled", false)) {
            final String registryName = settings.getString("metrics.graphite.instanceName", "metrics");
            final MetricRegistry registry = SharedMetricRegistries.getOrCreate(registryName);
            bind(MetricRegistry.class).toInstance(registry);
            bind(Metrics.class).asEagerSingleton();
            final GraphiteSettings graphiteSettings =
                    settings.map("metrics.graphite.", GraphiteSettings.class)
                            .get("metrics.graphite");
            if (graphiteSettings != null) {
                log.info("creating graphite reporter for registry {}, at {}:{}, with a reporting interval of {}",
                        registryName, graphiteSettings.getHost(),
                        graphiteSettings.getPort(), graphiteSettings.getReportingInterval());
                final Graphite graphite = new Graphite(new InetSocketAddress(
                        graphiteSettings.getHost(), graphiteSettings.getPort()));
                final String instanceName = instanceName(settings);
                final String prefix = graphiteSettings.getApiKey() +
                        (!Strings.isEmpty(instanceName) ? '.' + instanceName : "");
                final GraphiteReporter graphiteReporter = GraphiteReporter.forRegistry(registry)
                        .convertRatesTo(TimeUnit.SECONDS)
                        .convertDurationsTo(TimeUnit.MILLISECONDS)
                        .filter(MetricFilter.ALL)
                        .prefixedWith(prefix)
                        .build(graphite);
                graphiteReporter.start(graphiteSettings.getReportingInterval().getMilliseconds(),
                        TimeUnit.MILLISECONDS);
                bind(GraphiteReporter.class).toInstance(graphiteReporter);
                bind(ShutdownInitializer.class).asEagerSingleton();
                log.info("graphite reporter started");
            }
        } else {
            log.info("'metrics.graphite.enabled' setting not found or false; skipping metrics graphite setup");
        }
    }

    /**
     * If a specific name for this instance/ VM is available and should be used as a prefix
     * right after the api key when reporting to Graphite, supply it in a method override.
     *
     * @param settings settings
     * @return instance name (prefix) or null if it should be ignore (which is the default)
     */
    @Nullable
    protected String instanceName(final Settings settings) {
        return settings.getString("metrics.instanceName");
    }
}
