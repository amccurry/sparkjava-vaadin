package vaadin.util;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.InMemoryDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;

public class VaadinHelper {

  private static final String FILTER = "Filter";

  public static <T> Column<T> createColumn(Grid<T> grid, String header, ValueProvider<T, ?> valueProvider) {
    return grid.addColumn(valueProvider)
               .setSortable(true)
               .setHeader(header);
  }

  public static <T> TextField addStringFilter(Grid<T> grid, Column<T> column, HeaderRow filterRow,
      ValueProvider<T, ?> valueProvider) {
    return addFilter(grid, column, filterRow, valueProvider,
        (filterValue, value) -> StringUtils.containsIgnoreCase(value.toString(), filterValue));
  }

  @SuppressWarnings("unchecked")
  public static <T, COLUMN_VALUE> TextField addFilter(Grid<T> grid, Column<T> column, HeaderRow filterRow,
      ValueProvider<T, ?> valueProvider, ColumnFilter<COLUMN_VALUE> columnFilter) {
    TextField textField = new TextField();
    textField.addValueChangeListener(event -> {
      InMemoryDataProvider<T> dataProvider = (InMemoryDataProvider<T>) grid.getDataProvider();
      dataProvider.addFilter(t -> {
        Object value = valueProvider.apply(t);
        if (value == null) {
          return false;
        }
        return columnFilter.include(textField.getValue(), (COLUMN_VALUE) value);
      });
    });
    textField.setValueChangeMode(ValueChangeMode.EAGER);
    filterRow.getCell(column)
             .setComponent(textField);
    textField.setSizeFull();
    textField.setPlaceholder(FILTER);
    return textField;
  }

  public interface ColumnFilter<T> {
    boolean include(String filterValue, T value);
  }
}
