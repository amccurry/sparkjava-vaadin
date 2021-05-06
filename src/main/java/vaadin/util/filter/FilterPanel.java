package vaadin.util.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.function.SerializablePredicate;

import lombok.extern.slf4j.Slf4j;
import vaadin.util.push.ItemManager;
import vaadin.util.push.PushComponent;
import vaadin.util.push.PushManager;

@Slf4j
public class FilterPanel<ITEM> extends Div implements SerializablePredicate<ITEM>, PushComponent {

  private static final long serialVersionUID = 2143725702945792488L;

  private final List<SerializablePredicate<ITEM>> _predicates = new ArrayList<>();
  private final AtomicReference<UI> _uiRef = new AtomicReference<>();
  private final boolean _empty;
  private final List<Filter<ITEM>> _filters;
  private final List<Details> _details = new ArrayList<>();
  private final List<MultiSelectListBox<String>> _listBoxes = new ArrayList<>();
  private final List<List<String>> _optionsLists = new ArrayList<>();

  public FilterPanel(ItemManager<ITEM> itemManager, PushComponent pushComponent) {
    Style style = getStyle();
    style.set("background-color", "#F8F8F8");
    style.set("min-height", "20px");
    style.set("padding", "19px");
    style.set("margin-bottom", "20px");
    style.set("border", "1px solid #DCDCDC");
    style.set("border-radius", "2px");
    style.set("box-shadow", "inset 0 1px 1px rgb(0 0 0 / 5%)");

    add(new Label("Filters"));
    _filters = itemManager.getFilters(this, pushComponent);
    if (_filters.isEmpty()) {
      _empty = true;
    } else {
      _empty = false;
    }
    for (Filter<ITEM> filter : _filters) {
      _predicates.add(filter.getPredicate());
      FilterOptions options = filter.getOptions();
      List<String> optionsList = options.getOptions();
      _optionsLists.add(optionsList);
      MultiSelectListBox<String> listBox = new MultiSelectListBox<>();
      Details details = new Details(filter.getName() + " (" + optionsList.size() + ")", listBox);
      _details.add(details);
      listBox.addSelectionListener(filter.getListener());
      listBox.setItems(optionsList);
      _listBoxes.add(listBox);
      add(details);
    }
  }

  @Override
  public boolean test(ITEM t) {
    for (SerializablePredicate<ITEM> predicate : _predicates) {
      if (!predicate.test(t)) {
        return false;
      }
    }
    return true;
  }

  public boolean isEmpty() {
    return _empty;
  }

  @Override
  public void push() {
    UI ui = _uiRef.get();
    if (ui != null) {
      if (_empty) {
        return;
      }
      ui.access(() -> {
        for (int i = 0; i < _filters.size(); i++) {
          Filter<ITEM> filter = _filters.get(i);
          Details details = _details.get(i);
          FilterOptions options = filter.getOptions();
          List<String> newOptions = options.getOptions();
          List<String> currentOptions = _optionsLists.get(i);
          if (!currentOptions.equals(newOptions)) {
            details.setSummaryText(filter.getName() + " (" + newOptions.size() + ")");
            MultiSelectListBox<String> listBox = _listBoxes.get(i);
            Set<String> selectedItems = new HashSet<>(listBox.getSelectedItems());
            selectedItems.retainAll(newOptions);
            listBox.setItems(newOptions);
            listBox.select(selectedItems);
            _optionsLists.set(i, newOptions);
          }
        }
        ui.push();
      });
    }
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
    _uiRef.set(null);
    PushManager.INSTANCE.deregister(this);
  }

}
