package com.vesperin.tasks;

import java.io.PrintWriter;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Threads manages creation of executor services
 */
public class Threads {
  private Threads(){}

  public static ExecutorService threadPerCpuExecutor(String name){
    return threadPerCpuExecutor(new PrintWriter(System.err), name);
  }

  public static ExecutorService threadPerCpuExecutor(PrintWriter stderr, String name) {
    final String process = Optional.ofNullable(name)
        .filter(n -> !n.isEmpty())
        .orElseThrow(IllegalArgumentException::new);

    return fixedThreadsExecutor(
        stderr, process, Runtime.getRuntime().availableProcessors());
  }

  private static ExecutorService fixedThreadsExecutor(final PrintWriter stderr, String name, int count) {
    ThreadFactory threadFactory = daemonThreadFactory(name);

    return new ThreadPoolExecutor(count, count, 10, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(Integer.MAX_VALUE), threadFactory) {

      @Override
      protected void afterExecute(Runnable runnable, Throwable throwable) {
        if (throwable != null) {
          stderr.println("Unexpected failure from " + runnable + ": " + throwable.getMessage());
        }
      }
    };
  }

  private static ThreadFactory daemonThreadFactory(final String name) {
    return new ThreadFactory() {
      private int nextId = 0;

      public synchronized Thread newThread(Runnable r) {
        final Thread thread = new Thread(r, name + "-" + (nextId++));
        thread.setDaemon(true);
        return thread;
      }
    };
  }
}
