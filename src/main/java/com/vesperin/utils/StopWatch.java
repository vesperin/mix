package com.vesperin.utils;

import java.util.Objects;

/**
 * Measures the time that elapses between the start and end of a
 * programming task (wall-clock time).
 */
public final class StopWatch {

  private long start = System.currentTimeMillis();
  private static Log logger = Log.quiet();

  public static StopWatch make(){
    return make(null);
  }

  public static StopWatch make(Log log){
    if(!Objects.isNull(log)){
      logger = log;
    }

    return new StopWatch();
  }

  /**
   * Resets and returns elapsed time in milliseconds.
   */
  private double reset() {
    long now = System.currentTimeMillis();
    try {
      return (now - start)/1000.0;
    } finally {
      start = now;
    }
  }


  /**
   * Resets and then logs the elapsed CPU time (in seconds)
   * since the stopwatch was created.
   *
   */
  public void elapsedTime(String label) {
    logger.info(label + ": " + reset() + "s");
  }
}