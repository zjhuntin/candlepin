/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.liquibase;

import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

/**
 * Utility class to build PreparedStatements, etc.
 */
public class LiquibaseTaskUtil {
    private JdbcConnection connection;
    private String databaseProductName;
    private int nullType;

    public LiquibaseTaskUtil(JdbcConnection connection) throws DatabaseException {
        this.connection = connection;
        this.databaseProductName = connection.getDatabaseProductName();
        // Check which type we need to use for nulls (courtesy of Oracle's moody adapter)
        // See the comments on this SO question for details:
        // http://stackoverflow.com/questions/11793483/setobject-method-of-preparedstatement
        this.nullType =
            databaseProductName.matches(".*(?i:oracle).*") ? Types.VARCHAR : Types.NULL;
    }

    public String getDatabaseProductName() {
        return databaseProductName;
    }

    /**
     * Sets the parameter at the specified index to the given value. This method attempts to perform
     * safe assignment of parameters across all supported platforms.
     *
     * @param statement
     *  the statement on which to set a parameter
     *
     * @param index
     *  the index of the parameter to set
     *
     * @param value
     *  the value to set
     *
     * @throws NullPointerException
     *  if statement is null
     *
     * @throws SQLException
     *  if statement fails
     *
     * @return
     *  the PreparedStatement being updated
     */
    public PreparedStatement setParameter(PreparedStatement statement, int index, Object value)
        throws SQLException {
        if (value != null) {
            statement.setObject(index, value);
        }
        else {
            statement.setNull(index, nullType);
        }

        return statement;
    }

    /**
     * Fills the parameters of a prepared statement with the given arguments
     *
     * @param statement
     *  the statement to fill
     *
     * @param argv
     *  the collection of arguments with which to fill the statement's parameters
     *
     * @throws NullPointerException
     *  if statement is null
     *
     * @throws SQLException
     *  if SQL is incorrect
     *
     * @return
     *  the provided PreparedStatement
     */
    public PreparedStatement fillStatementParameters(PreparedStatement statement, Object... argv)
        throws SQLException {
        statement.clearParameters();

        if (argv != null) {
            for (int i = 0; i < argv.length; ++i) {
                this.setParameter(statement, i + 1, argv[i]);
            }
        }
        return statement;
    }

    /**
     * Prepares a statement and populates it with the specified arguments, pulling from cache when
     * possible.
     *
     * @param sql
     *  The SQL to execute. The given SQL may be parameterized.
     *
     * @param argv
     *  The arguments to use when executing the given query.
     *
     * @throws SQLException
     *  if SQL is incorrect
     *
     * @throws DatabaseException
     *  if a database problem occurs
     *
     * @return
     *  a PreparedStatement instance representing the specified SQL statement
     */
    public PreparedStatement prepareStatement(String sql, Object... argv)
        throws SQLException, DatabaseException {
        PreparedStatement statement = this.connection.prepareStatement(sql);
        return this.fillStatementParameters(statement, argv);
    }

    /**
     * Executes the given SQL query.
     *
     * @param sql
     *  The SQL to execute. The given SQL may be parameterized.
     *
     * @param argv
     *  The arguments to use when executing the given query.
     *
     * @throws SQLException
     *  if SQL is incorrect
     *
     * @throws DatabaseException
     *  if a database problem occurs
     *
     * @return
     *  A ResultSet instance representing the result of the query.
     */
    public ResultSet executeQuery(String sql, Object... argv) throws SQLException, DatabaseException {
        PreparedStatement statement = this.prepareStatement(sql, argv);
        return statement.executeQuery();
    }

    /**
     * Executes the given SQL update/insert.
     *
     * @param sql
     *  The SQL to execute. The given SQL may be parameterized.
     *
     * @param argv
     *  The arguments to use when executing the given update.
     *
     * @throws SQLException
     *  if SQL is incorrect
     *
     * @throws DatabaseException
     *  if a database problem occurs
     *
     * @return
     *  The number of rows affected by the update.
     */
    public int executeUpdate(String sql, Object... argv) throws SQLException, DatabaseException {
        PreparedStatement statement = this.prepareStatement(sql, argv);
        return statement.executeUpdate();
    }

    /**
     * Generates a 32-character UUID to use with object creation/migration.
     * <p></p>
     * The UUID is generated by creating a standard type 4 UUID and removing the hyphens. The UUID may be
     * standardized by reinserting the hyphens later, if necessary.
     *
     * @return
     *  a 32-character UUID
     *
     * @deprecated Use generateRandomUUID instead
     */
    @Deprecated
    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * @return a type 4 UUID complaint with RFC 4122
     */
    public static UUID generateRandomUUID() {
        return UUID.randomUUID();
    }
}
