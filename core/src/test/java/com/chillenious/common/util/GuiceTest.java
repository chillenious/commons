package com.chillenious.common.util;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class GuiceTest {

    static class NamedProperties {

        @Inject
        @Named("port")
        private int port;
    }

    @Test
    public void testNamed() {
        final Properties p = new Properties();
        p.setProperty("port", "8080");
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                Names.bindProperties(binder(), p);
            }
        });
        NamedProperties test = injector.getInstance(NamedProperties.class);
        Assert.assertEquals(8080, test.port);
    }
}
