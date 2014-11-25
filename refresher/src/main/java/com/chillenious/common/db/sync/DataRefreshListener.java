package com.chillenious.common.db.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message listener base class.
 *
 * @param <O> persistent object type
 */
public abstract class DataRefreshListener<O extends PersistentObject>
        extends BaseDataRefreshListener<O> {

    private static final Logger log = LoggerFactory.getLogger(DataRefreshListener.class);

    /**
     * Construct.
     */
    protected DataRefreshListener() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void onDataRefresh(DataRefreshEvent<O> evt) {
        if (topic == null) {
            throw new IllegalStateException(
                    "topic is not yet set; this listener is not properly initialized/ used");
        }
        if (evt instanceof MarkerReq) {
            log.info(String.format("received req; sending ack (listener = %s)", this));
            topic.publish(new MarkerAck(this, (MarkerReq) evt));
        } else {
            onEvent(evt);
        }
    }

    /**
     * Invoked when a new data refreshed event is received.
     *
     * @param evt received event
     */
    protected abstract void onEvent(DataRefreshEvent<O> evt);

    protected O getObject(DataRefreshEvent<O> evt) {
        if (evt instanceof DataCreatedEvent) {
            return ((DataCreatedEvent<O>) evt).getObject();
        } else if (evt instanceof DataChangedEvent) {
            return ((DataChangedEvent<O>) evt).getObject();
        }
        return null;
    }
}
