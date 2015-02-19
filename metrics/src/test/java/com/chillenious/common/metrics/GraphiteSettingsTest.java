package com.chillenious.common.metrics;

import com.chillenious.common.Settings;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class GraphiteSettingsTest {

    @Test
    public void testGraphiteSettings() {
        Settings settings = Settings.builder().addFromClassPath("test.properties").build();
        final GraphiteSettings graphiteSettings =
                settings.map("metrics.graphite.", GraphiteSettings.class)
                        .get("metrics.graphite");
        Assert.assertThat(graphiteSettings, CoreMatchers.notNullValue());
    }
}
