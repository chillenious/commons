package com.chillenious.common;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * This construct allows you to listen for settings that are added/ changed.
 */
public interface SettingsListener extends Serializable {

    /**
     * Whether to react of a change for the provided key.
     *
     * @param key key (setting name)
     * @return whether to react (in which case {@link #onChanged(String, Object, Object)} would
     * be called).
     */
    boolean accept(String key);

    /**
     * Called when a setting a added, changed or removed.
     *
     * @param key      setting key
     * @param oldValue previous value
     * @param newValue current value
     */
    void onChanged(String key, @Nullable Object oldValue, @Nullable Object newValue);
}
