package com.chillenious.common.util.guice;

import com.google.inject.matcher.Matcher;
import com.chillenious.common.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Adopted (slightly tweaked) from Google Sitebricks.
 * <p/>
 * Utility class that finds all the classes in a given package. (based on a
 * similar utility in TestNG)
 *
 * @author <a href="mailto:cedric@beust.com">Cedric Beust</a>
 * @author Dhanji R. Prasanna (dhanji@gmail.com)
 */
public class Classes {

    private static final Logger log = LoggerFactory.getLogger(Classes.class);

    private final Matcher<? super Class<?>> matcher;

    private Classes(Matcher<? super Class<?>> matcher) {
        this.matcher = matcher;
    }

    /**
     * @param pack A list of packages to scan, recursively.
     * @return A set of all the classes inside this package
     * @throws PackageScanFailedException Thrown when error reading from disk or jar.
     * @throws IllegalStateException      Thrown when something very odd is happening with
     *                                    classloaders.
     */
    public Set<Class<?>> in(Package pack) {
        return in(pack != null ? pack.getName() : "");
    }

    /**
     * Return all classes in the class path.
     *
     * @return A set of all the classes inside this package
     * @throws PackageScanFailedException Thrown when error reading from disk or jar.
     * @throws IllegalStateException      Thrown when something very odd is happening with
     *                                    classloaders.
     */
    public Set<Class<?>> all() {
        return in("");
    }

    /**
     * Return the classes in the provided packages and it's sub packages.
     *
     * @param pack A list of packages to scan, recursively.
     * @return A set of all the classes inside this package
     * @throws PackageScanFailedException Thrown when error reading from disk or jar.
     * @throws IllegalStateException      Thrown when something very odd is happening with class
     *                                    loaders.
     */
    public Set<Class<?>> in(String... pack) {
        if (pack == null) {
            return new HashSet<>();
        }
        Set<Class<?>> all = new HashSet<>();
        for (String p : pack) {
            all.addAll(in(p));
        }
        return all;
    }

    /**
     * Return the classes in the provided package and it's sub packages.
     *
     * @param pack the packages to scan, recursively.
     * @return A set of all the classes inside this package
     * @throws PackageScanFailedException Thrown when error reading from disk or jar.
     * @throws IllegalStateException      Thrown when something very odd is happening with class
     *                                    loaders.
     */
    public Set<Class<?>> in(String pack) {
        if (pack == null) {
            return new HashSet<>();
        }
        String packageName = pack;
        String packageOnly = pack;

        final boolean recursive = true;

        Set<Class<?>> classes = new LinkedHashSet<>();
        String packageDirName = pack.replace('.', '/');

        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(
                    packageDirName);

        } catch (IOException e) {
            throw new PackageScanFailedException(
                    "Could not read from package directory: " + packageDirName,
                    e);
        }

        while (dirs.hasMoreElements()) {
            URL url = dirs.nextElement();
            String protocol = url.getProtocol();

            if ("file".equals(protocol)) {
                try {
                    findClassesInDirPackage(packageOnly,
                            URLDecoder.decode(url.getFile(), "UTF-8"), recursive, classes);
                } catch (UnsupportedEncodingException e) {
                    throw new PackageScanFailedException(
                            "Could not read from file: " + url, e);
                }
            } else if ("jar".equals(protocol)) {
                JarFile jar;

                try {
                    jar = ((JarURLConnection) url.openConnection())
                            .getJarFile();
                } catch (IOException e) {
                    throw new PackageScanFailedException(
                            "Could not read from jar url: " + url, e);
                }

                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.charAt(0) == '/') {
                        name = name.substring(1);
                    }
                    if (name.startsWith(packageDirName)) {
                        int idx = name.lastIndexOf('/');
                        if (idx != -1) {
                            packageName = name.substring(0, idx).replace('/', '.');
                        }

                        if ((idx != -1) || recursive) {
                            // it's not inside a deeper dir
                            if (name.endsWith(".class") && !entry.isDirectory()) {
                                String className = name.substring(packageName
                                        .length() + 1, name.length() - 6);

                                // include this class in our results
                                add(packageName, classes, className);
                                // vResult.add();
                            }
                        }
                    }
                }
            }
        }

        return classes;
    }

    private void add(String packageName, Set<Class<?>> classes, String className) {

        Class<?> clazz = null;
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = Classes.class.getClassLoader();
            }
            clazz = loader.loadClass(packageName + '.' + className);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            logAndThrowClassloadingException(packageName, className, e);
        }

        if (matcher.matches(clazz))
            classes.add(clazz);
    }

    private void logAndThrowClassloadingException(String packageName, String className, Throwable e) {
        log.error("Class " + packageName + '.' + className + ", discovered by the scanner could not " +
                "be found by the ClassLoader, something very odd has happened with the classloading " +
                "(see root cause): " + e.toString());
        throw new IllegalStateException(
                "Class " + packageName + '.' + className +
                        " discovered by the scanner could not be found by the ClassLoader", e
        );
    }

    private void findClassesInDirPackage(String packageName,
                                         String packagePath, final boolean recursive, Set<Class<?>> classes) {
        File dir = new File(packagePath);

        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] dirfiles = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return (recursive && file.isDirectory())
                        || (file.getName().endsWith(".class"));
            }
        });

        for (File file : dirfiles) {
            if (file.isDirectory()) {
                findClassesInDirPackage((Strings.isEmpty(packageName) ? ""
                                : packageName + ".")
                                + file.getName(), file.getAbsolutePath(), recursive, classes
                );
            } else {
                String className = file.getName().substring(0,
                        file.getName().length() - 6);
                // include class
                add(packageName, classes, className);
            }
        }
    }

    public static Classes matching(Matcher<? super Class<?>> matcher) {
        return new Classes(matcher);
    }

}
