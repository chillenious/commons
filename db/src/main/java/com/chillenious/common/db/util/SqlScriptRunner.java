package com.chillenious.common.db.util;

import com.chillenious.common.util.Duration;
import com.chillenious.common.util.Strings;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Runs SQL scripts.
 */
@Singleton
public final class SqlScriptRunner {

    private static final Pattern[] stripPatterns = new Pattern[]{
            Pattern.compile("(?m)--(.*)$"),
            Pattern.compile("(?m)/\\\\*.*?\\\\*/"),
            Pattern.compile("comment '.*'")
    };

    private static final Logger log = LoggerFactory.getLogger(SqlScriptRunner.class);

    private final DataSource dataSource;

    @Inject
    public SqlScriptRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Parses a file from the class path and returns statements with comments stripped.
     * NOTE this doesn't currently support delimiter switching and possibly other
     * advanced syntax. Use for straightforward cases only (tests for instance).
     *
     * @param locations file location(s)
     * @return list of statements
     */
    public static String[] readSqlFromClassPath(String... locations) {
        List<String> all = new ArrayList<>();
        for (String location : locations) {
            try (InputStream is = Resources.getResource(location).openStream()) {
                String fileContents = Strings.readString(is);
                List<String> sql = new ArrayList<>();
                StringTokenizer t = new StringTokenizer(fileContents, ";");
                while (t.hasMoreTokens()) {
                    String block = t.nextToken();
                    for (Pattern p : stripPatterns) {
                        block = p.matcher(block).replaceAll("");
                    }
                    block = block.trim();
                    if (!Strings.isEmpty(block)) {
                        sql.add(block);
                    }
                }
                all.addAll(sql);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return all.toArray(new String[all.size()]);
    }

    /**
     * Reads file from class path, tries to parse statements from it and executes those.
     *
     * @param locations file location(s)
     */
    public void executeFromClasspath(String... locations) {
        execute(readSqlFromClassPath(locations));
    }

    public void execute(String... sql) {
        long start = System.currentTimeMillis();
        // use logger for the extending class
        Connection conn = null;
        boolean oldAutoCommit = true;
        int i = 0;
        try {
            conn = dataSource.getConnection();
            oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            for (String cmd : sql) {
                if (!Strings.isEmpty(cmd)) {
                    i++;
                    if (log.isDebugEnabled()) {
                        log.debug(cmd);
                    }
                    try (Statement stmt = conn.createStatement()) {
                        boolean hasResults = stmt.execute(cmd);
                        if (hasResults) {
                            printResults(stmt);
                        }
                    } catch (SQLException e) {
                        String errorMsg = "error executing SQL statement: '"
                                + cmd + "', exception: " + e.getMessage();
                        log.error(errorMsg, e);
                        conn.rollback();
                        throw new IllegalStateException(errorMsg, e);
                    }
                }
            }
            conn.commit();
            log.debug(String.format("executed %d statements in %s",
                    i, Duration.milliseconds(System.currentTimeMillis() - start)));
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.warn("error trying to roll back: " + e.getMessage(), e);
                }
            }
            throw new IllegalStateException(
                    "problem with setting up database connection for updates: "
                            + e.getMessage(), e
            );
        } finally {
            if (conn != null) {
                try {
                    if (!conn.isClosed()) {
                        conn.setAutoCommit(oldAutoCommit);
                        conn.close();
                    }
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    private void printResults(Statement statement) {
        try {
            StringBuilder b = new StringBuilder();
            ResultSet rs = statement.getResultSet();
            if (rs != null) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                for (int i = 0; i < cols; i++) {
                    String name = md.getColumnLabel(i + 1);
                    b.append(name).append("\t");
                }
                b.append("\n");
                while (rs.next()) {
                    for (int i = 0; i < cols; i++) {
                        String value = rs.getString(i + 1);
                        b.append(value).append("\t");
                    }
                    b.append("\n");
                }
            }
            log.info(b.toString());
        } catch (SQLException e) {
            log.error("error printing results: " + e.getMessage(), e);
        }
    }
}
