package com.chillenious.common;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Classes can register shutdown hooks with this registry. Depending on the
 * context this is used - i.e. within a web app or a JUnit test case - the
 * implementation differs. In a web app context, this should be called from e.g.
 * contextDestroyed, while in a test case or regular Java application, this
 * would be called from within a shutdown hook.
 * <p/>
 * The shutdown hooks are ran in the order that they are registered, but
 * you shouldn't depend on that.
 */
@Singleton
public class ShutdownHooks {

    private final Set<Runnable> shutdownHooks = new LinkedHashSet<>();

    private final Logger log = LoggerFactory.getLogger(ShutdownHooks.class);

    /**
     * Construct. While you should access this class through Guice to register
     * new hooks,
     */
    public ShutdownHooks() {
    }

    /**
     * Register a shutdown hook for the provided object so that
     * {@link WithShutdown#shutdown() it's shutdown method} will be called
     * when this is shut down
     *
     * @param withShutdown object that can be shut down
     */
    public synchronized void add(final WithShutdown withShutdown) {
        add(new Runnable() {
            @Override
            public void run() {
                withShutdown.shutdown();
            }
        });
    }

    /**
     * Register a new shutdown hook
     *
     * @param shutdownHook shutdown hook to register
     */
    public synchronized void add(Runnable shutdownHook) {
        Preconditions.checkNotNull(shutdownHook);
        shutdownHooks.add(shutdownHook);
    }

    /**
     * Run all registered shutdown hooks and de-register them.
     */
    public synchronized void runShutdownHooks() {
        for (Runnable r : shutdownHooks) {
            try {
                log.debug("running shutdown hook " + r + " in thread "
                        + Thread.currentThread().getName());
                r.run();
            } catch (Exception e) {
                log.error("failed to execute shutdownhook " + r + ": "
                        + e.getMessage(), e);
            }
        }
        shutdownHooks.clear();
    }

    @Override
    public String toString() {
        return "ShutdownHooks{" +
                "shutdownHooks=" + shutdownHooks +
                '}';
    }
}
