package com.chillenious.common.db.jooq.svc;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Filter that is comprised of a number of property specific filters and a default
 * filter to fall back to when no specific filter is found for a particular property.
 */
public final class CompositeRecordFieldFilter implements RecordFieldFilter {

    /**
     * Builds filter(s).
     */
    public static final class CompositePropertyFilterBuilder {

        private final List<FilterWithRegistration> registrations = new ArrayList<>();

        private RecordFieldFilter defaultFilter;

        /**
         * Add provided filter to the builder for the provided registration.
         */
        public CompositePropertyFilterBuilder withFilter(Registration r, RecordFieldFilter f) {
            registrations.add(new FilterWithRegistration(r, f));
            return this;
        }

        /**
         * Add provided filter to the builder registered as
         * {@link com.chillenious.common.db.jooq.svc.CompositeRecordFieldFilter.OnProperty} registrations
         * for all provided property names.
         */
        public CompositePropertyFilterBuilder withFilter(RecordFieldFilter f, String... propertyNames) {
            Arrays.stream(propertyNames).forEach(propertyName ->
                    registrations.add(new FilterWithRegistration(new OnProperty(propertyName), f)));
            return this;
        }

        /**
         * Add default filter to the builder. The default filter will be used for the fields that
         * don't have specific registered filters.
         */
        public CompositePropertyFilterBuilder withDefaultFilter(RecordFieldFilter defaultFilter) {
            this.defaultFilter = defaultFilter;
            return this;
        }

        /**
         * Get a filter instance from this builder.
         */
        public RecordFieldFilter build() {
            if (!registrations.isEmpty()) {
                return new CompositeRecordFieldFilter(defaultFilter, ImmutableList.copyOf(registrations));
            } else {
                return defaultFilter != null ? defaultFilter : RecordFieldFilter.ALLOW;
            }
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("registrations", registrations)
                    .add("defaultFilter", defaultFilter)
                    .toString();
        }
    }

    /**
     * Get a filter builder instance.
     */
    public static CompositePropertyFilterBuilder builder() {
        return new CompositePropertyFilterBuilder();
    }

    /**
     * Registration for a filter.
     */
    @FunctionalInterface
    public static interface Registration {

        /**
         * Whether this registration matches the provided field.
         */
        boolean accept(RecordField field);
    }

    public static final class OnProperty implements Registration {

        private final String property;

        public OnProperty(String property) {
            this.property = Objects.requireNonNull(property);
        }

        @Override
        public boolean accept(RecordField field) {
            return property.equalsIgnoreCase(field.getName());
        }

        @Override
        public String toString() {
            return property;
        }
    }

    static final class FilterWithRegistration {

        private final Registration registration;

        private final RecordFieldFilter filter;

        public FilterWithRegistration(Registration registration,
                                      RecordFieldFilter filter) {
            this.registration = Objects.requireNonNull(registration);
            this.filter = Objects.requireNonNull(filter);
        }

        public Registration getRegistration() {
            return registration;
        }

        public RecordFieldFilter getFilter() {
            return filter;
        }

        @Override
        public String toString() {
            return registration + " -> " + filter;
        }
    }

    // filter to use when property doesn't have a registered filter
    private final RecordFieldFilter defaultFilter;

    // property specific filters
    private final ImmutableList<FilterWithRegistration> filters;

    public CompositeRecordFieldFilter(RecordFieldFilter defaultFilter,
                                      ImmutableList<FilterWithRegistration> filters) {
        this.defaultFilter = defaultFilter != null ? defaultFilter : (instance, field) -> filters.isEmpty();
        this.filters = Objects.requireNonNull(filters);
    }

    @Override
    public boolean accept(Object instance, RecordField field) {
        Boolean accepted = null;
        for (FilterWithRegistration r : filters) {
            if (r.getRegistration().accept(field)) {
                accepted = r.getFilter().accept(instance, field);
                if (!accepted) {
                    break;
                }
            }
        }
        return accepted != null ? accepted : defaultFilter.accept(instance, field);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("defaultFilter", defaultFilter)
                .add("filters", filters)
                .toString();
    }
}
