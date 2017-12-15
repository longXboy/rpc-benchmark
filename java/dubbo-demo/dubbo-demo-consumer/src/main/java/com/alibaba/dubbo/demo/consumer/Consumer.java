package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.demo.DubboBenchmark;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by ken.lj on 2017/7/31.
 */
public class Consumer {

    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException {
        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();

        DemoService demoService = (DemoService) context.getBean("demoService"); // get remote service proxy

        DubboBenchmark.BenchmarkMessage msg = prepareArgs();
        final byte[] msgBytes = msg.toByteArray();

        try {
            byte[] reply = demoService.say(msgBytes);
            DubboBenchmark.BenchmarkMessage replyMsg = DubboBenchmark.BenchmarkMessage.parseFrom(reply);
            if (replyMsg != null && replyMsg.getField1().equals("OK")) {
                System.out.println(replyMsg); // get result
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


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
        int i = 100000;
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
                    }

                }
            }
        }

        return builder.build();

    }
}
