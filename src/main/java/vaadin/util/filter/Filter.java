package vaadin.util.filter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.data.selection.MultiSelectionListener;
import com.vaadin.flow.function.SerializablePredicate;

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
public class Filter<ITEM> {

  String name;
  MultiSelectionListener<MultiSelectListBox<String>, String> listener;
  FilterOptions options;
  SerializablePredicate<ITEM> predicate;

}
