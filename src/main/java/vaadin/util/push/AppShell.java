package vaadin.util.push;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

//@Meta(name = "Author", content = "Donald Duck")
//@PWA(name = "My Fun Application", shortName = "fun-app")
//@Inline("my-custom-javascript.js")
//@Viewport("width=device-width, initial-scale=1")
//@BodySize(height = "100vh", width = "100vw")
//@PageTitle("my-title")
@Push(value = PushMode.AUTOMATIC, transport = Transport.LONG_POLLING)
public class AppShell implements AppShellConfigurator {
  private static final long serialVersionUID = 1L;
}