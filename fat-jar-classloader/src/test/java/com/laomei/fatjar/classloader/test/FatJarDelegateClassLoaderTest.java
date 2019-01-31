/*
 * FatJarDelegateClassLoaderTest.java
 * Copyright 2019 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package com.laomei.fatjar.classloader.test;

import com.laomei.fatjar.base.TestClass;
import com.laomei.fatjar.clazz.HelloWorld;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author luobo.hwz on 2019/1/31 15:56
 */
@RunWith(FatJarRunner.class)
public class FatJarDelegateClassLoaderTest {

    @Test
    public void testFatJarDelegateClassLoader() {
        HelloWorld helloWorld = new HelloWorld(new TestClass());
        ClassLoader loader = helloWorld.getClass().getClassLoader();
        System.out.println(loader);
        System.out.println(Thread.currentThread().getContextClassLoader());
        String str = helloWorld.hello();
        System.out.println(str);
    }
}
