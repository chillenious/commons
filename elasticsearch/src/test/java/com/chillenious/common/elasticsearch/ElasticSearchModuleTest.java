package com.chillenious.common.elasticsearch;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.chillenious.common.Bootstrap;
import com.chillenious.common.Settings;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
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

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.smileBuilder;
import static org.elasticsearch.index.query.QueryBuilders.queryString;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

public class ElasticSearchModuleTest {

    private static final Logger log =
            LoggerFactory.getLogger(ElasticSearchModuleTest.class);

    static final String ROOT = "base";

    private final ObjectMapper mapper = new ObjectMapper(new JsonFactory());

    private Bootstrap bootstrap;

    @Inject
    Client client;

    @Before
    public void setup() {
        Settings settings = Settings.builder().build();
        bootstrap = new Bootstrap(settings, new ElasticSearchModule());
        bootstrap.getInjector().injectMembers(this);
        try {
            client.prepareDeleteByQuery(ROOT)
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
    public void testSimpleIndexAndSearch() throws IOException {

        IndexResponse indexResponse = client.prepareIndex(ROOT, "campaignz", "1")
                .setSource(smileBuilder()
                                .startObject()
                                .field("name", "Vonage February 2012")
                                .field("startDate", "20130827")
                                .endObject()
                )
                .execute()
                .actionGet();
        Assert.assertNotNull(indexResponse);
        Assert.assertTrue(indexResponse.isCreated());

        GetResponse getResponse = client.prepareGet(ROOT, "campaignz", "1")
                .execute()
                .actionGet();
        Assert.assertNotNull(getResponse);
        Assert.assertTrue(getResponse.isExists());
        Assert.assertFalse(getResponse.isSourceEmpty());
        log.info("source -> " + getResponse.getSourceAsString());

        client.admin().indices().prepareRefresh().execute().actionGet();

        SearchResponse searchResponse = client.prepareSearch(ROOT)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTypes("campaignz")
                .setQuery(termQuery("startDate", "20130827"))
                .execute()
                .actionGet();
        Assert.assertNotNull(searchResponse);
        Assert.assertNotNull(searchResponse.getHits());
        Assert.assertEquals(1L, searchResponse.getHits().getTotalHits());
        log.info("source -> " + searchResponse.getHits().getAt(0).getSourceAsString());

        searchResponse = client.prepareSearch(ROOT)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTypes("campaignz")
                .setQuery(queryString("startDate:20130827"))
                .execute()
                .actionGet();
        Assert.assertNotNull(searchResponse);
        Assert.assertNotNull(searchResponse.getHits());
        Assert.assertEquals(1L, searchResponse.getHits().getTotalHits());

        Foo foo = new Foo(2);
        String json = mapper.writeValueAsString(foo);
        IndexRequest indexRequest = new IndexRequest(ROOT, "foo", foo.getId().toString());
        indexRequest.source(json);
        indexResponse = client.index(indexRequest).actionGet();
        Assert.assertNotNull(indexResponse);
        Assert.assertTrue(indexResponse.isCreated());

        client.admin().indices().prepareRefresh().execute().actionGet();

        getResponse = client.prepareGet(ROOT, "foo", "2")
                .setType("foo")
                .execute()
                .actionGet();
        Assert.assertNotNull(getResponse);
        Assert.assertTrue(getResponse.isExists());
        Assert.assertFalse(getResponse.isSourceEmpty());
        log.info("source -> " + getResponse.getSourceAsString());

        searchResponse = client.prepareSearch(ROOT)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(queryString("name:2"))
                .execute()
                .actionGet();
        Assert.assertNotNull(searchResponse);
        Assert.assertNotNull(searchResponse.getHits());
        Assert.assertEquals(1L, searchResponse.getHits().getTotalHits());
    }
}
