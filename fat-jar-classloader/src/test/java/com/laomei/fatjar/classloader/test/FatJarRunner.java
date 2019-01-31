/*
 * FatJarRunner.java
 * Copyright 2019 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package com.laomei.fatjar.classloader.test;

import com.laomei.fatjar.classloader.FatJarDelegateClassLoader;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

/**
 * @author luobo.hwz on 2019/1/31 17:40
 */
public class FatJarRunner extends BlockJUnit4ClassRunner {

    public FatJarRunner(final Class<?> klass) throws InitializationError, ClassNotFoundException {
        super(getClassFromFatJarLoader(klass));
    }

    private static Class<?> getClassFromFatJarLoader(Class<?> klass) throws ClassNotFoundException {
        ClassLoader classLoader = initClassLoader();
        return (Class<?>) classLoader.loadClass(klass.getName());
    }

    private static URLClassLoader initClassLoader() {
        ClassLoader lastClassLoader = Thread.currentThread().getContextClassLoader();
        FatJarDelegateClassLoader fatJarClassLoader = createFatJarClassLoader(lastClassLoader);
        URL[] urls = ((URLClassLoader) lastClassLoader).getURLs();
        return new URLClassLoader(urls, fatJarClassLoader);
    }

    private static FatJarDelegateClassLoader createFatJarClassLoader(ClassLoader classLoader) {
        URL url = classLoader.getResource("test-fat-jar-class-1.0-SNAPSHOT-fat.jar");
        return new FatJarDelegateClassLoader(
                new URL[] { url },
                classLoader.getParent(),
                Collections.singleton("com.laomei.fatjar")
        );
    }
}
