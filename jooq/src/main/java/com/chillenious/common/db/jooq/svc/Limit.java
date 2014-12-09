package com.chillenious.common.db.jooq.svc;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.jooq.SelectQuery;

/**
 * Offset and number of rows to load with queries.
 */
public class Limit {

    /**
     * Return all rows from 0 (use with care!)
     */
    public static Limit EVERYTHING = new Limit(0, Integer.MAX_VALUE) {
        @Override
        protected void checkRange(int offset, int numberOfRows) {
        }
    };

    public static int MAX_LIMIT = 1000; // default max limit

    private static final String
            MSG_RANGE = String.format("offset must be an integer between 1 and %,d", MAX_LIMIT),
            MSG_MUST_BE_POSITIVE_INT = "offset must be a positive integer";

    /**
     * Get limit builder instance initialized with the provided limit (max rows to read).
     */
    public static LimitBuilder get(int limit) {
        return new LimitBuilder(limit);
    }

    /**
     * Get limit instance initialized with the provided limit (max rows to read), starting at row zero (0).
     */
    public static Limit getFirst(int limit) {
        return new Limit(0, limit);
    }

    /**
     * Get default limit instance (starting at row zero (0), 20 rows limit.
     */
    public static Limit getDefault() {
        return new Limit(0, 20);
    }

    /**
     * Builder for limit instances. Get an instance by calling
     * {@link LimitBuilder#get(int)}
     */
    public static class LimitBuilder {

        private final int limit;

        private LimitBuilder(int limit) {
            this.limit = limit;
        }

        /**
         * Get limit instance based on the previously provided limit and given offset.
         */
        public Limit startingAt(int offset) {
            return new Limit(offset, limit);
        }
    }

    private final int offset;

    private final int numberOfRows;

    protected Limit(int offset, int numberOfRows) {
        checkRange(offset, numberOfRows);
        this.offset = offset;
        this.numberOfRows = numberOfRows;
    }

    /**
     * Checks range of the limit when constructing. Throws IllegalStateException when range is invalid.
     */
    protected void checkRange(int offset, int numberOfRows) {
        Preconditions.checkState(offset >= 0, MSG_MUST_BE_POSITIVE_INT);
        Preconditions.checkState(numberOfRows > 0 && numberOfRows <= MAX_LIMIT, MSG_RANGE);
    }

    /**
     * Gets offset (start) query should start fetching results.
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Gets number of rows that should be fetched starting from the offset.
     */
    public int getNumberOfRows() {
        return numberOfRows;
    }

    /**
     * Add the limit clause to the provided query.
     */
    public void apply(SelectQuery query) {
        query.addLimit(offset, numberOfRows);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("offset", offset)
                .add("numberOfRows", numberOfRows)
                .toString();
    }
}
