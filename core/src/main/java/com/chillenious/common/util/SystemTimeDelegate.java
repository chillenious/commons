package com.chillenious.common.util;

/**
 * Default delegate that just uses {@link System#currentTimeMillis()}.
 */
public class SystemTimeDelegate implements CurrentTimeDelegate {

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
