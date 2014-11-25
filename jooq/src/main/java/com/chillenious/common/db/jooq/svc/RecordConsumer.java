package com.chillenious.common.db.jooq.svc;

import org.jooq.Record;

/**
 * Supports plugging in pre/ post processing relative to field mapping.
 */
@FunctionalInterface
public interface RecordConsumer<R extends Record, E> {

    /**
     * Can do manual population of the instance based on the record.
     * Use the filter to see whether properties should be included (optionally).
     */
    void populate(RecordFieldFilter filter, E instance, R record);
}
