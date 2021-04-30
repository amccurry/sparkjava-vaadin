package vaadin.util.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.vaadin.flow.component.UIDetachedException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import vaadin.util.push.ItemManager;
import vaadin.util.push.PushComponent;

@Slf4j
public class TaskManager extends ItemManager<Task> {

  public static final TaskManager INSTANCE = new TaskManager();

  private final ExecutorService _service;
  private final Map<String, TaskState> _stateMap = new ConcurrentHashMap<>();
  private final Set<PushComponent> _pushCache;

  @Data
  public static class TaskState {

    double progress = 0.0;
    Future<?> future;
    Task task;
    TaskStatus taskStatus;
    AtomicBoolean canceled = new AtomicBoolean();
    Throwable throwable;
    long stopped = -1;

    public boolean isCanceled() {
      return canceled.get();
    }
  }

  private TaskManager() {
    _pushCache = Collections.newSetFromMap(new MapMaker().weakKeys()
                                                         .weakValues()
                                                         .makeMap());
    _service = Executors.newCachedThreadPool();
    Runtime.getRuntime()
           .addShutdownHook(new Thread(() -> _service.shutdownNow()));
  }

  public synchronized void register(PushComponent pushComponent) {
    _pushCache.add(pushComponent);
  }

  public synchronized void submitTask(Task task) {
    String id = task.getId();
    TaskState state = new TaskState();
    state.setTask(task);
    state.setTaskStatus(TaskStatus.RUNNING);
    _stateMap.put(id, state);
    state.setFuture(_service.submit(() -> {
      try {
        String name = task.getName();
        Thread.currentThread()
              .setName("Task [" + name + "] Task Id [" + id + "]");
        task.getTaskRunnable()
            .call(() -> state.isCanceled(), progress -> state.setProgress(progress));
        if (!state.isCanceled()) {
          state.setTaskStatus(TaskStatus.COMPLETED);
        } else {
          state.setTaskStatus(TaskStatus.CANCELED);
        }
      } catch (Throwable t) {
        if (!state.isCanceled()) {
          log.error("Unknown error", t);
          state.setTaskStatus(TaskStatus.FAILED);
          state.setThrowable(t);
        } else {
          state.setTaskStatus(TaskStatus.CANCELED);
        }
      }
      state.setStopped(System.currentTimeMillis());
      updateCounts();
      return null;
    }));
    updateCounts();
    updateFilterValues();
  }

  private synchronized void updateCounts() {
    for (PushComponent pushComponent : ImmutableSet.copyOf(_pushCache)) {
      doPush(pushComponent);
    }
  }

  private void doPush(PushComponent pushComponent) {
    if (pushComponent != null) {
      try {
        log.debug("push count {}", pushComponent);
        pushComponent.push();
      } catch (Throwable t) {
        if (t instanceof UIDetachedException) {
          deregister(pushComponent);
        } else {
          log.error("Unknown error while trying to push {}", t);
        }
      }
    }
  }

  public void deregister(PushComponent pushComponent) {
    _pushCache.remove(pushComponent);
  }

  public int getRunningJobCount() {
    cleanupOldJobs();
    Collection<TaskState> values = _stateMap.values();
    int count = 0;
    for (TaskState state : values) {
      switch (state.getTaskStatus()) {
      case RUNNING:
      case CANCELING:
        count++;
        break;
      default:
        break;
      }
    }
    return count;
  }

  private void cleanupOldJobs() {
    Set<Entry<String, TaskState>> entrySet = new HashSet<>(_stateMap.entrySet());
    for (Entry<String, TaskState> entry : entrySet) {
      TaskState taskState = entry.getValue();
      if (taskState.getStopped() >= 0
          && taskState.getStopped() + TimeUnit.HOURS.toMillis(1) < System.currentTimeMillis()) {
        log.info("removing old task {}", entry.getKey());
        _stateMap.remove(entry.getKey());
      }
    }
  }

  @Override
  public List<Task> getItems() {
    cleanupOldJobs();
    Collection<TaskState> values = _stateMap.values();
    List<Task> tasks = new ArrayList<>();
    for (TaskState state : values) {
      tasks.add(state.getTask()
                     .toBuilder()
                     .taskStatus(state.getTaskStatus())
                     .build());
    }
    return new ArrayList<>(tasks);
  }

  public double getTaskProgress(String id) {
    TaskState taskState = _stateMap.get(id);
    if (taskState == null) {
      return 0;
    }
    return taskState.getProgress();
  }

  public void cancelTasks(Collection<Task> tasks) {
    for (Task task : tasks) {
      TaskState taskState = _stateMap.get(task.getId());
      Future<?> future = taskState.getFuture();
      if (future != null) {
        future.cancel(true);
      }
      taskState.getCanceled()
               .set(true);
      taskState.setTaskStatus(TaskStatus.CANCELING);
    }
  }

  @Override
  protected boolean doUpdateData() {
    return true;
  }

  @Override
  protected long getUpdatePeriod() {
    return -1;
  }

}
