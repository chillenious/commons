package com.chillenious.common.db.jooq.svc;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.jooq.Field;

/**
 * Field from a record/ query that can be filtered on.
 */
public final class RecordField {

    /**
     * Get instance based on the passed in jOOQ field.
     */
    public static RecordField of(Field<?> f) {
        return new RecordField(f.getName());
    }

    /**
     * Get instance based on the passed in name.
     */
    public static RecordField of(String name) {
        return new RecordField(name);
    }

    private final String name;

    private RecordField(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordField that = (RecordField) o;
        return Objects.equal(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .toString();
    }
}
