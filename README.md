# sparkjava-vaadin

This library allows [vaadin](https://https://vaadin.com/) to be run inside a [sparkjava](https://sparkjava.com/) service.

```
Service service = VaadinSparkService.ignite();
service.port(8080);
service.get("/api/hello", (req1, res1) -> "Hello World");
```

If you do not have any extra route you need to explicitly call ```init()``` on the Service object.

```
Service service = VaadinSparkService.ignite();
service.port(8080);
service.init();
```
