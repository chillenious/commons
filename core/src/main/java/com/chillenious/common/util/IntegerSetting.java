package com.chillenious.common.util;

import com.chillenious.common.DynamicSetting;
import com.chillenious.common.Setting;
import com.chillenious.common.SettingsListener;

/**
 * Setting that holds an integer value.
 */
public class IntegerSetting extends Setting<Integer> {

    private static final long serialVersionUID = 1L;

    public IntegerSetting(DynamicSetting setting) {
        super(setting, Integer.class);
    }

    public IntegerSetting(DynamicSetting setting, SettingsListener listener) {
        super(setting, Integer.class, listener);
    }
}
