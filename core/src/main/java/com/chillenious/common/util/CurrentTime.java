package com.chillenious.common.util;

public class CurrentTime {

    private static final CurrentTimeDelegate DEFAULT = new SystemTimeDelegate();

    private static CurrentTimeDelegate delegate = DEFAULT;

    /**
     * @return current time stamp (from the current delegate)
     */
    public static synchronized long currentTimeMillis() {
        return delegate.currentTimeMillis();
    }

    /**
     * Sets the current time delegate
     *
     * @param newDelegate delete to use going forward
     */
    public static synchronized void setDelegate(CurrentTimeDelegate newDelegate) {
        delegate = newDelegate;
    }

    /**
     * Sets delegate back to default (which is {@link SystemTimeDelegate}).
     */
    public static synchronized long reset() {
        delegate = DEFAULT;
        return delegate.currentTimeMillis();
    }

    /**
     * Sets the current time (actual current time or previously fixed, whatever the delegate produces) and fixes it.
     */
    public static synchronized long freeze() {
        delegate = new FixedTimeDelegate(delegate.currentTimeMillis());
        return delegate.currentTimeMillis();
    }

    /**
     * Fixes the time based on the current (from delegate), but plus the provided number of milliseconds.
     *
     * @param milliseconds milliseconds to add
     */
    public static synchronized long freezePlus(long milliseconds) {
        delegate = new FixedTimeDelegate(delegate.currentTimeMillis() + milliseconds);
        return delegate.currentTimeMillis();
    }

    /**
     * Fixes the time based on the current (from delegate), but minus the provided number of milliseconds.
     *
     * @param milliseconds milliseconds to subtract
     */
    public static synchronized long freezeMinus(long milliseconds) {
        delegate = new FixedTimeDelegate(delegate.currentTimeMillis() - milliseconds);
        return delegate.currentTimeMillis();
    }

    /**
     * Sets the current time to the provided milliseconds and fix it
     */
    public static synchronized long freezeAt(long milliseconds) {
        delegate = new FixedTimeDelegate(milliseconds);
        return delegate.currentTimeMillis();
    }
}
