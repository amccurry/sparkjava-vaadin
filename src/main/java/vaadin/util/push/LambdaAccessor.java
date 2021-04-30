package vaadin.util.push;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import com.google.common.base.Function;

import lombok.SneakyThrows;
import vaadin.util.test.TestItem;

public final class LambdaAccessor<TYPE, VALUE> {

  private static final String INVOKED_NAME = "apply";
  private final Function<TYPE, VALUE> _function;

  public static void main(String[] args) {
    LambdaAccessor<TestItem, String> accessor = new LambdaAccessor<>(TestItem.class, "getName", String.class);
    TestItem testItem = TestItem.builder()
                                .name("test")
                                .build();
    System.out.println(accessor.getValue(testItem));
  }

  @SneakyThrows
  public LambdaAccessor(Class<TYPE> clazz, String methodName, Class<?> methodType) {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodType invokedType = MethodType.methodType(Function.class);
    MethodType samMethodType = MethodType.methodType(Object.class, Object.class);
    MethodHandle implMethod = lookup.findVirtual(clazz, methodName, MethodType.methodType(methodType));
    MethodType instantiatedMethodType = MethodType.methodType(methodType, clazz);
    CallSite site = LambdaMetafactory.metafactory(lookup, INVOKED_NAME, invokedType, samMethodType, implMethod,
        instantiatedMethodType);
    _function = (Function<TYPE, VALUE>) site.getTarget()
                                            .invokeExact();
  }

  public VALUE getValue(TYPE bean) {
    return _function.apply(bean);
  }

}