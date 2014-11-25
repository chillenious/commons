package com.chillenious.common.util;

import com.chillenious.common.DynamicSetting;
import com.chillenious.common.Setting;
import com.chillenious.common.SettingsListener;

/**
 * Setting that holds an string value.
 */
public class StringSetting extends Setting<String> {

    private static final long serialVersionUID = 1L;

    public StringSetting(DynamicSetting setting) {
        super(setting, String.class);
    }

    public StringSetting(DynamicSetting setting, SettingsListener listener) {
        super(setting, String.class, listener);
    }
}
