package vaadin.util.push;

import java.util.Arrays;
import java.util.List;

import vaadin.util.action.Action;

public abstract class BasePushViewNoActions<ITEM> extends BasePushView<ITEM> {

  private static final long serialVersionUID = -6721924595284957595L;

  public BasePushViewNoActions() {

  }

  protected final boolean hasActions() {
    return false;
  }

  @Override
  protected final List<Action> getActions() {
    return Arrays.asList();
  }

}
