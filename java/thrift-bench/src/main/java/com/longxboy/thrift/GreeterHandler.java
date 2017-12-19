package com.longxboy.thrift;

import org.apache.thrift.TException;

import java.util.concurrent.atomic.AtomicInteger;

public class GreeterHandler  implements com.longxboy.thrift.Greeter.Iface {
    static public AtomicInteger at;


    public BenchmarkMessage say(BenchmarkMessage msg) throws TException {
        at.incrementAndGet();

        msg.setField1("OK");
        msg.setField2(100);
        return msg;
    }
}
