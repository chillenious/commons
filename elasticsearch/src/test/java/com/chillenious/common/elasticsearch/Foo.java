package com.chillenious.common.elasticsearch;

import com.chillenious.common.db.sync.PersistentObject;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class Foo implements PersistentObject {

    private Integer id;

    private String name;

    public Foo() {
    }

    public Foo(Integer id) {
        this.id = id;
        this.name = String.valueOf(id);
    }

    public Foo(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(id)
                .addValue(name)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Foo that = (Foo) o;

        return Objects.equal(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
