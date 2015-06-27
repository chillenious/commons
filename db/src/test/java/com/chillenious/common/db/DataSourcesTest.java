package com.chillenious.common.db;

import com.chillenious.common.Bootstrap;
import com.chillenious.common.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.sql.DataSource;

public class DataSourcesTest {

    private Bootstrap bootstrap;

    @Before
    public void setup() {
        Settings settings = Settings.builder()
                .addFromClassPath("test.properties")
                .build();
        bootstrap = new Bootstrap(settings, new DataSourcesModule());
        bootstrap.getInjector().injectMembers(this);
    }

    @After
    public void teardown() {
        bootstrap.shutdown();
    }

    @Test
    @Ignore // TODO [Eelco] use internal database/ driver for testing so that this works
    public void testExtraProperties() {
        DataSource dataSource = bootstrap.getInstance(DataSource.class);
    }

}
