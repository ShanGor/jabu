package cn.gzten;

import cn.gzten.sim.JSimEntryImpl;

public class Main {
    public static void main(String[] args) throws Exception {
        new JSimBoot(new JSimEntryImpl()).startServer();
    }
}