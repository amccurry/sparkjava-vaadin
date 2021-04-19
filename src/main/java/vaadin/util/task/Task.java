package vaadin.util.task;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
public class Task {

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

}
