package com.chillenious.common.util;

import com.chillenious.common.DynamicSetting;
import com.chillenious.common.Setting;
import com.chillenious.common.SettingsListener;

/**
 * Setting that holds a long value.
 */
public class LongSetting extends Setting<Long> {

    private static final long serialVersionUID = 1L;

    public LongSetting(DynamicSetting setting) {
        super(setting, Long.class);
    }

    public LongSetting(DynamicSetting setting, SettingsListener listener) {
        super(setting, Long.class, listener);
    }
}
