package com.chillenious.common;

import ch.qos.logback.classic.LoggerContext;
import com.google.common.base.Preconditions;
import com.google.inject.*;
import com.google.inject.name.Names;
import com.chillenious.common.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import static java.util.Arrays.asList;

/**
 * Bootstraps Guice and includes some extra work we want to do around it.
 * <p/>
 * If you construct this as part of a unit test or e.g. in a web application
 * (i.e. whenever you need to clean this up but you can't rely on JVM shutdown
 * hooks), you should call {@link #shutdown()}.
 */
public class Bootstrap {

    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);

    private final Settings settings;

    private final Injector injector;

    private final ShutdownHooks shutdownHooks = new ShutdownHooks();

    @Inject
    Bootstrap() {
        throw new IllegalStateException(
                "You can't use bootstrap as a injected dependency");
    }

    /**
     * Bootstrap with the provided settings and module(s).
     * The {@link Stage#DEVELOPMENT development}
     * stage (which is Guice's default) will be used.
     *
     * @param settings settings to bootstrap with
     * @param modules  module(s) to bootstrap with
     */
    public Bootstrap(Settings settings, Module... modules) {
        this(settings, Stage.DEVELOPMENT, modules);
    }

    /**
     * Bootstrap with the provided settings and module(s).
     *
     * @param settings settings to bootstrap with
     * @param modules  module(s) to bootstrap with
     */
    public Bootstrap(Settings settings, Stage stage, Module... modules) {
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(stage);
        Preconditions.checkNotNull(modules);
        long start = System.currentTimeMillis();
        this.settings = settings;
        String environment;
        if (!settings.isDefined("environment")) {
            environment = "development";
            settings.set("bootstrap", "environment", environment);
        } else {
            environment = settings.getString("environment");
        }
        Set<Module> m = new LinkedHashSet<>();
        m.add(new SettingsModule());
        m.add(new ShutdownHookModule());
        List<Module> passedInModules = Arrays.asList(modules);
        log.info("*************************************************");
        log.info("* Bootstrapping");
        log.info("*   Stage: " + stage);
        log.info("*   Environment: " + environment);
        if (!settings.getLocationsLoaded().isEmpty()) {
            log.info("*   Settings loaded from: ");
            for (String location : settings.getLocationsLoaded()) {
                log.info("*   - " + location);
            }
        } else if (!settings.asProperties().isEmpty()) {
            log.info("*   Settings [explicitly set]: " + settings.asProperties());
        } else {
            log.info("*   [empty Settings]");
        }
        log.info("*   Modules: " + asList(modules));
        log.info("*   Timezone: " + TimeZone.getDefault());
        log.info("*   Available processors (cores): " + Runtime.getRuntime().availableProcessors());
        log.info("*   Free memory (bytes): " + Runtime.getRuntime().freeMemory());
        log.info("*   Total memory (bytes): " + Runtime.getRuntime().totalMemory());
        long maxMemory = Runtime.getRuntime().maxMemory();
        log.info("*   Maximum memory (bytes): " + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));
        log.info("*************************************************");
        m.addAll(passedInModules);
        this.injector = Guice.createInjector(stage, m);
        log.info(String.format("bootstrapped in %s", Duration.milliseconds(System.currentTimeMillis() - start)));
    }

    /**
     * @return the injector that was created for this bootstrap
     */
    public Injector getInjector() {
        if (injector == null) {
            throw new IllegalStateException(
                    "Inject is not yet set. Either we're in the middle " +
                            "of bootstrapping, or this bootstrap was shut down"
            );
        }
        return injector;
    }

    /**
     * Injects dependencies into the fields and methods of {@code instance}. Ignores the presence or
     * absence of an injectable constructor.
     * <p/>
     * <p>Whenever Guice creates an instance, it performs this injection automatically (after first
     * performing constructor injection), so if you're able to let Guice create all your objects for
     * you, you'll never need to use this method.
     *
     * @param instance to inject members on
     * @see com.google.inject.Binder#getMembersInjector(Class) for a preferred alternative that supports checks before
     * run time
     */
    public void injectMembers(Object instance) {
        injector.injectMembers(instance);
    }

    /**
     * Returns the provider used to obtain instances for the given injection key. When feasible, avoid
     * using this method, in favor of having Guice inject your dependencies ahead of time.
     *
     * @param key
     * @throws com.google.inject.ConfigurationException if this injector cannot find or create the provider.
     * @see com.google.inject.Binder#getProvider(com.google.inject.Key) for an alternative that offers up front error detection
     */
    public <T> Provider<T> getProvider(Key<T> key) {
        return injector.getProvider(key);
    }

    /**
     * Returns the provider used to obtain instances for the given type. When feasible, avoid
     * using this method, in favor of having Guice inject your dependencies ahead of time.
     *
     * @param type
     * @throws com.google.inject.ConfigurationException if this injector cannot find or create the provider.
     * @see com.google.inject.Binder#getProvider(Class) for an alternative that offers up front error detection
     */
    public <T> Provider<T> getProvider(Class<T> type) {
        return injector.getProvider(type);
    }

    /**
     * Returns the appropriate instance for the given injection key; equivalent to {@code
     * getProvider(key).get()}. When feasible, avoid using this method, in favor of having Guice
     * inject your dependencies ahead of time.
     *
     * @param key
     * @throws com.google.inject.ConfigurationException if this injector cannot find or create the provider.
     * @throws com.google.inject.ProvisionException     if there was a runtime failure while providing an instance.
     */
    public <T> T getInstance(Key<T> key) {
        return injector.getInstance(key);
    }

    /**
     * Returns the appropriate instance for the given injection type; equivalent to {@code
     * getProvider(type).get()}. When feasible, avoid using this method, in favor of having Guice
     * inject your dependencies ahead of time.
     *
     * @param type
     * @throws com.google.inject.ConfigurationException if this injector cannot find or create the provider.
     * @throws com.google.inject.ProvisionException     if there was a runtime failure while providing an instance.
     */
    public <T> T getInstance(Class<T> type) {
        return injector.getInstance(type);
    }

    /**
     * Shuts down this bootstrap cleanly (runs all registered shutdown hooks). You should call
     * this manually when e.g. running in the context of unit tests (i.e. whenever you
     * can't rely on the JVM shutdown hooks to run).
     */
    public void shutdown() {
        log.info("shutting down");
        shutdownHooks.runShutdownHooks();
    }

    @Override
    public String toString() {
        return "Bootstrap{" +
                "injector=" + injector +
                ", settings=" + settings +
                ", shutdownHooks=" + shutdownHooks +
                '}';
    }

    /**
     * Bind settings to {@link Names} and settings to the instance this bootstrap uses.
     */
    class SettingsModule extends AbstractModule {

        @Override
        protected void configure() {
            Binder binder = binder();
            Names.bindProperties(binder, settings.asProperties());
            bind(Settings.class).toInstance(settings);
            for (String key : settings.keys()) {
                binder.bind(DynamicSetting.class)
                        .annotatedWith(Names.named(key))
                        .toInstance(new DynamicSetting(key, settings));
            }
            log.debug(String.format("bootstrapping with settings %s", settings));
        }
    }

    /**
     * Binds the shutdown hooks instance so that it is available for clients to register new hooks,
     * and bind a JVM shutdown hook for default shutdown.
     */
    class ShutdownHookModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(ShutdownHooks.class).toInstance(shutdownHooks);
            // the shutdown hooks might be called before the JVM shutdown hook
            // is executed if e.g. running in the context of unit tests or
            // a web application, but that's fine; the hooks will only be called once
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    // run shutdown hooks
                    shutdownHooks.runShutdownHooks();
                    // upon completion, shutdown logging
                    ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
                }
            }));
        }
    }
}
