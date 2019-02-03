package com.laomei.fatjar.base;

/**
 * 对于 fat jar，输出 fat jar class; 否则输出 not fat jar class
 * @author laomei on 2019/1/31 16:21
 */
public class TestClass {

    public String hello() {
        return "not fat jar class";
//        return "fat jar class";
    }
}
