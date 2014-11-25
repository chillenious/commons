package com.chillenious.common.util;

import com.chillenious.common.DynamicSetting;
import com.chillenious.common.Setting;
import com.chillenious.common.SettingsListener;

import java.math.BigDecimal;

/**
 * Setting that holds an big decimal value.
 */
public class BigDecimalSetting extends Setting<BigDecimal> {

    private static final long serialVersionUID = 1L;

    public BigDecimalSetting(DynamicSetting setting) {
        super(setting, BigDecimal.class);
    }

    public BigDecimalSetting(DynamicSetting setting, SettingsListener listener) {
        super(setting, BigDecimal.class, listener);
    }
}
