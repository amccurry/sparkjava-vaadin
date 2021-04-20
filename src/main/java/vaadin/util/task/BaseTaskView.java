package vaadin.util.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.data.provider.ListDataProvider;

import lombok.extern.slf4j.Slf4j;
import vaadin.util.VaadinGridBuilder;
import vaadin.util.action.Action;
import vaadin.util.push.BasePushView;

@Slf4j
public abstract class BaseTaskView extends BasePushView<Task> {

  private static final long serialVersionUID = -7101033781004158927L;

  private static final TaskManager TASK_MANAGER = TaskManager.INSTANCE;

  @Override
  protected List<Action> getActions() {
    Action.builder()
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

    Action cancel = Action.builder()
                          .name("Cancel")
                          .listener(event -> TASK_MANAGER.cancelTasks(_grid.getSelectedItems()))
                          .actionEnabled(() -> allSelectedAreRunning())
                          .build();

    return Arrays.asList(cancel);
  }

  protected abstract String getCurrentUser();

  private boolean allSelectedAreRunning() {
    Set<Task> selectedItems = _grid.getSelectedItems();
    for (Task task : selectedItems) {
      if (task.getTaskStatus() != TaskStatus.RUNNING) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected Grid<Task> createGrid(ListDataProvider<Task> dataProvider) {
    VaadinGridBuilder<Task> builder = VaadinGridBuilder.create(dataProvider);
    Grid<Task> grid = builder.add("Name", Task::getName)
                             .add("Description", Task::getDescription)
                             .add("User", Task::getUser)
                             .addDate("Created", Task::getCreated)
                             .add("Status", Task::getTaskStatus)
                             .add("Id", Task::getId)
                             .build();
    grid.addComponentColumn(source -> {
      ProgressBar progressBar = new ProgressBar();
      progressBar.setValue(TASK_MANAGER.getTaskProgress(source.getId()));
      TaskStatus taskStatus = source.getTaskStatus();
      if (taskStatus == TaskStatus.RUNNING || taskStatus == TaskStatus.CANCELING) {
        progressBar.setVisible(true);
      } else {
        progressBar.setVisible(false);
      }
      return progressBar;
    });
    List<Column<Task>> columns = new ArrayList<>(grid.getColumns());
    Column<Task> progressColumn = columns.get(columns.size() - 1);
    List<Column<Task>> newOrder = new ArrayList<>();
    newOrder.add(progressColumn);
    newOrder.addAll(columns.subList(0, columns.size() - 1));
    grid.setColumnOrder(newOrder);
    return grid;
  }

  @Override
  protected List<Task> getDataItems() {
    return TASK_MANAGER.getTasks();
  }
}
