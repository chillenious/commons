package com.chillenious.common.db.jooq.svc;

import com.chillenious.common.util.Memoize;
import com.chillenious.common.util.Strings;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.tools.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Writes to mutable POJOs, either via matching JPA annotated members and setters, or via
 * matching members and setters by name. It'll do either one or the other, just like the
 * {@link org.jooq.impl.DefaultRecordMapper default record mapper} does for POJOs. This class
 * is based on that implementation, but works on a per-field level rather than per-record.
 */
public final class MutablePOJOFieldMapper<R extends Record, E> implements FieldMapper<R, E> {

    public static final String
            INTROSPECTION_FIELD_MAPPER_HAS_COLUMN_ANNOTATIONS = "IntrospectionFieldMapper.hasColumnAnnotations",
            INTROSPECTION_FIELD_MAPPER_GET_ANNOTATED_MEMBERS = "IntrospectionFieldMapper.getAnnotatedMembers",
            INTROSPECTION_FIELD_MAPPER_GET_MATCHING_SETTERS = "IntrospectionFieldMapper.getMatchingSetters",
            INTROSPECTION_FIELD_MAPPER_GET_ANNOTATED_SETTERS = "IntrospectionFieldMapper.getAnnotatedSetters",
            INTROSPECTION_FIELD_MAPPER_GET_MATCHING_MEMBERS = "IntrospectionFieldMapper.getMatchingMembers";

    // Indicating whether JPA (<code>javax.persistence</code>) is on the classpath
    private static Boolean isJPAAvailable;

    private final boolean useAnnotations;

    private final Map<String, List<java.lang.reflect.Field>> members = new HashMap<>();

    private final Map<String, List<Method>> methods = new HashMap<>();

    @SuppressWarnings("unchecked")
    public MutablePOJOFieldMapper(Class<? extends E> type, Field<?>[] fields) {
        useAnnotations = hasColumnAnnotations(type);

        for (int i = 0; i < fields.length; i++) {
            Field<?> field = fields[i];

            // Annotations are available and present
            if (useAnnotations) {
                members.put(field.getName(), getAnnotatedMembers(type, field.getName()));
                methods.put(field.getName(), getAnnotatedSetters(type, field.getName()));
            }

            // No annotations are present
            else {
                members.put(field.getName(), getMatchingMembers(type, field.getName()));
                methods.put(field.getName(), getMatchingSetters(type, field.getName()));
            }
        }
    }


    @Override
    public void populate(E instance, R record, Field<?> field) {

        Objects.requireNonNull(instance);
        Objects.requireNonNull(record);
        Objects.requireNonNull(field);
        try {
            for (java.lang.reflect.Field member : members.get(field.getName())) {
                // avoid setting final fields
                if ((member.getModifiers() & Modifier.FINAL) == 0) {
                    map(record, instance, member, field);
                }
            }

            // jOOQ does this both, so let's do the same for consistency's sake
            for (Method method : methods.get(field.getName())) {
                method.invoke(instance, record.getValue(field, method.getParameterTypes()[0]));
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("unable to perform population on %s based on field %s",
                            instance, field), e);
        }
    }

    private static void map(Record record, Object result, java.lang.reflect.Field member, Field<?> field)
            throws IllegalAccessException {
        Class<?> mType = member.getType();
        if (mType.isPrimitive()) {
            if (mType == byte.class) {
                member.setByte(result, record.getValue(field, byte.class));
            } else if (mType == short.class) {
                member.setShort(result, record.getValue(field, short.class));
            } else if (mType == int.class) {
                member.setInt(result, record.getValue(field, int.class));
            } else if (mType == long.class) {
                member.setLong(result, record.getValue(field, long.class));
            } else if (mType == float.class) {
                member.setFloat(result, record.getValue(field, float.class));
            } else if (mType == double.class) {
                member.setDouble(result, record.getValue(field, double.class));
            } else if (mType == boolean.class) {
                member.setBoolean(result, record.getValue(field, boolean.class));
            } else if (mType == char.class) {
                member.setChar(result, record.getValue(field, char.class));
            }
        } else {
            member.set(result, record.getValue(field, mType));
        }
    }

    private static boolean isJPAAvailable() {
        if (isJPAAvailable == null) {
            try {
                Class.forName(Column.class.getName());
                isJPAAvailable = true;
            } catch (Throwable e) {
                isJPAAvailable = false;
            }
        }
        return isJPAAvailable;
    }

    /**
     * Check whether <code>type</code> has any {@link javax.persistence.Column} annotated members
     * or methods
     */
    private static boolean hasColumnAnnotations(final Class<?> type) {

        return Memoize.get(() -> {
            if (!isJPAAvailable()) {
                return false;
            }
            // An @Entity or @Table usually has @Column annotations, too
            if (type.getAnnotation(Entity.class) != null ||
                    type.getAnnotation(javax.persistence.Table.class) != null) {
                return true;
            }
            for (java.lang.reflect.Field member : getInstanceMembers(type)) {
                if (member.getAnnotation(Column.class) != null) {
                    return true;
                }
            }
            for (Method method : getInstanceMethods(type)) {
                if (method.getAnnotation(Column.class) != null) {
                    return true;
                }
            }
            return false;
        }, INTROSPECTION_FIELD_MAPPER_HAS_COLUMN_ANNOTATIONS, type);
    }

