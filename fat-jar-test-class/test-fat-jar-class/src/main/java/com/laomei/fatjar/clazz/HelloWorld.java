package com.laomei.fatjar.clazz;

import com.laomei.fatjar.base.TestClass;

/**
 * @author laoemi on 2019/1/31 16:21
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
