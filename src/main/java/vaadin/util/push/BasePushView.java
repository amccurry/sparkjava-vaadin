package vaadin.util.push;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyDownEvent;
import com.vaadin.flow.component.KeyUpEvent;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.selection.MultiSelectionEvent;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.data.selection.SelectionListener;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializablePredicate;

import lombok.extern.slf4j.Slf4j;
import vaadin.util.action.Action;
import vaadin.util.action.ActionEnabled;
import vaadin.util.filter.FilterPanel;
import vaadin.util.filter.FilterPanelView;

@Slf4j
public abstract class BasePushView<ITEM extends Item<ITEM>> extends Div implements PushComponent {

  private static final Splitter SPACE_SPLITTER = Splitter.on(" ");
  private static final long serialVersionUID = 9064915736318224076L;
  private static final Comparator<Action> COMPARATOR = (o1, o2) -> o1.getName()
                                                                     .compareTo(o2.getName());

  private final NumberFormat _format = NumberFormat.getInstance();
  private final AtomicReference<UI> _uiRef = new AtomicReference<>();
  private final ListDataProvider<ITEM> _dataProvider;
  private final Grid<ITEM> _grid;
  private final AtomicBoolean _shift = new AtomicBoolean();
  private final DataCommunicator<ITEM> _dataCommunicator;
  private final MenuBar _actionMenuBar;
  private final List<Action> _actions;
  private final Map<String, MenuItem> _actionMenuItems = new ConcurrentHashMap<>();
  private final Div _menuDiv;
  private final FilterPanel<ITEM> _filterPanel;
  private final SerializablePredicate<ITEM> _filterPredicate;
  private final Label _countText;
  private final SearchableType<ITEM> _searchableType;
  private final Button _clearSelections;
  private final ItemManager<ITEM> _itemManager;
  private SerializablePredicate<ITEM> _searchPredicate = t -> true;

  public BasePushView(ItemManager<ITEM> itemManager) {
    _itemManager = itemManager;
    Class<ITEM> itemClass = getItemClass();

    _dataProvider = DataProvider.ofCollection(getDataItems());
    GridBuilder<ITEM> builder = GridBuilder.create(_dataProvider)
                                           .withFilter(getGridFilter());
    _grid = createGrid(builder);
    _grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    _dataProvider.setSortComparator((o1, o2) -> o1.compareTo(o2));
    _dataCommunicator = _grid.getDataCommunicator();
    _searchableType = new SearchableType<ITEM>(itemClass);

    List<Action> actions = getActions();
    if (actions == null) {
      actions = Arrays.asList();
    }
    _actions = new ArrayList<>(actions);
    Collections.sort(_actions, COMPARATOR);
    if (!_actions.isEmpty()) {
      _grid.setSelectionMode(Grid.SelectionMode.MULTI);
    }

    ComponentUtil.addListener(this, KeyDownEvent.class, event -> {
      if (event.getKey()
               .matches("Shift")) {
        log.debug("Shift key down");
        _shift.set(true);
      }
    });

    ComponentUtil.addListener(this, KeyUpEvent.class, event -> {
      if (event.getKey()
               .matches("Shift")) {
        log.debug("Shift key up");
        _shift.set(false);
      }
    });

    _actionMenuBar = new MenuBar();
    _actionMenuBar.addThemeVariants(MenuBarVariant.LUMO_PRIMARY);
    _menuDiv = new Div();
    updateMenuLabel();

    _filterPanel = new FilterPanel<>(_itemManager);
    _filterPredicate = _filterPanel;

    MenuItem actionsMenuItem = _actionMenuBar.addItem(_menuDiv);
    SubMenu actionsSubMenu = actionsMenuItem.getSubMenu();
    for (Action action : _actions) {
      MenuItem menuItem = actionsSubMenu.addItem(action.getName());
      menuItem.addClickListener(getActionListener(action));
      menuItem.setEnabled(action.getActionEnabled()
                                .isEnabled());
      _actionMenuItems.put(action.getName(), menuItem);
    }

    Div topDiv = new Div();
    Div actionsDiv = new Div();
    actionsDiv.getStyle()
              .set("display", "inline-block");

    actionsDiv.add(_actionMenuBar);

    if (_actions.isEmpty()) {
      actionsDiv.setVisible(false);
    }

    Button button = new Button();
    button.setIcon(new Icon(VaadinIcon.REFRESH));
    button.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
    button.addClickListener(event -> {
      refreshItems();
      push();
    });

    _clearSelections = new Button("Clear Selections");
    _clearSelections.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    _clearSelections.setEnabled(false);
    _clearSelections.addClickListener(event -> {
      _grid.deselectAll();
      push();
    });

    topDiv.add(button);
    topDiv.add(actionsDiv);
    topDiv.add(_clearSelections);

    TextField searchField = new TextField();
    searchField.setPlaceholder("search");
    searchField.addValueChangeListener(event -> {
      String value = searchField.getValue();
      List<String> parts = SPACE_SPLITTER.splitToList(value);
      List<String> tokens = new ArrayList<>();
      for (String part : parts) {
        String s = part.trim()
                       .toLowerCase();
        if (!s.isEmpty()) {
          tokens.add(part);
        }
      }
      if (tokens.isEmpty()) {
        _searchPredicate = t -> true;
      } else {
        _searchPredicate = t -> {
          String searchString = getSimpleSearchString(t);
          if (searchString == null) {
            return true;
          }
          String lowerCase = searchString.toLowerCase();
          for (String token : tokens) {
            if (lowerCase.contains(token)) {
              return true;
            }
          }
          return false;
        };
      }
      push();
    });
    searchField.setValueChangeMode(ValueChangeMode.LAZY);
    searchField.setWidth("70%");
    topDiv.add(searchField);
    _countText = new Label();
    updateCount();
    topDiv.add(_countText);
    add(topDiv);
    _grid.addSelectionListener(getSelectionListener());
    _grid.addItemClickListener(onRowClickSelectOrDeselect());
    add(_grid);
  }

