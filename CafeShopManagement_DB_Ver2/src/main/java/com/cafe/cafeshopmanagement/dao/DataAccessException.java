package com.cafe.cafeshopmanagement.dao;

/** Persistence failure, distinct from a valid empty result or missing row. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message) { super(message); }
    public DataAccessException(String message, Throwable cause) { super(message, cause); }
}
