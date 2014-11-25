package com.chillenious.common.util;

import com.chillenious.common.Bootstrap;
import com.chillenious.common.DynamicSetting;
import com.chillenious.common.Settings;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JavaScriptFnSettingTest {

    Settings settings;

    Bootstrap bootstrap;

    @Inject
    @Named("test.fn")
    DynamicSetting dynamicSetting;

    @Before
    public void setup() {
        settings = Settings.builder()
                .add("test.fn", "return domains < 20 ? domains * 2.0 : domains * 0.8;")
                .build();
        bootstrap = new Bootstrap(settings);
        bootstrap.getInjector().injectMembers(this);
    }

    @Test
    public void testJsSetting() {

        JavaScriptFnSetting fnSetting = new JavaScriptFnSetting(dynamicSetting, "domains");
        JavaScriptFn fn = fnSetting.getNonOptional();
        double result = (Double) fn.invoke(9.2d);
        Assert.assertEquals(18.4d, result, 0);
        result = (Double) fn.invoke(50d);
        Assert.assertEquals(40d, result, 0);

        // giving that caching is used, make sure it works twice
        fn = fnSetting.getNonOptional();
        result = (Double) fn.invoke(9.2d);
        Assert.assertEquals(18.4d, result, 0);

        // and that changing the function also works
        settings.set("test.fn", "return domains * 3");
        fn = fnSetting.getNonOptional();
        result = (Double) fn.invoke(9d);
        Assert.assertEquals(27d, result, 0);
    }
}