  protected void refreshItems() {
    _itemManager.updateData();
    sendSuccessNotification("Data Refreshed");
  }

  protected abstract List<Action> getActions();

  protected abstract Grid<ITEM> createGrid(GridBuilder<ITEM> builder);

  protected Collection<ITEM> getDataItems() {
    return _itemManager.getItems();
  }

  // protected List<Filter<ITEM>> getFilters() {
  // return _itemManager.getFilters(this);
  // }

  public Set<ITEM> getSelectedItems() {
    return _grid.getSelectedItems();
  }

  protected Grid<ITEM> getGrid() {
    return _grid;
  }

  private void updateCount() {
    int totalCount = _dataProvider.getItems()
                                  .size();
    int filterCount = _dataCommunicator.getItemCount();
    if (filterCount == totalCount) {
      _countText.setText(" Item Count: " + formatNumber(totalCount));
    } else {
      _countText.setText(" Item Count: " + formatNumber(filterCount) + " of " + formatNumber(totalCount));
    }
  }

  private synchronized String formatNumber(long l) {
    return _format.format(l);
  }

  private void updateMenuLabel() {
    int size = _grid.getSelectedItems()
                    .size();
    if (size > 0) {
      _clearSelections.setEnabled(true);
    }
    _menuDiv.removeAll();
    if (size > 0) {
      _menuDiv.add(new Text("Actions (" + formatNumber(size) + ") "));
      _menuDiv.add(new Icon(VaadinIcon.CHEVRON_CIRCLE_DOWN_O));
    } else {
      _menuDiv.add(new Text("Actions "));
      _menuDiv.add(new Icon(VaadinIcon.CHEVRON_CIRCLE_DOWN_O));
    }
  }

  private String getSimpleSearchString(ITEM item) {
    return _searchableType.getSearchString(item);
  }

  private SerializablePredicate<ITEM> getGridFilter() {
    return t -> _filterPredicate.test(t) && _searchPredicate.test(t);
  }

