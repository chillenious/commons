package com.chillenious.common;

import com.google.common.base.Preconditions;
import com.chillenious.common.util.Objects;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Wrapper for {@link DynamicSetting} to make accessing values a little nicer.
 * <p/>
 * Example:
 * <pre>
 * {@code
 *   public class SomeClass {
 *
 *     private final Setting<String> guitar;
 *
 *     @Inject
 *     SomeClass(@Named("guitar") DynamicSetting guitar) {
 *       this.guitar = new Setting<>(guitar, String.class);
 *     }
 *
 *     public String getGuitar() {
 *       return guitar.get();
 *     }
 *   }
 * }
 * </pre>
 */
public class Setting<T> implements SettingsListener, Serializable {

    private static final long serialVersionUID = 1L;

    protected final DynamicSetting setting;

    protected final Class<T> type;

    protected final SettingsListener listener;

    public Setting(DynamicSetting setting, Class<T> type) {
        this(setting, type, null);
    }

    public Setting(DynamicSetting setting, Class<T> type, SettingsListener listener) {
        Preconditions.checkNotNull(setting);
        Preconditions.checkNotNull(type);
        this.setting = setting;
        this.type = type;
        setting.settings.addListener(this);
        if (listener == this) {
            throw new IllegalArgumentException("can't pass in the listener as " +
                    "the setting, as this would result in a cycle; instead just extend" +
                    "this class and override onChanged");
        }
        this.listener = listener;
    }

    /**
     * @return the current value
     */
    @Nullable
    protected Object getCurrentValue() {
        return setting.settings.properties.get(setting.key);
    }

    /**
     * @return current value as a string
     */
    @Nullable
    public String getCurrentValueAsString() {
        Object current = getCurrentValue();
        if (current instanceof String) {
            return (String) current;
        } else if (current != null) {
            return current.toString();
        } else {
            return null;
        }
    }

    /**
     * Gets the value for the setting or null if it is not set.
     *
     * @return setting value or null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public T get() {
        Object value = getCurrentValue();
        if (value != null) {
            if (type.isAssignableFrom(value.getClass())) {
                return (T) value;
            } else {
                return Objects.convertValue(value, type);
            }
        }
        return null;
    }

    /**
     * Gets the value for the setting.
     *
     * @return setting
     * @throws IllegalStateException if the value is null
     */
    public T getNonOptional() {
        T value = get();
        if (value == null) {
            throw new IllegalStateException();
        }
        return value;
    }

    /**
     * Get setting value or the passed in default value if it is not set.
     *
     * @param defaultValue default
     * @return setting value or default
     * @deprecated use {@link #getOr(Object)} instead
     */
    @Deprecated
    public T get(T defaultValue) {
        T value = get();
        return value != null ? value : defaultValue;
    }

    /**
     * Get setting value or the passed in default value if it is not set.
     *
     * @param defaultValue default
     * @return setting value or default
     */
    public T getOr(T defaultValue) {
        T value = get();
        return value != null ? value : defaultValue;
    }

    /**
     * Accepts changes that are for the particular setting this instance
     * was created for.
     *
     * @param key key (setting name)
     * @return true if the key equals the setting name
     */
    @Override
    public final boolean accept(String key) {
        return Objects.equal(key, setting.key);
    }

    @Override
    public final void onChanged(String key, @Nullable Object oldValue, @Nullable Object newValue) {
        onChanged(oldValue, newValue);
        if (listener != null) {
            if (accept(setting.key)) {
                listener.onChanged(setting.key, oldValue, newValue);
            }
        }
    }

    /**
     * You can override this to react to changes for this setting. Note however that if you passed
     * in a listener to the setting, that this method is called before the listener is called.
     *
     * @param oldValue previous value
     * @param newValue current value
     */
    public void onChanged(@Nullable Object oldValue, @Nullable Object newValue) {
    }
}
