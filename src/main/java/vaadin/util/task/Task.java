package vaadin.util.task;

import java.util.Date;
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
public class Task implements Item<Task> {

  @Builder.Default
  @Searchable
  String id = UUID.randomUUID()
                  .toString();

  @Builder.Default
  @Searchable
  Date created = new Date();

  @Searchable
  String name;

  @Searchable
  String description;

  @Searchable
  @Filterable("User")
  String user;

  @Searchable
  TaskStatus taskStatus;

  @Searchable
  Throwable throwable;

  TaskRunnable taskRunnable;

  public String getDescriptionThrowable() {
    StringBuilder builder = new StringBuilder();
    if (description != null) {
      builder.append(description);
    }
    if (throwable != null) {
      builder.append('{')
             .append(throwable.getMessage())
             .append('}');
    }
    return builder.toString();
  }

  @Override
  public int compareTo(Task o) {
    return o.getCreated()
            .compareTo(created);
  }

}
