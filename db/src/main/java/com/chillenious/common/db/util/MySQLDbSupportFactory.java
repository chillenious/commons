package com.chillenious.common.db.util;

import org.flywaydb.core.internal.dbsupport.DbSupport;
import org.flywaydb.core.internal.dbsupport.mysql.MySQLDbSupport;

import java.sql.Connection;

/**
 * Implementation for {@link org.flywaydb.core.internal.dbsupport.mysql.MySQLDbSupport}
 */
public final class MySQLDbSupportFactory implements DbSupportFactory {

    @Override
    public DbSupport create(Connection connection) {
        return new MySQLDbSupport(connection);
    }
}
