package com.vesperin.tasks;

import static com.vesperin.tasks.Threads.threadPerCpuExecutor;

import com.vesperin.utils.Immutable;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author Huascar Sanchez
 */
public class TaskQueue {

  private static final int FOREVER = 60 * 60 * 24 * 28; // four weeks
  private static final int DEFAULT_CONCURRENT_ACTIONS = 1;

  private PrintWriter stdout;
  private PrintWriter stderr;

  private int runningTasks;
  private int runningActions;
  private int maxConcurrentActions;


  private final LinkedList<Task>  tasks           = new LinkedList<>();
  private final LinkedList<Task>  runnableActions = new LinkedList<>();
  private final LinkedList<Task>  runnableTasks   = new LinkedList<>();

  private final List<Task>        failedTasks     = new ArrayList<>();

  /**
   * Construct a new TaskQueue object using default values.
   */
  public TaskQueue(){
    this(
        new PrintWriter(System.out, true),
        new PrintWriter(System.err, true),
        DEFAULT_CONCURRENT_ACTIONS
    );
  }

  /**
   * Construct a new TaskQueue object using default values and log object.
   */
  public TaskQueue(PrintWriter stdout, PrintWriter stderr){
    this(stdout, stderr, DEFAULT_CONCURRENT_ACTIONS);
  }

  /**
   * Construct a new TaskQueue object
   */
  public TaskQueue(PrintWriter stdout, PrintWriter stderr, int maxConcurrentActions) {
    this.stdout = stdout;
    this.stderr = stderr;
    this.maxConcurrentActions = maxConcurrentActions;
  }


  /**
   * Adds a task to the queue.
   */
  public synchronized void enqueue(Task task) {
    tasks.add(task);
  }

  /**
   * Adds the entire collection of tasks to the queue.
   *
   * @param tasks collection of tasks.
   */
  public void enqueueAll(Collection<Task> tasks) {
    this.tasks.addAll(tasks);
  }

  /**
   * @return the first task in the queue.
   */
  public Task getFirst(){
    return tasks.getFirst();
  }

  /**
   * @return the last task in the collection
   */
  public Task getLast(){
    return tasks.getLast();
  }

  /**
   * @return an immutable copy of the tasks kept by the queue.
   */
  public synchronized List<Task> getTasks() {
    return Immutable.listOf(tasks);
  }

  private void calibrateMaxConcurrentActions(){
    final int k = (int) ((Runtime.getRuntime().availableProcessors()) / (1 - 0.03));

    stdout.println(String.format("Expected number of threads: %d", k));

    compareMaxConcurrentActionsAndSet(k);
  }

  private void compareMaxConcurrentActionsAndSet(int maxConcurrentActions){
    this.maxConcurrentActions = Math.min(
      tasks.size(),
      Math.max(this.maxConcurrentActions, maxConcurrentActions)
    );
  }

  /**
   * Calibrates the max concurrent actions this queue can handle
   * and then run all stored tasks.
   */
  public void calibrateAndRunTask(){
    calibrateMaxConcurrentActions();

    runTasks();
  }

  /**
   * Run all the tasks kept this queue.
   */
  public void runTasks() {
    promoteBlockedTasks();

    final AtomicInteger counter = new AtomicInteger(0);

    ExecutorService runners = threadPerCpuExecutor(stderr, "TaskQueue");

    for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
      runners.execute(() -> {
        while (runOneTask()) {
          // running one task at a time
          counter.getAndIncrement();
        }
      });
    }

    runners.shutdown();

    try {
      runners.awaitTermination(FOREVER, TimeUnit.SECONDS);

      stdout.println(String.format("Executed tasks: %d", counter.get()));

    } catch (InterruptedException e) {
      stdout.println("failed task: " + e.getMessage());
      e.printStackTrace(stderr);
      throw new AssertionError();
    }
  }

  private boolean runOneTask() {
    Task task = takeTask();
    if (task == null) {
      return false;
    }

    String threadName = Thread.currentThread().getName();

    Thread.currentThread().setName(task.toString());

    try {
      task.run(stdout, stderr);
    } finally {
      doneTask(task);
      Thread.currentThread().setName(threadName);
      stdout.flush();
      stderr.flush();
    }

    return true;
  }

  /**
   * Takes the next task to process conditioned by
   * the max concurrent actions handled by this queue.
   *
   * @return next task
   */
  private synchronized Task takeTask() {
    while (true) {
      Task task = null;
      if (runningActions < maxConcurrentActions) {
        task = runnableActions.poll();
      }
      if (task == null) {
        task = runnableTasks.poll();
      }

      if (task != null) {
        runningTasks++;
        if (task.isAction()) {
          runningActions++;
        }
        return task;
      }

      if (isExhausted()) {
        return null;
      }

      try {
        wait();
      } catch (InterruptedException e) {
        throw new AssertionError();
      }
    }
  }

  private synchronized void doneTask(Task task) {
    if (task.result != TaskResult.SUCCESS) {
      failedTasks.add(task);
    }

    runningTasks--;

    if (task.isAction()) {
      runningActions--;
    }

    promoteBlockedTasks();

    if (isExhausted()) {
      notifyAll();
    }
  }

  /**
   * Returns true if there are no tasks to run and no tasks currently running.
   */
  private boolean isExhausted() {
    return runnableTasks.isEmpty()
      && runnableActions.isEmpty() && runningTasks == 0;
  }

  /**
   * Prints a summary of completed tasks and those which successfully
   * completed.
   */
  public void printTasks() {
    int i = 0;
    for (Task task : tasks) {
      StringBuilder message = new StringBuilder()
        .append("Task ").append(i++).append(": ").append(task);

      for (Task blocker : task.firstToFinish) {
        message.append("\n  depends on completed task: ").append(blocker);
      }

      for (Task blocker : task.firstToSuccessfullyFinish) {
        message.append("\n  depends on successful task: ").append(blocker);
      }

      stdout.println(message.toString());
    }
  }

  /**
   * Prints those tasks that failed to complete.
   */
  public void printProblemTasks() {
    for (Task task : failedTasks) {
      String message = "Failed task: " + task + " " + task.result;
      if (task.thrown != null) {
        stderr.println(message + " " + task.thrown.getMessage());
      } else {
        stdout.println(message);
      }
    }

    for (Task task : tasks) {
      StringBuilder message = new StringBuilder()
        .append("Failed to execute task: ").append(task);
      for (Task blocker : task.firstToFinish) {
        if (blocker.result == null) {
          message.append("\n  blocked by non-executed task: ").append(blocker);
        }
      }
      for (Task blocker : task.firstToSuccessfullyFinish) {
        if (blocker.result == null) {
          message.append("\n  blocked by non-executed task: ").append(blocker);
        } else if (blocker.result != TaskResult.SUCCESS) {
          message.append("\n  blocked by unsuccessful task: ").append(blocker);
        }
      }
      stdout.println(message.toString());
    }
  }


  private synchronized void promoteBlockedTasks() {
    for (Iterator<Task> it = tasks.iterator(); it.hasNext(); ) {
      final Task potentiallyUnblocked = it.next();

      if (potentiallyUnblocked.isRunnable()) {
        it.remove();

        if (potentiallyUnblocked.isAction()) {
          runnableActions.add(potentiallyUnblocked);
        } else {
          runnableTasks.add(potentiallyUnblocked);
        }

        notifyAll();
      }
    }
  }

}
