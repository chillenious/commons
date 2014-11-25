package com.chillenious.common.elasticsearch;

import com.chillenious.common.ShutdownHooks;
import com.chillenious.common.WithShutdown;
import com.chillenious.common.util.Duration;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.chillenious.common.db.sync.DataChangedEvent;
import com.chillenious.common.db.sync.DataCreatedEvent;
import com.chillenious.common.db.sync.DataDeletedEvent;
import com.chillenious.common.db.sync.DataRefreshEvent;
import com.chillenious.common.db.sync.DataRefreshListener;
import com.chillenious.common.db.sync.DataRefresher;
import com.chillenious.common.db.sync.PersistentObject;
import com.chillenious.common.db.sync.PersistentObjectLookup;
import com.chillenious.common.db.sync.RefreshResults;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Uses a refresher to build up search indexes in ElasticSearch and provides a
 * {@link Searcher} to help make that easily available
 * for searching.
 *
 * @param <O> type that is indexed
 */
public class SearchIndexer<O extends PersistentObject> implements WithShutdown {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexer.class);

    private final ObjectMapper mapper = new ObjectMapper(new JsonFactory());

    protected final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    protected final DataRefresher<O> refresher;

    protected final Client searchClient;

    protected final String indexName;

    protected final String typeName;

    protected final Searcher<O> searcher;

    /**
     * Listens in on refreshes and updates the index
     */
    final class RefreshListener extends DataRefreshListener<O> {

        @Override
        protected void onEvent(DataRefreshEvent<O> evt) {
            if (evt instanceof DataDeletedEvent) {
                Object id = evt.getId();
                removeFromIndex(id);
            } else if (evt instanceof DataCreatedEvent) {
                O object = ((DataCreatedEvent<O>) evt).getObject();
                updateIndex(object);
            } else if (evt instanceof DataChangedEvent) {
                O object = ((DataChangedEvent<O>) evt).getObject();
                updateIndex(object);
            } // else ignore; it's probably a special purpose event like req/ ack
        }
    }

    final class Refresher implements Runnable {
        @Override
        public void run() {
            try {
                log.debug(String.format("do refresh run for %s", typeName));
                try {
                    RefreshResults results = refresher.refresh();
                    if (results.getNumberRecordsFound() > 0) {
                        log.info(String.format("indexes %s updated refreshed: %s", typeName, results));
                    }
                } catch (Exception e) {
                    log.error(String.format("problem with index update for %s: %s",
                            typeName, e.getMessage()), e);
                }
                log.debug(String.format("exiting refresh daemon %s", this));
            } catch (Exception e) {
                log.error(String.format(
                        "problem with refresh run: %s; exiting",
                        e.getMessage()), e);
            }
        }
    }

    /**
     * Update the search index given the provided object (which can be new or updated).
     *
     * @param object new or updated object
     * @return result of indexing
     */
    protected IndexResponse updateIndex(O object) {
        Preconditions.checkNotNull(object);
        Preconditions.checkNotNull(object.getId());
        Object id = object.getId();
        try {
            String source = mapper.writeValueAsString(object);
            IndexResponse response = searchClient.prepareIndex(
                    indexName, typeName, id.toString())
                    .setSource(source)
                    .execute()
                    .actionGet();
            if (log.isDebugEnabled()) {
                log.debug(String.format("updated index for %s (version %,d, created: %s)",
                        source, response.getVersion(), response.isCreated()));
            }
            return response;
        } catch (Exception e) {
            log.error(String.format("unable to update index: %s (object=%s",
                    e.getMessage(), object), e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Update the search index to remove the index entries for the object that has the given id
     *
     * @param id id of the object to remove index entries for
     * @return result of deletion
     */
    protected DeleteResponse removeFromIndex(Object id) {
        Preconditions.checkNotNull(id);
        try {
            return searchClient.prepareDelete(
                    indexName, typeName, id.toString())
                    .execute().actionGet();
        } catch (Exception e) {
            log.error(String.format("unable to delete object with id %s from index: %s",
                    id, e.getMessage()), e);
            throw new IllegalStateException(e);
        }
    }

    public SearchIndexer(Class<O> type, IndexAddressing addressing,
                         ShutdownHooks shutdownHooks, DataRefresher<O> refresher,
                         PersistentObjectLookup<O> lookup, Duration refreshInterval,
                         boolean startWithRefresh, Client searchClient) {
        Preconditions.checkNotNull(refresher);
        Preconditions.checkNotNull(lookup);
        Preconditions.checkNotNull(searchClient);
        Preconditions.checkNotNull(addressing);
        this.refresher = refresher;
        shutdownHooks.add(this);
        this.searchClient = searchClient;
        this.indexName = addressing.getIndexName();
        this.typeName = addressing.getTypeName();
        this.searcher = new Searcher<>(type, mapper, indexName, typeName, searchClient, lookup);
        initRefresher(refreshInterval, startWithRefresh);
    }

    /*
     * Initialize the refresher.
     */
    private void initRefresher(
            Duration refreshInterval,
            boolean startWithRefresh) {
        refresher.addListener(new RefreshListener()); // the listener will update the cache
        Duration refreshDaemonDelay;
        if (startWithRefresh) {
            refreshDaemonDelay = refreshInterval;
            log.info(String.format("start refresh for %s", typeName));
            RefreshResults results = refresher.refresh(); // load initial data
            syncAndRefresh(Duration.seconds(4));
            requestElasticSearchRefresh();
            log.info(String.format("initialized %s with %,d objects, in %,d milliseconds",
                    typeName, (results.getNumberCreated() + results.getNumberChanged()),
                    results.getMillisecondsItTook()));
        } else {
            refreshDaemonDelay = Duration.milliseconds(0);
        }
        if (refreshInterval != null) {
            scheduler.scheduleAtFixedRate(
                    new Refresher(),
                    refreshDaemonDelay.getMilliseconds(),
                    refreshInterval.getMilliseconds(),
                    TimeUnit.MILLISECONDS);
        }
        log.info(String.format("cache %s ready", typeName));
    }

    /**
     * @return search context that can be used for this indexer
     */
    public final Searcher<O> getSearcher() {
        return searcher;
    }

    /**
     * Sync listeners (with timeout) and ask elastic search to do a refresh
     *
     * @param timeout maximum time to wait
     */
    public final void syncAndRefresh(Duration timeout) {
        refresher.waitForListeners(timeout);
        requestElasticSearchRefresh();
    }

    /**
     * Send request to elastic search to flush and refresh indexes.
     */
    protected final void requestElasticSearchRefresh() {
        searchClient.admin().indices().prepareRefresh().execute().actionGet();
    }

    @Override
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
