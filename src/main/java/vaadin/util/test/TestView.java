package vaadin.util.test;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.vaadin.flow.component.grid.Grid;

import vaadin.util.action.Action;
import vaadin.util.push.BasePushView;
import vaadin.util.push.GridBuilder;

public class TestView extends BasePushView<TestItem> {

  private static final long serialVersionUID = -362315035363471960L;

  private static final TestManager TEST_MANAGER = TestManager.INSTANCE;

  public TestView() {
    super(TEST_MANAGER);
  }

  @Override
  protected List<Action> getActions() {
    Builder<Action> builder = ImmutableList.<Action>builder();
    builder.add(Action.builder()
                      .listener(event -> deleteItems())
                      .name("Delete")
                      .build());
    return builder.build();
  }

  @Override
  protected Grid<TestItem> createGrid(GridBuilder<TestItem> builder) {
    return builder.add("Name", TestItem::getName)
                  .add("Id", TestItem::getId)
                  .add("Value", TestItem::getValue)
                  .add("Test", TestItem::getTestEnum)
                  .add("Id1", TestItem::getId)
                  .add("Id2", TestItem::getId)
                  .add("Id3", TestItem::getId)
                  .add("Id4", TestItem::getId)
                  .add("Id5", TestItem::getId)
                  .build();
  }

  private void deleteItems() {
    Set<TestItem> items = getSelectedItems();
    TEST_MANAGER.delete(items);
  }
}
