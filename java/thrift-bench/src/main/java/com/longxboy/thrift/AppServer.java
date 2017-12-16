package com.longxboy.thrift;

import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.server.TThreadedSelectorServer;

public class AppServer
{
    public static GreeterHandler handler;

    public static Greeter.Processor processor;

    public static void main( String[] args )
    {
        try {
            handler = new GreeterHandler();
            processor = new Greeter.Processor(handler);

            simple(processor);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void simple(com.longxboy.thrift.Greeter.Processor processor) {
        try {
            TNonblockingServerSocket serverTransport = new TNonblockingServerSocket(8972);

            TThreadedSelectorServer server = new TThreadedSelectorServer(
                    new TThreadedSelectorServer.Args(serverTransport).processor(processor).
                            selectorThreads(2).workerThreads(512));

            System.out.println("Starting the simple server...");
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
