package com.chillenious.common.util;

/**
 * When this is set, you can set the 'current' time stamp, and it won't change.
 */
public class FixedTimeDelegate implements CurrentTimeDelegate {

    private final long fixedTime;

    public FixedTimeDelegate(long fixedTime) {
        this.fixedTime = fixedTime;
    }

    @Override
    public long currentTimeMillis() {
        return fixedTime;
    }
}
