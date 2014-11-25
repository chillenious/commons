package com.chillenious.common.util;

/**
 * Tells interested parties that the meat of the object is wrapped, e.g. when converting to JSON.
 */
public interface Wrapped<T> {

    /**
     * @return the wrapped object
     */
    T getWrapped();
}
