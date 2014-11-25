package com.chillenious.common;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.chillenious.common.util.Duration;
import com.chillenious.common.util.DurationSetting;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Properties;

public class SettingsTest {

    @Test
    public void testLoadFromClassPath() {

        Settings p = new Settings();
        p.loadFromClassPath("com/chillenious/common/util/p1.properties");
        Assert.assertEquals("bar", p.getString("foo"));
        Assert.assertEquals("fender", p.getString("guitar"));

        p = Settings.builder()
                .addFromClassPath("com/chillenious/common/util/p1.properties")
                .build();
        Assert.assertEquals("bar", p.getString("foo"));
        Assert.assertEquals("fender", p.getString("guitar"));
    }

    @Test
    public void testLoadFromClassPathWithOverridesFromFile() {

        Settings p = new Settings();
        p.loadFromClassPath("com/chillenious/common/util/p1.properties");
        p.loadFromClassPath("com/chillenious/common/util/p2.properties");
        Assert.assertEquals("baz", p.getString("foo"));
        Assert.assertEquals("fender", p.getString("guitar"));
        Assert.assertEquals("ibanez", p.getString("bass"));

        p = Settings.builder()
                .addFromClassPath("com/chillenious/common/util/p1.properties")
                .addFromClassPath("com/chillenious/common/util/p2.properties")
                .buildWithoutOverrides();
        Assert.assertEquals("baz", p.getString("foo"));
        Assert.assertEquals("fender", p.getString("guitar"));
        Assert.assertEquals("ibanez", p.getString("bass"));
    }

    @Test
    public void testLoadFromClassPathWithOverridesFromFileAndSystemProperties() {

        System.setProperty("bass", "gibson");
        Settings p = new Settings();
        p.loadFromClassPath("com/chillenious/common/util/p1.properties");
        p.loadFromClassPath("com/chillenious/common/util/p2.properties");
        p.loadOverrides();
        Assert.assertEquals("baz", p.getString("foo"));
        Assert.assertEquals("fender", p.getString("guitar"));
        Assert.assertEquals("gibson", p.getString("bass"));

        p = Settings.builder()
                .addFromClassPath("com/chillenious/common/util/p1.properties")
                .addFromClassPath("com/chillenious/common/util/p2.properties")
                .build();
        Assert.assertEquals("baz", p.getString("foo"));
        Assert.assertEquals("fender", p.getString("guitar"));
        Assert.assertEquals("gibson", p.getString("bass"));
        System.getProperties().remove("bass");
    }

    @Test
    public void testSubstitutions() {

        Properties p = new Properties();
        p.setProperty("test.name", "Ben");
        p.setProperty("test.message1", "Hello ${test.name}");
        p.setProperty("test.message2", "${test.name} says hello");
        p.setProperty("test.message3", "Teh ${test.name} iz ${test.name} here");
        p.setProperty("test.message4",
                "${test.name} has a ${test.name} in his ${test.name}, ${test.name}");
        p.setProperty("test.nosub", "$Rev: 1234$");
        p.setProperty("test.message5", "this var is ${irreplaceable}!");
        Settings settings = new Settings();
        settings.substituteVariables(p);
        Assert.assertEquals("Hello Ben", p.getProperty("test.message1"));
        Assert.assertEquals("Ben says hello", p.getProperty("test.message2"));
        Assert.assertEquals("Teh Ben iz Ben here", p.getProperty("test.message3"));
        Assert.assertEquals("Ben has a Ben in his Ben, Ben", p.getProperty("test.message4"));
        Assert.assertEquals("this var is ${irreplaceable}!", p.getProperty("test.message5"));
    }

    static class SomeClassUsingStaticNameBinding {

        @Inject
        @Named("guitar")
        private String guitar;
    }

    @Test
    public void testStaticNameBinding() {

        Settings settings = Settings.builder()
                .addFromClassPath("com/chillenious/common/util/p1.properties")
                .build();
        Bootstrap bootstrap = new Bootstrap(settings);
        SomeClassUsingStaticNameBinding o =
                bootstrap.getInjector().getInstance(SomeClassUsingStaticNameBinding.class);
        Assert.assertEquals("fender", o.guitar);

    }

    static class SomeClassUsingDynamicSettings {

        @Inject
        @Named("guitar")
        private DynamicSetting guitar;
    }

    @Test
    public void testDynamicSettings() {

        Settings settings = Settings.builder()
                .addFromClassPath("com/chillenious/common/util/p1.properties")
                .build();
        Bootstrap bootstrap = new Bootstrap(settings);
        SomeClassUsingDynamicSettings o =
                bootstrap.getInjector().getInstance(SomeClassUsingDynamicSettings.class);
        Assert.assertEquals("fender", o.guitar.getString());
        settings.set("guitar", "gibson");
        Assert.assertEquals("gibson", o.guitar.getString());
    }

    static class SomeOtherClassUsingDynamicSettings {

