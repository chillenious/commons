package com.chillenious.common.db.sync;

import com.chillenious.common.util.CurrentTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;

public class SorterTest {

    @After
    public void teardown() {
        CurrentTime.reset();
    }

    @Test
    public void testSortAsc() {
        SortKeyFactory<Bam, Integer> f = (object, isNew) ->
                new SortKey<>(object, CurrentTime.currentTimeMillis());
        Sorter<Bam> sorter = new Sorter<>(f);

        Bam first = new Bam(1, "first");
        Bam second = new Bam(2, "second");
        Bam third = new Bam(3, "third");

        CurrentTime.freezeAt(1000);
        sorter.put(first);

        CurrentTime.freezeAt(2000);
        sorter.put(second);

        CurrentTime.freezeAt(3000);
        sorter.put(third);

        Iterator<Bam> i = sorter.values().iterator();
        Assert.assertNotNull(i);
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals(first, i.next());
        Assert.assertEquals(second, i.next());
        Assert.assertEquals(third, i.next());

        Assert.assertEquals(3, sorter.ids().size());
    }

    @Test
    public void testSortDesc() {
        SortKeyFactory<Bam, Integer> f = (object, isNew) ->
                new SortKey<>(object, new DescendingLong(CurrentTime.currentTimeMillis()));

        Sorter<Bam> sorter = new Sorter<>(f);

        Bam first = new Bam(1, "first");
        Bam second = new Bam(2, "second");
        Bam third = new Bam(3, "third");

        CurrentTime.freezeAt(1000);
        sorter.put(first);

        CurrentTime.freezeAt(2000);
        sorter.put(second);

        CurrentTime.freezeAt(3000);
        sorter.put(third);

        Iterator<Bam> i = sorter.values().iterator();
        Assert.assertNotNull(i);
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals(third, i.next());
        Assert.assertEquals(second, i.next());
        Assert.assertEquals(first, i.next());
    }

    @Test
    public void testMruOnlyNew() {

        // test that only new objects that are put in the cache are promoted
        SortKeyFactory<Bam, DescendingLong> f = (object, isNew) -> {
            if (isNew) {
                DescendingLong lastAccessed = new DescendingLong(
                        CurrentTime.currentTimeMillis());
                return new SortKey<>(object.getId(), lastAccessed);
            } else {
                return null; // should result in the update being ignored
            }
        };
        Sorter<Bam> sorter = new Sorter<>(f);

        Bam first = new Bam(1, "first");
        Bam second = new Bam(2, "second");

        CurrentTime.freezeAt(1000);
        sorter.put(first);
        System.out.println(sorter.values());
        CurrentTime.freezeAt(2000);
        sorter.put(second);
        System.out.println(sorter.values());
        CurrentTime.freezeAt(3000);
        sorter.put(first); // this is an update, so second should still be in front

        Assert.assertEquals(second, sorter.values().iterator().next());
    }
}
