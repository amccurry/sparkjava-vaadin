package vaadin.util.push;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.InMemoryDataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.internal.HasUrlParameterFormat;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GridBuilder<T> {

  private static final String YYYY_MM_DD_HH_MM_SS = "yyyy'-'MM'-'dd' 'HH':'mm':'ss";
  private static final String FILTER = "filter";

  private final Grid<T> _grid;
  private final List<ColumnDef<T, ?>> _columnDefs = new ArrayList<>();
  private SerializablePredicate<T> _filter;

  private GridBuilder(Grid<T> grid, InMemoryDataProvider<T> dataProvider) {
    _grid = grid;
  }

  public static <T> GridBuilder<T> create(List<T> list) {
    ListDataProvider<T> dataProvider = DataProvider.ofCollection(list);
    Grid<T> grid = new Grid<>();
    grid.setHeight("90vh");
    grid.setItems(dataProvider);
    return create(grid, dataProvider);
  }

  public static <T> GridBuilder<T> create(ListDataProvider<T> dataProvider) {
    Grid<T> grid = new Grid<>();
    grid.setHeight("90vh");
    grid.setItems(dataProvider);
    return create(grid, dataProvider);
  }

  public static <T> GridBuilder<T> create(Grid<T> grid, InMemoryDataProvider<T> dataProvider) {
    return new GridBuilder<>(grid, dataProvider);
  }

  public <N extends Component> GridBuilder<T> addDate(String header, ValueProvider<T, Date> valueProvider) {
    ColumnDef<T, N> columnDef = new ColumnDef<T, N>();
    columnDef.header = header;
    columnDef.valueProvider = formatDate(valueProvider);
    _columnDefs.add(columnDef);
    return this;
  }

  public <N extends Component> GridBuilder<T> add(String header, ValueProvider<T, ?> valueProvider) {
    ColumnDef<T, N> columnDef = new ColumnDef<T, N>();
    columnDef.header = header;
    columnDef.valueProvider = valueProvider;
    _columnDefs.add(columnDef);
    return this;
  }

  public <N extends Component, C extends Component> GridBuilder<T> addComponentColumn(String header,
      ValueProvider<T, C> valueProvider) {
    ColumnDef<T, N> columnDef = new ColumnDef<T, N>();
    columnDef.header = header;
    columnDef.valueProvider = valueProvider;
    columnDef.componentColumn = true;
    _columnDefs.add(columnDef);
    return this;
  }

  private ValueProvider<T, String> formatDate(ValueProvider<T, Date> valueProvider) {
    return new ValueProvider<T, String>() {
      private static final long serialVersionUID = 2438298511206864923L;
      private final SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);

      @Override
      public String apply(T source) {
        Date date = valueProvider.apply(source);
        if (date == null) {
          return null;
        }
        return format.format(date);
      }
    };
  }

  public <C extends Component> GridBuilder<T> add(String header, ValueProvider<T, ?> valueProvider,
      Class<? extends C> navigationTarget) {
    if (!HasUrlParameter.class.isAssignableFrom(navigationTarget)) {
      throw new RuntimeException("Class " + navigationTarget + " does not implement " + HasUrlParameter.class);
    }
    ColumnDef<T, C> columnDef = new ColumnDef<T, C>();
    columnDef.header = header;
    columnDef.valueProvider = valueProvider;
    columnDef.navigationTarget = navigationTarget;
    _columnDefs.add(columnDef);
    return this;
  }

  public <C extends Component> GridBuilder<T> withFilter(SerializablePredicate<T> filter) {
    _filter = filter;
    return this;
  }

  @SuppressWarnings("unchecked")
  public Grid<T> build() {
    for (ColumnDef<T, ?> columnDef : _columnDefs) {
      if (columnDef.componentColumn) {
        columnDef.column = createComponentColumn(_grid, columnDef.header, cast(columnDef.valueProvider));
      } else {
        columnDef.column = createColumn(_grid, columnDef.header, columnDef.valueProvider);
      }
      columnDef.column.setAutoWidth(true);
      columnDef.column.setResizable(true);
      columnDef.column.setKey(columnDef.header);
    }
    HeaderRow filterRow = _grid.appendHeaderRow();
    addFilters(_grid, _columnDefs, filterRow, _filter);
    ComponentEventListener<ItemClickEvent<T>> listener = event -> {
      Column<T> column = event.getColumn();
      if (column == null) {
        return;
      }
      T item = event.getItem();
      String key = column.getKey();
      for (ColumnDef<T, ?> columnDef : _columnDefs) {
        if (columnDef.header.equals(key) && columnDef.navigationTarget != null) {
          Object o = columnDef.valueProvider.apply(item);
          if (o != null) {
            UI.getCurrent()
              .navigate(columnDef.navigationTarget, HasUrlParameterFormat.getParameters(o.toString()));
          }
        }
      }
    };
    if (_filter != null) {
      InMemoryDataProvider<T> dataProvider = (InMemoryDataProvider<T>) _grid.getDataProvider();
      dataProvider.setFilter(_filter);
    }
    _grid.addItemClickListener(listener);
    return _grid;
  }

  @SuppressWarnings("unchecked")
  private <V extends Component> ValueProvider<T, V> cast(ValueProvider<T, ?> valueProvider) {
    return (ValueProvider<T, V>) valueProvider;
  }

  public static class ColumnDef<T, N extends Component> {
    public boolean componentColumn;
    String header;
    ValueProvider<T, ?> valueProvider;
    Column<T> column;
    Class<? extends N> navigationTarget;
  }

  public static <T> Column<T> createColumn(Grid<T> grid, String header, ValueProvider<T, ?> valueProvider) {
    return grid.addColumn(valueProvider)
               .setSortable(true)
               .setHeader(header);
  }

  public static <T, V extends Component> Column<T> createComponentColumn(Grid<T> grid, String header,
      ValueProvider<T, V> valueProvider) {
    return grid.addComponentColumn(valueProvider)
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