        private final Setting<String> guitar;

        @Inject
        SomeOtherClassUsingDynamicSettings(
                @Named("guitar") DynamicSetting guitar) {
            this.guitar = new Setting<>(guitar, String.class);
        }
    }

    @Test
    public void testDynamicSettingsWithWrapper() {

        Settings settings = Settings.builder()
                .addFromClassPath("com/chillenious/common/util/p1.properties")
                .build();
        Bootstrap bootstrap = new Bootstrap(settings);
        SomeOtherClassUsingDynamicSettings o =
                bootstrap.getInjector().getInstance(SomeOtherClassUsingDynamicSettings.class);
        Assert.assertEquals("fender", o.guitar.get());
        settings.set("guitar", "gibson");
        Assert.assertEquals("gibson", o.guitar.get());
    }

    static class YetAnotherClassUsingDynamicSettings {

        private final Setting<String> guitar;

        private String updatedValue;

        @Inject
        YetAnotherClassUsingDynamicSettings(
                @Named("guitar") DynamicSetting guitar) {
            this.guitar = new Setting<String>(guitar, String.class) {
                @Override
                public void onChanged(@Nullable Object oldValue, @Nullable Object newValue) {
                    YetAnotherClassUsingDynamicSettings.this.updatedValue = String.valueOf(newValue);
                }
            };
        }
    }

    @Test
    public void testDynamicSettingsWithListener() {

        Settings settings = Settings.builder()
                .addFromClassPath("com/chillenious/common/util/p1.properties")
                .build();
        Bootstrap bootstrap = new Bootstrap(settings);
        YetAnotherClassUsingDynamicSettings o =
                bootstrap.getInjector().getInstance(YetAnotherClassUsingDynamicSettings.class);
        Assert.assertEquals("fender", o.guitar.get());
        Assert.assertNull(o.updatedValue);
        settings.set("guitar", "gibson");
        Assert.assertEquals("gibson", o.guitar.get());
        Assert.assertEquals("gibson", o.updatedValue);
    }

    static class ClassUsingDynamicDurationSettings {

        private final Setting<Duration> duration;

        @Inject
        ClassUsingDynamicDurationSettings(
                @Named("duration") DynamicSetting duration) {
            this.duration = new DurationSetting(duration);
        }
    }

    @Test
    public void testDynamicDurationSetting() {

        Settings settings = Settings.builder()
                .addFromClassPath("com/chillenious/common/util/p1.properties")
                .build();
        Bootstrap bootstrap = new Bootstrap(settings);
        ClassUsingDynamicDurationSettings o =
                bootstrap.getInjector().getInstance(ClassUsingDynamicDurationSettings.class);
        Assert.assertNotNull(o.duration.get());
        Assert.assertEquals(1, o.duration.get().seconds(), 0);
        Assert.assertEquals(1000, o.duration.get().getMilliseconds());
        settings.set("duration", "4 hours");
        Assert.assertEquals(4, o.duration.get().hours(), 0);
    }

    @Test
    public void testTryLoadFromClassPath() {

        Settings p = Settings.builder()
                .tryAddFromClassPath("com/chillenious/common/util/nonexistent.properties")
                .build();
        Assert.assertTrue(p.asProperties().isEmpty());
    }

    @Test
    public void testIntegerStorage() {
        Settings settings = new Settings();
        settings.put("integer", "24");
        Assert.assertEquals((int) settings.getInteger("integer"), 24);
    }

    @Test
    public void testBooleanStorage() {
        Settings settings = new Settings();
        settings.put("boolean", "true");
        Assert.assertTrue(settings.getBoolean("boolean"));
    }

    @Test
    public void testDoubleStorage() {
        Settings settings = new Settings();
        settings.put("double", "123.4");
        Assert.assertEquals(123.4, settings.getDouble("double"), 0);
    }

    @Test
    public void testLongStorage() {
        Settings settings = new Settings();
        settings.put("long", "1234");
        Assert.assertEquals(1234l, (long) settings.getLong("long"));
    }

    @Test
    public void testIsDefined() {
        Settings settings = new Settings();
        settings.put("here!", "something");
        Assert.assertTrue(settings.isDefined("here!"));
    }

    @Test
    public void testKeyFilters() {
        Settings settings = new Settings();
        settings.put("something.webly.thingy", "something");
        Assert.assertTrue(settings.keys(Settings.contains("web")).iterator().hasNext());
        Assert.assertFalse(settings.keys(Settings.contains("bar")).iterator().hasNext());
    }

    static class WhatsYourFoo {
        @Named("foo")
        @Inject
        private String foo;
    }

    @Test
    public void testSettingsModule() {
        Settings settings = new Settings();
        settings.put("foo", "bar");
        Bootstrap bootstrap = new Bootstrap(settings);
        WhatsYourFoo instance = bootstrap.getInjector().getInstance(WhatsYourFoo.class);
        Assert.assertEquals("bar", instance.foo);
    }
}
