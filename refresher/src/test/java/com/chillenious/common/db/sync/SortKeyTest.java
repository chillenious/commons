package com.chillenious.common.db.sync;

import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class SortKeyTest {

    @Test
    public void testBasicSort() {
        NavigableMap<DescendingLong, Bam> sorted =
                new ConcurrentSkipListMap<>();
        DescendingLong a1 = new DescendingLong(1000);
        Bam first = new Bam(1, "first");
        DescendingLong a2 = new DescendingLong(2000);
        Bam second = new Bam(2, "second");
        DescendingLong a3 = new DescendingLong(3000);
        Bam third = new Bam(3, "third");
        DescendingLong a4 = new DescendingLong(4000);
        Bam fourth = new Bam(4, "fourth");
        sorted.put(a2, second);
        sorted.put(a4, fourth);
        sorted.put(a1, first);
        sorted.put(a3, third);

        Iterator<Bam> i = sorted.values().iterator();
        Assert.assertNotNull(i);
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals(i.next(), fourth);
        Assert.assertEquals(i.next(), third);
        Assert.assertEquals(i.next(), second);
        Assert.assertEquals(i.next(), first);
    }

    @Test
    public void testBasicSortWithSortKey() {
        NavigableMap<SortKey, Bam> sorted =
                new ConcurrentSkipListMap<>();
        DescendingLong a1 = new DescendingLong(1000);
        Bam first = new Bam(1, "first");
        DescendingLong a2 = new DescendingLong(2000);
        Bam second = new Bam(2, "second");
        DescendingLong a3 = new DescendingLong(3000);
        Bam third = new Bam(3, "third");
        DescendingLong a4 = new DescendingLong(4000);
        Bam fourth = new Bam(4, "fourth");
        sorted.put(new SortKey(fourth, a4), fourth);
        sorted.put(new SortKey(first, a1), first);
        sorted.put(new SortKey(third, a3), third);
        sorted.put(new SortKey(second, a2), second);

        Iterator<Bam> i = sorted.values().iterator();
        Assert.assertNotNull(i);
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals(i.next(), fourth);
        Assert.assertEquals(i.next(), third);
        Assert.assertEquals(i.next(), second);
        Assert.assertEquals(i.next(), first);
    }
}
