package com.chillenious.common.util;

import com.chillenious.common.DynamicSetting;
import com.chillenious.common.Setting;
import com.chillenious.common.SettingsListener;

import javax.annotation.Nullable;

/**
 * A setting that holds a {@link com.chillenious.common.util.Duration}. The setting itself should
 * be a string in the form that Duration can parse, for instance
 * {@code}2 seconds{@code} or {@code}1 hour{@code}
 */
public class DurationSetting extends Setting<Duration> {

    private static final long serialVersionUID = 1L;

    public DurationSetting(DynamicSetting setting, SettingsListener listener) {
        super(setting, Duration.class, listener);
    }

    public DurationSetting(DynamicSetting setting) {
        super(setting, Duration.class);
    }

    @Nullable
    @Override
    public Duration get() {
        String stringValue = getCurrentValueAsString();
        return (!Strings.isEmpty(stringValue)) ? Duration.valueOf(stringValue) : null;
    }
}
