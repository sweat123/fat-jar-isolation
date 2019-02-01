package com.laomei.fatjar.classloader.test;

import com.laomei.fatjar.classloader.FatJarDelegateClassLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

/**
 * @author laomei on 2019/1/31 15:56
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class FatJarDelegateClassLoaderTest {

    private ClassLoader classLoader;

    @Before
    public void init() {
        classLoader = initClassLoader();
    }

    @Test
    public void testFatJarDelegateClassLoader() throws ClassNotFoundException {
        Class<?> klass = Class.forName("com.laomei.fatjar.base.TestClass", true, classLoader);
        System.out.println(klass);
    }

    private static URLClassLoader initClassLoader() {
        ClassLoader lastClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader urlClassLoader = (URLClassLoader) lastClassLoader;
        return createFatJarClassLoader(urlClassLoader);
    }

    private static FatJarDelegateClassLoader createFatJarClassLoader(ClassLoader classLoader) {
        URL url = classLoader.getResource("test-fat-jar-class-1.0-SNAPSHOT-fat.jar");
        return new FatJarDelegateClassLoader(
                new URL[] { url },
                null,
                Collections.singleton("com.laomei.fatjar.base")
        );
    }
}
