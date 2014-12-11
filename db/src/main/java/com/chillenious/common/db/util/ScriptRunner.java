/*
 *    Copyright 2009-2012 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.chillenious.common.db.util;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Clinton Begin
 * @author Eelco Hillenius (misc tweaks)
 */
public class ScriptRunner {

    private static final Logger log = LoggerFactory.getLogger(ScriptRunner.class);

    private static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

    private static final String DEFAULT_DELIMITER = ";";

    private boolean stopOnError;
    private boolean autoCommit;
    private boolean sendFullScript;
    private boolean removeCRs;
    private boolean escapeProcessing = true;

    private String delimiter = DEFAULT_DELIMITER;
    private boolean fullLineDelimiter = false;

    private final DataSource dataSource;

    @Inject
    public ScriptRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public void setSendFullScript(boolean sendFullScript) {
        this.sendFullScript = sendFullScript;
    }

    public void setRemoveCRs(boolean removeCRs) {
        this.removeCRs = removeCRs;
    }

    /**
     * @since 3.1.1
     */
    public void setEscapeProcessing(boolean escapeProcessing) {
        this.escapeProcessing = escapeProcessing;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public void setFullLineDelimiter(boolean fullLineDelimiter) {
        this.fullLineDelimiter = fullLineDelimiter;
    }

    /**
     * Reads file from class path, tries to parse statements from it and executes those.
     *
     * @param locations file location(s)
     */
    public void runScriptsFromClasspath(String... locations) {
        for (String location : locations) {
            try (InputStream is = Resources.getResource(location).openStream()) {
                runScript(new InputStreamReader(is));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public void runScript(Reader reader) {
        Connection connection = getConnection();
        setAutoCommit(connection);

        try {
            if (sendFullScript) {
                executeFullScript(connection, reader);
            } else {
                executeLineByLine(connection, reader);
            }
        } finally {
            rollbackConnection(connection);
        }
    }

    private void executeFullScript(Connection connection, Reader reader) {
        StringBuilder script = new StringBuilder();
        try {
            BufferedReader lineReader = new BufferedReader(reader);
            String line;
            while ((line = lineReader.readLine()) != null) {
                script.append(line);
                script.append(LINE_SEPARATOR);
            }
            executeStatement(connection, script.toString());
            commitConnection(connection);
        } catch (Exception e) {
            String message = "Error executing: " + script + ".  Cause: " + e;
            log.error(message);
            throw new IllegalStateException(message, e);
        }
    }

    private void executeLineByLine(Connection connection, Reader reader) {
        StringBuilder command = new StringBuilder();
        try {
            BufferedReader lineReader = new BufferedReader(reader);
            String line;
            while ((line = lineReader.readLine()) != null) {
                command = handleLine(connection, command, line);
            }
            commitConnection(connection);
            checkForMissingLineTerminator(command);
        } catch (Exception e) {
            String message = "Error executing: " + command + ".  Cause: " + e;
            log.error(message);
            throw new IllegalStateException(message, e);
        }
    }

    public void closeConnection(Connection connection) {
        try {
            connection.close();
        } catch (Exception e) {
            // ignore
        }
    }

    private void setAutoCommit(Connection connection) {
        try {
            if (autoCommit != connection.getAutoCommit()) {
                connection.setAutoCommit(autoCommit);
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Could not set AutoCommit to " + autoCommit + ". Cause: " + t, t);
        }
    }

    private void commitConnection(Connection connection) {
        try {
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Could not commit transaction. Cause: " + t, t);
        }
    }

    private void rollbackConnection(Connection connection) {
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    private void checkForMissingLineTerminator(StringBuilder command) {
        if (command != null && command.toString().trim().length() > 0) {
            throw new IllegalStateException("Line missing end-of-line terminator (" + delimiter + ") => " + command);
        }
    }

    private StringBuilder handleLine(Connection connection,
                                     StringBuilder command,
                                     String line) throws SQLException, UnsupportedEncodingException {
        String trimmedLine = line.trim();
        if (lineIsComment(trimmedLine)) {
            log.debug(trimmedLine);
        } else if (commandReadyToExecute(trimmedLine)) {
            command.append(line.substring(0, line.lastIndexOf(delimiter)));
            command.append(LINE_SEPARATOR);
            log.debug(command.toString());
            executeStatement(connection, command.toString());
            command.setLength(0);
        } else if (trimmedLine.length() > 0) {
            command.append(line);
            command.append(LINE_SEPARATOR);
        }
        return command;
    }

    private boolean lineIsComment(String trimmedLine) {
        return trimmedLine.startsWith("//") || trimmedLine.startsWith("--");
    }

    private boolean commandReadyToExecute(String trimmedLine) {
        // issue #561 remove anything after the delimiter
        return !fullLineDelimiter && trimmedLine.contains(delimiter) || fullLineDelimiter && trimmedLine.equals(delimiter);
    }

    private void executeStatement(Connection connection, String command) throws SQLException, UnsupportedEncodingException {
        boolean hasResults = false;
        Statement statement = connection.createStatement();
        statement.setEscapeProcessing(escapeProcessing);
        String sql = command;
        if (removeCRs)
            sql = sql.replaceAll("\r\n", "\n");
        if (stopOnError) {
            hasResults = statement.execute(sql);
        } else {
            try {
                hasResults = statement.execute(sql);
            } catch (SQLException e) {
                String message = "Error executing: " + command + ".  Cause: " + e;
                log.error(message);
            }
        }
        printResults(statement, hasResults);
        try {
            statement.close();
        } catch (Exception e) {
            // Ignore to workaround a bug in some connection pools
        }
    }

    private void printResults(Statement statement, boolean hasResults) {
        try {
            if (hasResults) {
                ResultSet rs = statement.getResultSet();
                StringBuilder b = new StringBuilder();
                if (rs != null) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();
                    for (int i = 0; i < cols; i++) {
                        String name = md.getColumnLabel(i + 1);
                        b.append(name).append('\t');
                    }
                    b.append('\n');
                    while (rs.next()) {
                        for (int i = 0; i < cols; i++) {
                            String value = rs.getString(i + 1);
                            b.append(value).append('\t');
                        }
                        b.append('\n');
                    }
                }
                log.debug(b.toString());
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

}
