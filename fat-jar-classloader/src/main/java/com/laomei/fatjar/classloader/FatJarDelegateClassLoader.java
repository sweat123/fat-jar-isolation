package com.laomei.fatjar.classloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author laomei on 2019/1/9 16:01
 */
public class FatJarDelegateClassLoader extends URLClassLoader {

    private static final Logger logger              = LoggerFactory.getLogger(FatJarDelegateClassLoader.class);

    private static final String FAT_JAR_TOOL        = "Fat-Jar-Build-Tool";

    private static final String FAT_JAR_TOOL_VALUE  = "laomei-Fat-Jar-Plugin";

    private final List<FatJarClassLoader> fatJarClassLoaders;

    private final Collection<String>      resourcePrefixes;

    private ClassLoader                   extClassLoader;

    public FatJarDelegateClassLoader(final URL[] urls, final ClassLoader parent, Collection<String> resourcePrefixes) {
        super(urls, parent);
        this.resourcePrefixes = resourcePrefixes;
        this.fatJarClassLoaders = new ArrayList<>(4);
        init();
    }

    @Override
    public URL getResource(final String name) {
        URL url = getParent().getResource(name);
        if (url != null) {
            return url;
        }
        if (!containsResources(name)) {
            return null;
        }
        for (FatJarClassLoader fatJarClassLoader : fatJarClassLoaders) {
            url = fatJarClassLoader.getResource(name);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        LinkedList<URL> urlLinkedList = new LinkedList<>();
        Enumeration<URL> enumeration = getParent().getResources(name);
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                urlLinkedList.add(enumeration.nextElement());
            }
        }
        if (containsResources(name)) {
            for (FatJarClassLoader fatJarClassLoader : fatJarClassLoaders) {
                Enumeration<URL> enumeration1 = fatJarClassLoader.getResources(name);
                while (enumeration1.hasMoreElements()) {
                    urlLinkedList.add(enumeration1.nextElement());
                }
            }
        }
        return Collections.enumeration(urlLinkedList);
    }

    @Override
    public InputStream getResourceAsStream(final String name) {
        InputStream in = getParent().getResourceAsStream(name);
        if (in != null) {
            return in;
        }
        if (containsResources(name)) {
            for (FatJarClassLoader fatJarClassLoader : fatJarClassLoaders) {
                in = fatJarClassLoader.getResourceAsStream(name);
                if (in != null) {
                    return in;
                }
            }
        }
        return null;
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = null;
        try {
            clazz = getParent().loadClass(name);
        } catch (ClassNotFoundException ignore) {
        }
        if (clazz == null) {
            if (!containsResources(name)) {
                return null;
            }
            for (FatJarClassLoader fatJarClassLoader : fatJarClassLoaders) {
                try {
                    clazz = fatJarClassLoader.loadClass(name, resolve);
                    if (clazz != null) {
                        break;
                    }
                } catch (ClassNotFoundException ignore) {
                }
            }
        }
        if (clazz != null) {
            if (resolve) {
                resolveClass(clazz);
            }
        }
        return null;
    }

    private boolean containsResources(String name) {
        for (String prefix : resourcePrefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void init() {
        URL[] urls = getURLs();
        for (URL url : urls) {
            initWithUrl(url);
        }

        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        while (parent.getParent() != null) {
            parent = parent.getParent();
        }
        this.extClassLoader = parent;
    }

    private void initWithUrl(URL url) {
        final List<File> jarFiles = new ArrayList<>();
        File file0 = new File(url.getFile());
        listAllJarFiles(jarFiles, file0);
        final List<File> fatJarFiles = filterFatJarFiles(jarFiles);
        initFatJarClassLoaders(fatJarFiles);
    }

    private void initFatJarClassLoaders(final List<File> fatJarFiles) {
        for (File file : fatJarFiles) {
            try {
                initFatJarClassLoader(file);
            } catch (IOException e) {
                throw new IllegalStateException("create jarFile failed", e);
            }
        }
    }

    private void initFatJarClassLoader(File file) throws IOException {
        final URL rootUrl = file.toURI().toURL();
        final JarFile jarFile = new JarFile(file);
        final List<URL> nestedJars = new ArrayList<>(16);
        final Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            if (!jarEntry.isDirectory() && jarEntry.getName().endsWith(".jar")) {
                // nested jar
                String nestedJarPath = rootUrl.toString() + "!/" + jarEntry.getName();
                URL nestedJarUrl = new URL(nestedJarPath);
                nestedJars.add(nestedJarUrl);
            }
        }
        FatJarClassLoader fatJarClassLoader = new FatJarClassLoader(nestedJars.toArray(new URL[0]), extClassLoader);
        fatJarClassLoaders.add(fatJarClassLoader);
    }

    private List<File> filterFatJarFiles(final List<File> jarFiles) {
        final List<File> fatJarFiles = new ArrayList<>(4);
        for (File file : jarFiles) {
            try (JarFile jarFile = new JarFile(file)) {
                Manifest manifest = jarFile.getManifest();
                String value = manifest.getMainAttributes().getValue(FAT_JAR_TOOL);
                if (FAT_JAR_TOOL_VALUE.equals(value)) {
                    fatJarFiles.add(file);
                }
            } catch (IOException e) {
                throw new IllegalStateException("create jarFile failed", e);
            }
        }
        return fatJarFiles;
    }

    private void listAllJarFiles(List<File> jarFiles, File file0) {
        if (!file0.canRead() || !file0.exists()) {
            return;
        }
        if (file0.isDirectory()) {
            if (file0.getName().startsWith(".")) {
                //ignore
                return;
            }
            File[] files = file0.listFiles();
            if (files != null) {
                for (File file : files) {
                    listAllJarFiles(jarFiles, file);
                }
            }
        } else {
            if (file0.getName().endsWith(".jar")) {
                jarFiles.add(file0);
            }
        }
    }
}
