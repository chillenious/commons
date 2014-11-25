package com.chillenious.common;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A setting that may be changed at runtime.
 * <p>
 * They should be initialized as regular settings, but with the understanding that you
 * should not keep references to their values, but instead use this indirection. If
 * you use this with {@link com.chillenious.common.Bootstrap}, they will be linked with
 * their key for {@link com.google.inject.name.Named}.
 * <p>
 * You can use such settings like:
 * <pre>
 * {@code
 *   @Inject
 *   @Named("guitar")
 *   private DynamicSetting guitar;
 * }
 * </pre>
 */
public final class DynamicSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    final String key;

    final Settings settings;

    public DynamicSetting(String setting, Settings settings) {
        Preconditions.checkNotNull(setting);
        Preconditions.checkNotNull(settings);
        this.key = setting;
        this.settings = settings;
    }

    /**
     * @return whether this setting has a value
     */
    public boolean isSet() {
        return settings.isDefined(key);
    }

    /**
     * @return get the string value
     */
    @Nullable
    public String getString() {
        return settings.getString(key);
    }

    /**
     * @return get the integer value
     */
    @Nullable
    public Integer getInteger() {
        return settings.getInteger(key);
    }

    /**
     * @return get the long value
     */
    @Nullable
    public Long getLong() {
        return settings.getLong(key);
    }

    /**
     * @return get the double value
     */
    @Nullable
    public Double getDouble() {
        return settings.getDouble(key);
    }

    /**
     * @return get big decimal value
     */
    @Nullable
    public BigDecimal getBigDecimal() {
        return settings.getBigDecimal(key);
    }

    /**
     * @return get boolean value
     */
    @Nullable
    public Boolean getBoolean() {
        return settings.getBoolean(key);
    }

    /**
     * Get value with default as a fall back
     *
     * @param defaultValue default
     * @return value or default
     */
    public String getString(String defaultValue) {
        return settings.getString(key, defaultValue);
    }

    /**
     * Get value with default as a fall back
     *
     * @param defaultValue default
     * @return value or default
     */
    public Long getLong(Long defaultValue) {
        return settings.getLong(key, defaultValue);
    }

    /**
     * Get value with default as a fall back
     *
     * @param defaultValue default
     * @return value or default
     */
    public Integer getInteger(Integer defaultValue) {
        return settings.getInteger(key, defaultValue);
    }

    /**
     * Get value with default as a fall back
     *
     * @param defaultValue default
     * @return value or default
     */
    public Double getDouble(Double defaultValue) {
        return settings.getDouble(key, defaultValue);
    }

    /**
     * Get value with default as a fall back
     *
     * @param defaultValue default
     * @return value or default
     */
    public Boolean getBoolean(Boolean defaultValue) {
        return settings.getBoolean(key, defaultValue);
    }

    /**
     * Get value with default as a fall back
     *
     * @param defaultValue default
     * @return value or default
     */
    public BigDecimal getBigDecimal(BigDecimal defaultValue) {
        return settings.getBigDecimal(key, defaultValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DynamicSetting that = (DynamicSetting) o;

        return Objects.equal(this.key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("setting", key)
                .add("value", settings.getString(key))
                .toString();
    }
}
