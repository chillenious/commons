package com.chillenious.common.db.sync;

/**
 * Returned by {@link com.chillenious.common.db.sync.DataRefresher#refresh()} as a summary of the work that was done.
 */
public class RefreshResults {

    /**
     * Helper class for gathering results during a refresh.
     */
    public static final class Counter {

        private int recordsFound, numberCreated, numberChanged, numberDeleted;

        private long start;

        Counter() {
            this.start = System.currentTimeMillis();
        }

        public void inc(DataRefreshEvent evt) {
            if (evt == null) {
                throw new NullPointerException();
            }
            recordsFound++;
            if (evt instanceof DataCreatedEvent) {
                this.numberCreated++;
            } else if (evt instanceof DataChangedEvent) {
                this.numberChanged++;
            } else if (evt instanceof DataDeletedEvent) {
                this.numberDeleted++;
            } else {
                throw new IllegalStateException(
                        String.format("unexpected event type :%s (evt = %s)", evt.getClass(), evt));
            }
        }

        public RefreshResults asResults() {
            return new RefreshResults(
                    recordsFound, numberCreated, numberChanged,
                    numberDeleted, System.currentTimeMillis() - start);
        }
    }

    /**
     * @return a new counter helper
     */
    public static Counter newCounter() {
        return new Counter();
    }

    private final int numberRecordsFound, numberCreated, numberChanged, numberDeleted;

    private final long millisecondsItTook;

    public RefreshResults(int numberRecordsFound,
                          int numberCreated,
                          int numberChanged,
                          int numberDeleted,
                          long millisecondsItTook) {
        this.numberRecordsFound = numberRecordsFound;
        this.numberCreated = numberCreated;
        this.numberChanged = numberChanged;
        this.numberDeleted = numberDeleted;
        this.millisecondsItTook = millisecondsItTook;
    }

    /**
     * @return the total number of records handled during the refresh
     */
    public int getNumberRecordsFound() {
        return numberRecordsFound;
    }

    /**
     * @return number of persistent objects recognized during the refresh as newly created
     */
    public int getNumberCreated() {
        return numberCreated;
    }

    /**
     * @return number of persistent objects recognized during the refresh as changed (updated)
     */
    public int getNumberChanged() {
        return numberChanged;
    }

    /**
     * @return number of persistent objects recognized during the refresh as deleted
     */
    public int getNumberDeleted() {
        return numberDeleted;
    }

    /**
     * @return milliseconds total it took for the refresh to execute (note that
     * this does not include the processing time of the listeners for this refresh, so
     * this is effectively the load time)
     */
    public long getMillisecondsItTook() {
        return millisecondsItTook;
    }

    @Override
    public String toString() {
        return "RefreshResults{" +
                "numberRecordsFound=" + numberRecordsFound +
                ", numberCreated=" + numberCreated +
                ", numberChanged=" + numberChanged +
                ", numberDeleted=" + numberDeleted +
                ", millisecondsItTook=" + millisecondsItTook +
                '}';
    }
}
