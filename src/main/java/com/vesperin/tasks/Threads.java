package com.vesperin.tasks;

import com.vesperin.utils.Expect;
import com.vesperin.utils.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Huascar Sanchez
 */
public class Threads {
  private Threads(){}

  public static ExecutorService threadPerCpuExecutor(Log log, String name) {
    Expect.validArgument(name != null && !name.isEmpty());

    return fixedThreadsExecutor(log, name, Runtime.getRuntime().availableProcessors());
  }

  private static ExecutorService fixedThreadsExecutor(final Log log, String name, int count) {
    ThreadFactory threadFactory = daemonThreadFactory(name);

    return new ThreadPoolExecutor(count, count, 10, TimeUnit.SECONDS,
      new LinkedBlockingQueue<>(Integer.MAX_VALUE), threadFactory) {

      @Override protected void afterExecute(Runnable runnable, Throwable throwable) {
        if (throwable != null) {
          log.error("Unexpected failure from " + runnable, throwable);
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
