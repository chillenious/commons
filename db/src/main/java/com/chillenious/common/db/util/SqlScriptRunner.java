package com.chillenious.common.db.util;

import com.chillenious.common.util.Strings;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import org.flywaydb.core.internal.dbsupport.DbSupport;
import org.flywaydb.core.internal.dbsupport.SqlScript;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Runs SQL scripts.
 */
public final class SqlScriptRunner {

    private final DataSource dataSource;

    private final DbSupportFactory dbSupportFactory;

    @Inject
    public SqlScriptRunner(DataSource dataSource,
                           DbSupportFactory dbSupportFactory) {
        this.dataSource = dataSource;
        this.dbSupportFactory = dbSupportFactory;
    }

    /**
     * Reads files from class path and executes them after breaking them up in statements.
     *
     * @param locations file location(s)
     */
    public void executeFromClasspath(String... locations) {
        try (Connection connection = dataSource.getConnection()) {
            DbSupport dbSupport = dbSupportFactory.create(connection);
            for (String location : locations) {
                try (InputStream is = Resources.getResource(location).openStream()) {
                    SqlScript script = new SqlScript(Strings.readString(is), dbSupport);
                    script.execute(dbSupport.getJdbcTemplate());
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Executes a bunch of sql statements.
     *
     * @param sql sql statements to execute
     */
    public void execute(String... sql) {
        try (Connection connection = dataSource.getConnection()) {
            DbSupport dbSupport = dbSupportFactory.create(connection);
            Arrays.stream(sql).forEach(stmt -> {
                try {
                    dbSupport.getJdbcTemplate().executeStatement(stmt);
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
