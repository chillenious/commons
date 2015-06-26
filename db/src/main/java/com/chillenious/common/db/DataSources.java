package com.chillenious.common.db;

import com.chillenious.common.Settings;
import com.chillenious.common.ShutdownHooks;
import com.chillenious.common.WithShutdown;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static com.chillenious.common.Settings.notContains;

/**
 * Stores the data sources known to the application. Use
 * {@link #getDataSource(String)} to get a specific data
 * source instance.
 */
@Singleton
public class DataSources implements WithShutdown {

    private final static Logger log = LoggerFactory.getLogger(DataSources.class);

    private final Settings.MappedSettings<HikariConfigExtended> dataSourceConfigs;

    private final Map<String, HikariDataSource> dataSources = new HashMap<>();

    private final Settings settings;


    @Inject
    public DataSources(Settings settings,
                       ShutdownHooks shutdownHooks)
            throws SQLException {

        this.settings = settings;
        shutdownHooks.add(this);
        // initialize with any single data source setup


        dataSourceConfigs = settings.map("dataSource.", HikariConfigExtended.class);

        // add any additionally configured (multi-)datasources
        String dataSourceGroups = settings.getString("dataSourceGroups");
        if (dataSourceGroups != null
                && !(dataSourceGroups = dataSourceGroups.trim()).equals("")) {
            log.info("loading data sources " + dataSourceGroups);
            for (String group : dataSourceGroups.split(",")) {
                loadDataSourceGroupConfig(group);
            }
        }

        // create actual data sources
        for (Entry<String, HikariConfigExtended> entry : dataSourceConfigs.entrySet()) {

            HikariConfigExtended config = entry.getValue();
            config.initExtraDataSourceProperties();
            String dataSourceName = entry.getKey();
            // try loading the data source class
            try {
                Class.forName(config.getDataSourceClassName());
            } catch (ClassNotFoundException ex) {
                throw new IllegalArgumentException(
                        String.format("unable to load driver %s for data source %s; " +
                                        "make sure the setting is spelled correctly and that the " +
                                        "driver class is available on the class path",
                                config.getDataSourceClassName(), dataSourceName), ex);
            }

            dataSources.put(dataSourceName, new HikariDataSource(config));
            log.info(String.format("'%s' loaded '{'url=%s, user=%s'}'",
                    dataSourceName, config.getJdbcUrl(), config.getUsername()));
        }
    }

    private Settings.MappedSettings<HikariConfigExtended> map(String poolName) {
        Settings.MappedSettings<HikariConfigExtended> map =
                settings.map(poolName + ".", HikariConfigExtended.class, notContains("default"));
        Settings.MappedSettings<HikariConfigExtended> defaults =
                settings.map(poolName + ".default", HikariConfigExtended.class);
        map.mergeDefaults(defaults);
        return map;
    }

    /**
     * Load a group of data sources.
     * <p>
     * Data sources are grouped by prefixes, such as fooDataSource, every group
     * having a defaults section (&lt;group&gt;.default.&lt;property&gt;) and a
     * number of specific data sources (&lt;group&gt;.&lt;specific data source
     * name&gt;.&lt;property&gt;).
     * </p>
     *
     * @param group name of the group
     */
    public void loadDataSourceGroupConfig(String group) {
        Settings.MappedSettings<HikariConfigExtended> mappedGroup = map(group);
        dataSourceConfigs.merge(mappedGroup);
    }

    /**
     * Gets the data source that is registered with the provided pool name.
     *
     * @param poolName pool name to look the data source up for
     * @return data source
     * @throws IllegalArgumentException when no data source could be found for the provided pool name
     */
    public DataSource getDataSource(String poolName) {
        DataSource ds = dataSources.get(poolName);
        if (ds == null) {
            throw new IllegalArgumentException(
                    "data source with pool name " + poolName + " not found");
        }
        return ds;
    }

    /**
     * Whether a data source is registered with the provided pool name.
     *
     * @param poolName pool name to look the data source up for
     * @return true if found, false otherwise
     */
    public boolean hasDataSource(String poolName) {
        return dataSourceConfigs.containsKey(poolName);
    }

    @Override
    public void shutdown() {
        for (Entry<String, HikariDataSource> e : dataSources.entrySet()) {
            log.info("shutting down '" + e.getKey() + "'");
            try {
                e.getValue().close();
            } catch (Throwable t) {
                log.error(String.format("error shutting down data source: %s", t.getMessage()));
            }
        }
        dataSourceConfigs.clear();
        dataSources.clear();
    }

    @Override
    public String toString() {
        return "DataSources {pools: " + dataSourceConfigs.keySet() + "}";
    }
}
