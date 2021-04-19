package vaadin.util.task;

public interface TaskRunnable {

  void call(TaskCanceled taskCanceled, TaskProgress taskProgress) throws Exception;

}
