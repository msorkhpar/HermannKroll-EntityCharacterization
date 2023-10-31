package de.bs.tu.ifis.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.bs.tu.ifis.model.Entity;
import de.bs.tu.ifis.model.Literal;
import de.bs.tu.ifis.model.graph.Node;
import de.bs.tu.ifis.model.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

/**
 * offers a connection to a virtuoso server
 *
 * @author Hermann Kroll
 */
public class VirtuosoDB {
    private Logger logger = LogManager.getLogger();

    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private HikariDataSource dataSource;

    public VirtuosoDB(final String host, final int port, final String username, final String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        init();
    }

    public VirtuosoDB(final String host, final int port) {
        this.host = host;
        this.port = port;
        this.username = "dba";
        this.password = "dba";
        init();
    }

    private void init() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:virtuoso://" + this.host + ":" + this.port + "/charset=UTF-8");
        config.setUsername(this.username);
        config.setPassword(this.password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setConnectionTimeout(30000);
        this.dataSource = new HikariDataSource(config);
    }

    /**
     * extracts a long from the result set with error catching
     *
     * @param rs ResultSet
     * @return the count result or -1 if an error occured
     */
    private long extractLongFromCountResultSet(final ResultSet rs) {
        // Exctract amount
        try {
            if (rs.next()) {
                long amount = rs.getLong(1);
                return amount;
            } else {
                logger.error("empty result set while couting");
            }
        } catch (SQLException ex) {
            logger.error("SQL Exception while extracting long from counting because: " + ex);
            ex.printStackTrace();
        }
        return -1;
    }

    /**
     * Returns the amount of predicates with a specific type
     *
     * @param predicate predicate type
     * @param graph     graph to query on
     * @return the amount or -1 if an error occured
     */
    public long countPredicateTypeInDatabase(final String predicate, final String graph) {
        return this.executeQuery("SPARQL SELECT COUNT(*) " +
                "FROM " + graph + " " +
                "WHERE { ?s <" + predicate + "> ?o }");
    }


    /**
     * counts the amount of incoming predicates of a edge
     *
     * @param entity the entity to count for
     * @param graph  graph to query on
     * @return the amount of incoming predicates or -1 of an error occured
     */
    public long countEntityIncomingPredicates(final String entity, final String graph) {
        return this.executeQuery("SPARQL SELECT COUNT(*) " +
                "FROM " + graph + " " +
                "WHERE {  ?s ?p  <" + entity + ">  }");
    }


    /**
     * counts the amount of incoming predicates of a edge
     *
     * @param literal the literal to count for
     * @param graph   graph to query on
     * @return the amount of incoming predicates or -1 of an error occured
     */
    public long countLiteralIncomingPredicates(final String literal, final String graph) {
        return this.executeQuery("SPARQL SELECT COUNT(*) " +
                "FROM " + graph + " " +
                "WHERE {  ?s ?p " + literal + " }");
    }


    /**
     * counts the amount of outgoing predicates of a edge
     *
     * @param entity the entity to count for
     * @param graph  graph to query on
     * @return the amount of outgoing predicates or -1 of an error occured
     */
    public long countEntityOutgoingPredicates(final String entity, final String graph) {
        String query = "SPARQL SELECT COUNT(*) " +
                "FROM " + graph + " " +
                "WHERE {  <" + entity + "> ?p ?o }";
        logger.info(query);
        return this.executeQuery(query);
    }


    /**
     * executes a sql query on the connection
     *
     * @param sql query
     * @return resultset or null if an error occured
     */
    public long executeQuery(final String sql) {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet set = stmt.executeQuery(sql);
            if (set == null) {
                return -1;
            }
            return extractLongFromCountResultSet(set);
        } catch (SQLException e) {
            logger.error("Query: " + sql + " could not be executed: " + e.getMessage());
            return -1;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }

}
