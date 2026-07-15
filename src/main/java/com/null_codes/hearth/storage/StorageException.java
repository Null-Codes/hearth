package com.null_codes.hearth.storage;

/** Reports persistence failures through asynchronous completion stages. */
public final class StorageException extends RuntimeException {
  public StorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
