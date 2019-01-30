package com.laomei.fatjar.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author laomei on 2019/1/9 17:32
 */
public class FatJarClassLoader extends URLClassLoader {
    public FatJarClassLoader(final URL[] urls, final ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }
}
