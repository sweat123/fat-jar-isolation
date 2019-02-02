package com.laomei.fatjar.classloader;

import com.laomei.fatjar.classloader.boot.archive.Archive;
import com.laomei.fatjar.classloader.boot.archive.JarFileArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
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

    private ClassLoader extClassLoader;

    public FatJarDelegateClassLoader(final URL[] urls, final ClassLoader parent, Collection<String> resourcePrefixes) {
        super(urls, parent);
        this.resourcePrefixes = resourcePrefixes;
        this.fatJarClassLoaders = new ArrayList<>(4);
        init();
    }

    @Override
    public Enumeration<URL> findResources(final String name) throws IOException {
        LinkedList<URL> urlLinkedList = new LinkedList<>();
        if (containsResources(name)) {
            for (FatJarClassLoader fatJarClassLoader : fatJarClassLoaders) {
                Enumeration<URL> enumeration = fatJarClassLoader.getResources(name);
                while (enumeration.hasMoreElements()) {
                    urlLinkedList.add(enumeration.nextElement());
                }
            }
        }
        return Collections.enumeration(urlLinkedList);
    }

    @Override
    public URL findResource(final String name) {
        if (!containsResources(name)) {
            return null;
        }
        for (FatJarClassLoader fatJarClassLoader : fatJarClassLoaders) {
            URL url = fatJarClassLoader.getResource(name);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        Class<?> clazz = null;
        if (!containsResources(name)) {
            return null;
        }
        System.out.println(name + ", " + this);
        for (FatJarClassLoader fatJarClassLoader : fatJarClassLoaders) {
            try {
                clazz = fatJarClassLoader.loadClass(name);
                if (clazz != null) {
                    break;
                }
            } catch (ClassNotFoundException ignore) {
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
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        while (classLoader.getParent() != null) {
            classLoader = classLoader.getParent();
        }
        extClassLoader = classLoader;
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
        JarFileArchive jarFileArchive = new JarFileArchive(file);
        List<Archive> archives = jarFileArchive.getNestedArchives(this::isNestedArchive);
        List<URL> urlList = new ArrayList<>();
        for (Archive archive : archives) {
            urlList.add(archive.getUrl());
        }
        FatJarClassLoader fatJarClassLoader = new FatJarClassLoader(urlList.toArray(new URL[0]));
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

    private boolean isNestedArchive(Archive.Entry entry) {
        return entry.getName().endsWith(".jar");
    }
}
