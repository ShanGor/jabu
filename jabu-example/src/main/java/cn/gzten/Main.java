package cn.gzten;

import cn.gzten.jabu.JabuBoot;
import cn.gzten.jabu.JabuEntryImpl;

public class Main {
    public static void main(String[] args) throws Exception {
        new JabuBoot(new JabuEntryImpl()).startServer();
    }
}