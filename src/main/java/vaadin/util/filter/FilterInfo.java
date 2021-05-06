package vaadin.util.filter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.vaadin.flow.component.UIDetachedException;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import vaadin.util.push.LambdaAccessor;
import vaadin.util.push.PushComponent;

@Slf4j
public class FilterInfo<ITEM> {

  private final Map<String, LambdaAccessor<ITEM, ?>> _lambdaAccessorMap;
  private final Map<String, Set<String>> _uniqueValueMap = new ConcurrentHashMap<>();
  private final Set<PushComponent> _pushCache;

  public FilterInfo(Class<ITEM> clazz) {
    _pushCache = Collections.newSetFromMap(new MapMaker().weakKeys()
                                                         .weakValues()
                                                         .makeMap());
    Map<String, LambdaAccessor<ITEM, ?>> lambdaAccessorMap = new HashMap<>();
    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
      Filterable filterable = field.getAnnotation(Filterable.class);
      if (filterable != null) {
        String getterMethod = findGetterMethod(field);
        Method method = findMethod(clazz, getterMethod);
        if (method == null) {
          log.error("Getter method for field {} not found", field.getName());
        } else {
          addLambdaAccessor(clazz, lambdaAccessorMap, filterable, method);
        }
      }
    }
    for (Method method : clazz.getDeclaredMethods()) {
      Filterable filterable = method.getAnnotation(Filterable.class);
      if (filterable != null) {
        addLambdaAccessor(clazz, lambdaAccessorMap, filterable, method);
      }
    }
    _lambdaAccessorMap = ImmutableMap.copyOf(lambdaAccessorMap);
  }

  private void addLambdaAccessor(Class<ITEM> clazz, Map<String, LambdaAccessor<ITEM, ?>> lambdaAccessorMap,
      Filterable filterable, Method method) {
    lambdaAccessorMap.put(filterable.value(), new LambdaAccessor<>(clazz, method.getName(), method.getReturnType()));
  }

  public List<Filter<ITEM>> getFilters(PushComponent... pushComponents) {
    List<String> filterNames = new ArrayList<>(_lambdaAccessorMap.keySet());
    if (filterNames.isEmpty()) {
      return ImmutableList.of();
    }
    _pushCache.addAll(Arrays.asList(pushComponents));
    Collections.sort(filterNames);
    Builder<Filter<ITEM>> builder = ImmutableList.builder();
    for (String filterName : filterNames) {
      LambdaAccessor<ITEM, ?> accessor = _lambdaAccessorMap.get(filterName);
      Set<String> selectedItems = new HashSet<>();
      builder.add(Filter.<ITEM>builder()
                        .name(filterName)
                        .listener(event -> {
                          Set<String> allSelectedItems = event.getAllSelectedItems();
                          if (allSelectedItems == null) {
                            selectedItems.clear();
                          }
                          selectedItems.addAll(allSelectedItems);
                          selectedItems.retainAll(allSelectedItems);
                          for (PushComponent pushComponent : pushComponents) {
                            pushComponent.push();
                          }
                        })
                        .options(() -> getOptions(filterName))
                        .predicate(t -> {
                          if (selectedItems.isEmpty()) {
                            return true;
                          } else if (t == null) {
                            return false;
                          } else {
                            return selectedItems.contains(accessor.getValue(t));
                          }
                        })
                        .build());
    }
    return builder.build();
  }

  private List<String> getOptions(String filterName) {
    Set<String> options = _uniqueValueMap.get(filterName);
    if (options == null) {
      return ImmutableList.of();
    }
    return sort(options);
  }

  private List<String> sort(Set<String> set) {
    List<String> result = new ArrayList<>(set);
    Collections.sort(result);
    return result;
  }

  public void updatePossibleValues(Collection<ITEM> items) {
    boolean change = false;
    for (Entry<String, LambdaAccessor<ITEM, ?>> entry : _lambdaAccessorMap.entrySet()) {
      String name = entry.getKey();
      LambdaAccessor<ITEM, ?> lambdaAccessor = entry.getValue();
      Set<String> values = getValues(lambdaAccessor, items);
      Set<String> currentValues = _uniqueValueMap.get(name);
      if (currentValues == null) {
        _uniqueValueMap.put(name, currentValues = newSet());
      }
      if (currentValues.addAll(values)) {
        change = true;
      }
      if (currentValues.retainAll(values)) {
        change = true;
      }
    }
    if (change) {
      pushFilterValues();
    }
  }

  private synchronized void pushFilterValues() {
    for (PushComponent pushComponent : ImmutableSet.copyOf(_pushCache)) {
      doPush(pushComponent);
    }
  }

  private void doPush(PushComponent pushComponent) {
    if (pushComponent != null) {
      try {
        log.debug("push {}", pushComponent);
        pushComponent.push();
      } catch (Throwable t) {
        if (t instanceof UIDetachedException) {
          _pushCache.remove(pushComponent);
        } else {
          log.error("Unknown error while trying to push {}", t);
        }
      }
    }
  }

  private Set<String> newSet() {
    return Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  private Set<String> getValues(LambdaAccessor<ITEM, ?> lambdaAccessor, Collection<ITEM> items) {
    Set<String> values = new HashSet<>();
    for (ITEM item : items) {
      Object object = lambdaAccessor.getValue(item);
      if (object != null) {
        values.add(object.toString());
      }
    }
    return values;
  }

  @SneakyThrows
  private Method findMethod(Class<ITEM> clazz, String getterMethod) {
    try {
      return clazz.getDeclaredMethod(getterMethod);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private String findGetterMethod(Field field) {
    Class<?> clazz = field.getType();
    String name = field.getName();
    name = name.substring(0, 1)
               .toUpperCase()
        + name.substring(1);
    if (clazz == Boolean.TYPE) {
      return "is" + name;
    }
    return "get" + name;
  }

}
