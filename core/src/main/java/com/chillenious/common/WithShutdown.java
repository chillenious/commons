package com.chillenious.common;

/**
 * Classes of which objects are managed by Guice can implement this interface
 * to communicate that they have a method for shutting themselves down cleanly,
 * e.g. after unit tests run or when the VM is shut down.
 */
public interface WithShutdown {

    /**
     * Shut this baby down.
     */
    void shutdown();
}