  private ComponentEventListener<ClickEvent<MenuItem>> getActionListener(Action action) {
    ComponentEventListener<ClickEvent<MenuItem>> listener = action.getListener();
    return event -> {
      try {
        listener.onComponentEvent(event);
      } catch (Throwable t) {
        log.error("Unknown error", t);
        sendErrorNotification(t.getMessage(), t);
      }
      if (action.isClearSelectionsAndPushAfterAction()) {
        clearAndPush();
      }
    };
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    log.debug("attach");
    _uiRef.set(attachEvent.getUI());
    Optional<Component> parent = getParent();
    if (parent.isPresent()) {
      Component component = parent.get();
      if (component instanceof FilterPanelView) {
        FilterPanelView filterPanelView = (FilterPanelView) component;
        if (!_filterPanel.isEmpty()) {
          filterPanelView.clearFilterPanel();
          filterPanelView.setFilterPanel(_filterPanel);
        } else {
          filterPanelView.clearFilterPanel();
        }
      }
    }
    PushManager.INSTANCE.register(this);
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    log.debug("detach");
    _uiRef.set(null);
    PushManager.INSTANCE.deregister(this);
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

  protected interface DialogAction {
    void action();
  }

  protected interface DialogValidation {
    boolean validate();
  }

  public void createDialogBox(Div providedMessage, String confirmButtonLabel, DialogAction dialogAction) {
    Dialog dialog = new Dialog();

    dialog.setWidth("600px");
    dialog.setCloseOnEsc(true);
    dialog.setCloseOnOutsideClick(true);

    Button confirmButton = new Button(confirmButtonLabel, e -> {
      dialog.close();
      dialogAction.action();
      clearAndPush();
    });
    confirmButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    confirmButton.setEnabled(true);

    Div message = new Div();
    message.add(providedMessage);
    dialog.add(message);

    Button cancelButton = new Button("Cancel", c -> {
      dialog.close();
    });
    cancelButton.setAutofocus(true);
    // Cancel action on ESC press
    Shortcuts.addShortcutListener(dialog, () -> {
      dialog.close();
    }, Key.ESCAPE);

    dialog.add(new Div(confirmButton, cancelButton));
    dialog.open();
  }

  public void clearAndPush() {
    _grid.deselectAll();
    push();
  }

  public <FIELD extends AbstractField<?, ?>> void createDialogBoxWithField(Div providedMessage,
      String confirmButtonLabel, FIELD field, DialogAction dialogAction, DialogValidation validation) {
    Dialog dialog = new Dialog();

    dialog.setWidth("600px");
    dialog.setCloseOnEsc(true);
    dialog.setCloseOnOutsideClick(true);

    if (field instanceof HasValueChangeMode) {
      HasValueChangeMode mode = (HasValueChangeMode) field;
      mode.setValueChangeMode(ValueChangeMode.LAZY);
    }

    Button confirmButton = new Button(confirmButtonLabel, e -> {
      dialog.close();
      dialogAction.action();
      clearAndPush();
    });
    confirmButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    confirmButton.setEnabled(validation.validate());

    Div message = new Div();
    field.addValueChangeListener(e -> {
      confirmButton.setEnabled(validation.validate());
    });
    if (providedMessage != null) {
      message.add(providedMessage);
    }
    message.add(field);
    dialog.add(message);

    Button cancelButton = new Button("Cancel", c -> {
      dialog.close();
    });
    cancelButton.setAutofocus(true);
    // Cancel action on ESC press
    Shortcuts.addShortcutListener(dialog, () -> {
      dialog.close();
    }, Key.ESCAPE);

    dialog.add(new Div(confirmButton, cancelButton));
    dialog.open();
  }

  private void doPush(UI ui) {
    ui.access(() -> {
      _dataCommunicator.reset();
      Collection<ITEM> currentItems = _dataProvider.getItems();
      Collection<ITEM> dataItems = getDataItems();
      updateDataProviderIfNeeded(currentItems, dataItems);
      updateMenuLabel();
      updateCount();
      ui.push();
    });
  }

  private void updateDataProviderIfNeeded(Collection<ITEM> currentItems, Collection<ITEM> dataItems) {
    if (currentItems == dataItems) {
      // same instance
      return;
    }
    log.debug("start push prep");
    for (ITEM item : dataItems) {
      if (!currentItems.contains(item)) {
        currentItems.add(item);
      }
    }
    ImmutableList<ITEM> list = ImmutableList.copyOf(currentItems);
    for (ITEM item : list) {
      if (!dataItems.contains(item)) {
        currentItems.remove(item);
      }
    }
    log.debug("finish push prep");
  }

  private ComponentEventListener<ItemClickEvent<ITEM>> onRowClickSelectOrDeselect() {
    return event -> {
      ITEM item = event.getItem();
      if (_grid.getSelectedItems()
               .contains(item)) {
        _grid.deselect(item);
      } else {
        _grid.select(item);
      }
      updateActionMenuItemsEnablement();
      push();
    };
  }

  public ActionEnabled disabledWhenNothingSelected(ActionEnabled actionEnabled) {
    return () -> {
      Set<ITEM> selectedItems = _grid.getSelectedItems();
      if (selectedItems == null || selectedItems.isEmpty()) {
        return false;
      }
      return actionEnabled.isEnabled();
    };
  }

  public ActionEnabled disabledWhenNothingSelected() {
    return () -> {
      Set<ITEM> selectedItems = _grid.getSelectedItems();
      if (selectedItems == null || selectedItems.isEmpty()) {
        return false;
      }
      return true;
    };
  }

  private void updateActionMenuItemsEnablement() {
    if (hasActions()) {
      for (Action action : _actions) {
        MenuItem menuItem = _actionMenuItems.get(action.getName());
        menuItem.setEnabled(action.getActionEnabled()
                                  .isEnabled());
      }
    }
  }

  private boolean hasActions() {
    return _actions != null && !_actions.isEmpty();
  }

  public void sendSuccessNotification(String message, Object... args) {
    Text text = getMessageText(message, args);
    sendNotificationInternal(text, (int) TimeUnit.SECONDS.toMillis(3), NotificationVariant.LUMO_SUCCESS, args);
  }

  public void sendErrorNotification(String message) {
    sendErrorNotification(message, null);
  }

  public void sendErrorNotification(String message, Throwable t, Object... args) {
    Div div = new Div();

    Notification notification = new Notification();

    Button button = new Button();
    button.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
    button.setIcon(new Icon(VaadinIcon.CLOSE_CIRCLE_O));
    button.addClickListener(event -> notification.close());

    Text text = getMessageText(message, args);
    div.add(button, text);

    if (t != null) {
      StringWriter writer = new StringWriter();
      try (PrintWriter pw = new PrintWriter(writer)) {
        t.printStackTrace(pw);
      }
      Details component = new Details("Stack Trace", new Text(writer.toString()));
      div.add(component);
    }

    notification.setPosition(Notification.Position.TOP_END);
    notification.setDuration(0);
    notification.add(div);
    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    notification.open();

  }

  public void sendNotification(String message, Object... args) {
    Text text = getMessageText(message, args);
    sendNotificationInternal(text, (int) TimeUnit.SECONDS.toMillis(3), NotificationVariant.LUMO_PRIMARY, args);
  }

  private void sendNotificationInternal(Component component, int duration, NotificationVariant variant,
      Object... args) {
    Notification notification = new Notification();
    notification.setPosition(Notification.Position.TOP_END);
    notification.setDuration(duration);
    notification.add(component);
    notification.addThemeVariants(variant);
    notification.open();
  }

  private Text getMessageText(String message, Object... args) {
    FormattingTuple ft = MessageFormatter.arrayFormat(message, args, null);
    Text text = new Text(ft.getMessage());
    return text;
  }

  private SelectionListener<Grid<ITEM>, ITEM> getSelectionListener() {
    return new SelectionListener<Grid<ITEM>, ITEM>() {

      private static final long serialVersionUID = 365720388006611903L;

      private ITEM _prevSelection;

      @Override
      public void selectionChange(SelectionEvent<Grid<ITEM>, ITEM> event) {
        MultiSelectionEvent<Grid<ITEM>, ITEM> mevent = (MultiSelectionEvent<Grid<ITEM>, ITEM>) event;
        if (!mevent.isFromClient()) {
          return;
        }
        try {
          if (!mevent.getRemovedSelection()
                     .isEmpty()) {
            _prevSelection = null;
            return;
          }
          ITEM newItem = getFirstItem(mevent.getAddedSelection());
          if (_prevSelection != null && _shift.get()) {
            int index1 = getSelectedItemIndex(_prevSelection);
            int index2 = getSelectedItemIndex(newItem);
            if (index1 < index2) {
              selectItemsBetweenForward(index1, index2);
            } else {
              selectItemsBetweenForward(index2, index1);
            }
          }
          _prevSelection = newItem;
        } finally {
          updateActionMenuItemsEnablement();
          push();
        }
      }
    };

  }

  private int getSelectedItemIndex(ITEM item) {
    for (int i = 0; i < _dataCommunicator.getItemCount(); i++) {
      if (item == _dataCommunicator.getItem(i)) {
        return i;
      }
    }
    return -1;
  }

  private void selectItemsBetweenForward(int firstItemIndex, int lastItemIndex) {
    int itemCount = _dataCommunicator.getItemCount();
    for (int i = firstItemIndex; i < lastItemIndex && i < itemCount; i++) {
      ITEM item = _dataCommunicator.getItem(i);
      _grid.select(item);
    }
  }

  private ITEM getFirstItem(Set<ITEM> items) {
    if (items == null || items.isEmpty()) {
      return null;
    }
    return items.iterator()
                .next();
  }

  private <T> Class<T> getItemClass() {
    return ClassHelper.getItemClass(BasePushView.class, getClass());
  }

}
