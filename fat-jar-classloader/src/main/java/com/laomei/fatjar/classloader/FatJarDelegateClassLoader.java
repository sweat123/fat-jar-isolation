package com.laomei.fatjar.classloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
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

    private static final String FAT_JAR_TOOL_VALUE  = "Pilot-Fat-Jar-Plugin";

    private final List<FatJarClassLoader> fatJarClassLoaders;

    private final Collection<String> resourcePrefixes;

    public FatJarDelegateClassLoader(final URL[] urls, final ClassLoader parent, Collection<String> resourcePrefixes) {
        super(urls, parent);
        this.resourcePrefixes = resourcePrefixes;
        this.fatJarClassLoaders = new ArrayList<>(4);
        init();
    }

    @Override
    public URL findResource(String name) {
        if (!containsResources(name)) {
            return null;
        }
        for (FatJarClassLoader classLoader : fatJarClassLoaders) {
            URL url = classLoader.findResource(name);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        if (!containsResources(name)) {
            return null;
        }
        for (FatJarClassLoader classLoader : fatJarClassLoaders) {
            Enumeration<URL> urlEnumeration = classLoader.findResources(name);
            if (urlEnumeration != null) {
                return urlEnumeration;
            }
        }
        return null;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (!containsResources(name)) {
            return null;
        }
        for (FatJarClassLoader classLoader : fatJarClassLoaders) {
            Class<?> clazz = classLoader.findClass(name);
            if (clazz != null) {
                return clazz;
            }
        }
        return null;
    }

    private boolean containsResources(String name) {
        return resourcePrefixes.contains(name);
    }

    private void init() {
        URL[] urls = getURLs();
        for (URL url : urls) {
            initWithUrl(url);
        }
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
        FatJarClassLoader fatJarClassLoader = new FatJarClassLoader(nestedJars.toArray(new URL[0]), this);
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
