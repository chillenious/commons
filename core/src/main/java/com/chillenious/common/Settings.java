package com.chillenious.common;

import com.chillenious.common.util.AnySetter;
import com.chillenious.common.util.PropertyNotFoundException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Beefed up properties that in addition to being a key/ value store, can deal with a few type conversions,
 * loading of files from file system and class path, do overrides and substitutions and map to special objects.
 * <p>
 * It is recommended that you use {@link com.chillenious.common.Settings.SettingsBuilder} to create an instance
 * instead of using this class directly. Generally, you should consider settings immutable after the
 * {@link com.chillenious.common.Bootstrap} is created with it, as there is no reliable way to know whether
 * classes keep references to settings from this class instead of evaluating dynamically.
 */
public final class Settings implements Serializable {

    /**
     * The name of the system property/ environment variable that is used to load settings
     * overrides from when {@link #loadOverrides()} is called. The value is <code>settings</code>,
     * so you provide this (typically) by passing in -Dsettings=some/file.properties when you
     * run your project.
     */
    public static final String OVERRIDES_FILE_SYSTEM_PROPERTY = "settings";

    private static final Logger log = LoggerFactory.getLogger(Settings.class);

    private static final long serialVersionUID = 1L;

    private static final String DIRECT_PROPERTIES_LOCATION = "[direct properties]";

    final Properties properties = new Properties();

    private final List<String> locationsLoaded = new ArrayList<>();

    private final Set<SettingsListener> listeners = new HashSet<>();

