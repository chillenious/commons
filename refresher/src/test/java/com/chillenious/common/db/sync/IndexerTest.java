package com.chillenious.common.db.sync;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class IndexerTest {

    @Test
    public void testIndexOnName() {

        Indexer<Bam, String> indexer = new Indexer<>(Bam::getName);

        Bam first = new Bam(1, "foo");
        Bam second = new Bam(2, "foo");
        Bam third = new Bam(3, "foo");
        Bam fourth = new Bam(4, "bar");
        Bam fifth = new Bam(5, "bar");

        indexer.put(first);
        indexer.put(second);
        indexer.put(third);
        indexer.put(fourth);
        indexer.put(fifth);

        Assert.assertTrue(indexer.get("noset").isEmpty());

        Set<Bam> fooSet = indexer.get("foo");
        Assert.assertNotNull(fooSet);
        Assert.assertEquals(3, fooSet.size());
        Assert.assertTrue(fooSet.contains(first));
        Assert.assertTrue(fooSet.contains(second));
        Assert.assertTrue(fooSet.contains(third));
        Assert.assertFalse(fooSet.contains(fourth));

        Set<Bam> barSet = indexer.get("bar");
        Assert.assertNotNull(barSet);
        Assert.assertEquals(2, barSet.size());
        Assert.assertTrue(barSet.contains(fourth));
        Assert.assertTrue(barSet.contains(fifth));
        Assert.assertFalse(barSet.contains(first));
    }
}
