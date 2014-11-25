package com.chillenious.common.db.sync;

/**
 * Acknowledgement sent by listeners is gathered by
 * {@link DataRefreshListenerMarker} for synchronizing all listeners.
 */
final class MarkerAck extends DataRefreshEvent {

    private final DataRefreshListener sender;

    public MarkerAck(DataRefreshListener sender, MarkerReq req) {
        super(req.getId());
        this.sender = sender;
    }

    @Override
    public String toString() {
        return String.format("Marker Acknowledgement {id = %s, sender = %s}",
                getId(), sender);
    }
}
