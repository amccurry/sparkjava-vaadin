package vaadin.util.push;

import java.util.Collections;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.vaadin.flow.component.UIDetachedException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PushManager {

  public static final PushManager INSTANCE = new PushManager();
  private final Set<PushComponent> _pushCache;
  private final Timer _timer;

  private PushManager() {
    _pushCache = Collections.newSetFromMap(new MapMaker().weakKeys()
                                                         .weakValues()
                                                         .makeMap());
    _timer = new Timer("push-thread", true);
    long delay = TimeUnit.SECONDS.toMillis(5);
    long period = TimeUnit.SECONDS.toMillis(5);
    _timer.schedule(new TimerTask() {
      @Override
      public void run() {
        for (PushComponent pushView : ImmutableSet.copyOf(_pushCache)) {
          doPush(pushView);
        }
      }

    }, delay, period);
  }

  public void register(PushComponent pushView) {
    _pushCache.add(pushView);
  }

  public void deregister(PushComponent pushView) {
    _pushCache.remove(pushView);
  }

  private void doPush(PushComponent pushView) {
    if (pushView != null) {
      try {
        pushView.push();
      } catch (Throwable t) {
        if (t instanceof UIDetachedException) {
          deregister(pushView);
        } else {
          log.error("Unknown error while trying to push {}", t);
        }
      }
    }
  }

}
