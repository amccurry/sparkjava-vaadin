package vaadin.util.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableList;
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

@Slf4j
public class FilterPanel<ITEM> extends Div implements SerializablePredicate<ITEM>, PushComponent {

  private static final long serialVersionUID = 2143725702945792488L;

  private final AtomicReference<UI> _uiRef = new AtomicReference<>();
  private final List<Filter<ITEM>> _filters;
  private final List<Details> _details = new ArrayList<>();
  private final List<List<String>> _currentOptions = new ArrayList<>();

  public FilterPanel(ItemManager<ITEM> itemManager) {
    List<Filter<ITEM>> filters = itemManager.getFilters(this);
    _filters = filters;
    if (_filters.isEmpty()) {
      return;
    }
    Style style = getStyle();
    style.set("background-color", "#F8F8F8");
    style.set("min-height", "20px");
    style.set("padding", "19px");
    style.set("margin-bottom", "20px");
    style.set("border", "1px solid #DCDCDC");
    style.set("border-radius", "2px");
    style.set("box-shadow", "inset 0 1px 1px rgb(0 0 0 / 5%)");

    add(new Label("Filters"));
    for (Filter<ITEM> filter : filters) {
      MultiSelectListBox<String> listBox = new MultiSelectListBox<>();
      listBox.addSelectionListener(filter.getListener());
      Details details = new Details();
      details.setContent(listBox);
      _currentOptions.add(new ArrayList<>());
      configureDetails(filter, details, ImmutableList.of());
      add(details);
      _details.add(details);
    }
  }

  public boolean isEmpty() {
    return _filters.isEmpty();
  }

  @Override
  public boolean test(ITEM t) {
    for (Filter<ITEM> filter : _filters) {
      if (!filter.getPredicate()
                 .test(t)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    log.debug("attach");
    _uiRef.set(attachEvent.getUI());
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    log.debug("detach");
    _uiRef.set(null);
  }

  @Override
  public void push() {
    UI ui = _uiRef.get();
    if (ui == null) {
      return;
    }
    ui.access(() -> {
      for (int i = 0; i < _filters.size(); i++) {
        Filter<ITEM> filter = _filters.get(i);
        Details details = _details.get(i);
        List<String> currentOptions = _currentOptions.get(i);
        _currentOptions.set(i, configureDetails(filter, details, currentOptions));
      }
      ui.push();
    });
  }

  private List<String> configureDetails(Filter<ITEM> filter, Details details, List<String> currentOptions) {
    FilterOptions options = filter.getOptions();
    List<String> optionsList = options.getOptions();
    if (hasNewValues(optionsList, currentOptions)) {
      details.setSummaryText(filter.getName() + " (" + optionsList.size() + ")");
      MultiSelectListBox<String> listBox = getListBox(details);
      // Set<String> selectedItems = listBox.getSelectedItems();
      listBox.setItems(optionsList);
?????      listBox.addSelectionListener(filter.getListener());
      // listBox.select(selectedItems);
      return optionsList;
    }
    return currentOptions;
  }

  private boolean hasNewValues(List<String> optionsList, List<String> currentOptions) {
    return !optionsList.equals(currentOptions);
  }

  @SuppressWarnings("unchecked")
  private MultiSelectListBox<String> getListBox(Details details) {
    return (MultiSelectListBox<String>) details.getContent()
                                               .findFirst()
                                               .get();
  }

}
