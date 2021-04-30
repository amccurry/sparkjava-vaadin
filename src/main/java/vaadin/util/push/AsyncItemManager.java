package vaadin.util.push;

import java.util.concurrent.atomic.AtomicBoolean;

import vaadin.util.task.Task;
import vaadin.util.task.TaskManager;
import vaadin.util.task.TaskRunnable;

public abstract class AsyncItemManager<ITEM> extends ItemManager<ITEM> {

  private final AtomicBoolean _runningUpdate = new AtomicBoolean();

  @Override
  protected boolean doUpdateData() {
    if (_runningUpdate.get()) {
      return false;
    }
    Task task = getTask();
    TaskManager.INSTANCE.submitTask(getTask().toBuilder()
                                             .taskRunnable(wrapRunnable(task.getTaskRunnable()))
                                             .build());
    return true;
  }

  private TaskRunnable wrapRunnable(TaskRunnable taskRunnable) {
    return (taskCanceled, taskProgress) -> {
      try {
        _runningUpdate.set(true);
        taskRunnable.call(taskCanceled, taskProgress);
        updateFilterValues();
      } finally {
        _runningUpdate.set(false);
      }
    };
  }
  
  protected Class<?> getLookupClass() {
    return AsyncItemManager.class;
  }

  protected abstract Task getTask();

}
