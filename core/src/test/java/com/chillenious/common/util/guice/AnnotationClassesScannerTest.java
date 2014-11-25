package com.chillenious.common.util.guice;

import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AnnotationClassesScannerTest {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Simple {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface SimpleWithKeyValue {
        String value() default "default";
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface SimpleWithKeyValues {
        String fooKey();

        String barKey();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ToIgnore {
        String nada();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface SimpleWithArray {
        String[] values();
    }

    @SimpleWithKeyValue
    @SimpleWithKeyValues(fooKey = "fooValue", barKey = "barValue")
    @Simple
    @ToIgnore(nada = "adan")
    @SimpleWithArray(values = {"a", "b", "c"})
    public static class ClassWithABunchOfAnnotations {
    }

    @ToIgnore(nada = "adan")
    @SimpleWithKeyValue(value = "notDefault")
    public static class AnotherClassWithABunchOfAnnotations {
    }

    public static class ClassWithoutAnyAnnotations {
    }

    @Test
    public void testAnnotationScanning() {
        Set<Class<?>> classes = Classes.matching(
                annotatedWith(SimpleWithKeyValue.class).or(
                        annotatedWith(SimpleWithKeyValues.class))
        ).in(
                AnnotationClassesScannerTest.class.getPackage());
        assertEquals(2, classes.size());
        assertTrue(classes.contains(ClassWithABunchOfAnnotations.class));
        assertTrue(classes.contains(AnotherClassWithABunchOfAnnotations.class));
        assertFalse(classes.contains(ClassWithoutAnyAnnotations.class));
    }
}
