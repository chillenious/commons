package com.chillenious.common.elasticsearch;

import com.google.inject.Inject;
import com.chillenious.common.Bootstrap;
import com.chillenious.common.Settings;
import com.chillenious.common.util.Duration;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.elasticsearch.index.query.QueryBuilders.queryString;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

public class SearchIndexerTest {

    private static final Logger log =
            LoggerFactory.getLogger(SearchIndexerTest.class);

    private Bootstrap bootstrap;

    @Inject
    Client client;

    @Inject
    SearchIndexerBuilder indexerBuilder;

    @Inject
    FooDataRefresher refresher;

    @Inject
    FooDatabase database;

    @Before
    public void setup() {
        Settings settings = Settings.builder().build();
        bootstrap = new Bootstrap(settings, new ElasticSearchModule());
        bootstrap.getInjector().injectMembers(this);
        try {
            client.prepareDeleteByQuery("base")
                    .setQuery(queryString("*"))
                    .execute()
                    .actionGet();
        } catch (IndexMissingException e) {
            // fine
        }
    }

    @After
    public void teardown() {
        bootstrap.shutdown();
    }

    @Test
    public void testIndexer() {
        SearchIndexer<Foo> indexer = indexerBuilder.with(
                Foo.class, new IndexAddressing("base", "testcampaigns"),
                refresher, database)
                .withRefreshOnConstruction()
                .build();

        indexer.syncAndRefresh(Duration.seconds(1));
        Searcher<Foo> searcher = indexer.getSearcher();
        SearchResponse searchResponse = client.prepareSearch(searcher.getIndexName())
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTypes(searcher.getTypeName())
                .setQuery(termQuery("name", "10"))
                .execute()
                .actionGet();
        Assert.assertNotNull(searchResponse);
        Assert.assertNotNull(searchResponse.getHits());
        Assert.assertEquals(1L, searchResponse.getHits().getTotalHits());
        log.info("source -> " + searchResponse.getHits().getAt(0).getSourceAsString());

        Foo foo = searcher.findSingleForQuery("name:10");
        Assert.assertNotNull(foo);
        Assert.assertEquals("10", foo.getName());

        ListPage<Foo> foos = searcher.findForQuery("name:*", 0, 10);
        Assert.assertNotNull(foos);
        Assert.assertNotNull(foos.getObjects());
        Assert.assertEquals(10, foos.getObjects().length);
        Assert.assertEquals(50, foos.getCount());
        Assert.assertEquals(0, foos.getStart());
        Assert.assertEquals(10, foos.getPageSize());

        ListPage<Foo> otherFoos = searcher.findForQuery("name:*", 45, 10);
        Assert.assertNotNull(otherFoos);
        Assert.assertNotNull(otherFoos.getObjects());
        Assert.assertEquals(5, otherFoos.getObjects().length);
        Assert.assertEquals(50, otherFoos.getCount());
        Assert.assertEquals(45, otherFoos.getStart());
        Assert.assertEquals(10, otherFoos.getPageSize());

        ListPage<Foo> yetOtherFoos = searcher.findForQuery("name:*", 55, 10);
        Assert.assertNotNull(yetOtherFoos);
        Assert.assertNotNull(yetOtherFoos.getObjects());
        Assert.assertEquals(0, yetOtherFoos.getObjects().length);
    }
}
