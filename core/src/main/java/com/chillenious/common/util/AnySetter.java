package com.chillenious.common.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation that can be used to define a non-static,
 * two-argument method (first argument name of property, second value
 * to set), to be used as a "fallback" handler
 * for all otherwise unrecognized properties found from mapping.
 * <p>
 * If used, all otherwise unmapped key-value pairs from mapping
 * are added to the property (of type Map&lt;String, String&gt;).
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AnySetter {
}
