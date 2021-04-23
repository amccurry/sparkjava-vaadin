package vaadin.util.push;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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
public abstract class BasePushView<ITEM extends Item<ITEM>> extends Div
    implements PushComponent, SerializablePredicate<ITEM> {

  private static final Splitter SPACE_SPLITTER = Splitter.on(" ");
  private static final long serialVersionUID = 9064915736318224076L;
  private static final Comparator<Action> COMPARATOR = (o1, o2) -> o1.getName()
                                                                     .compareTo(o2.getName());

  protected final AtomicReference<UI> _uiRef = new AtomicReference<>();
  protected final ListDataProvider<ITEM> _dataProvider;
  protected final Grid<ITEM> _grid;
  protected final AtomicBoolean _shift = new AtomicBoolean();
  protected final DataCommunicator<ITEM> _dataCommunicator;
  protected final MenuBar _actionMenuBar;
  protected final List<Action> _actions;
  protected final Map<String, MenuItem> _actionMenuItems = new ConcurrentHashMap<>();
  protected final AtomicReference<SerializablePredicate<ITEM>> _searchPredicate = new AtomicReference<>(t -> true);
  protected final Div _menuDiv;

  public BasePushView() {
    _dataProvider = DataProvider.ofCollection(getDataItems());
    _grid = createGrid(_dataProvider);
    _grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    _dataProvider.setSortComparator((o1, o2) -> o1.compareTo(o2));
    _dataCommunicator = _grid.getDataCommunicator();

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
              .set("display", "inline-block")
              .set("margin-right", "10px");

    actionsDiv.add(_actionMenuBar);

    if (_actions.isEmpty()) {
      actionsDiv.setVisible(false);
    }

    Button button = new Button();
    button.setIcon(new Icon(VaadinIcon.REFRESH));
    button.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
    button.addClickListener(event -> push());

    topDiv.add(button);
    topDiv.add(actionsDiv);

    TextField textField = new TextField();
    textField.setPlaceholder("search");
    textField.addValueChangeListener(event -> {
      String value = textField.getValue();
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
        _searchPredicate.set(t -> true);
      } else {
        _searchPredicate.set(t -> {
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
        });
      }
      push();
    });
    textField.setValueChangeMode(ValueChangeMode.LAZY);
    textField.setWidth("70%");
    topDiv.add(textField);
    add(topDiv);
    _grid.addSelectionListener(getSelectionListener());
    _grid.addItemClickListener(onRowClickSelectOrDeselect());
    add(_grid);

  }

  protected void updateMenuLabel() {
    int size = _grid.getSelectedItems()
                    .size();
    _menuDiv.removeAll();
    if (size > 0) {
      _menuDiv.add(new Text("Actions (" + size + ") "));
      _menuDiv.add(new Icon(VaadinIcon.CHEVRON_CIRCLE_DOWN_O));
    } else {
      _menuDiv.add(new Text("Actions "));
      _menuDiv.add(new Icon(VaadinIcon.CHEVRON_CIRCLE_DOWN_O));
    }
  }

  protected String getSimpleSearchString(ITEM item) {
    return item.getSearchString();
  }

  @Override
  public boolean test(ITEM t) {
    FilterPanel<ITEM> filterPanel = getFilterPanel();
    if (filterPanel == null) {
      return _searchPredicate.get()
                             .test(t);
    }
    return filterPanel.test(t) && _searchPredicate.get()
                                                  .test(t);
  }

  private ComponentEventListener<ClickEvent<MenuItem>> getActionListener(Action action) {
    ComponentEventListener<ClickEvent<MenuItem>> listener = action.getListener();
    return event -> {
      try {
        listener.onComponentEvent(event);
      } catch (Throwable t) {
        log.error("Unknown error", t);
        sendError(t.getMessage(), t);
      }
      if (action.isClearSelectionsAndPushAfterAction()) {
        clearAndPush();
      }
    };
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    log.debug("attach");
    Optional<Component> parent = getParent();
    if (parent.isPresent()) {
      Component component = parent.get();
      if (component instanceof FilterPanelView) {
        FilterPanelView filterPanelView = (FilterPanelView) component;
        FilterPanel<ITEM> filterPanel = getFilterPanel();
        if (filterPanel != null) {
          filterPanelView.clearFilterPanel();
          filterPanelView.setFilterPanel(filterPanel);
        } else {
          filterPanelView.clearFilterPanel();
        }
      }
    }
    _uiRef.set(attachEvent.getUI());
    PushManager.INSTANCE.register(this);
  }

  protected FilterPanel<ITEM> getFilterPanel() {
    return null;
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    log.debug("detach");
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

  protected void applyFilter() {
    SerializablePredicate<ITEM> filter = _dataProvider.getFilter();
    _dataProvider.clearFilters();
    _dataProvider.setFilter(filter);
  }

  protected interface DialogAction {
    void action();
  }

  protected interface DialogValidation {
    boolean validate();
  }

  protected <FIELD extends AbstractField<?, ?>> void popupField(Div providedMessage, String confirmButtonLabel,
      DialogAction dialogAction) {
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

  protected void clearAndPush() {
    _grid.deselectAll();
    push();
  }

  protected <FIELD extends AbstractField<?, ?>> void popupField(Div providedMessage, String confirmButtonLabel,
      FIELD field, DialogAction dialogAction, DialogValidation validation) {
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

  protected void doPush(UI ui) {
    ui.access(() -> {
      _dataCommunicator.reset();
      updateMenuLabel();

      Collection<ITEM> currentItems = _dataProvider.getItems();
      List<ITEM> dataItems = getDataItems();
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
      ui.push();
    });
  }

  protected ComponentEventListener<ItemClickEvent<ITEM>> onRowClickSelectOrDeselect() {
    return event -> {
      ITEM item = event.getItem();
      if (_grid.getSelectedItems()
               .contains(item)) {
        _grid.deselect(item);
      } else {
        _grid.select(item);
      }
      updateActionMenuItemsEnablement();
    };
  }

  protected ActionEnabled disabledWhenNothingSelected(ActionEnabled actionEnabled) {
    return () -> {
      Set<ITEM> selectedItems = _grid.getSelectedItems();
      if (selectedItems == null || selectedItems.isEmpty()) {
        return false;
      }
      return actionEnabled.isEnabled();
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

  protected boolean hasActions() {
    return _actions != null && !_actions.isEmpty();
  }

  protected void sendSuccess(String message, Object... args) {
    Text text = getMessageText(message, args);
    sendNotificationInternal(text, (int) TimeUnit.SECONDS.toMillis(3), NotificationVariant.LUMO_SUCCESS, args);
  }

  protected void sendError(String message, Throwable t, Object... args) {
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

  protected void sendNotification(String message, Object... args) {
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

  protected abstract List<Action> getActions();

  protected abstract Grid<ITEM> createGrid(ListDataProvider<ITEM> dataProvider);

  protected abstract List<ITEM> getDataItems();

  private SelectionListener<Grid<ITEM>, ITEM> getSelectionListener() {
    return new SelectionListener<Grid<ITEM>, ITEM>() {

      private static final long serialVersionUID = 9034050090339718079L;

      private final Set<ITEM> _prevItems = new HashSet<>();
      private final AtomicReference<ITEM> _lastSelected = new AtomicReference<>();
      private final AtomicBoolean _alreadyProcessing = new AtomicBoolean();

      @Override
      public void selectionChange(SelectionEvent<Grid<ITEM>, ITEM> event) {
        if (_alreadyProcessing.get()) {
          return;
        }
        _alreadyProcessing.set(true);
        try {
          Set<ITEM> currentItems = _grid.getSelectedItems();
          for (ITEM item : currentItems) {
            if (!_prevItems.contains(item)) {
              // System.out.println("Added " + item + " " + _control.get());
              if (_shift.get()) {
                if (_lastSelected.get() != null) {

                  int index1 = getIndex(_lastSelected.get());
                  int index2 = getIndex(item);
                  if (index1 < index2) {
                    selectItemsBetweenForward(_lastSelected.get(), item);
                  } else {
                    selectItemsBetweenForward(item, _lastSelected.get());
                  }
                }
              }
              _lastSelected.set(item);
            }
          }
          _prevItems.clear();
          _prevItems.addAll(currentItems);
          updateActionMenuItemsEnablement();
        } finally {
          _alreadyProcessing.set(false);
        }
        push();
      }

      private int getIndex(ITEM item) {
        for (int i = 0; i < _dataCommunicator.getItemCount(); i++) {
          if (item == _dataCommunicator.getItem(i)) {
            return i;
          }
        }
        return -1;
      }

      private void selectItemsBetweenForward(ITEM firstItem, ITEM lastItem) {
        boolean mark = false;
        for (int i = 0; i < _dataCommunicator.getItemCount(); i++) {
          ITEM item = _dataCommunicator.getItem(i);
          if (item == firstItem) {
            mark = true;
          } else if (item == lastItem) {
            mark = false;
          }
          if (mark) {
            _grid.select(item);
          }
        }
      }
    };
  }
}
