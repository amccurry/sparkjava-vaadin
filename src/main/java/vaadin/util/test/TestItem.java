package vaadin.util.test;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import vaadin.util.filter.Filterable;
import vaadin.util.push.Item;
import vaadin.util.push.Searchable;

@Value
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestItem implements Item<TestItem> {

  @Builder.Default
  @Searchable
  String id = UUID.randomUUID()
                  .toString();
  @Builder.Default
  @Searchable
  String name = UUID.randomUUID()
                    .toString()
      + "|" + UUID.randomUUID()
                  .toString();
  @Builder.Default
  @Searchable
  String value = UUID.randomUUID()
                     .toString();

  TestEnum testEnum;

  @Searchable
  @Filterable("Test")
  public String getTestEnumStr() {
    if (testEnum == null) {
      return null;
    }
    return testEnum.name();
  }

  @Override
  public int compareTo(TestItem o) {
    return id.compareTo(o.getId());
  }

}
