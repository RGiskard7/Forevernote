package com.example.forevernote.exceptions;

public class DataAccessException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

}
