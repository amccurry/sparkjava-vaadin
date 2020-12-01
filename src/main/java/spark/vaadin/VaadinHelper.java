package spark.vaadin;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.Grid.Column;
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

  public static <T> TextField addFilter(InMemoryDataProvider<T> dataProvider, Column<T> column, HeaderRow filterRow,
      ValueProvider<T, String> valueProvider) {
    TextField textField = new TextField();
    textField.addValueChangeListener(event -> {
      dataProvider.addFilter(
          volume -> StringUtils.containsIgnoreCase(valueProvider.apply(volume), textField.getValue()));
    });
    textField.setValueChangeMode(ValueChangeMode.EAGER);
    filterRow.getCell(column)
             .setComponent(textField);
    textField.setSizeFull();
    textField.setPlaceholder(FILTER);
    return textField;
  }
}
