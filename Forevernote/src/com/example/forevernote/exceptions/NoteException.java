package com.example.forevernote.exceptions;

public class NoteException extends Exception {
    private static final long serialVersionUID = 1L;

	public NoteException(String message) {
        super(message);
    }

    public NoteException(String message, Throwable cause) {
        super(message, cause);
    }
}
