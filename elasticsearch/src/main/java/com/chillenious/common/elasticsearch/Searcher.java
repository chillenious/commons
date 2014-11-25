package com.chillenious.common.elasticsearch;

import com.chillenious.common.db.sync.PersistentObject;
import com.chillenious.common.db.sync.PersistentObjectLookup;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Array;

import static org.elasticsearch.index.query.QueryBuilders.queryString;

/**
 * Search client together with the appropriate index and type names.
 */
public class Searcher<O extends PersistentObject> {

    private static final Logger log = LoggerFactory.getLogger(Searcher.class);

    private final IndexAddressing addressing;

    private final Client searchClient;

    private final PersistentObjectLookup<O> lookup;

    private final ObjectMapper mapper;

    private final Class<O> type; // sooo lame, Java sux0rz

    Searcher(Class<O> type,
             ObjectMapper mapper,
             String indexName,
             String typeName,
             Client searchClient,
             PersistentObjectLookup<O> lookup) {
        this(type, mapper, new IndexAddressing(indexName, typeName),
                searchClient, lookup);
    }

    @SuppressWarnings("unchecked")
    Searcher(Class<O> type,
             ObjectMapper mapper,
             IndexAddressing addressing,
             Client searchClient,
             PersistentObjectLookup<O> lookup) {
        Preconditions.checkNotNull(mapper);
        Preconditions.checkNotNull(addressing);
        Preconditions.checkNotNull(searchClient);
        Preconditions.checkNotNull(lookup);
        this.type = type;
        this.mapper = mapper;
        this.addressing = addressing;
        this.searchClient = searchClient;
        this.lookup = lookup;
    }

    public String getIndexName() {
        return addressing.getIndexName();
    }

    public String getTypeName() {
        return addressing.getTypeName();
    }

    public Client getSearchClient() {
        return searchClient;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    @SuppressWarnings("unchecked")
    public ListPage<O> findForQuery(String query, int from, int pageSize) {
        SearchResponse searchResponse = searchClient.prepareSearch(addressing.getIndexName())
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTypes(addressing.getTypeName())
                .setQuery(queryString(query))
                .setFrom(from)
                .setSize(pageSize)
                .execute()
                .actionGet();
        SearchHit[] hits = searchResponse.getHits().hits();
        O[] objects = (O[]) Array.newInstance(type, hits.length);
        for (int i = 0, l = hits.length; i < l; i++) {
            byte[] source = hits[i].source();
            objects[i] = convert(query, source);
        }
        return new ListPage<O>()
                .count((int) searchResponse.getHits().getTotalHits())
                .start(from).pageSize(pageSize).objects(objects);
    }

    @Nullable
    public O findSingleForQuery(String query) {
        SearchResponse searchResponse = searchClient.prepareSearch(addressing.getIndexName())
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTypes(addressing.getTypeName())
                .setQuery(queryString(query))
                .execute()
                .actionGet();
        SearchHits hits = searchResponse.getHits();
        long totalHits = hits.getTotalHits();
        if (totalHits == 1) {
            byte[] source = hits.getAt(0).source();
            return convert(query, source);
        } else if (totalHits > 1) {
            throw new IllegalArgumentException(String.format("more than 1 result (in fact, %,d) for query '%s'",
                    hits.totalHits(), query));
        } else {
            return null;
        }
    }

    private O convert(String query, byte[] source) {
        try {
            return mapper.readValue(source, type);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("unable to result to type %s: %s (query = '%s')",
                    type, e.getMessage(), query));
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(addressing)
                .addValue(searchClient)
                .addValue(lookup)
                .toString();
    }
}
