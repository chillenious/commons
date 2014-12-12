package com.chillenious.common.db.util;

import com.google.inject.ImplementedBy;
import org.flywaydb.core.internal.dbsupport.DbSupport;

import java.sql.Connection;

/**
 * Supplies db support instances given a connection.
 */
@ImplementedBy(MySQLDbSupportFactory.class)
@FunctionalInterface
public interface DbSupportFactory {

    /**
     * Create db support instance with the provided connection. Clients
     * are responsible for managing (providing, closing) this connection.
     *
     * @param connection sql connection
     * @return db support instance
     */
    DbSupport create(Connection connection);
}
