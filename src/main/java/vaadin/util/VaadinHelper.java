package vaadin.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.InMemoryDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;

import lombok.extern.slf4j.Slf4j;
import vaadin.util.VaadinGridBuilder.ColumnDef;

@Slf4j
public class VaadinHelper {

  private static final String FILTER = "filter";

  public static <T> Column<T> createColumn(Grid<T> grid, String header, ValueProvider<T, ?> valueProvider) {
    return grid.addColumn(valueProvider)
               .setSortable(true)
               .setHeader(header);
  }

  @SuppressWarnings("unchecked")
  public static <T> void addFilters(Grid<T> grid, List<ColumnDef<T, ?>> columnDefs, HeaderRow filterRow,
      SerializablePredicate<T> filter) {
    InMemoryDataProvider<T> dataProvider = (InMemoryDataProvider<T>) grid.getDataProvider();
    List<TextField> textFields = new ArrayList<>();
    for (ColumnDef<T, ?> columnDef : columnDefs) {
      Column<T> column = columnDef.column;
      TextField textField = new TextField();
      textField.addValueChangeListener(event -> {
        log.debug("Building new predicate and setting data provider filter");
        SerializablePredicate<T> predicate = buildPredicate(textFields, columnDefs);
        if (filter != null) {
          dataProvider.setFilter(t -> filter.test(t) && predicate.test(t));
        } else {
          dataProvider.setFilter(predicate);
        }
      });
      textField.setValueChangeMode(ValueChangeMode.LAZY);
      filterRow.getCell(column)
               .setComponent(textField);
      textField.setSizeFull();
      textField.setPlaceholder(FILTER);
      textFields.add(textField);
    }
  }

  private static <T> SerializablePredicate<T> buildPredicate(List<TextField> textFields,
      List<ColumnDef<T, ?>> columnDefs) {
    List<ExecuteFilter<T>> executeFilters = new ArrayList<>();
    for (int i = 0; i < textFields.size(); i++) {
      TextField textField = textFields.get(i);
      String filterValue = textField.getValue();
      if (filterValue != null && !filterValue.isEmpty()) {
        ColumnDef<T, ?> columnDef = columnDefs.get(i);
        Pattern pattern = Pattern.compile(filterValue);
        ExecuteFilter<T> executeFilter = new ExecuteFilter<T>() {

          final ValueProvider<T, ?> _valueProvider = columnDef.valueProvider;

          @Override
          public boolean execute(T t) {
            Object value = _valueProvider.apply(t);
            if (value == null) {
              return false;
            }
            return pattern.matcher(value.toString())
                          .find();
          }
        };
        executeFilters.add(executeFilter);
      }
    }

    return t -> {
      for (ExecuteFilter<T> executeFilter : executeFilters) {
        if (!executeFilter.execute(t)) {
          return false;
        }
      }
      return true;
    };
  }

  private interface ExecuteFilter<T> {
    boolean execute(T t);
  }

}
