package com.chillenious.common.util.guice;

import com.google.common.base.Preconditions;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;

import java.io.Serializable;

/**
 * Some more prefab {@link com.google.inject.matcher.Matcher matchers} on top of the ones Guice already
 * provides.
 */
public class MoreMatchers {

    @SuppressWarnings("unchecked")
    private static class InSubpackages extends AbstractMatcher<Class> implements
            Serializable {

        private static final long serialVersionUID = 0;

        private final String targetPackageName;

        public InSubpackages(String targetPackageName) {
            this.targetPackageName = targetPackageName;
        }

        public boolean matches(Class c) {
            Package pkg = c.getPackage();
            if (pkg == null) {
                return "".equals(targetPackageName);
            }
            String classPackageName = pkg.getName();
            return classPackageName.equals(targetPackageName)
                    || classPackageName.startsWith(targetPackageName + ".");
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof InSubpackages
                    && ((InSubpackages) other).targetPackageName
                    .equals(targetPackageName);
        }

        @Override
        public int hashCode() {
            return 37 * targetPackageName.hashCode();
        }

        @Override
        public String toString() {
            return "inSubpackage(" + targetPackageName + ")";
        }
    }

    /**
     * Returns a matcher which matches classes in the given packages and their
     * subpackages. Unlike inPackage(Package), this matches
     * classes from any classloader.
     *
     * @see com.google.inject.matcher.Matchers#inSubpackage(String)
     */
    @SuppressWarnings("unchecked")
    public static Matcher<Class> inSubpackages(
            final String... targetPackageName) {
        Matcher<Class> m = null;
        for (String pkg : targetPackageName) {
            m = (m != null) ? m.or(new InSubpackages(pkg)) : new InSubpackages(
                    pkg);
        }
        return m;
    }

    /**
     * Returns a matcher which matches classes equal to the given class.
     */
    @SuppressWarnings("unchecked")
    public static Matcher<Class> only(Class cls) {
        return new Only(cls);
    }

    @SuppressWarnings("unchecked")
    private static class Only extends AbstractMatcher<Class> implements
            Serializable {
        private final Class cls;

        public Only(Class cls) {
            this.cls = Preconditions.checkNotNull(cls, "cls should not be null");
        }

        public boolean matches(Class other) {
            return cls.equals(other);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Only && ((Only) other).cls.equals(cls);
        }

        @Override
        public int hashCode() {
            return 37 * cls.hashCode();
        }

        @Override
        public String toString() {
            return "only(" + cls + ")";
        }

        private static final long serialVersionUID = 0;
    }

    public static Matcher<? super TypeLiteral<?>> subtypeOf(
            final Class<?> superclass) {
        return new SubtypeOf(TypeLiteral.get(superclass));
    }

    public static Matcher<? super TypeLiteral<?>> subtypeOf(
            final TypeLiteral<?> supertype) {
        return new SubtypeOf(supertype);
    }

    private static class SubtypeOf extends AbstractMatcher<TypeLiteral<?>>

            implements Serializable {
        private static final long serialVersionUID = 1239939466206498961L;
        private final TypeLiteral<?> supertype;

        public SubtypeOf(TypeLiteral<?> superType) {
            super();
            this.supertype = Preconditions.checkNotNull(superType, "supertype should not be null");
        }

        public boolean matches(TypeLiteral<?> subtype) {
            return (subtype.equals(supertype) || supertype.getRawType()
                    .isAssignableFrom(subtype.getRawType()));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof SubtypeOf
                    && ((SubtypeOf) other).supertype.equals(supertype);
        }

        @Override
        public int hashCode() {
            return 37 * supertype.hashCode();
        }

        @Override
        public String toString() {
            return "subtypeOf(" + supertype.getRawType() + ".class)";
        }
    }
}
