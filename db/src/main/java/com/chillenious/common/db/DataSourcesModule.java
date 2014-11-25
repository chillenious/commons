package com.chillenious.common.db;

import com.google.inject.AbstractModule;

import javax.sql.DataSource;

/**
 * Configures {@link javax.sql.DataSource data sources} from settings. A single/ main data
 * source is set up like:
 * <p/>
 * <pre>
 * dataSource.driverClass=com.mysql.jdbc.Driver
 * dataSource.jdbcUrl=jdbc:mysql://localhost/tsplatform?
 * dataSource.username=root
 * dataSource.password=
 * dataSource.partitionCount=3
 * dataSource.minConnectionsPerPartition=2
 * dataSource.maxConnectionsPerPartition=10
 * dataSource.acquireIncrement=2
 * dataSource.statementsCacheSize=0
 * dataSource.logStatementsEnabled=false
 * </pre>
 * <p/>
 * and multiple ones like this:
 * <pre>
 * dataSourceGroups=aDataSource,anotherDataSource
 * aDataSource.driverClass=com.mysql.jdbc.Driver
 * aDataSource.jdbcUrl=jdbc:mysql://localhost/foo?profileSQL=false
 * aDataSource.minConnectionsPerPartition=3
 * aDataSource.maxConnectionsPerPartition=15
 * aDataSource.username=foouser
 * aDataSource.password=foouser
 * anotherDataSource.driverClass=com.mysql.jdbc.Driver
 * anotherDataSource.jdbcUrl=jdbc:mysql://localhost/bar?profileSQL=false
 * anotherDataSource.minConnectionsPerPartition=3
 * anotherDataSource.maxConnectionsPerPartition=15
 * anotherDataSource.username=baruser
 * anotherDataSource.password=baruser
 * </pre>
 * <p/>
 */
public class DataSourcesModule extends AbstractModule {

    public DataSourcesModule() {
    }

    @Override
    protected void configure() {
        bind(DataSource.class).toProvider(DefaultDataSourceProvider.class);
    }
}
