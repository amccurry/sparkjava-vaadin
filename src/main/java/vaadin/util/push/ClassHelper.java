package vaadin.util.push;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ClassHelper {

  @SuppressWarnings("unchecked")
  public static <T> Class<T> getItemClass(Class<?> baseClass, Class<?> cls) {
    Class<?> clazz = getClassRequiredForTypeLookup(baseClass, cls);
    Type genericSuperclass = clazz.getGenericSuperclass();
    ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
    return (Class<T>) parameterizedType.getActualTypeArguments()[0];
  }

  private static Class<?> getClassRequiredForTypeLookup(Class<?> baseClass, Class<?> cls) {
    if (cls.getSuperclass()
           .equals(baseClass)) {
      return cls;
    } else {
      return getClassRequiredForTypeLookup(baseClass, cls.getSuperclass());
    }
  }
}
