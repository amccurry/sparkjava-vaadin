package vaadin.util.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import vaadin.util.filter.Filter;
import vaadin.util.filter.FilterInfo;
import vaadin.util.push.ItemManager;
import vaadin.util.push.PushComponent;

public class TestManager extends ItemManager<TestItem> {

  public static final TestManager INSTANCE = new TestManager();

  private final AtomicReference<List<TestItem>> _itemsRef = new AtomicReference<>();
  private final FilterInfo<TestItem> _filterInfo;

  private TestManager() {
    Random random = new Random();
    TestEnum[] values = TestEnum.values();
    List<TestItem> items = new ArrayList<>();
    for (int i = 0; i < 100000; i++) {
      items.add(TestItem.builder()
                        .testEnum(values[random.nextInt(values.length)])
                        .build());
      UUID.randomUUID()
          .toString();
    }
    _itemsRef.set(items);
    _filterInfo = new FilterInfo<>(TestItem.class);
    _filterInfo.updatePossibleValues(items);
  }

  @Override
  public Collection<TestItem> getItems() {
    return _itemsRef.get();
  }

  public void delete(Collection<TestItem> items) {
    _itemsRef.get()
             .removeAll(items);
  }

  public List<Filter<TestItem>> getFilters(PushComponent pushComponent) {
    return _filterInfo.getFilters(pushComponent);
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
