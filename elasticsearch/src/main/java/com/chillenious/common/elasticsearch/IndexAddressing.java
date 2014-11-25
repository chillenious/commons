package com.chillenious.common.elasticsearch;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.chillenious.common.db.sync.PersistentObject;

/**
 * Combination of index and type names for index addressing. The index name would typically be unique
 * to the application, e.g. 'campaigns', and the type a particular indexing withing that application,
 * for instance 'campaign'. Another way of looking at this is to view the index as the 'schema' and the
 * type as the 'table' if you compare it to databases.
 */
public final class IndexAddressing {

    private final String indexName;

    private final String typeName;

    public IndexAddressing(String indexName, String typeName) {
        Preconditions.checkNotNull(indexName);
        Preconditions.checkNotNull(typeName);
        this.indexName = indexName;
        this.typeName = typeName;
    }

    public IndexAddressing(String indexName, Class<? extends PersistentObject> type) {
        this(indexName, type.getClass().getName());
    }

    public String getIndexName() {
        return indexName;
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(indexName)
                .addValue(typeName)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexAddressing that = (IndexAddressing) o;

        return Objects.equal(this.indexName, that.indexName) &&
                Objects.equal(this.typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(indexName, typeName);
    }
}
