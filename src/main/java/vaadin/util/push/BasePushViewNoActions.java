package vaadin.util.push;

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.function.SerializablePredicate;

public abstract class BasePushViewNoActions<ITEM> extends BasePushView<ITEM, Void> {

  private static final long serialVersionUID = -6721924595284957595L;

  public BasePushViewNoActions() {

  }

  @Override

  protected final ItemLabelGenerator<Void> actionLabels() {
    throw new RuntimeException("Not Implemented");
  }

  @Override
  protected final ValueChangeListener<? super ComponentValueChangeEvent<Select<Void>, Void>> actionListener() {
    throw new RuntimeException("Not Implemented");
  }

  @Override
  protected final SerializablePredicate<Void> actionsEnabled() {
    throw new RuntimeException("Not Implemented");
  }

  @Override
  protected final List<Void> getActions() {
    return Arrays.asList();
  }

}
