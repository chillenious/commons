package com.chillenious.common.db;

import com.zaxxer.hikari.HikariConfig;

/**
 * Fake JavaBean properties for things that can't be populated like that
 * in Hikari.
 */
public class HikariConfigExtended extends HikariConfig {

    public void setUser(String user) {
        addDataSourceProperty("user", user);
    }

    public void setServerName(String serverName) {
        addDataSourceProperty("serverName", serverName);
    }

    public void setPort(String port) {
        addDataSourceProperty("port", port);
    }

    public void setDatabaseName(String databaseName) {
        addDataSourceProperty("databaseName", databaseName);
    }
}
