package vaadin.util.task;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import vaadin.util.push.Item;

@Value
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Task implements Item<Task> {

  @Builder.Default
  String id = UUID.randomUUID()
                  .toString();

  @Builder.Default
  Date created = new Date();

  String name;
  String description;

  String user;

  TaskStatus taskStatus;

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
  public String getSearchString() {
    return id + " " + created + " " + name + " " + description + " " + user + " " + taskStatus + " " + throwable;
  }

  @Override
  public int compareTo(Task o) {
    return name.compareTo(o.getName());
  }

}
