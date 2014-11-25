package com.chillenious.common.hazelcast;

import com.chillenious.common.Settings;
import com.chillenious.common.ShutdownHooks;
import com.chillenious.common.util.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Module for starting up and shutting down the default Hazelcast instance. It also makes
 * various relevant objects available for dependency injection.
 */
public class HazelcastModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(HazelcastModule.class);

    static class HazelcastShutdownHook {

        @Inject
        HazelcastShutdownHook(ShutdownHooks shutdownHooks) {
            shutdownHooks.add(new Runnable() {
                @Override
                public void run() {
                    Hazelcast.shutdownAll();
                }
            });
        }
    }

    private final Settings settings;

    public HazelcastModule(final Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        String overrideSettings = settings.getString("hazelcast.config");
        HazelcastInstance hazelcast;
        if (!Strings.isEmpty(overrideSettings)) {
            if (overrideSettings.startsWith("file://")) {
                String fileLocation = overrideSettings.substring(7);
                File file = new File(fileLocation);
                if (file.canRead()) {
                    log.info("loading Hazelcast configuration from file override at {}", file);
                    try {
                        hazelcast = Hazelcast.getOrCreateHazelcastInstance(new FileSystemXmlConfig(file));
                    } catch (FileNotFoundException e) {
                        throw new IllegalArgumentException(fileLocation + " is not a valid file", e);
                    }
                } else {
                    throw new IllegalArgumentException(fileLocation + " is not a valid file");
                }
            } else {
                log.info("loading Hazelcast configuration from classpath at {}", overrideSettings);
                hazelcast = Hazelcast.getOrCreateHazelcastInstance(new ClasspathXmlConfig(overrideSettings));
            }
        } else {
            log.info("creating Hazelcast using default loading");
            hazelcast = Hazelcast.getOrCreateHazelcastInstance(new XmlConfigBuilder().build());
        }

        bind(HazelcastInstance.class).toInstance(hazelcast);
        bind(HazelcastShutdownHook.class).asEagerSingleton();
    }
}
