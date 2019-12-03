package com.vesperin.tasks;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Task define work unit to be run in paralell.
 */
public abstract class Task {
  private final String name;

  final List<Task> firstToFinish              = new ArrayList<>();
  final List<Task> firstToSuccessfullyFinish  = new ArrayList<>();

  volatile TaskResult result;

  Exception thrown;


  /**
   * Construct a new task object.
   *
   * @param name the name of the task object.
   */
  public Task(String name){
    this.name = name;
  }

  /**
   * Adds a task that must be completed before this task.
   *
   * @param prerequisite the pre requisite task.
   * @return self
   */
  public Task after(Task prerequisite){
    Optional.ofNullable(prerequisite).ifPresent(firstToFinish::add);
    return this;
  }

  /**
   * Adds a collection of tasks that must be completed before this task.
   *
   * @param prerequisites the collection of pre requisites
   * @return self
   */
  public Task after(Collection<Task> prerequisites){
    prerequisites.forEach(this::after);
    return this;
  }

  /**
   * Adds a successful task that must be completed before this task.
   *
   * @param prerequisite the pre requisite and successful task.
   * @return self
   */
  public Task afterSuccess(Task prerequisite){
    Optional.ofNullable(prerequisite).ifPresent(firstToSuccessfullyFinish::add);
    return this;
  }

  /**
   * Adds a collection of successful tasks that must be completed before this task.
   *
   * @param prerequisites the collection of pre requisites and successful tasks.
   * @return self
   */
  public Task afterSuccess(Collection<Task> prerequisites){
    prerequisites.forEach(this::afterSuccess);
    return this;
  }

  protected abstract TaskResult execute() throws Exception;

  /**
   * @return true if this is an action task. A task queue imposes certain
   * limits on how many actions may be run concurrently.
   */
  public boolean isAction(){
    return false;
  }


  /**
   * @return true if this is a runnable task; false otherwise.
   */
  public final boolean isRunnable(){

    for(Task each : firstToFinish){
      if(each.result == null){
        return false;
      }
    }

    for(Task each : firstToSuccessfullyFinish){
      if(each.result != TaskResult.SUCCESS){
        return false;
      }
    }


    return true;
  }

  /**
   * Runs this task
   */
  public final void run(){
    run(new PrintWriter(System.out), new PrintWriter(System.err));
  }

  /**
   * Runs this task.
   */
  public final void run(PrintWriter stdout, PrintWriter stderr){
    if(result != null) throw new IllegalStateException();

    try {
      stdout.println("Running " + this);
      result  = execute();
    } catch (Exception e){
      stdout.println(name + " failed");
      e.printStackTrace(stderr);

      thrown  = e;
      result  = TaskResult.ERROR;
    }

    if(TaskResult.SUCCESS != result){
      stdout.println(this + " " + result);
    }
  }

  @Override public String toString() {
    return name;
  }
}
