package vaadin.util.task;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.progressbar.ProgressBar;

import lombok.extern.slf4j.Slf4j;
import vaadin.util.action.Action;
import vaadin.util.push.BasePushView;
import vaadin.util.push.GridBuilder;

@Slf4j
public abstract class BaseTaskView extends BasePushView<Task> {

  private static final long serialVersionUID = -7101033781004158927L;

  private static final TaskManager TASK_MANAGER = TaskManager.INSTANCE;

  public BaseTaskView() {
    super(TASK_MANAGER);
  }

  @Override
  protected List<Action> getActions() {
    Action test = Action.builder()
                        .name("Test Task")
                        .listener(event -> {
                          TaskRunnable taskRunnable = (cancelled, actionProgress) -> {
                            // && !cancelled.isCanceled()
                            for (int i = 0; i < 50; i++) {
                              log.debug("{}", i);
                              actionProgress.setProgress(i / (double) 50);
                              if (cancelled.isCanceled()) {
                                Thread.interrupted();
                                Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                                log.error("interrupted");
                                return;
                              }
                              try {
                                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                              } catch (InterruptedException e) {
                                Thread.interrupted();
                                Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                                log.error("interrupted");
                                return;

                              }
                            }
                            throw new RuntimeException("error");
                          };

                          String id = UUID.randomUUID()
                                          .toString();

                          Task task = Task.builder()
                                          .name("Test " + System.currentTimeMillis())
                                          .description("Description " + id)
                                          .taskRunnable(taskRunnable)
                                          .user(getCurrentUser())
                                          .build();

                          log.info("user {} {}", getCurrentUser(), task.getUser());

                          TASK_MANAGER.submitTask(task);
                        })
                        .actionEnabled(() -> true)
                        .build();

    Action error = Action.builder()
                         .name("Error Task")
                         .listener(event -> {
                           throw new RuntimeException("error");
                         })
                         .actionEnabled(() -> true)
                         .build();

    Action cancel = Action.builder()
                          .name("Cancel")
                          .listener(event -> TASK_MANAGER.cancelTasks(getSelectedItems()))
                          .actionEnabled(() -> allSelectedAreRunning())
                          .build();

    if (TestMode.isTesting()) {
      return Arrays.asList(cancel, test, error);
    } else {
      return Arrays.asList(cancel);
    }
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    TASK_MANAGER.register(this);
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    super.onDetach(detachEvent);
    TASK_MANAGER.deregister(this);
  }

  protected abstract String getCurrentUser();

  private boolean allSelectedAreRunning() {
    Set<Task> selectedItems = getSelectedItems();
    for (Task task : selectedItems) {
      if (task.getTaskStatus() != TaskStatus.RUNNING) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected Grid<Task> createGrid(GridBuilder<Task> builder) {
    return builder.addComponentColumn("Progress", source -> {
      ProgressBar progressBar = new ProgressBar();
      progressBar.setValue(TASK_MANAGER.getTaskProgress(source.getId()));
      TaskStatus taskStatus = source.getTaskStatus();
      if (taskStatus == TaskStatus.RUNNING || taskStatus == TaskStatus.CANCELING) {
        progressBar.setVisible(true);
      } else {
        progressBar.setVisible(false);
      }
      return progressBar;
    })
                  .add("Name", Task::getName)
                  .add("Description", Task::getDescription)
                  .add("User", Task::getUser)
                  .addDate("Created", Task::getCreated)
                  .add("Status", Task::getTaskStatus)
                  .add("Id", Task::getId)
                  .build();
  }
}
