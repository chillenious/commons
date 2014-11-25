package com.chillenious.common.util;

import com.chillenious.common.DynamicSetting;
import com.chillenious.common.Setting;
import com.chillenious.common.SettingsListener;

/**
 * Setting that holds an integer value.
 */
public class DoubleSetting extends Setting<Double> {

    private static final long serialVersionUID = 1L;

    public DoubleSetting(DynamicSetting setting) {
        super(setting, Double.class);
    }

    public DoubleSetting(DynamicSetting setting, SettingsListener listener) {
        super(setting, Double.class, listener);
    }
}
