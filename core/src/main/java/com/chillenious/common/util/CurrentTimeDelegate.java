package com.chillenious.common.util;

/**
 * Strategy for actually getting the current time stamp.
 */
public interface CurrentTimeDelegate {

    /**
     * @return current time stamp
     */
    long currentTimeMillis();
}
