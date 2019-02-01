package com.laomei.fatjar.classloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author laomei on 2019/1/9 17:32
 */
public class FatJarClassLoader extends URLClassLoader {

    private final ClassLoader extClassLoader;

    private final JarFile     jarFile;

    public FatJarClassLoader(final URL[] urls, final JarFile jarFile, final ClassLoader extClassLoader) {
        super(urls, null);
        this.jarFile = jarFile;
        this.extClassLoader = extClassLoader;
        initNestedFatJar();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(name);

        if (clazz == null) {
            try {
                clazz = extClassLoader.loadClass(name);
            } catch (ClassNotFoundException ignore) {
            }
        }

        if (clazz == null) {
            clazz = findClassInterval(name);
        }

        if (clazz != null && resolve) {
            resolveClass(clazz);
        }

        return clazz;
    }

    private Class<?> findClassInterval(String name) {
        String classPath = name.replace(".", "/") + ".class";
        URL url = findResourceInternal(name, classPath);
        return null;
    }

    private URL findResourceInternal(String name, String path) {

        return null;
    }

    private void initNestedFatJar() {
        Enumeration<JarEntry> enumeration = jarFile.entries();
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = enumeration.nextElement();

        }
    }
}
