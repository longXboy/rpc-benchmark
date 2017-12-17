/*
 * Copyright 2015, gRPC Authors All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.examples.helloworld;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * A simple client that requests a greeting from the {@link HelloWorldServer}.
 */
public class HelloWorldClient {
  private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

  private final ManagedChannel channel;
  private final HelloGrpc.HelloBlockingStub blockingStub;

  /** Construct client connecting to HelloWorld server at {@code host:port}. */
  public HelloWorldClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext(true)
        .build());
  }

  /** Construct client for accessing RouteGuide server using the existing channel. */
  HelloWorldClient(ManagedChannel channel) {
    this.channel = channel;
    blockingStub = HelloGrpc.newBlockingStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /**
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting.
   */
  public static void main(String[] args) throws Exception {

    BenchmarkMessage msg = prepareArgs();
    int threads = 500;
    int n = 5000000;

    final CountDownLatch latch = new CountDownLatch(threads);
    ExecutorService es = Executors.newFixedThreadPool(threads);

    final DescriptiveStatistics stats = new SynchronizedDescriptiveStatistics();
    final AtomicInteger trans = new AtomicInteger(0);
    final AtomicInteger transOK = new AtomicInteger(0);
    final int count = n / threads; //count per client

      long start = System.currentTimeMillis();

      for (int i = 0; i < threads; i++) {
          es.submit(() -> {
              try {
                  HelloWorldClient client = new HelloWorldClient("localhost", 50051);

                  for (int j = 0; j < count; j++) {

                      long t = System.currentTimeMillis();
                      BenchmarkMessage replyMsg = client.blockingStub.say(msg);
                      t = System.currentTimeMillis() - t;

                      stats.addValue(t);
                      trans.incrementAndGet();
                      if (replyMsg != null && replyMsg.getField1().equals("OK")) {
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

      System.out.printf("msg      length      : %d\n", msg.getSerializedSize());
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



  }

  static BenchmarkMessage prepareArgs() throws InvocationTargetException, IllegalAccessException {

    boolean b = true;
    int i = 2276543;
    long l =1;
    String s = "许多往事在眼前一幕一幕，变的那麼模糊";


    BenchmarkMessage.Builder builder = BenchmarkMessage.newBuilder();

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
