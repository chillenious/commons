package com.chillenious.common.db.jooq.svc;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.jooq.SelectQuery;

/**
 * Offset and number of rows to load with queries.
 */
public final class Limit {

    private static final int MAX_LIMIT = 1000;

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
     * {@link com.chillenious.common.db.jooq.svc.Limit.LimitBuilder#get(int)}
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

    private Limit(int offset, int numberOfRows) {
        Preconditions.checkState(offset >= 0, MSG_MUST_BE_POSITIVE_INT);
        Preconditions.checkState(numberOfRows > 0 && numberOfRows <= MAX_LIMIT, MSG_RANGE);
        this.offset = offset;
        this.numberOfRows = numberOfRows;
    }

    public int getOffset() {
        return offset;
    }

    public int getNumberOfRows() {
        return numberOfRows;
    }

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
