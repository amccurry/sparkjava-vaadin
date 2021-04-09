package vaadin.util.push;

import java.util.Collections;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.MapMaker;

public class PushManager {

  public static final PushManager INSTANCE = new PushManager();
  private final Set<PushView> _pushCache;
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
        for (PushView pushView : _pushCache) {
          if (pushView != null) {
            pushView.push();
          }
        }
      }
    }, delay, period);
  }

  public void register(PushView pushView) {
    _pushCache.add(pushView);
  }

  public void deregister(PushView pushView) {
    _pushCache.remove(pushView);
  }

}
