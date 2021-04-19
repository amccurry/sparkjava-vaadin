package vaadin.util.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.contextmenu.MenuItem;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Action {

  String name;
  ComponentEventListener<ClickEvent<MenuItem>> listener;
  @Builder.Default
  ActionEnabled actionEnabled = () -> true;
  @Builder.Default
  boolean clearSelectionsAndPushAfterAction = true;

}
