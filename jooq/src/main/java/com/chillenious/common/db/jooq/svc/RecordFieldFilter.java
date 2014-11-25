package com.chillenious.common.db.jooq.svc;

import com.chillenious.common.util.Strings;

import java.util.Objects;

/**
 * Describes how media reference queries should be filtered.
 */
@FunctionalInterface
public interface RecordFieldFilter {

    /**
     * Whether the given property should be included (possibly looking
     * at the instance it is on).
     */
    boolean accept(Object instance, RecordField field);

    /**
     * Run command when filter accepts the passed in instance and field.
     */
    default void whenAccept(Object instance, RecordField field, Runnable cmd) {
        if (accept(instance, field)) {
            cmd.run();
        }
    }

    /**
     * Create a filter builder.
     */
    static CompositeRecordFieldFilter.CompositePropertyFilterBuilder builder() {
        return CompositeRecordFieldFilter.builder();
    }

    /**
     * Get a filter instance that only lets the properties provided through.
     */
    static RecordFieldFilter only(String... properties) {
        return CompositeRecordFieldFilter.builder().withDefaultFilter(DENY).withFilter(ALLOW, properties).build();
    }

    /**
     * Get a filter instance that lets all properties through, except the ones provided.
     */
    static RecordFieldFilter except(String... properties) {
        return CompositeRecordFieldFilter.builder().withDefaultFilter(ALLOW).withFilter(DENY, properties).build();
    }

    /**
     * Get an {@link com.chillenious.common.db.jooq.svc.RecordFieldFilter#only(String...) 'only' filter} based on the expression,
     * which should be a comma separated list of properties.
     */
    static RecordFieldFilter parse(String expr) {
        return only(Strings.split(Objects.requireNonNull(expr), ','));
    }

    /**
     * Filter that allows the field(s) that it is consulted for.
     */
    static RecordFieldFilter ALLOW = new RecordFieldFilter() {

        @Override
        public boolean accept(Object instance, RecordField field) {
            return true;
        }

        @Override
        public String toString() {
            return "Allow";
        }
    };

    /**
     * Filter that denies the field(s) that it is consulted for.
     */
    static RecordFieldFilter DENY = new RecordFieldFilter() {

        @Override
        public boolean accept(Object instance, RecordField field) {
            return false;
        }

        @Override
        public String toString() {
            return "Deny";
        }
    };
}
