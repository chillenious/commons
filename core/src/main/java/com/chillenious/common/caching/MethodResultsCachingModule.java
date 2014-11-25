package com.chillenious.common.caching;

import com.chillenious.common.Settings;
import com.chillenious.common.util.guice.AbstractAopModule;
import com.google.common.base.Preconditions;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module for setting up caching of method call results based on (AOP) method interception.
 */
public class MethodResultsCachingModule extends AbstractAopModule {

    private static final Logger log = LoggerFactory.getLogger(MethodResultsCachingModule.class);

    private final Settings settings;

    public MethodResultsCachingModule(Settings settings, Matcher<? super Class> classMatcher) {
        super(classMatcher);
        this.settings = settings;
    }

    public MethodResultsCachingModule(Settings settings, String... packages) {
        super(packages);
        this.settings = settings;
    }

    public MethodResultsCachingModule(Settings settings, Package[] packages) {
        super(packages);
        this.settings = settings;
    }

    /**
     * Construct.
     *
     * @param settings        settings
     * @param scanRootPackage package to start scanning from when looking for methods on classes to intercept
     */
    public MethodResultsCachingModule(Settings settings, String scanRootPackage) {
        Preconditions.checkNotNull(settings);
        this.settings = settings;
    }

    @Override
    protected void configure() {

        if (settings.getBoolean("methodcache.enabled", false)) {
            log.info("method caching is ON");
            MethodResultsCacheInterceptor interceptor = new MethodResultsCacheInterceptor();
            requestInjection(interceptor);
            bindInterceptor(
                    classMatcher,
                    Matchers.annotatedWith(CacheReturnValue.class),
                    interceptor);
        } else {
            log.info("method caching is OFF");
        }
    }
}
