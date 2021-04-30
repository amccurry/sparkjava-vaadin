package vaadin.util.push;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import vaadin.util.test.TestItem;

@Slf4j
public class SearchableType<TYPE> {

  public static void main(String[] args) {
    SearchableType<TestItem> type = new SearchableType<>(TestItem.class);
    TestItem testItem = TestItem.builder()
                                .name("test")
                                .build();

    System.out.println(type.getSearchString(testItem));

  }

  private final List<LambdaAccessor<TYPE, ?>> _lambdaAccessors = new ArrayList<>();

  public SearchableType(Class<TYPE> clazz) {
    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
      Searchable searchable = field.getAnnotation(Searchable.class);
      if (searchable != null) {
        String getterMethod = findGetterMethod(field);
        Method method = findMethod(clazz, getterMethod);
        if (method == null) {
          log.error("Getter method for field {} not found", field.getName());
        } else {
          _lambdaAccessors.add(new LambdaAccessor<>(clazz, method.getName(), method.getReturnType()));
        }
      }
    }
    Method[] methods = clazz.getDeclaredMethods();
    for (Method method : methods) {
      Searchable searchable = method.getAnnotation(Searchable.class);
      if (searchable != null) {
        _lambdaAccessors.add(new LambdaAccessor<>(clazz, method.getName(), method.getReturnType()));
      }
    }
  }

  public String getSearchString(TYPE t) {
    StringBuilder builder = new StringBuilder();
    for (LambdaAccessor<TYPE, ?> lambdaAccessor : _lambdaAccessors) {
      Object object = lambdaAccessor.getValue(t);
      if (object != null) {
        builder.append(object.toString())
               .append(' ');
      }
    }
    return builder.toString();
  }

  @SneakyThrows
  private Method findMethod(Class<TYPE> clazz, String getterMethod) {
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
