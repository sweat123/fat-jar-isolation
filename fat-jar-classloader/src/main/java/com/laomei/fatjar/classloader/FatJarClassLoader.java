package com.laomei.fatjar.classloader;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.jar.JarFile;

/**
 * @author laomei on 2019/1/9 17:32
 */
public class FatJarClassLoader extends URLClassLoader {

    public FatJarClassLoader(final URL[] urls) {
        super(urls, null);
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        System.out.println(name + ", " + this);
        try {
            definePackageIfNecessary(name);
        }
        catch (IllegalArgumentException ex) {
            // Tolerate race condition due to being parallel capable
            if (getPackage(name) == null) {
                // This should never happen as the IllegalArgumentException indicates
                // that the package has already been defined and, therefore,
                // getPackage(name) should not return null.
                throw new AssertionError("Package " + name + " has already been "
                        + "defined but it could not be found");
            }
        }
        return super.loadClass(name, resolve);
    }

    /**
     * Define a package before a {@code findClass} call is made. This is necessary to
     * ensure that the appropriate manifest for nested JARs is associated with the
     * package.
     * @param className the class name being found
     */
    private void definePackageIfNecessary(String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot >= 0) {
            String packageName = className.substring(0, lastDot);
            if (getPackage(packageName) == null) {
                try {
                    definePackage(className, packageName);
                }
                catch (IllegalArgumentException ex) {
                    // Tolerate race condition due to being parallel capable
                    if (getPackage(packageName) == null) {
                        // This should never happen as the IllegalArgumentException
                        // indicates that the package has already been defined and,
                        // therefore, getPackage(name) should not have returned null.
                        throw new AssertionError(
                                "Package " + packageName + " has already been defined "
                                        + "but it could not be found");
                    }
                }
            }
        }
    }

    private void definePackage(final String className, final String packageName) {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws ClassNotFoundException {
                    String packageEntryName = packageName.replace('.', '/') + "/";
                    String classEntryName = className.replace('.', '/') + ".class";
                    for (URL url : getURLs()) {
                        try {
                            URLConnection connection = url.openConnection();
                            if (connection instanceof JarURLConnection) {
                                JarFile jarFile = ((JarURLConnection) connection)
                                        .getJarFile();
                                if (jarFile.getEntry(classEntryName) != null
                                        && jarFile.getEntry(packageEntryName) != null
                                        && jarFile.getManifest() != null) {
                                    definePackage(packageName, jarFile.getManifest(),
                                            url);
                                    return null;
                                }
                            }
                        }
                        catch (IOException ex) {
                            // Ignore
                        }
                    }
                    return null;
                }
            }, AccessController.getContext());
        }
        catch (java.security.PrivilegedActionException ex) {
            // Ignore
        }
    }
}
