package com.chillenious.common.db.jooq.svc;

import org.jooq.Field;
import org.jooq.Record;

/**
 * Strategy for populating objects for a given field.
 */
@FunctionalInterface
public interface FieldMapper<R extends Record, E> {

    /**
     * Sets value (via method or member access) on the target corresponding
     * to the field value.
     */
    void populate(E instance, R record, Field<?> field);
}
