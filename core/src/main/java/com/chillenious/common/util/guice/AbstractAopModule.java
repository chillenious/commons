package com.chillenious.common.util.guice;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;

/**
 * Guice module that can serve as a convenience base class for modules that
 * install method interceptors.
 */
@SuppressWarnings("unchecked")
public abstract class AbstractAopModule extends AbstractModule {

    private static final String DEFAULT_SCAN_PACKAGES = "com.chillenious";

    protected Matcher<? super Class> classMatcher;

    /**
     * Create with a custom class matcher.
     *
     * @param classMatcher classes to intercept for audit logging
     */
    public AbstractAopModule(Matcher<? super Class> classMatcher) {
        this.classMatcher = addAdditionalMatchingRules(classMatcher);
    }

    /**
     * Create with zero or more packages. All classes in these packages
     * <strong>and their sub packages</strong> will be intercepted. If no
     * package is provided, the default package "com.chillenious" will be used.
     *
     * @param packages packages (and their sub packages) to intercept
     */
    public AbstractAopModule(String... packages) {
        if (packages == null || packages.length == 0) {
            packages = DEFAULT_SCAN_PACKAGES.split(",");
        }
        addSubPackageMatcher(packages);
        this.classMatcher = addAdditionalMatchingRules(classMatcher);
    }

    /**
     * Create with zero or more packages. All classes in these packages will be
     * intercepted. If no package is provided, the default package
     * "com.chillenious" will be used.
     *
     * @param packages packages (and NOT their sub packages) to intercept
     */
    public AbstractAopModule(Package[] packages) {
        if (packages != null && packages.length == 0) {
            for (Package pkg : packages) {
                if (this.classMatcher == null) {
                    this.classMatcher = Matchers.inPackage(pkg);
                } else {
                    this.classMatcher = Matchers.inPackage(pkg)
                            .or(classMatcher);
                }
            }
        } else {
            addSubPackageMatcher(DEFAULT_SCAN_PACKAGES.split(","));
        }
        this.classMatcher = addAdditionalMatchingRules(classMatcher);
    }

    /**
     * @param packages packages to match
     */
    protected void addSubPackageMatcher(String... packages) {
        for (String pkg : packages) {
            if (this.classMatcher == null) {
                this.classMatcher = MoreMatchers.inSubpackages(pkg);
            } else {
                this.classMatcher = MoreMatchers.inSubpackages(pkg).or(classMatcher);
            }
        }
    }

    /**
     * Template method for adding additional rules to the mather. Will be called
     * upon construction of the module, and is primarily meant for when
     * extending modules want to add to the rules regardless of what clients of
     * the module passed into one of the constructors. Note that since this is
     * called during construction by this super class, the extending object
     * won't be fully initialized (so don't depend on members etc).
     *
     * @param matcher matcher to work with
     * @return the new matcher based on the passed in matcher with any new rules
     */
    protected Matcher<? super Class> addAdditionalMatchingRules(
            Matcher<? super Class> matcher) {
        return matcher;
    }
}
