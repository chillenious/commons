package com.chillenious.common.db.sync;

import com.chillenious.common.ShutdownHooks;

/**
 * Refresher for testing purposes; always returns results with nothing new.
 */
public class NoopRefresher<O extends PersistentObject> extends DataRefresher<O> {

    static final RefreshResults NOTHING_REALLY = RefreshResults.newCounter().asResults();

    public NoopRefresher() {
        super(new ShutdownHooks());
    }

    @Override
    public RefreshResults refresh() {
        return NOTHING_REALLY;
    }
}
