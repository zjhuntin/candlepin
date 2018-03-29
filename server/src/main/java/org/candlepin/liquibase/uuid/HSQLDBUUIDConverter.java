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
 * HSQLDB specific UUID conversion implementation.  Note that this converter will only work on an empty
 * database.  That should be fine since we create an empty database for test classes and truncate it after
 * every individual test.
 */
public class HSQLDBUUIDConverter implements UUIDConverter {
    // JDBC doesn't support bind parameters for table and column names
    public static final String ALTER_TEMPLATE =
        "ALTER TABLE %s ALTER COLUMN %s BINARY(16)";

    private String schemaName;
    private LiquibaseTaskUtil taskUtil;
    private CustomTaskLogger logger;
    private JdbcConnection connection;

    public HSQLDBUUIDConverter(JdbcConnection connection, String schemaName, CustomTaskLogger logger)
        throws DatabaseException {
        this.connection = connection;
        this.schemaName = schemaName;
        this.logger = logger;
        this.taskUtil = new LiquibaseTaskUtil(connection);
    }

    @Override
    public void disableConstraints(List<ForeignKeyConstraint> constraints, String table)
        throws DatabaseException, SQLException {
        String sqlTemplate = "ALTER TABLE %s DROP CONSTRAINT %s";

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
        String sql = "SELECT fk_name, fkcolumn_name, pktable_name, pkcolumn_name FROM" +
            " information_schema.system_crossreference WHERE fktable_name = ?";
        ResultSet rs = taskUtil.executeQuery(sql, tableName.toUpperCase());
        List<ForeignKeyConstraint> constraints = new ArrayList<>();
        while (rs.next()) {
            String definition = String.format(
                "FOREIGN KEY (%s) REFERENCES %s(%s)",
                rs.getString("fkcolumn_name"),
                rs.getString("pktable_name"),
                rs.getString("pkcolumn_name")
            );
            constraints.add(new ForeignKeyConstraint(rs.getString("fk_name"), definition));
        }
        return constraints;
    }

    @Override
    public void alterColumn(String table, String column) throws DatabaseException, SQLException {
        connection.createStatement().execute(String.format(ALTER_TEMPLATE, table, column));
    }
}
