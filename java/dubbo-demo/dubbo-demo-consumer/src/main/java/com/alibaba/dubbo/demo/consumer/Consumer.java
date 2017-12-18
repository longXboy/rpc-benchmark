package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.demo.DubboBenchmark;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ken.lj on 2017/7/31.
 */
public class Consumer {

    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException,InterruptedException {
        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();


        DubboBenchmark.BenchmarkMessage msg = prepareArgs();

        int threads = 100;
        int n = 1000000;
        final int count = n / threads; //count per client

        final CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService es = Executors.newFixedThreadPool(threads);

        final DescriptiveStatistics stats = new SynchronizedDescriptiveStatistics();
        final AtomicInteger trans = new AtomicInteger(0);
        final AtomicInteger transOK = new AtomicInteger(0);

        long start = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            es.submit(() -> {
                try {
                    DemoService demoService = (DemoService) context.getBean("demoService"); // get remote service proxy

                    for (int j = 0; j < count; j++) {
                        long t = System.currentTimeMillis();
                        final byte[] msgBytes = msg.toByteArray();
                        byte[] reply = demoService.say(msgBytes);
                        DubboBenchmark.BenchmarkMessage replyMsg = DubboBenchmark.BenchmarkMessage.parseFrom(reply);
                        t = System.currentTimeMillis() - t;

                        stats.addValue(t);
                        trans.incrementAndGet();
                        if (replyMsg != null) {
                            transOK.incrementAndGet();
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        start = System.currentTimeMillis() - start;

        System.out.printf("concurrent     num   : %d\n", threads);
        System.out.printf("sent     requests    : %d\n", n);
        System.out.printf("received requests    : %d\n", trans.get());
        System.out.printf("received requests_OK : %d\n", transOK.get());
        long total = n;
        System.out.printf("throughput  (TPS)    : %d\n", total * 1000 / start);


        System.out.printf("mean: %f\n", stats.getMean());
        System.out.printf("median: %f\n", stats.getPercentile(50));
        System.out.printf("max: %f\n", stats.getMax());
        System.out.printf("min: %f\n", stats.getMin());

        System.out.printf("TP99: %f\n", stats.getPercentile(99));
        System.out.printf("TP999: %f\n", stats.getPercentile(99.9));

      /*  while (true) {
            try {
                Thread.sleep(1000);
                String hello = demoService.sayHello("world"); // call remote method
                System.out.println(hello); // get result

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }


        }*/

    }


    static DubboBenchmark.BenchmarkMessage prepareArgs() throws InvocationTargetException, IllegalAccessException {

        boolean b = true;
        int i = 2276543;
        long l =1;
        String s = "许多往事在眼前一幕一幕，变的那麼模糊";


        DubboBenchmark.BenchmarkMessage.Builder builder = DubboBenchmark.BenchmarkMessage.newBuilder();

        Method[] methods = builder.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (m.getName().startsWith("setField") && ((m.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC)) {
                Parameter[] params = m.getParameters();
                if (params.length == 1) {
                    String n = params[0].getParameterizedType().getTypeName();
                    m.setAccessible(true);
                    if (n.endsWith("java.lang.String")) {
                        m.invoke(builder, new Object[]{s});
                    } else if (n.endsWith("int")) {
                        m.invoke(builder, new Object[]{i});
                    } else if (n.equals("boolean")) {
                        m.invoke(builder, new Object[]{b});
                    }else if (n.equals("long")){
                        m.invoke(builder, new Object[]{l});
                    }

                }
            }
        }

        return builder.build();

    }
}
