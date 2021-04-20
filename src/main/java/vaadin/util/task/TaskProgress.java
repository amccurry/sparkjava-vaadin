package vaadin.util.task;

public interface TaskProgress {

  public static TaskProgress scale(TaskProgress taskProgress, double scale) {
    return progress -> taskProgress.setProgress(progress * scale);
  }

  public static TaskProgress scaleWithOffset(TaskProgress taskProgress, double scale, double offset) {
    return progress -> taskProgress.setProgress((progress * scale) + offset);
  }

  void setProgress(double progress);

  default void completed() {
    setProgress(1.0);
  }

}
