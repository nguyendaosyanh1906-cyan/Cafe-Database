package com.cafe.cafeshopmanagement.dao.impl;

import com.cafe.cafeshopmanagement.DBConnection;
import com.cafe.cafeshopmanagement.dao.DataAccessException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Connection ownership shared by all JDBC DAO implementations. */
public abstract class JdbcDao {
    private final Connection sharedConnection;
    protected JdbcDao() { this.sharedConnection = null; }
    protected JdbcDao(Connection connection) {
        this.sharedConnection = Objects.requireNonNull(connection, "connection");
        try {
            if (connection.isClosed() || connection.getAutoCommit()) {
                throw new IllegalArgumentException("Shared connection must be open with autoCommit=false");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Cannot inspect shared connection", e);
        }
    }
    protected ConnectionScope openConnection() {
        return new ConnectionScope(sharedConnection == null ? DBConnection.getConnection() : sharedConnection,
                sharedConnection == null);
    }
    protected static final class ConnectionScope implements AutoCloseable {
        private final Connection connection;
        private final boolean owned;
        private ConnectionScope(Connection connection, boolean owned) {
            this.connection = connection;
            this.owned = owned;
        }
        public Connection connection() { return connection; }
        public boolean owned() { return owned; }
        @Override public void close() throws SQLException { if (owned) connection.close(); }
    }
}
