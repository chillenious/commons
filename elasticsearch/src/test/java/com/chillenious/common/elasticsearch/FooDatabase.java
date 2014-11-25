package com.chillenious.common.elasticsearch;

import com.google.inject.Singleton;
import com.chillenious.common.db.sync.PersistentObjectLookup;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Singleton
public class FooDatabase implements PersistentObjectLookup<Foo>, Iterable<Foo> {

    Map<Integer, Foo> db = new HashMap<>();

    public FooDatabase() {
        for (int i = 0; i < 50; i++) {
            db.put(i, new Foo(i));
        }
    }

    public Foo get(int i) {
        return db.get(i);
    }

    @Override
    public Foo[] lookup(Object... ids) {
        Foo[] foos = new Foo[ids.length];
        for (int i = 0; i < ids.length; i++) {
            foos[i] = get(Integer.valueOf(String.valueOf(ids[i])));
        }
        return foos;
    }

    @Override
    public Iterator<Foo> iterator() {
        return db.values().iterator();
    }
}
