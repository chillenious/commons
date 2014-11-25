package com.chillenious.common.db.sync;

import com.google.common.base.Objects;

/**
 * Comparable for longs in descending order. A salt is added because when this is
 * used with timestamps, the order in which they are created can matter.
 */
public final class DescendingLong implements Comparable<DescendingLong> {

    private static volatile int globalCounter = 0;

    private final long value;

    private final long salt;

    public DescendingLong(long value) {
        this.value = value;
        this.salt = next();
    }

    private static int next() {
        synchronized (DescendingLong.class) {
            if (globalCounter == Integer.MAX_VALUE) {
                try {
                    Thread.sleep(1); // in case you create a new instance after this, this should be marked 'earlier'
                } catch (InterruptedException e) {
                }
                globalCounter = 0;
            } else {
                globalCounter++;
            }
            return globalCounter;
        }
    }

    @Override
    public int compareTo(final DescendingLong that) {
        if (this.value < that.value) {
            return 1;
        } else if (this.value > that.value) {
            return -1;
        } else if (this.salt < that.salt) {
            return 1;
        } else if (this.salt > that.salt) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof DescendingLong &&
                Objects.equal(this.value, ((DescendingLong) o).value) &&
                Objects.equal(this.salt, ((DescendingLong) o).salt));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(DescendingLong.class, value, salt);
    }

    @Override
    public String toString() {
        return value + "-" + salt;
    }
}
