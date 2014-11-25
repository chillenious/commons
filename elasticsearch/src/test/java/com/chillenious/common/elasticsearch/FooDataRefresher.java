package com.chillenious.common.elasticsearch;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chillenious.common.ShutdownHooks;
import com.chillenious.common.db.sync.DataCreatedEvent;
import com.chillenious.common.db.sync.DataRefresher;
import com.chillenious.common.db.sync.RefreshResults;

@Singleton
public class FooDataRefresher extends DataRefresher<Foo> {

    private final FooDatabase fooDatabase;

    @Inject
    public FooDataRefresher(ShutdownHooks shutdownHooks,
                            FooDatabase fooDatabase) {
        super(shutdownHooks);
        this.fooDatabase = fooDatabase;
    }

    @Override
    public RefreshResults refresh() {
        for (Foo foo : fooDatabase) {
            publish(new DataCreatedEvent<Foo>(foo));
        }
        return new RefreshResults(fooDatabase.db.size(), fooDatabase.db.size(), 0, 0, 1);
    }
}
