package vaadin.util;

import static vaadin.util.VaadinHelper.createColumn;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.InMemoryDataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.internal.HasUrlParameterFormat;

public class VaadinGridBuilder<T> {

  private static final String YYYY_MM_DD_HH_MM_SS = "YYYY'-'MM'-'dd' 'HH':'mm':'ss";
  private final Grid<T> _grid;
  private final List<ColumnDef<T, ?>> _columnDefs = new ArrayList<>();
  private SerializablePredicate<T> _filter;

  private VaadinGridBuilder(Grid<T> grid, InMemoryDataProvider<T> dataProvider) {
    _grid = grid;
  }

  public static <T> VaadinGridBuilder<T> create(List<T> list) {
    ListDataProvider<T> dataProvider = DataProvider.ofCollection(list);
    Grid<T> grid = new Grid<>();
    grid.setHeight("90vh");
    grid.setItems(dataProvider);
    return create(grid, dataProvider);
  }

  public static <T> VaadinGridBuilder<T> create(ListDataProvider<T> dataProvider) {
    Grid<T> grid = new Grid<>();
    grid.setHeight("90vh");
    grid.setItems(dataProvider);
    return create(grid, dataProvider);
  }

  public static <T> VaadinGridBuilder<T> create(Grid<T> grid, InMemoryDataProvider<T> dataProvider) {
    return new VaadinGridBuilder<>(grid, dataProvider);
  }

  public <C extends Component> VaadinGridBuilder<T> addDate(String header, ValueProvider<T, Date> valueProvider) {
    ColumnDef<T, C> columnDef = new ColumnDef<T, C>();
    columnDef.header = header;
    columnDef.valueProvider = formatDate(valueProvider);
    _columnDefs.add(columnDef);
    return this;
  }

  public <C extends Component> VaadinGridBuilder<T> add(String header, ValueProvider<T, ?> valueProvider) {
    ColumnDef<T, C> columnDef = new ColumnDef<T, C>();
    columnDef.header = header;
    columnDef.valueProvider = valueProvider;
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

  public <C extends Component> VaadinGridBuilder<T> add(String header, ValueProvider<T, ?> valueProvider,
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

  public <C extends Component> VaadinGridBuilder<T> withFilter(SerializablePredicate<T> filter) {
    _filter = filter;
    return this;
  }

  @SuppressWarnings("unchecked")
  public Grid<T> build() {
    for (ColumnDef<T, ?> columnDef : _columnDefs) {
      columnDef.column = createColumn(_grid, columnDef.header, columnDef.valueProvider);
      columnDef.column.setAutoWidth(true);
      columnDef.column.setKey(columnDef.header);
    }
    HeaderRow filterRow = _grid.appendHeaderRow();
    VaadinHelper.addFilters(_grid, _columnDefs, filterRow, _filter);
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

  public static class ColumnDef<T, C extends Component> {
    String header;
    ValueProvider<T, ?> valueProvider;
    Column<T> column;
    Class<? extends C> navigationTarget;
  }

}