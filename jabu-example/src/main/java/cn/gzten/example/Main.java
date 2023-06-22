package cn.gzten.example;

import cn.gzten.jabu.JabuApplication;
import cn.gzten.jabu.annotation.JabuBoot;

@JabuBoot
public class Main {
    public static void main(String[] args) throws Exception {
        new JabuApplication(new JabuEntryImpl()).startServer();
    }
}