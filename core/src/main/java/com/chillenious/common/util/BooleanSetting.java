package com.chillenious.common.util;

import com.chillenious.common.DynamicSetting;
import com.chillenious.common.Setting;
import com.chillenious.common.SettingsListener;

/**
 * Setting that holds a boolean value.
 */
public class BooleanSetting extends Setting<Boolean> {

    private static final long serialVersionUID = 1L;

    public BooleanSetting(DynamicSetting setting) {
        super(setting, Boolean.class);
    }

    public BooleanSetting(DynamicSetting setting, SettingsListener listener) {
        super(setting, Boolean.class, listener);
    }
}
