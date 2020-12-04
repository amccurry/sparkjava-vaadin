package vaadin.util;

import static vaadin.util.VaadinHelper.addStringFilter;
import static vaadin.util.VaadinHelper.createColumn;

import java.util.ArrayList;
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
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.internal.HasUrlParameterFormat;

public class VaadinGridBuilder<T> {

  private final Grid<T> _grid;
  private final InMemoryDataProvider<T> _dataProvider;
  private final List<ColumnDef<T, ?>> _columnDefs = new ArrayList<>();

  private VaadinGridBuilder(Grid<T> grid, InMemoryDataProvider<T> dataProvider) {
    _grid = grid;
    _dataProvider = dataProvider;
  }

  public static <T> VaadinGridBuilder<T> create(List<T> list) {
    ListDataProvider<T> dataProvider = DataProvider.ofCollection(list);
    Grid<T> grid = new Grid<>();
    grid.setHeightByRows(true);
    grid.setItems(dataProvider);
    return create(grid, dataProvider);
  }

  public static <T> VaadinGridBuilder<T> create(Grid<T> grid, InMemoryDataProvider<T> dataProvider) {
    return new VaadinGridBuilder<>(grid, dataProvider);
  }

  public <C extends Component> VaadinGridBuilder<T> add(String header, ValueProvider<T, ?> valueProvider) {
    ColumnDef<T, C> columnDef = new ColumnDef<T, C>();
    columnDef.header = header;
    columnDef.valueProvider = valueProvider;
    _columnDefs.add(columnDef);
    return this;
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

  public Grid<T> build() {
    for (ColumnDef<T, ?> columnDef : _columnDefs) {
      columnDef.column = createColumn(_grid, columnDef.header, columnDef.valueProvider);
      columnDef.column.setAutoWidth(true);
      columnDef.column.setKey(columnDef.header);
    }
    HeaderRow filterRow = _grid.appendHeaderRow();
    for (ColumnDef<T, ?> columnDef : _columnDefs) {
      addStringFilter(_dataProvider, columnDef.column, filterRow, columnDef.valueProvider);
    }
    ComponentEventListener<ItemClickEvent<T>> listener = event -> {
      Column<T> column = event.getColumn();
      T tackNetwork = event.getItem();
      String key = column.getKey();
      for (ColumnDef<T, ?> columnDef : _columnDefs) {
        if (columnDef.header.equals(key) && columnDef.navigationTarget != null) {
          Object o = columnDef.valueProvider.apply(tackNetwork);
          if (o != null) {
            UI.getCurrent()
              .navigate(columnDef.navigationTarget, HasUrlParameterFormat.getParameters(o.toString()));
          }
        }
      }
    };
    _grid.addItemClickListener(listener);
    return _grid;
  }

  private static class ColumnDef<T, C extends Component> {
    String header;
    ValueProvider<T, ?> valueProvider;
    Column<T> column;
    Class<? extends C> navigationTarget;
  }

}