    private static List<Method> getInstanceMethods(Class<?> type) {
        List<Method> result = new ArrayList<>();
        for (Method method : type.getMethods()) {
            if ((method.getModifiers() & Modifier.STATIC) == 0) {
                result.add(method);
            }
        }
        return result;
    }

    private static List<java.lang.reflect.Field> getInstanceMembers(Class<?> type) {
        List<java.lang.reflect.Field> result = new ArrayList<>();
        for (java.lang.reflect.Field field : type.getFields()) {
            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                result.add(field);
            }
        }
        return result;
    }

    /**
     * Get all members annotated with a given column name
     */
    private static List<java.lang.reflect.Field> getAnnotatedMembers(
            final Class<?> type, final String name) {

        return Memoize.get(() -> {
            List<java.lang.reflect.Field> result = new ArrayList<>();
            for (java.lang.reflect.Field member : getInstanceMembers(type)) {
                Column annotation = member.getAnnotation(Column.class);

                if (annotation != null) {
                    if (name.equals(annotation.name())) {
                        result.add(accessible(member));
                    }
                }
            }
            return result;
        }, INTROSPECTION_FIELD_MAPPER_GET_ANNOTATED_MEMBERS, type, name);
    }

    /**
     * Get all members matching a given column name
     */
    private static List<java.lang.reflect.Field> getMatchingMembers(
            final Class<?> type, final String name) {

        return Memoize.get(() -> {
            List<java.lang.reflect.Field> result = new ArrayList<>();
            String camelCaseLC = StringUtils.toCamelCaseLC(name);
            for (java.lang.reflect.Field member : getInstanceMembers(type)) {
                if (name.equals(member.getName())) {
                    result.add(accessible(member));
                } else if (camelCaseLC.equals(member.getName())) {
                    result.add(accessible(member));
                }
            }
            return result;
        }, INTROSPECTION_FIELD_MAPPER_GET_MATCHING_MEMBERS, type, name);
    }

    /**
     * Get all setter methods annotated with a given column name
     */
    private static List<Method> getAnnotatedSetters(final Class<?> type, final String name) {
        return Memoize.get(() -> {
            List<Method> result = new ArrayList<>();

            for (Method method : getInstanceMethods(type)) {
                Column annotation = method.getAnnotation(Column.class);

                if (annotation != null && name.equals(annotation.name())) {

                    // Annotated setter
                    if (method.getParameterTypes().length == 1) {
                        result.add(accessible(method));
                    }

                    // Annotated getter with matching setter
                    else if (method.getParameterTypes().length == 0) {
                        String m = method.getName();
                        String suffix = m.startsWith("get")
                                ? m.substring(3)
                                : m.startsWith("is")
                                ? m.substring(2)
                                : null;

                        if (suffix != null) {
                            try {
                                Method setter = type.getMethod("set" + suffix, method.getReturnType());

                                // Setter annotation is more relevant
                                if (setter.getAnnotation(Column.class) == null) {
                                    result.add(accessible(setter));
                                }
                            } catch (NoSuchMethodException ignore) {
                            }
                        }
                    }
                }
            }
            return result;

        }, INTROSPECTION_FIELD_MAPPER_GET_ANNOTATED_SETTERS, type, name);
    }

    /**
     * Get all setter methods matching a given column name
     */
    private static List<Method> getMatchingSetters(final Class<?> type, final String name) {
        return Memoize.get(() -> {
            List<Method> result = new ArrayList<>();

            String simpleCamelCase = Strings.capitalize(name);
            String camelCase = StringUtils.toCamelCase(name);
            String camelCaseLC = StringUtils.toLC(name);

            for (Method method : getInstanceMethods(type)) {
                Class<?>[] parameterTypes = method.getParameterTypes();

                if (parameterTypes.length == 1) {
                    if (name.equals(method.getName())) {
                        result.add(accessible(method));
                    } else if (("set" + simpleCamelCase).equals(method.getName())) {
                        result.add(accessible(method));
                    } else if (camelCaseLC.equals(method.getName())) {
                        result.add(accessible(method));
                    } else if (("set" + name).equals(method.getName())) {
                        result.add(accessible(method));
                    } else if (("set" + camelCase).equals(method.getName())) {
                        result.add(accessible(method));
                    }
                }
            }

            return result;

        }, INTROSPECTION_FIELD_MAPPER_GET_MATCHING_SETTERS, type, name);
    }


    /**
     * Conveniently render an {@link java.lang.reflect.AccessibleObject} accessible.
     * <p>
     * To prevent {@link SecurityException}, this is only done if the argument
     * object and its declaring class are non-public.
     *
     * @param accessible The object to render accessible
     * @return The argument object rendered accessible
     */
    private static <T extends AccessibleObject> T accessible(T accessible) {
        if (accessible == null) {
            return null;
        }
        if (accessible instanceof Member) {
            Member member = (Member) accessible;

            if (Modifier.isPublic(member.getModifiers()) &&
                    Modifier.isPublic(member.getDeclaringClass().getModifiers())) {

                return accessible;
            }
        }
        // [jOOQ #3392] The accessible flag is set to false by default, also for public members.
        if (!accessible.isAccessible()) {
            accessible.setAccessible(true);
        }
        return accessible;
    }
}
