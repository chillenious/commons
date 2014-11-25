package com.chillenious.common.db.jooq.svc;

import com.google.common.collect.ImmutableMap;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.exception.MappingException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Uses introspection based mapper by default but allows overriding of population on a per-field basis
 * (which is only used when the field is actually used).
 */
public final class CompositeRecordMapper<R extends Record, E> implements RecordMapper<R, E> {

    /**
     * Builder for {@link com.chillenious.common.db.jooq.svc.CompositeRecordMapper record mappers}.
     */
    public static final class CompositeRecordMapperBuilder<R extends Record, E> {

        private final Class<? extends E> type;

        private Map<String, FieldMapper<R, E>> fieldMappers = new HashMap<>();

        private E instance;

        private RecordConsumer<R, E> preProcessor;

        private RecordConsumer<R, E> postProcessor;

        private RecordFieldFilter filter;

        private CompositeRecordFieldFilter.CompositePropertyFilterBuilder filterBuilder;

        public CompositeRecordMapperBuilder(Class<? extends E> type, E instance) {
            this.type = type;
        }

        /**
         * Add provided mapper to builder for the provided field field.
         */
        public CompositeRecordMapperBuilder<R, E> withMapper(String fieldName, FieldMapper<R, E> mapper) {
            fieldMappers.put(fieldName, mapper);
            return this;
        }

        /**
         * Add provided pre-processor to builder.
         */
        public CompositeRecordMapperBuilder<R, E> withPreProcessor(RecordConsumer<R, E> preProcessor) {
            this.preProcessor = preProcessor;
            return this;
        }

        /**
         * Add provided post-processor to builder.
         */
        public CompositeRecordMapperBuilder<R, E> withPostProcessor(RecordConsumer<R, E> postProcessor) {
            this.postProcessor = postProcessor;
            return this;
        }

        /**
         * Add provided record-level filter to builder.
         *
         * @throws IllegalStateException when a filter was already added to the builder
         */
        public CompositeRecordMapperBuilder<R, E> withFilter(RecordFieldFilter filter) {
            if (this.filter != null) {
                throw new IllegalStateException("filter " + this.filter + " was already set");
            }
            this.filter = filter;
            return this;
        }

        /**
         * Add provided field level filter to builder.
         *
         * @throws IllegalStateException when a record-level filter was already added to the builder
         */
        public CompositeRecordMapperBuilder<R, E> withFilter(String property,
                                                             RecordFieldFilter filter) {
            return withFilter(new CompositeRecordFieldFilter.OnProperty(property), filter);
        }

        /**
         * Add provided field level filter to builder.
         *
         * @throws IllegalStateException when a record-level filter was already added to the builder
         */
        public CompositeRecordMapperBuilder<R, E> withFilter(CompositeRecordFieldFilter.Registration registration,
                                                             RecordFieldFilter filter) {
            if (this.filter != null) {
                throw new IllegalStateException("filter " + this.filter + " was already set");
            }
            if (this.filterBuilder == null) {
                this.filterBuilder = CompositeRecordFieldFilter.builder();
            }
            filterBuilder.withFilter(Objects.requireNonNull(registration), Objects.requireNonNull(filter));
            return this;
        }


        /**
         * Add instance to the builder. The instance will be populated rather than a new instance created.
         */
        public CompositeRecordMapperBuilder<R, E> withInstance(E instance) {
            this.instance = instance;
            return this;
        }

        /**
         * Get a new record mapper instance based on the state build up in the builder.
         */
        public CompositeRecordMapper<R, E> build() {
            RecordFieldFilter filter = this.filter;
            if (filterBuilder != null) {
                filter = filterBuilder.build();
            }
            return new CompositeRecordMapper<>(
                    type, ImmutableMap.copyOf(fieldMappers),
                    preProcessor, postProcessor, filter, instance);
        }
    }

    /**
     * Get builder instance based on the provided type.
     */
    public static <R extends Record, E> CompositeRecordMapperBuilder<R, E> of(Class<? extends E> type) {
        return of(type, null);
    }

    /**
     * Get builder instance based on the provided type and instance.
     */
    public static <R extends Record, E> CompositeRecordMapperBuilder<R, E> of(Class<? extends E> type, E instance) {
        return new CompositeRecordMapperBuilder<>(type, instance);
    }

    // target type
    private final Class<? extends E> type;

    // optional target instance to use instead of creating new instances
    private Optional<E> instance;

    private final ImmutableMap<String, FieldMapper<R, E>> fieldMappers;

    private final Optional<RecordConsumer<R, E>> preProcessor;

    private final Optional<RecordConsumer<R, E>> postProcessor;

    private final RecordFieldFilter filter;

    // field mapper used when nothing explicit is set
    // set lazily when first record comes in (as we need to get the fields from the result set
    // but want to initialize just once)
    private FieldMapper<R, E> defaultFieldMapper = null;

    /**
     * Constructor for builder.
     */
    private CompositeRecordMapper(Class<? extends E> type,
                                  ImmutableMap<String, FieldMapper<R, E>> fieldMappers,
                                  RecordConsumer<R, E> preProcessor,
                                  RecordConsumer<R, E> postProcessor,
                                  RecordFieldFilter filter,
                                  E instance) {
        this.type = type;
        this.fieldMappers = fieldMappers != null ? fieldMappers : ImmutableMap.<String, FieldMapper<R, E>>of();
        this.preProcessor = Optional.ofNullable(preProcessor);
        this.postProcessor = Optional.ofNullable(postProcessor);
        this.filter = Optional.ofNullable(filter).orElse(RecordFieldFilter.ALLOW);
        this.instance = Optional.ofNullable(instance);
    }

    /**
     * Return a new instance of the mapper with the provided filter set, or this instance if filter is null.
     *
     * @param filter filter, optional
     * @return mapper instance
     */
    public CompositeRecordMapper<R, E> withFilter(RecordFieldFilter filter) {
        if (filter == null) {
            return this;
        } else {
            return new CompositeRecordMapper<>(type, fieldMappers,
                    preProcessor.orElse(null), postProcessor.orElse(null),
                    filter, instance.orElse(null));
        }
    }

    @Override
    public E map(R record) {

        if (defaultFieldMapper == null) {
            this.defaultFieldMapper = new MutablePOJOFieldMapper<>(type, record.fields());
        }

        try {
            E result = instance.orElse(type.getDeclaredConstructor().newInstance());

            preProcessor.ifPresent(c -> c.populate(filter, result, record));

            Arrays.stream(record.fields()).forEach(field -> {
                if (filter.accept(result, RecordField.of(field))) {
                    FieldMapper<R, E> fieldMapper =
                            fieldMappers.getOrDefault(field.getName(), defaultFieldMapper);
                    fieldMapper.populate(result, record, field);
                }
            });

            postProcessor.ifPresent(c -> c.populate(filter, result, record));

            return result;
        } catch (Exception e) {
            throw new MappingException(String.format(
                    "An error occurred when mapping record to type %s: %s; record:\n%s",
                    type, e.getMessage(), record), e);
        }
    }
}
