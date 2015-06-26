package com.chillenious.common.db;

import com.chillenious.common.util.AnySetter;
import com.zaxxer.hikari.HikariConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Fake JavaBean properties for things that can't be populated like that
 * in Hikari.
 */
public class HikariConfigExtended extends HikariConfig {

    private final Map<String, String> extraDataSourceProperties = new HashMap<>();

    public void setUser(String user) {
        setDataSourceProperty("user", user);
    }

    public void setServerName(String serverName) {
        setDataSourceProperty("serverName", serverName);
    }

    public void setPort(String port) {
        setDataSourceProperty("port", port);
    }

    @AnySetter
    public void setDatabaseName(String databaseName) {
        setDataSourceProperty("databaseName", databaseName);
    }

    public void setDataSourceProperty(String key, String value) {
        extraDataSourceProperties.put(key, value);
    }

    // assume that all unrecognized properties should be interpreted as data source properties
    public void initExtraDataSourceProperties() {
        extraDataSourceProperties.forEach(this::addDataSourceProperty);
    }
}