    /**
     * Loads property file from class path. Replaces all variables in values with
     * pattern ${var} with values of previously loaded properties or system properties
     * (latter takes precedence).
     * <p>
     * If properties were already loaded, any properties with the same name will
     * be overridden. A message will be logged when that happens.
     *
     * @param location location in class path to load properties from
     */
    public synchronized void loadFromClassPath(String location) {
        Preconditions.checkNotNull(location);
        Properties p = new Properties();
        try (InputStream is = Resources.getResource(location).openStream()) {
            p.load(is);
        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("problem loading %s from the classpath: %s",
                            location, e.getMessage()), e
            );
        }
        substituteVariables(p);
        putAll(location, p);
        locationsLoaded.add(String.format("[classpath]: %s ", location));
    }

    /**
     * Loads properties directly. Replaces all variables in values with
     * pattern ${var} with values of previously loaded properties or system properties
     * (latter takes precedence).
     * <p>
     * If properties were already loaded, any properties with the same name will
     * be overridden. A message will be logged when that happens.
     *
     * @param p properties to load in
     */
    public synchronized void loadFromProperties(Properties p) {
        Preconditions.checkNotNull(p);
        substituteVariables(p);
        putAll(DIRECT_PROPERTIES_LOCATION, p);
        locationsLoaded.add(String.format("[classpath]: %s ", DIRECT_PROPERTIES_LOCATION));
    }

    /**
     * Loads property file from file system. Replaces all variables in values with
     * pattern ${var} with values of previously loaded properties or system properties
     * (latter takes precedence).
     * <p>
     * If properties were already loaded, any properties with the same name will
     * be overridden. A message will be logged when that happens.
     *
     * @param location location in class path to load properties from
     */
    public synchronized void loadFromFile(String location) {
        Preconditions.checkNotNull(location);
        Properties p = new Properties();
        Path path = FileSystems.getDefault().getPath(location);
        try (InputStream is = Files.newInputStream(path)) {
            p.load(is);
        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("problem loading %s from the file system: %s",
                            location, e.getMessage()), e
            );
        }
        substituteVariables(p);
        putAll(location, p);
        locationsLoaded.add(String.format("[file]: %s ", location));
    }

    /**
     * Looks at the existence of an overrides file and if available loads that, and also looks at the system
     * properties. Settings from either will be used to override any of the current properties, with
     * system (environment settings) taking priority over these from the overrides file.
     */
    synchronized void loadOverrides() {
        // see if there is an overrides file we should load
        String overridesFiles = System.getProperty(OVERRIDES_FILE_SYSTEM_PROPERTY);
        if (!Strings.isNullOrEmpty(overridesFiles)) {
            String[] files = overridesFiles.split(",");
            for (String file : files) {
                if (!Strings.isNullOrEmpty(file)) {
                    file = file.trim();
                    log.info(String.format("loading overrides from %s", file));
                    loadFromFile(file);
                }
            }
        } else {
            log.info("no overrides file specified (use -Dsettings to provide one)");
        }
        // process any environment variables we can match with settings
        Properties p = new Properties();
        p.putAll(System.getProperties());
        substituteVariables(p);
        for (Map.Entry<Object, Object> entry : p.entrySet()) {
            Object current = properties.get(entry.getKey());
            if (current != null) {
                // TODO [Eelco] keep track of the old value so that we can report on the overrides afterwards
                log.info(String.format(
                        "override setting '%s' with value '%s' from system properties (previous value was '%s')",
                        entry.getKey(), entry.getValue(), current));
                properties.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Scan all values for ${var} instances and try to replace them.
     *
     * @param p properties to do scan for var substitution
     */
    void substituteVariables(Properties p) {
        Preconditions.checkNotNull(p);
        for (Map.Entry<Object, Object> entry : p.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            StringBuilder b = new StringBuilder();
            int pos, prev = 0, end = 0;
            if (value.matches(".*(\\$\\{(\\s*\\w.+\\s*)\\}).*")) {
                // search for the next instance of $ from the 'prev' position
                while ((pos = value.indexOf('$', prev)) >= 0) {
                    if (value.charAt(pos + 1) != '{') {
                        prev = pos + 1;
                    } else {
                        if (pos > prev) {
                            prev = value.charAt(prev) == '}' ? prev + 1 : prev;
                            b.append(value.substring(prev, pos));
                        }
                        end = value.indexOf('}', pos);
                        if (end == -1) {
                            throw new IllegalStateException(
                                    "can't substitute value in " + value
                                            + " (there is no ending '}')"
                            );
                        }
                        String variable = value.substring(pos + 2, end);
                        String substitutedValue = p.getProperty(variable);
                        if (!Strings.isNullOrEmpty(substitutedValue)) {
                            log.debug(String.format(
                                    "resolved variable %s to value %s from same settings",
                                    variable, substitutedValue));
                        } else {
                            substitutedValue = properties.getProperty(variable);
                            if (!Strings.isNullOrEmpty(substitutedValue)) {
                                log.debug(String.format(
                                        "resolved variable %s to value %s from previously loaded settings",
                                        variable, substitutedValue));
                            } else {
                                substitutedValue = System.getProperty(variable);
                                if (!Strings.isNullOrEmpty(substitutedValue)) {
                                    log.debug(String.format(
                                            "resolved variable %s to value %s from system properties",
                                            variable, substitutedValue));
                                } else {
                                    String msg = String.format(
                                            "unable to substitute variable %s for property %s (value = %s)",
                                            variable, key, value);
                                    log.warn(msg);
                                    substitutedValue = "${" + variable + "}"; // just keep it as-is
                                }
                            }
                        }
                        b.append(substitutedValue);
                        prev = end;
                    }
                }
            }
            if (b.length() != 0) {
                if (value.length() > end + 1) {
                    b.append(value.substring(end + 1));
                }
                p.setProperty(key, b.toString());
            }
        }
    }

    /**
     * Construct. Package protected to force people to use the builder.
     */
    Settings() {
    }

    /**
     * Set all properties.
     *
     * @param source the source of the properties to set
     * @param p      properties to set
     */
    private void putAll(String source, Properties p) {
        Preconditions.checkNotNull(p);
        for (Map.Entry<Object, Object> entry : p.entrySet()) {
            Object key = entry.getKey();
            put(source, key != null ? key.toString() : null, entry.getValue());
        }
    }

    /**
     * Set a single property. Log when this overrides an existing one.
     *
     * @param key   key
     * @param value value
     * @return any old value
     */
    Object put(String key, @Nullable Object value) {
        Preconditions.checkNotNull(key);
        return put("<direct call>", key, value);
    }

    /**
     * Remove a property.
     *
     * @param key key
     */
    Object remove(String source, String key) {
        Preconditions.checkNotNull(key);
        Object old = properties.remove(key);
        if (old != null) {
            log.info(String.format(
                    "removed existing setting '%s' from '%s' (previous value was '%s')",
                    key, source, old));
        }
        broadcastSettingChanged(key, old, null);
        return old;
    }

    /**
     * Remove a property.
     *
     * @param key key
     * @return any old value
     */
    Object remove(String key) {
        return remove("<direct call>", key);
    }

    /**
     * Set a single property. Log when this overrides an existing one.
     *
     * @param source source of the property to set
     * @param key    key
     * @param value  value
     * @return any old value
     */
    private Object put(String source, String key, @Nullable Object value) {
        Preconditions.checkNotNull(key);
        Object old = properties.put(key, value);
        if (old != null && (!old.equals(value))) {
            log.info(String.format(
                    "override setting '%s' with value '%s' from '%s' (previous value was '%s')",
                    key, value, source, old));
        } else {
            if (log.isDebugEnabled()) {
                log.debug("set " + key + "=" + value);
            }
        }
        broadcastSettingChanged(key, old, value);
        return old;
    }

    private void broadcastSettingChanged(
            String key, Object oldValue, Object newValue) {
        for (SettingsListener l : listeners) {
            if (l.accept(key)) {
                l.onChanged(key, oldValue, newValue);
            }
        }
    }

    /**
     * Set/ change a setting.
     * <strong>warning: only do this for settings you are absolutely sure of will only ever be used as
     * dynamic settings. The case for settings is that they are immutable after bootstrapping. You can't
     * be sure that clients hold references to the values (which wouldn't be changed after calling this
     * method), and changing settings at runtime also introduces the possibility to have a value for
     * a setting that is bad that would otherwise have been caught while bootstrapping (and now really,
     * we'll just have to wait and see it used to catch any problems with it).</strong>
     *
     * @param source  source of the property to set
     * @param setting name/ key of the setting
     * @param value   the value to set. If this is null, the setting is effectively removed
     * @return any old value
     */
    public Object set(String source, String setting, @Nullable String value) {
        Preconditions.checkNotNull(setting);
        if (value != null) {
            return put(source, setting, value);
        } else {
            return remove(source, setting);
        }
    }

    /**
     * Set/ change a setting.
     * <strong>warning: only do this for settings you are absolutely sure of will only ever be used as
     * dynamic settings. The case for settings is that they are immutable after bootstrapping. You can't
     * be sure that clients hold references to the values (which wouldn't be changed after calling this
     * method), and changing settings at runtime also introduces the possibility to have a value for
     * a setting that is bad that would otherwise have been caught while bootstrapping (and now really,
     * we'll just have to wait and see it used to catch any problems with it).</strong>
     *
     * @param setting name/ key of the setting
     * @param value   the value to set. If this is null, the setting is effectively removed
     * @return any old value
     */
    public Object set(String setting, @Nullable String value) {
        return set("<direct call>", setting, value);
    }

    /**
     * You can use this to iterate over keys, filtering out the ones you aren't
     * interested in.
     */
    public static interface KeyIteratorFilter {

        /**
         * Whether the key should be included in the iteration.
         *
         * @param key key to test
         * @return true if it should be included, false otherwise
         */
        boolean accept(String key);
    }

    /**
     * Selects keys that start with the provided prefix.
     */
    public static KeyIteratorFilter startsWith(String prefix) {
        return new StartsWithFilter(prefix, false);
    }

    /**
     * Selects keys that start with the provided prefix.
     */
    public static KeyIteratorFilter endsWith(String prefix) {
        return new EndsWithFilter(prefix, false);
    }

    /**
     * Selects keys that contain the provided string.
     */
    public static KeyIteratorFilter contains(String string) {
        return new ContainsFilter(string, false);
    }

    /**
     * Selects keys that don't contain the provided string.
     */
    public static KeyIteratorFilter notContains(String string) {
        return new ContainsFilter(string, true);
    }

    /**
     * Filters based on a {@link java.util.regex.Pattern}. If a key matches, it
     * will be part of the iteration, otherwise it won't.
     */
    public static KeyIteratorFilter matches(String regex) {
        Preconditions.checkNotNull(regex);
        return new RegExFilter(Pattern.compile(regex), false);
    }

    /**
     * Appends filter to tail of filters and returns resulting new array.
     */
    public static KeyIteratorFilter[] join(KeyIteratorFilter filter,
                                           KeyIteratorFilter... filters) {
        if (filter == null) {
            return filters;
        }
        if (filters == null) {
            return new KeyIteratorFilter[]{filter};
        }
        KeyIteratorFilter[] all = new KeyIteratorFilter[filters.length + 1];
        System.arraycopy(filters, 0, all, 0, filters.length);
        all[all.length - 1] = filter;
        return all;
    }

    /**
     * Filters keys that don't start with the provided prefix.
     */
    private static final class StartsWithFilter implements KeyIteratorFilter {

        private final String prefix;

        private final boolean not;

        StartsWithFilter(String prefix, boolean not) {
            Preconditions.checkNotNull(prefix);
            this.prefix = prefix;
            this.not = not;
        }

        public boolean accept(String key) {
            return not ? !key.startsWith(prefix) : key.startsWith(prefix);
        }
    }

    /**
     * Filters keys that don't end with the provided prefix.
     */
    private static final class EndsWithFilter implements KeyIteratorFilter {

        private final String postfix;

        private final boolean not;

        EndsWithFilter(String postfix, boolean not) {
            Preconditions.checkNotNull(postfix);
            this.postfix = postfix;
            this.not = not;
        }

        public boolean accept(String key) {
            return not ? !key.endsWith(postfix) : key.endsWith(postfix);
        }
    }

    /**
     * Filters keys that don't contain the given sub string.
     */
    private static final class ContainsFilter implements KeyIteratorFilter {

        private final String string;

        private final boolean not;

        public ContainsFilter(String string, boolean not) {
            Preconditions.checkNotNull(string);
            this.string = string;
            this.not = not;
        }

        public boolean accept(String key) {
            return not ? !key.contains(string) : key.contains(string);
        }
    }

    /**
     * Filters keys that do(n't) match the regex.
     */
    private static final class RegExFilter implements KeyIteratorFilter {

        private final Pattern pattern;

        private final boolean not;

        public RegExFilter(Pattern pattern, boolean not) {
            Preconditions.checkNotNull(pattern);
            this.pattern = pattern;
            this.not = not;
        }

        public boolean accept(String key) {
            return not ? !pattern.matcher(key).matches() : pattern.matcher(key)
                    .matches();
        }
    }

    /**
     * Just accepts all keys.
     */
    private static KeyIteratorFilter[] NOOP_FILTERS =
            new KeyIteratorFilter[]{new KeyIteratorFilter() {

                public boolean accept(String key) {
                    return true;
                }
            }};

    /**
     * Iterates over the keys of settings. Get a handle by calling {@link com.chillenious.common.Settings#keys()}.
     */
    public class Keys implements Iterable<String> {

        private final KeyIteratorFilter[] filters;

        Keys(KeyIteratorFilter... filters) {
            this.filters = filters != null ? filters : NOOP_FILTERS;
        }

        public Iterator<String> iterator() {
            return values().iterator();
        }

        public Set<String> values() {
            Set<String> filtered = new HashSet<>();
            for (Object k : properties.keySet()) {
                String key = (String) k;
                boolean add = true;
                for (KeyIteratorFilter filter : filters) {
                    if (!filter.accept(key)) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    filtered.add(key);
                }
            }
            return filtered;
        }
    }

    /**
     * @return all loaded setting keys
     */
    public Keys keys() {
        return keys(NOOP_FILTERS);
    }

    /**
     * Iterate over all settings keys that are accepted by the filter.
     *
     * @param filter filter accepts or rejects keys
     * @return all loaded settings keys that are accepted by the filter
     */
    public Keys keys(@Nullable KeyIteratorFilter... filter) {
        return new Keys(filter);
    }

    /**
     * Settings of the form <prefix><name>"."<property> will be used to populate
     * a map<<name>,<type instance>> where instances will be instantiated and
     * populated with the values found for the <property> with each
     * <prefix><name>"." setting. So foo.bar.name=Eelco with "foo." and
     * Person.class passed in for prefix and type will result in the map being
     * populated with a Person instance, stored with key "bar", and it's name
     * property set to "Eelco".
     *
     * @param <T>    Type
     * @param prefix optional prefix
     * @param type   type to instantiate to make values
     * @return Map populated with objects of the provided type, populated
     * according to the settings
     */
    public <T> MappedSettings<T> map(@Nullable String prefix, Class<T> type) {
        return map(prefix, type, (KeyIteratorFilter[]) null);
    }

    /**
     * Settings of the form <prefix><name>"."<property> will be used to populate
     * a map<<name>,<type instance>> where instances will be instantiated and
     * populated with the values found for the <property> with each
     * <prefix><name>"." setting. So foo.bar.name=Eelco with "foo." and
     * Person.class passed in for prefix and type will result in the map being
     * populated with a Person instance, stored with key "bar", and it's name
     * property set to "Eelco".
     *
     * @param <T>     Type
     * @param prefix  optional prefix
     * @param type    type to instantiate to make values
     * @param filters filter accepts or rejects keys
     * @return Map populated with objects of the provided type, populated
     * according to the settings
     */
    public <T> MappedSettings<T> map(@Nullable String prefix,
                                     Class<T> type,
                                     KeyIteratorFilter... filters) {
        Preconditions.checkNotNull(type);
        prefix = prefix != null ? prefix : "";
        MappedSettings<T> map = new MappedSettings<>(type);
        // Aggregate keyed items
        Map<String, Map<String, Properties>> maps = new HashMap<>();
        for (String key : keys(join(startsWith(prefix), filters))) {
            int lastDotIndex = key.lastIndexOf('.');
            if (lastDotIndex == -1 || lastDotIndex == key.length()) {
                throw new IllegalStateException(
                        String.format("setting %s does not have the property form " +
                                "(ending with .<property>) that is required to map it", key)
                );
            }
            String tail = key.substring(lastDotIndex + 1);
            // Aggregate keyed items into a temp map
            if (tail.startsWith("'") && tail.endsWith("'")) {
                String name = prefix.substring(0, prefix.lastIndexOf("."));
                String mapName;
                if (lastDotIndex - prefix.length() > 0) {
                    mapName = key.substring(prefix.length(), lastDotIndex);
                } else {
                    mapName = prefix.substring(0, lastDotIndex);
                }
                String mapField = tail.substring(1, tail.length() - 1);
                String mapValue = getString(key);
                if (!maps.containsKey(name)) {
                    maps.put(name, new HashMap<String, Properties>());
                }
                Map<String, Properties> thisMap = maps.get(name);
                if (!thisMap.containsKey(mapName)) {
                    thisMap.put(mapName, new Properties());
                }
                Properties p = thisMap.get(mapName);
                p.put(mapField, mapValue);
            } else {
                String field = key.substring(lastDotIndex + 1);
                String name;
                if (lastDotIndex - prefix.length() > 0) {
                    name = key.substring(prefix.length(), lastDotIndex);
                } else {
                    name = prefix.substring(0, lastDotIndex);
                }
                String sValue = getString(key);
                map.setValue(name, field, sValue);
            }
        }
        // Process maps of keyed items
        for (String name : maps.keySet()) {
            Map<String, Properties> data = maps.get(name);
            for (String key : data.keySet()) {
                map.setObjectValue(name, key, data.get(key));
            }
        }
        return map;
    }

    /**
     * Determines if a given key has a defined value
     *
     * @param key the setting's name or key
     * @return true if defined, false otherwise
     */
    public boolean isDefined(String key) {
        Preconditions.checkNotNull(key);
        return properties.getProperty(key) != null;
    }

    /**
     * Gets a setting value or null when not set.
     * <p>
     * NOTE: Where possible clients should depend on Guice's @Named annotation,
     * but in case clients need direct access to a setting, this can be used.
     *
     * @param key the setting's name or key
     * @return the value of the setting or null
     */
    @Nullable
    public String getString(String key) {
        Preconditions.checkNotNull(key);
        return properties.getProperty(key);
    }

    /**
     * Same as {@link #getString(String)} but converts to an integer when not
     * null.
     *
     * @see #getString(String)
     */
    @Nullable
    public Integer getInteger(String key) {
        Preconditions.checkNotNull(key);
        Object val = properties.get(key);
        if (val instanceof Integer) {
            return (Integer) val;
        } else if (val instanceof String) {
            return Integer.valueOf(((String) val).trim());
        }
        return null;
    }

    /**
     * Same as {@link #getString(String)} but converts to a long when not null.
     *
     * @see #getString(String)
     */
    @Nullable
    public Long getLong(String key) {
        Preconditions.checkNotNull(key);
        Object val = properties.get(key);
        if (val instanceof Long) {
            return (Long) val;
        } else if (val instanceof String) {
            return Long.valueOf(((String) val).trim());
        }
        return null;
    }

    /**
     * Same as {@link #getString(String)} but converts to a double when not
     * null.
     *
     * @see #getString(String)
     */
    @Nullable
    public Double getDouble(String key) {
        Preconditions.checkNotNull(key);
        Object val = properties.get(key);
        if (val instanceof Double) {
            return (Double) val;
        } else if (val instanceof String) {
            return Double.valueOf(((String) val).trim());
        }
        return null;
    }

    /**
     * Same as {@link #getString(String)} but converts to a boolean when not
     * null. Unlike the other get methods, this method never returns
     * null, but rather false if the setting doesn't exists.
     *
     * @see #getString(String)
     */
    public Boolean getBoolean(String key) {
        Preconditions.checkNotNull(key);
        Object val = properties.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        } else if (val instanceof String) {
            return Boolean.valueOf(((String) val).trim());
        }
        return false;
    }

    /**
     * Same as {@link #getString(String)} but converts to a BigDecimal when not
     * null.
     *
     * @see #getString(String)
     */
    @Nullable
    public BigDecimal getBigDecimal(String key) {
        Preconditions.checkNotNull(key);
        Object val = properties.get(key);
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        } else if (val instanceof String) {
            DecimalFormat nf = (DecimalFormat) NumberFormat.getInstance();
            nf.setParseBigDecimal(true);
            return (BigDecimal) nf.parse(((String) val).trim(), new ParsePosition(0));
        }
        return null;
    }

    /**
     * Gets a setting, falling back to the passed in defaultValue when not
     * found.
     *
     * @param key          setting key
     * @param defaultValue value for fall back
     * @return either setting or fallback value
     */
    public String getString(String key, String defaultValue) {
        String v = getString(key);
        return v != null ? v : defaultValue;
    }

    /**
     * Gets a setting, falling back to the passed in defaultValue when not
     * found.
     *
     * @param key          setting key
     * @param defaultValue value for fall back
     * @return either setting or fallback value
     */
    public Long getLong(String key, @Nullable Long defaultValue) {
        Long v = getLong(key);
        return v != null ? v : defaultValue;
    }

    /**
     * Gets a setting, falling back to the passed in defaultValue when not
     * found.
     *
     * @param key          setting key
     * @param defaultValue value for fall back
     * @return either setting or fallback value
     */
    public Integer getInteger(String key, @Nullable Integer defaultValue) {
        Integer v = getInteger(key);
        return v != null ? v : defaultValue;
    }

    /**
     * Gets a setting, falling back to the passed in defaultValue when not
     * found.
     *
     * @param key          setting key
     * @param defaultValue value for fall back
     * @return either setting or fall back value
     */
    public Double getDouble(String key, @Nullable Double defaultValue) {
        Double v = getDouble(key);
        return v != null ? v : defaultValue;
    }

    /**
     * Gets a setting, falling back to the passed in defaultValue when not
     * found.
     *
     * @param key          setting key
     * @param defaultValue value for fall back
     * @return either setting or fall back value
     */
    @Nullable
    public BigDecimal getBigDecimal(String key, @Nullable BigDecimal defaultValue) {
        BigDecimal v = getBigDecimal(key);
        return v != null ? v : defaultValue;
    }

    /**
     * Gets a setting, falling back to the passed in defaultValue when not
     * found.
     *
     * @param key          setting key
     * @param defaultValue value for fall back
     * @return either setting or fall back value
     */
    public Boolean getBoolean(String key, @Nullable Boolean defaultValue) {
        Boolean v = getBoolean(key);
        return v != null ? v : defaultValue;
    }

    /**
     * Gets the properties object. The properties object is immutable.
     *
     * @return get immutable properties object
     */
    public Properties asProperties() {
        return new ReadOnlyProperties(properties);
    }

    /**
     * Gets a subset of the settings as an immutable properties object
     * which start with the given prefix, minus that prefix. So e.g.
     * <code>someprefix.foo=bar</code> would return <code>foo=bar</code>
     * if you call this with <code>someprefix.</code>
     *
     * @param prefixForCutoff prefix for filtering and cutting
     * @return immutable properties object
     */
    public Properties asProperties(@Nullable String prefixForCutoff) {
        if (Strings.isNullOrEmpty(prefixForCutoff)) {
            return asProperties();
        }
        Properties subset = new Properties();
        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            String key = String.valueOf(e.getKey());
            if (key.startsWith(prefixForCutoff)) {
                String cutOffKey = key.substring(prefixForCutoff.length());
                subset.put(cutOffKey, e.getValue());
            }
        }
        return new ReadOnlyProperties(subset);
    }

    /**
     * Gets a map. The map is immutable.
     *
     * @return get immutable map
     */
    public Map<String, String> asMap() {
        Map<String, String> p = new HashMap<>();
        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            String key = String.valueOf(e.getKey());
            p.put(key, e.getValue() != null ? e.getValue().toString() : null);
        }
        return Collections.unmodifiableMap(p);
    }

    /**
     * Gets a subset of the settings as an immutable map
     * which start with the given prefix, minus that prefix. So e.g.
     * <code>someprefix.foo=bar</code> would return <code>foo=bar</code>
     * if you call this with <code>someprefix.</code>
     *
     * @param prefixForCutoff prefix for filtering and cutting
     * @return immutable map
     */
    public Map<String, String> asMap(@Nullable String prefixForCutoff) {
        if (Strings.isNullOrEmpty(prefixForCutoff)) {
            return asMap();
        }
        Map<String, String> subset = new HashMap<>();
        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            String key = String.valueOf(e.getKey());
            if (key.startsWith(prefixForCutoff)) {
                String cutOffKey = key.substring(prefixForCutoff.length());
                subset.put(cutOffKey, e.getValue() != null ? e.getValue().toString() : null);
            }
        }
        return Collections.unmodifiableMap(subset);
    }

    /**
     * @return the locations that were loaded (in order)
     */
    public synchronized List<String> getLocationsLoaded() {
        return Collections.unmodifiableList(locationsLoaded);
    }

    /**
     * Add a settings listener.
     *
     * @param l listener to add
     */
    public void addListener(SettingsListener l) {
        Preconditions.checkNotNull(l);
        listeners.add(l);
    }

    /**
     * Remove a settings listener.
     *
     * @param l listener to remove
     */
    public void removeListener(SettingsListener l) {
        Preconditions.checkNotNull(null);
        listeners.remove(l);
    }

    @Override
    public String toString() {
        // TODO [Eelco] make this a neat (line separated?) list
        // TODO [Eelco] we should track the overrides and report them together with the locations
        return String.format("Settings {%s}", asProperties());
    }

    /**
     * Deep clone that doesn't allow setting.
     */
    public static final class ReadOnlyProperties extends Properties {

        public ReadOnlyProperties(Properties properties) {
            for (Map.Entry<Object, Object> e : properties.entrySet()) {
                super.put(e.getKey(), e.getValue());
            }
        }

        @Override
        public Object put(Object key, Object value) {
            throw new UnsupportedOperationException(
                    "these properties are read-only");
        }
    }

    /**
     * Specialized {@link java.util.HashMap} that is returned when calling
     * {@link com.chillenious.common.Settings#map(String, Class)}.
     *
     * @param <T> type
     */
    public static final class MappedSettings<T> extends HashMap<String, T> {

        private static final long serialVersionUID = 1L;

        private final Map<String, Map<String, String>> props = new HashMap<>();

        private final Class<T> type;

        public MappedSettings(Class<T> type) {
            this.type = type;
        }

        /**
         * @return value instances type
         */
        public Class<T> getType() {
            return type;
        }

        T getOrNew(String key) {
            T value = super.get(key);
            if (value == null) {
                try {
                    value = type.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                super.put(key, value);
            }
            return value;
        }

        void recordProperty(String name, String field, String rawValue) {
            Map<String, String> m = props.get(name);
            if (m == null) {
                props.put(name, m = new HashMap<>());
            }
            m.put(field, rawValue);
        }

        void setValue(String name, String field, String sValue) {
            recordProperty(name, field, sValue);
            T o = getOrNew(name);
            setValue(field, sValue, o);
        }

        private void setValue(String field, String sValue, T instance) {
            try {
                Method setter = getSetter(type, field);
                if (setter != null) {
                    setter.setAccessible(true);
                    Class<?> fieldType =
                            convertPrimitiveIfNeeded(setter.getParameterTypes()[0]);
                    if (String.class.isAssignableFrom(fieldType)) {
                        setter.invoke(instance, sValue);
                    } else {
                        Method valueOf = valueOfMethod(fieldType);
                        Object convertedValue = valueOf.invoke(null, sValue);
                        setter.invoke(instance, convertedValue);
                    }
                } else {
                    Field f = getField(type, field);
                    if (f == null) {
                        Method fallback = getAnySetter(type);
                        if (fallback == null) {
                            throw new PropertyNotFoundException(
                                    "neither a setter nor a field found that " +
                                            "conforms to property name "
                                            + field + ", instance=" + instance
                            );
                        } else {
                            fallback.invoke(instance, field, sValue);
                        }
                    } else {
                        f.setAccessible(true);
                        Class<?> fieldType = convertPrimitiveIfNeeded(f.getType());
                        if (String.class.isAssignableFrom(fieldType)) {
                            f.set(instance, sValue);
                        } else {
                            Method valueOf = valueOfMethod(f.getType());
                            Object convertedValue = valueOf.invoke(null, sValue);
                            f.set(instance, convertedValue);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("error setting field " + field
                        + " to " + sValue + " on instance " + instance + ": "
                        + e.getMessage(), e);
            }
        }

        public void setObjectValue(String name, String field, Object sValue) {
            T instance = getOrNew(name);
            try {
                Method setter = getSetter(type, field);
                if (setter != null) {
                    setter.setAccessible(true);
                    setter.invoke(instance, sValue);
                } else {
                    Field f = getField(type, field);
                    if (f == null) {
                        throw new IllegalStateException(
                                "neither a setter nor a field found that " +
                                        "conforms to property name "
                                        + field + ", instance=" + instance
                        );
                    }
                    f.setAccessible(true);
                    f.set(instance, sValue);
                }
            } catch (Exception e) {
                throw new RuntimeException("error setting field " + field
                        + " to " + sValue + " on instance " + instance + ": "
                        + e.getMessage(), e);
            }
        }

        /**
         * Fills in any properties that are set in the single instance in
         * default that are not set in instances of this.
         *
         * @param defaults map containing exactly one instance that represents the
         *                 defaults
         */
        public MappedSettings<T> mergeDefaults(MappedSettings<T> defaults) {
            if (defaults == null) throw new NullPointerException();
            if (defaults.isEmpty()) {
                return this;
            }
            if (defaults.size() != 1) {
                throw new IllegalArgumentException(
                        "merging defaults can only be done with a MappedSettings " +
                                "object that contains  one record (namely the " +
                                "one that contains the defaults) or is empty"
                );
            }
            Map<String, String> defaultsMap = defaults.props.values()
                    .iterator().next();
            for (Entry<String, T> e : entrySet()) {
                Set<String> defaultsNotSet = new HashSet<>(defaultsMap
                        .keySet());
                defaultsNotSet.removeAll(props.get(e.getKey()).keySet());
                for (String field : defaultsNotSet) {
                    String defaultValue = defaultsMap.get(field);
                    recordProperty(e.getKey(), field, defaultValue);
                    setValue(field, defaultValue, e.getValue());
                }
            }
            return this;
        }

        /**
         * Merge the provided map with this map.
         *
         * @param map map to put in this
         */
        public MappedSettings<T> merge(MappedSettings<T> map) {
            props.putAll(map.props);
            putAll(map);
            return this;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("Mapped Settings, target object:").append(
                    type.getSimpleName()).append(", instances:\n");
            for (Entry<String, Map<String, String>> e : props.entrySet()) {
                b.append(e.getKey());
                b.append("->");
                b.append(e.getValue());
                b.append("\n");
            }
            return b.toString();
        }

        private Class<?> convertPrimitiveIfNeeded(Class<?> type) {
            if (type.isPrimitive()) {
                if (Integer.TYPE.isAssignableFrom(type)) {
                    return Integer.class;
                } else if (Double.TYPE.isAssignableFrom(type)) {
                    return Double.class;
                } else if (Long.TYPE.isAssignableFrom(type)) {
                    return Long.class;
                } else if (Byte.TYPE.isAssignableFrom(type)) {
                    return Byte.class;
                } else if (Short.TYPE.isAssignableFrom(type)) {
                    return Short.class;
                } else if (Float.TYPE.isAssignableFrom(type)) {
                    return Float.class;
                } else if (Boolean.TYPE.isAssignableFrom(type)) {
                    return Boolean.class;
                } else if (Character.TYPE.isAssignableFrom(type)) {
                    return Character.class;
                }
                throw new IllegalStateException("can't cast primitive type "
                        + type + " to a regular object class");
            }
            return type;
        }

        private Field getField(Class<?> type, String field) {
            if (Object.class.equals(type)) {
                return null;
            }
            try {
                return type.getDeclaredField(field);
            } catch (SecurityException | NoSuchFieldException e) {
                if (log.isDebugEnabled()) {
                    log.debug("error: " + e.getMessage());
                }
            }
            return getField(type.getSuperclass(), field);
        }

        private Method getSetter(Class<?> type, String name) {
            if (Object.class.equals(type)) {
                return null;
            }
            String setMethodName = "set"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            for (Method m : type.getDeclaredMethods()) {
                if (m.getName().equals(setMethodName)) {
                    return m;
                }
            }
            return getSetter(type.getSuperclass(), name);
        }

        // tries to find first method annotated with AnySetter, which should have
        // two string arguments (key/ value)
        private Method getAnySetter(Class<?> type) {
            // TODO [Eelco] could be cached
            if (Object.class.equals(type)) {
                return null;
            }
            for (Method m : type.getDeclaredMethods()) {
                if (Arrays.stream(m.getDeclaredAnnotations())
                        .anyMatch(a -> a instanceof AnySetter)) {
                    if (m.getParameterCount() == 2 &&
                            m.getParameterTypes()[0].equals(String.class) &&
                            m.getParameterTypes()[1].equals(String.class)) {
                        m.setAccessible(true);
                        return m;
                    } else {
                        throw new IllegalStateException(String.format(
                                "method %s is annotated with AnySetter, but does not have the " +
                                        "right signature (two args of type string)", m));
                    }
                }
            }
            return getAnySetter(type.getSuperclass());
        }

        /**
         * Get the static valueOf(String ) method.
         *
         * @param c The class to obtain the method.
         * @return the method, otherwise null if the method is not present.
         */
        @SuppressWarnings("unchecked")
        private Method valueOfMethod(Class c) {
            try {
                Method m = c.getDeclaredMethod("valueOf", String.class);
                if (!Modifier.isStatic(m.getModifiers())
                        && m.getReturnType() == c) {
                    return null;
                }
                return m;
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("valueOf does not exist on class "
                        + c, e);
            }

        }
    }

    /**
     * Builder utility for settings.
     */
    public static class SettingsBuilder {

        private final Settings settings;

        SettingsBuilder(Settings settings) {
            this.settings = settings;
        }

        /**
         * Loads property file from class path. Replaces all variables in values with
         * pattern ${var} with values of previously loaded properties or system properties
         * (latter takes precedence).
         * <p>
         * If properties were already loaded, any properties with the same name will
         * be overridden. A message will be logged when that happens.
         *
         * @param location location in class path to load properties from
         */
        public SettingsBuilder addFromClassPath(String location) {
            settings.loadFromClassPath(location);
            return this;
        }

        /**
         * Loads property file from file system. Replaces all variables in values with
         * pattern ${var} with values of previously loaded properties or system properties
         * (latter takes precedence).
         * <p>
         * If properties were already loaded, any properties with the same name will
         * be overridden. A message will be logged when that happens.
         *
         * @param location location in class path to load properties from
         */
        public SettingsBuilder addFromFile(String location) {
            settings.loadFromFile(location);
            return this;
        }

        /**
         * Loads properties directly. Replaces all variables in values with
         * pattern ${var} with values of previously loaded properties or system properties
         * (latter takes precedence).
         * <p>
         * If properties were already loaded, any properties with the same name will
         * be overridden. A message will be logged when that happens.
         *
         * @param p properties to load in
         */
        public SettingsBuilder addAll(Properties p) {
            settings.loadFromProperties(p);
            return this;
        }

        /**
         * Loads a property directly. Replaces all variables in values with
         * pattern ${var} with values of previously loaded properties or system properties
         * (latter takes precedence).
         * <p>
         * If properties were already loaded, any properties with the same name will
         * be overridden. A message will be logged when that happens.
         *
         * @param key   setting name
         * @param value setting value
         */
        public SettingsBuilder add(String key, @Nullable String value) {
            Preconditions.checkNotNull(key);
            Properties p = new Properties();
            p.setProperty(key, value);
            return addAll(p);
        }

        /**
         * Try to load property file from class path. Replaces all variables in values with
         * pattern ${var} with values of previously loaded properties or system properties
         * (latter takes precedence). Will be ignored if loading fails.
         * <p>
         * If properties were already loaded, any properties with the same name will
         * be overridden. A message will be logged when that happens.
         *
         * @param location location in class path to load properties from
         */
        public SettingsBuilder tryAddFromClassPath(String location) {
            try {
                addFromClassPath(location);
            } catch (RuntimeException e) {
                log.info(String.format("%s not found in classpath; skipping", location));
            }
            return this;
        }

        public SettingsBuilder tryAddFromFile(String location) {
            try {
                addFromFile(location);
            } catch (RuntimeException e) {
                log.info(String.format("%s not found in file system; skipping", location));
            }
            return this;
        }

        /**
         * Build the settings object. Overrides (from -Dsettings and/ or environment variables) will be applied
         *
         * @return settings instance
         */
        public Settings build() {
            settings.loadOverrides();
            return settings;
        }

        /**
         * Build the settings object. No overrides will be applied.
         * You should typically use {@link #build} instead.
         *
         * @return settings instance
         */
        public Settings buildWithoutOverrides() {
            return settings;
        }
    }

    /**
     * Create a builder to guide you through creating a {@link com.chillenious.common.Settings} object
     *
     * @return new builder for settings
     */
    public static SettingsBuilder builder() {
        return new SettingsBuilder(new Settings());
    }
}
