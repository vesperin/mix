package com.vesperin.utils;

import java.io.PrintStream;

/**
 * @author Huascar Sanchez
 */
public class Log {
  private final boolean silenced;
  private PrintStream   out;

  /**
   * Construct a Log or log viewer using
   * System.out as a print stream object.
   */
  private Log(){
    this(false, System.out);
  }

  /**
   * Construct a Log or log viewer using
   * a print stream object.
   *
   * @param silenced silence log output
   * @param out the current print stream
   */
  private Log(boolean silenced, PrintStream out){
    this.silenced = silenced;
    this.out      = out;
  }

  /**
   * @return a new Log that silences its output.
   */
  public static Log quiet(){
    return new Log(true, System.out);
  }

  /**
   * @return a new Log that logs its output.
   */
  public static Log verbose(){
    return new Log();
  }

  /**
   * Logs an error caused by some throwable.
   *
   * @param s the error label
   * @param exception the actual error
   */
  public void error(String s, Throwable exception){
    if(!silenced){
      out.println("ERROR: " + s);
      exception.printStackTrace(System.err);
    }
  }

  /**
   * Logs some important information
   *
   * @param s the important information
   */
  public void info(String s){
    if(!silenced){
      out.println("INFO: " + s);
    }
  }

  /**
   * Checks if Log is a chatter box.
   *
   * @return true if it is in verbose mode; false otherwise.
   */
  public boolean isVerbose(){
    return !silenced;
  }

  /**
   * Logs a message without assigning a proper label and adding a line break.
   *
   * @param s message.
   */
  public void log(String s){
    if (!silenced) out.print(s);
  }

  /**
   * Logs a warning message
   *
   * @param s the warning message
   */
  public void warn(String s){
    if (!silenced) out.println("WARN: " + s);
  }
}
