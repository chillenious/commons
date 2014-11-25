package com.chillenious.common.util.guice;

/**
 * Exception that says that scanning a package for classes was unsuccessful.
 */
public class PackageScanFailedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PackageScanFailedException() {
		super();
	}

	public PackageScanFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public PackageScanFailedException(String message) {
		super(message);
	}

	public PackageScanFailedException(Throwable cause) {
		super(cause);
	}
}
