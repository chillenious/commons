package com.chillenious.common.db.sync;

import java.util.Random;

/**
 * Event that is sent out so that all registered listeners can send
 * and ack, which would be done after they are done with whatever is
 * currently in their queue.
 */
final class MarkerReq extends DataRefreshEvent {

    private static final Random random = new Random();

    public MarkerReq() {
        super(random.nextLong());
    }

    @Override
    public String toString() {
        return String.format("Marker Request {id=%s}", getId());
    }
}
