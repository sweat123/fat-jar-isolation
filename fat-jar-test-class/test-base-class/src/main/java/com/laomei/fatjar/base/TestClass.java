package com.laomei.fatjar.base;

import org.hellojavaer.fatjar.core.boot.FatJarBoot;

/**
 * 对于 fat jar，输出 fat jar class; 否则输出 not fat jar class
 * @author laomei on 2019/1/31 16:21
 */
public class TestClass {

    public String hello() {
        return "not fat jar class";
//        return "fat jar class";
    }

    public static void main(String[] args) {
        FatJarBoot.run();
        System.out.println(System.getProperty("java.io.tmpdir"));
    }
}
