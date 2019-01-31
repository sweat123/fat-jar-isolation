/*
 * HelloWorld.java
 * Copyright 2019 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package com.laomei.fatjar.clazz;

import com.laomei.fatjar.base.TestClass;

/**
 * @author luobo.hwz on 2019/1/31 16:21
 */
public class HelloWorld {

    private final TestClass testClass;

    public HelloWorld(TestClass testClass) {
        this.testClass = testClass;
    }

    public String hello() {
        return testClass.hello();
    }
}
