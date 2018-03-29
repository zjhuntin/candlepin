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
package org.candlepin.liquibase.uuid;

import org.candlepin.liquibase.CustomTaskLogger;
import org.candlepin.liquibase.LiquibaseTaskUtil;

import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL/MariaDB specific UUID conversion implementation
 */
public class MySQLUUIDConverter implements UUIDConverter {
    private String schemaName;
    private LiquibaseTaskUtil taskUtil;
    private CustomTaskLogger logger;
    private JdbcConnection connection;

    public MySQLUUIDConverter(JdbcConnection connection, String schemaName, CustomTaskLogger logger)
        throws DatabaseException {
        this.connection = connection;
        this.schemaName = schemaName;
        this.logger = logger;
        this.taskUtil = new LiquibaseTaskUtil(connection);
    }

    @Override
    public void disableConstraints(List<ForeignKeyConstraint> constraints, String table)
        throws DatabaseException, SQLException {
        String sqlTemplate = "ALTER TABLE %s DROP FOREIGN KEY %s";

        for (ForeignKeyConstraint fk : constraints) {
            connection.createStatement().execute(String.format(sqlTemplate, table, fk.getName()));
        }
    }

    @Override
    public void enableConstraints(List<ForeignKeyConstraint> constraints, String table)
        throws DatabaseException, SQLException {
        String sqlTemplate = "ALTER TABLE %s ADD CONSTRAINT %s %s";

        for (ForeignKeyConstraint fk : constraints) {
            connection.createStatement().execute(
                String.format(sqlTemplate, table, fk.getName(), fk.getDefinition())
            );
        }
    }

    @Override
    public List<ForeignKeyConstraint> gatherConstraints(String tableName)
        throws DatabaseException, SQLException {
        String sql = "SELECT constraint_name, column_name, referenced_table_name, referenced_column_name " +
            "FROM information_schema.key_column_usage WHERE table_schema = ? AND table_name = ? AND " +
            "referenced_table_name IS NOT NULL";

        ResultSet rs = taskUtil.executeQuery(sql, schemaName, tableName);
        List<ForeignKeyConstraint> constraints = new ArrayList<>();
        while (rs.next()) {
            String definition = String.format(
                "FOREIGN KEY (%s) REFERENCES %s(%s)",
                rs.getString("column_name"),
                rs.getString("referenced_table_name"),
                rs.getString("referenced_column_name")
            );
            constraints.add(new ForeignKeyConstraint(rs.getString("constraint_name"), definition));
        }
        logger.info("Constraints are " + constraints);
        return constraints;
    }

    @Override
    public void alterColumn(String table, String column) throws DatabaseException, SQLException {
        String change = "ALTER TABLE %s MODIFY COLUMN %s VARBINARY(36)";
        String convert = "UPDATE %s SET %s=UNHEX(%s)";
        String resize = "ALTER TABLE %s MODIFY COLUMN %s BINARY(16)";

        // MySQL "conveniently" does not retain the full column definition when altering a column so we
        // need to preserve it ourselves.
        String nullStatus = "SELECT IS_NULLABLE, COLUMN_DEFAULT FROM information_schema.columns " +
            "WHERE TABLE_NAME=? AND COLUMN_NAME=?";
        ResultSet rs = taskUtil.executeQuery(nullStatus, table, column);

        boolean hasNotNullConstraint = false;
        String defaultValue = "";

        while (rs.next()) {
            hasNotNullConstraint = "NO".equals(rs.getString("IS_NULLABLE"));

            String columnDefault = rs.getString("COLUMN_DEFAULT");
            logger.info("Column default is " + columnDefault + " for " + column);
            defaultValue = (columnDefault == null || "NULL".equals(columnDefault)) ? "" : columnDefault;
        }

        if (hasNotNullConstraint) {
            change += " NOT NULL";
            resize += " NOT NULL";
        }

        if (!defaultValue.isEmpty()) {
            change += " DEFAULT '" + defaultValue + "'";
            resize += " DEFAULT UNHEX('" + defaultValue + "')";
        }

        connection.createStatement().execute(String.format(change, table, column));
        connection.createStatement().execute(String.format(convert, table, column, column));
        connection.createStatement().execute(String.format(resize, table, column));
    }
}
