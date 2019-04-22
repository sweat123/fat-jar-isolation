package com.laomei.fatjar.classloader;

import com.laomei.fatjar.common.boot.jar.Archive;
import com.laomei.fatjar.common.boot.jar.JarFileArchive;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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

import static com.laomei.fatjar.common.Constant.FAT_JAR_TOOL;
import static com.laomei.fatjar.common.Constant.FAT_JAR_TOOL_VALUE;
import static com.laomei.fatjar.common.Constant.FAT_MDW_PATH;

/**
 * 管理多个 Far jar ClassLoader。 默认读取中间件jar包的路径
 * /opt
 *   |___fat
 *        |___mdw
 *            |___mdw1
 *            |___mdw2
 *            |___mdw3
 *            .
 *            .
 *            .
 *
 * 由于传入 FatJarDelegateClassLoader 的 url 都是 fat jar url，即使 FatJarDelegateClassLoader url 里含有这些 jar url，
 * 但是它无法成功加载这些 fat jar (默认的 URLClassLoader 没有解析 fat jar 能力)。所以不需要担心 FatJarDelegateClassLoader
 * 本身会加载一些类，影响应用类加载顺序。
 *
 * FatJarDelegateClassLoader 实现使用了大量的 Spring Boot 的代码。
 *
 * @author laomei on 2019/1/9 16:01
 */
@Slf4j
public class FatJarDelegateClassLoader extends URLClassLoader {

    /**
     * 这是一个留的后门，用来指定中间件根文件夹，如果配置了fatDir，需要调用 getInstance(ClassLoader parentClassloader, Collection<String> resourcePrefixes)
     */
    public static String fatDir = null;

    public static FatJarDelegateClassLoader getInstance(ClassLoader parentClassloader, Collection<String> resourcePrefixes) {
        File rt = null;
        if (fatDir != null) {
            rt = new File(fatDir);
        } else {
            rt = new File(FAT_MDW_PATH);
        }
        return getInstance(parentClassloader, rt, resourcePrefixes);
    }

    public static FatJarDelegateClassLoader getInstance(ClassLoader parentClassloader, File rootDir, Collection<String> resourcePrefixes) {
        log.info("load middleware jar files from {}", rootDir);
        if (!rootDir.exists()) {
            throw new IllegalStateException("根目录'" + rootDir + "'不存在");
        }
        File[] files = rootDir.listFiles();
        if (files == null || files.length == 0) {
            throw new IllegalStateException("根目录'" + rootDir + "'内容为空");
        }
        final List<URL> urls = new LinkedList<>();
        for (final File file : files) {
            if (!file.isDirectory()) {
                log.debug("{} is not a directory, we will ignore this file", file);
            }
            List<URL> jarUrls = getJarUrls(file);
            urls.addAll(jarUrls);
        }
        log.debug("add {} into custom classloader", urls);
        return new FatJarDelegateClassLoader(urls.toArray(new URL[0]), parentClassloader, resourcePrefixes);
    }

    /**
     * 获取 dir 下的所有 .jar 结尾的文件
     */
    private static List<URL> getJarUrls(File dir) {
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        final List<URL> urls = new ArrayList<>(files.length);
        for (final File file : files) {
            if (!file.getAbsolutePath().endsWith(".jar")) {
                log.debug("{} is not a jar file, ignore this file", file);
            }
            URL url = null;
            try {
                url = new URL(file.toURI() + "");
            } catch (MalformedURLException ignore) {
            }
            if (url != null) {
                urls.add(url);
            }
        }
        return urls;
    }

    /**
     * 中间件 classloader
     */
    private final List<FatJarClassLoader> fatJarClassLoaders;

    /**
     * 允许访问的资源前缀名
     */
    private final Collection<String>        resourcePrefixes;

    public FatJarDelegateClassLoader(final URL[] urls, final ClassLoader parent,
            final Collection<String> resourcePrefixes) {
        super(urls, parent);
        this.resourcePrefixes = resourcePrefixes;
        this.fatJarClassLoaders = new ArrayList<>(1 << 2);
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
        for (FatJarClassLoader fatJarClassLoader : fatJarClassLoaders) {
            try {
                clazz = fatJarClassLoader.loadClass(name);
                if (clazz != null) {
                    return clazz;
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

    /**
     * 初始化，获取传入的 url 里，所有的由 fat jar plugin 打包的 fat jar，对每个 fat jar 构建一个 FatJarClassLoader
     */
    private void init() {
        final List<URL> unFatJarUrls = new LinkedList<>();
        URL[] urls = getURLs();
        for (URL url : urls) {
            initWithUrl(url, unFatJarUrls);
        }
        if (!unFatJarUrls.isEmpty()) {
            initJarClassLoader(unFatJarUrls);
        }
    }

    private void initJarClassLoader(List<URL> unFarJarUrls) {
        fatJarClassLoaders.add(new FatJarClassLoader(unFarJarUrls.toArray(new URL[0]), null));
    }

    private void initWithUrl(URL url, List<URL> unFatJarUrls) {
        final List<File> fatJarFiles = getFatJarFiles(url);
        if (!fatJarFiles.isEmpty()) {
            initFatJarClassLoaders(fatJarFiles);
        } else {
            unFatJarUrls.add(url);
        }
    }

    /**
     * 获取当前目录下所有的 fat jar 文件
     */
    private List<File> getFatJarFiles(URL url) {
        final List<File> jarFiles = new ArrayList<>();
        File file0 = new File(url.getFile());
        listAllJarFiles(jarFiles, file0);
        return filterFatJarFiles(jarFiles);
    }

    /**
     * 初始化 fat jar classloader
     */
    private void initFatJarClassLoaders(final List<File> fatJarFiles) {
        for (File file : fatJarFiles) {
            try {
                List<URL> urlList = getUrlsForFatJar(file);
                String urlStr = "jar:" + file.toURI() + "!/";
                URL currentJarPath = new URL(urlStr);
                urlList.add(currentJarPath);
                FatJarClassLoader fatJarClassLoader = new FatJarClassLoader(urlList.toArray(new URL[0]), null);
                fatJarClassLoaders.add(fatJarClassLoader);
            } catch (IOException e) {
                throw new IllegalStateException("create jarFile failed", e);
            }
        }
    }

    /**
     * 获取 fat jar 的含有的所有 jar urls
     */
    private List<URL> getUrlsForFatJar(File file) throws IOException {
        JarFileArchive jarFileArchive = new JarFileArchive(file);
        List<Archive> archives = jarFileArchive.getNestedArchives(this::isNestedArchive);
        List<URL> urlList = new LinkedList<>();
        for (Archive archive : archives) {
            urlList.add(archive.getUrl());
        }
        return urlList;
    }

    /**
     * 过滤出由 fat jar plugin 构建的 fat jar
     */
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

    /**
     * fat jar plugin 打成的 fat jar，所有的内置 jar 包都在 lib内。
     */
    private boolean isNestedArchive(Archive.Entry entry) {
        return entry.getName().startsWith("lib/");
    }
}
