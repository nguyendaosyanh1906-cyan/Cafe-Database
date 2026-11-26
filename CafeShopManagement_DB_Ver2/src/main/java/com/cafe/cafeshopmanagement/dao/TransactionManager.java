package com.cafe.cafeshopmanagement.dao;

import com.cafe.cafeshopmanagement.DBConnection;
import java.sql.Connection;

/** Owns one connection. DAOs constructed with it must not commit or close it. */
public final class TransactionManager {
    private TransactionManager() {}
    @FunctionalInterface
    public interface Work<T> { T execute(Connection connection) throws Exception; }
    public static <T> T inTransaction(Work<T> work) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (Exception | Error failure) {
                try { connection.rollback(); }
                catch (Exception rollbackFailure) { failure.addSuppressed(rollbackFailure); }
                if (failure instanceof Error) throw (Error) failure;
                if (failure instanceof RuntimeException) throw (RuntimeException) failure;
                throw new DataAccessException("Transaction failed", failure);
            }
        } catch (DataAccessException e) { throw e; }
        catch (Exception e) { throw new DataAccessException("Cannot complete transaction", e); }
    }
}
