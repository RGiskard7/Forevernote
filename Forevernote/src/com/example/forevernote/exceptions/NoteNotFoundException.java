package com.example.forevernote.exceptions;

public class NoteNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

	public NoteNotFoundException(String message) {
        super(message);
    }

    public NoteNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

