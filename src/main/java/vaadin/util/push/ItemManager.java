package vaadin.util.push;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import vaadin.util.filter.Filter;
import vaadin.util.filter.FilterInfo;

@Slf4j
public abstract class ItemManager<ITEM> {

  public static <T> void updateCollection(Collection<T> collection, Collection<T> deltaCollection) {
    collection.addAll(deltaCollection);
    collection.retainAll(deltaCollection);
  }

  public static <T> Set<T> newSet() {
    return Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  private final Class<ITEM> _itemClass;
  private final FilterInfo<ITEM> _filterInfo;
  private final Timer _timer;

  protected ItemManager() {
    _itemClass = ClassHelper.getItemClass(getLookupClass(), getClass());
    _filterInfo = new FilterInfo<>(_itemClass);
    _timer = new Timer(getClass().getName(), true);
    long delay = TimeUnit.SECONDS.toMillis(1);
    long period = getUpdatePeriod();
    if (period > 0) {
      _timer.schedule(new TimerTask() {
        @Override
        public void run() {
          try {
            updateData();
          } catch (Throwable t) {
            log.error("Unknown error", t);
          }
        }
      }, delay, period);
    }
  }

  protected Class<?> getLookupClass() {
    return ItemManager.class;
  }

  protected abstract long getUpdatePeriod();

  public synchronized boolean updateData() {
    boolean result = doUpdateData();
    if (result) {
      updateFilterValues();
    }
    return result;
  }

  protected synchronized void updateFilterValues() {
    _filterInfo.updatePossibleValues(getItems());
  }

  protected abstract boolean doUpdateData();

  public abstract Collection<ITEM> getItems();

  public List<Filter<ITEM>> getFilters(PushComponent pushComponent) {
    return _filterInfo.getFilters(pushComponent);
  }

}
