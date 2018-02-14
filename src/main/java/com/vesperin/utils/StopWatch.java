package com.vesperin.utils;

/**
 * Measures the time that elapses between the start and end of a
 * programming task (wall-clock time).
 */
public final class StopWatch {

  private long start = System.currentTimeMillis();

  /**
   * Resets and returns elapsed time in seconds.
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
   * @return the elapsed CPU time (in seconds) since the stopwatch was created.
   *    after this value has been returned, the stop watch automatically resets
   *    this time (makes a new call to {@link System#currentTimeMillis()}).
   */
  public double elapsedTime(){
    return reset();
  }

  /**
   * Resets and reports the elapsed CPU time (in seconds)
   * since the stopwatch was created.
   *
   * @param label user-provided label
   * @return reported elapsed time
   */
  public String reportElapsedTime(String label){
    return (label + ": " + elapsedTime() + "s");
  }

}