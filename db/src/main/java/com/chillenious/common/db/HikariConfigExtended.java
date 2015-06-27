package com.chillenious.common.db;

import com.chillenious.common.util.AnySetter;
import com.zaxxer.hikari.HikariConfig;

/**
 * Fake JavaBean properties for things that can't be populated like that
 * in Hikari.
 */
public class HikariConfigExtended extends HikariConfig {

    public void setUser(String user) {
        setDataSourceProperty("user", user);
    }

    public void setServerName(String serverName) {
        setDataSourceProperty("serverName", serverName);
    }

    public void setPort(String port) {
        setDataSourceProperty("port", port);
    }

    public void setDatabaseName(String databaseName) {
        setDataSourceProperty("databaseName", databaseName);
    }

    @AnySetter
    public void setDataSourceProperty(String key, String value) {
        addDataSourceProperty(key, value);
    }
}
