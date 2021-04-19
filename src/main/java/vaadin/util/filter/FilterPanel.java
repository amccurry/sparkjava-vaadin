package vaadin.util.filter;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.function.SerializablePredicate;

public class FilterPanel<ITEM> extends Div implements SerializablePredicate<ITEM> {

  private static final long serialVersionUID = 2143725702945792488L;

  private final List<SerializablePredicate<ITEM>> _predicates = new ArrayList<>();

  public FilterPanel(List<Filter<ITEM>> filters) {
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
      _predicates.add(filter.getPredicate());
      MultiSelectListBox<String> listBox = new MultiSelectListBox<>();
      listBox.addSelectionListener(filter.getListener());
      FilterOptions options = filter.getOptions();
      listBox.setItems(options.getOptions());
      Details details = new Details(filter.getName(), listBox);
      details.addOpenedChangeListener(event -> listBox.setItems(options.getOptions()));
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

}
