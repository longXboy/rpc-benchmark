package com.alibaba.dubbo.demo.provider;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ken.lj on 2017/7/31.
 */
public class Provider {

    public static void main(String[] args) throws Exception {
        DemoServiceImpl.at =  new AtomicInteger(0);
        ExecutorService es = Executors.newFixedThreadPool(1);
        es.submit(() -> {
            try {
                int lastAt = 0;
                long lastTs = System.currentTimeMillis();

                for (int i =0; i <=9999999; i++){
                    Thread.sleep(3000);
                    long nowTs = System.currentTimeMillis();
                    int nowAt = DemoServiceImpl.at.get();
                    System.out.printf("%d\n", ((nowAt - lastAt)*1000)/(nowTs-lastTs));
                    lastAt = nowAt;
                    lastTs=nowTs;
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } );


        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-provider.xml"});
        context.start();

        System.in.read(); // press any key to exit
    }

}
