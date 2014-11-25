package com.chillenious.common.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Exposes metrics.
 */
@Singleton
public final class Metrics {

    private final MetricRegistry registry;

    @Inject
    public Metrics(final MetricRegistry registry) {
        this.registry = registry;
    }

    /**
     * Concatenates elements to form a dotted name, eliding any null values or empty strings.
     *
     * @param name  the first element of the name
     * @param names the remaining elements of the name
     * @return {@code name} and {@code names} concatenated by periods
     */
    public static String name(final String name, final String... names) {
        return MetricRegistry.name(name, names);
    }

    /**
     * Concatenates a class name and elements to form a dotted name, eliding any null values or
     * empty strings.
     *
     * @param klass the first element of the name
     * @param names the remaining elements of the name
     * @return {@code klass} and {@code names} concatenated by periods
     */
    public static String name(final Class<?> klass, final String... names) {
        return MetricRegistry.name(klass, names);
    }

    /**
     * Creates a new {@link com.codahale.metrics.Meter} and registers it under the given name.
     *
     * @param name the name of the metric
     * @return a new {@link com.codahale.metrics.Meter}
     */
    public Meter meter(final String name) {
        return registry.meter(name);
    }

    /**
     * Creates a new {@link com.codahale.metrics.Timer} and registers it under the given name.
     *
     * @param name the name of the metric
     * @return a new {@link com.codahale.metrics.Timer}
     */
    public Timer timer(final String name) {
        return registry.timer(name);
    }

    /**
     * Creates a new {@link com.codahale.metrics.Histogram} and registers it under the given name.
     *
     * @param name the name of the metric
     * @return a new {@link com.codahale.metrics.Histogram}
     */
    public Histogram histogram(final String name) {
        return registry.histogram(name);
    }

    /**
     * Creates a new {@link com.codahale.metrics.Counter} and registers it under the given name.
     *
     * @param name the name of the metric
     * @return a new {@link com.codahale.metrics.Counter}
     */
    public Counter counter(final String name) {
        return registry.counter(name);
    }

    /**
     * Given a {@link com.codahale.metrics.Metric}, registers it under the given name.
     *
     * @param name   the name of the metric
     * @param metric the metric
     * @return {@code metric}
     * @throws IllegalArgumentException if the name is already registered
     */
    public <T extends Metric> T register(final String name, final T metric) throws IllegalArgumentException {
        return registry.register(name, metric);
    }

    /**
     * Given a metric set, registers them.
     *
     * @param metrics a set of metrics
     * @throws IllegalArgumentException if any of the names are already registered
     */
    public void registerAll(final MetricSet metrics) throws IllegalArgumentException {
        registry.registerAll(metrics);
    }

    public MetricRegistry getRegistry() {
        return registry;
    }
}
