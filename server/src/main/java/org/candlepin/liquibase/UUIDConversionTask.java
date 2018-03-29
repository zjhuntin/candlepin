/**
 * Copyright (c) 2009 - 2017 Red Hat, Inc.
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

import org.candlepin.liquibase.uuid.ForeignKeyConstraint;
import org.candlepin.liquibase.uuid.HSQLDBUUIDConverter;
import org.candlepin.liquibase.uuid.MySQLUUIDConverter;
import org.candlepin.liquibase.uuid.PostgresqlUUIDConverter;
import org.candlepin.liquibase.uuid.UUIDConverter;

import liquibase.database.Database;
import liquibase.exception.DatabaseException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The PerOrgProductsMigrationValidationTask performs pre-migration validation on the current state of
 * the database before migrating to per-org products.
 */
public class UUIDConversionTask extends LiquibaseCustomTask {
    private UUIDConverter converter;

    public UUIDConversionTask(Database database, CustomTaskLogger logger) {
        super(database, logger);
        try {
            logger.info("Using " + database.getDatabaseProductName());
            String db = database.getDatabaseProductName();
            if ("PostgreSQL".equals(db)) {
                converter = new PostgresqlUUIDConverter(connection, "public", logger);
            }
            else if ("HSQL Database Engine".equals(db)) {
                converter = new HSQLDBUUIDConverter(connection, "public", logger);
            }
            else if ("MySQL".equals(db)) {
                converter = new MySQLUUIDConverter(connection, "candlepin", logger);
            }
            else {
                throw new DatabaseException("Database type \"" + db + "\" not found");
            }
        }
        catch (DatabaseException e) {
            throw new RuntimeException("Could not build converter", e);
        }
    }

    /**
     * Executes this task
     *
     * @throws DatabaseException
     *  if an error occurs while performing a database operation
     *
     * @throws SQLException
     *  if an error occurs while executing an SQL statement
     */
    @Override
    public void execute() throws DatabaseException, SQLException {
        Map<String, String> tableAndColumns = new HashMap<>();
        tableAndColumns.put("cp2_product_content", "id");

        for (Map.Entry<String, String> tableAndColumn : tableAndColumns.entrySet()) {
            String table = tableAndColumn.getKey();
            String column = tableAndColumn .getValue();
            converter.alterColumn(table, column);
        }

        String table = "cp_pool";
        List<ForeignKeyConstraint> tableConstraints = converter.gatherConstraints(table);
        converter.disableConstraints(tableConstraints, table);
        converter.alterColumn(table, "cdn_id");
        converter.alterColumn("cp_cdn", "id");
        converter.enableConstraints(tableConstraints, table);
    }



}
