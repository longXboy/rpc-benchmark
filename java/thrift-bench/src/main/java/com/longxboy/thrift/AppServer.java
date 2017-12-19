package com.longxboy.thrift;

import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.server.TThreadedSelectorServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TBinaryProtocol;

import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;

public class AppServer
{
    public static GreeterHandler handler;

    public static Greeter.Processor processor;

    public static void main( String[] args )
    {
        GreeterHandler.at =  new AtomicInteger(0);
        ExecutorService es = Executors.newFixedThreadPool(1);
        es.submit(() -> {
            try {
                int lastAt = 0;
                long lastTs = System.currentTimeMillis();

                for (int i =0; i <=9999999; i++){
                    Thread.sleep(3000);
                    long nowTs = System.currentTimeMillis();
                    int nowAt = GreeterHandler.at.get();
                    System.out.printf("%d\n", ((nowAt - lastAt)*1000)/(nowTs-lastTs));
                    lastAt = nowAt;
                    lastTs=nowTs;
                }



            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } );
        try {
            handler = new GreeterHandler();
            processor = new Greeter.Processor(handler);

            simple(processor);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void simple(com.longxboy.thrift.Greeter.Processor processor) {
        /*try {
            TNonblockingServerSocket serverTransport = new TNonblockingServerSocket(8972);

            TThreadedSelectorServer server = new TThreadedSelectorServer(
                    new TThreadedSelectorServer.Args(serverTransport).processor(processor).
                            selectorThreads(2).workerThreads(512));

            System.out.println("Starting the simple server...");
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        try {
            TNonblockingServerSocket socket = new TNonblockingServerSocket(8972);

            THsHaServer.Args arg = new THsHaServer.Args(socket);
            arg.protocolFactory(new TBinaryProtocol.Factory());
            arg.transportFactory(new TFramedTransport.Factory());
            arg.processorFactory(new TProcessorFactory(processor));
            TServer server = new THsHaServer(arg);
            server.serve();
        } catch (TTransportException e) {
            e.printStackTrace();
        }
    }
}
