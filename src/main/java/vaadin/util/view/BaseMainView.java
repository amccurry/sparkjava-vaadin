package vaadin.util.view;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLink;

import lombok.extern.slf4j.Slf4j;
import tack.manager.app.task.TaskView;
import vaadin.util.filter.FilterPanel;
import vaadin.util.filter.FilterPanelView;
import vaadin.util.push.PushComponent;
import vaadin.util.push.PushManager;
import vaadin.util.task.TaskManager;

@Slf4j
public abstract class BaseMainView extends AppLayout implements BeforeEnterObserver, PushComponent, FilterPanelView {

  private static final long serialVersionUID = -7822811916895268645L;

  private static final TaskManager TASK_MANAGER = TaskManager.INSTANCE;

  private final Tabs _tabs = new Tabs();
  private final Map<Class<? extends Component>, Tab> _navigationTargetToTab = new ConcurrentHashMap<>();
  private final RouterLink _taskRouterLink;
  private final AtomicReference<UI> _uiRef = new AtomicReference<>();
  private final Div _filterDiv;

  public BaseMainView() {
    _tabs.setOrientation(Tabs.Orientation.VERTICAL);
    _taskRouterLink = addMenuTab("Tasks", TaskView.class, false);

    addToDrawer(_tabs);
    _filterDiv = new Div();
    Style style = _filterDiv.getStyle();
    style.set("padding", "5px");
    _filterDiv.setVisible(false);
    addToDrawer(_filterDiv);

    addToNavbar(new DrawerToggle());
    setPrimarySection(Section.NAVBAR);
    addToNavbar(new Span("Tack Manager"));
    updateTaskCount();
  }

  protected RouterLink addMenuTab(String label, Class<? extends Component> target) {
    return addMenuTab(label, target, true);
  }

  private RouterLink addMenuTab(String label, Class<? extends Component> target, boolean normal) {
    RouterLink routerLink = new RouterLink();
    routerLink.setText(label);
    routerLink.setRoute(target);
    Tab tab = new Tab(routerLink);
    _navigationTargetToTab.put(target, tab);
    if (normal) {
      _tabs.addComponentAtIndex(_tabs.getComponentCount() - 1, tab);
    } else {
      _tabs.add(tab);
    }
    return routerLink;
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    _tabs.setSelectedTab(_navigationTargetToTab.get(event.getNavigationTarget()));
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    log.debug("attach");
    _uiRef.set(attachEvent.getUI());
    PushManager.INSTANCE.register(this);
    TaskManager.INSTANCE.register(this);
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    log.debug("detach");
    _uiRef.set(null);
    PushManager.INSTANCE.deregister(this);
    TaskManager.INSTANCE.deregister(this);
  }

  @Override
  public void push() {
    UI ui = _uiRef.get();
    ui.access(() -> {
      updateTaskCount();
      ui.push();
    });
  }

  private void updateTaskCount() {
    int runningJobCount = TASK_MANAGER.getRunningJobCount();
    if (runningJobCount == 0) {
      _taskRouterLink.setText("Tasks");
    } else {
      _taskRouterLink.setText("Tasks (" + runningJobCount + ")");
    }
  }

  @Override
  public void setFilterPanel(FilterPanel<?> filterPanel) {
    _filterDiv.setVisible(true);
    _filterDiv.add(filterPanel);
  }

  @Override
  public void clearFilterPanel() {
    _filterDiv.setVisible(false);
    _filterDiv.removeAll();
  }

}
