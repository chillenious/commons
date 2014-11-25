/**
 *
 */
package com.chillenious.common.db;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Provider for data source configured through dataSource.* properties.
 */
@Singleton
public class DefaultDataSourceProvider implements Provider<DataSource> {

    private static final Logger log = LoggerFactory.getLogger(DefaultDataSourceProvider.class);

    private final DataSources dataSources;

    @Inject
    public DefaultDataSourceProvider(DataSources dataSources) {
        this.dataSources = dataSources;
        if (!dataSources.hasDataSource("dataSource")) {
            log.warn("No default dataSource is configured (settings dataSource.driverClass " +
                     "and dataSource.jdbcUrl are not defined)");
        }
    }

    @Override
    public DataSource get() {
        DataSource dataSource = dataSources.getDataSource("dataSource");
        if (dataSource == null) {
            throw new IllegalStateException(String.format("no default dataSource is defined"));
        }
        return dataSource;
    }
}
