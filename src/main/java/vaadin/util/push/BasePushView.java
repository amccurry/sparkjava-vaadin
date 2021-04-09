package vaadin.util.push;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.function.SerializablePredicate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BasePushView<ITEM, ACTION> extends Div implements PushView {

  private static final long serialVersionUID = 9064915736318224076L;
  protected final AtomicReference<UI> _uiRef = new AtomicReference<>();
  protected final ListDataProvider<ITEM> _dataProvider;
  protected final Grid<ITEM> _grid;
  protected final Select<ACTION> _actions;

  public BasePushView() {
    _dataProvider = DataProvider.ofCollection(getDataItems());
    _grid = createGrid(_dataProvider);
    _grid.setSelectionMode(Grid.SelectionMode.MULTI);
    _grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

    List<ACTION> actions = getActions();
    if (!actions.isEmpty()) {
      _actions = new Select<>();
      _actions.setItems(actions);
      _actions.setItemLabelGenerator(actionLabels());
      _actions.setItemEnabledProvider(actionsEnabled());
      ValueChangeListener<? super ComponentValueChangeEvent<Select<ACTION>, ACTION>> actionListener = actionListener();
      _actions.addValueChangeListener(event -> {
        actionListener.valueChanged(event);
        // clear after action processed
        _actions.clear();
      });

      Div div = new Div();
      div.add(new Icon(VaadinIcon.CHEVRON_CIRCLE_RIGHT_O));
      div.add(new Text(" "));
      div.add(_actions);
      add(div);
    } else {
      _actions = null;
    }

    _grid.addItemClickListener(onRowClickSelectOrDeselect());
    add(_grid);
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    log.debug("attach");
    _uiRef.set(attachEvent.getUI());
    PushManager.INSTANCE.register(this);
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    log.debug("detach");
    PushManager.INSTANCE.register(this);
  }

  @Override
  public synchronized void push() {
    ForkJoinPool.commonPool()
                .submit(new Callable<Void>() {
                  @Override
                  public Void call() throws Exception {
                    doPush(_uiRef.get());
                    return null;
                  }
                });
  }

  protected void doPush(UI ui) {
    List<ITEM> dataItems = getDataItems();
    ui.access(() -> {
      ListDataProvider<ITEM> listDataProvider = _dataProvider;
      Collection<ITEM> items = listDataProvider.getItems();
      items.clear();
      items.addAll(dataItems);
      listDataProvider.refreshAll();
      ui.push();
    });
  }

  protected ComponentEventListener<ItemClickEvent<ITEM>> onRowClickSelectOrDeselect() {
    return event -> {
      ITEM item = event.getItem();
      if (_grid.getSelectedItems()
               .contains(item)) {
        _grid.deselect(item);
      } else {
        _grid.select(item);
      }
    };
  }

  protected abstract ItemLabelGenerator<ACTION> actionLabels();

  protected abstract ValueChangeListener<? super ComponentValueChangeEvent<Select<ACTION>, ACTION>> actionListener();

  protected abstract SerializablePredicate<ACTION> actionsEnabled();

  protected abstract List<ACTION> getActions();

  protected abstract Grid<ITEM> createGrid(ListDataProvider<ITEM> dataProvider);

  protected abstract List<ITEM> getDataItems();

}
