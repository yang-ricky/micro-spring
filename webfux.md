下面给你一份**更新后**的任务列表，参考了你原先的 6 大任务，并结合了刚才讨论的额外建议（例如 Body 解析、Filter、
异常处理等）。整体依然保持“**循序渐进、分步骤**”的风格，供你在实现时一步步推进, 使得整体更贴近**生产级** WebFlux 设计思路


---

## 任务1：实现最简 Reactive Http Server

**场景**：在原有的 MicroTomcat（阻塞式）之外，你需要一个基于 NIO/Netty 的 **非阻塞**服务器，用来处理 HTTP 请求。

- [ ] **编写 `ReactiveHttpServer`**  
  - 构造时传入监听端口 `port`  
  - 提供 `start(ReactiveHttpHandler handler)` 方法，用于启动服务器并将请求处理委托给 `handler`  
  - 你可以选择： 
    - 纯 Java NIO + `Selector`  
    - 或直接使用 **Netty** (`io.netty.bootstrap.ServerBootstrap`) 来监听端口、接受连接
  - **线程模型提示**：若使用 Netty，注意 `EventLoopGroup` 默认线程数配置，避免在 IO 线程中做阻塞操作。可提醒开发者将耗时逻辑切换到“业务线程池”或使用 Reactor 的异步操作。

- [ ] **定义最简的请求响应模型**  
  - `ReactiveServerRequest`：  
    - 记录请求 URI、HTTP 方法、Headers；提供读取 Body 的接口（先做简单文本或二进制 buffer）  
  - `ReactiveServerResponse`：  
    - 提供写响应数据的方法，如 `write(String data)`  
    - 支持异步完成响应，如 `end()` 之类

- [ ] **引入或自定义“Reactive”返回类型**  
  - 可以先做简易版 `Mono<T>` / `Flux<T>`，或直接依赖 `Reactor` (`Mono`, `Flux`)  
  - `ReactiveHttpHandler`：  
    ```java
    interface ReactiveHttpHandler {
        Mono<Void> handle(ReactiveServerRequest req, ReactiveServerResponse resp);
    }
    ```
  - 返回 `Mono<Void>` 便于异步处理链式结束。

**产出要求**：  
1. 编写一个 `main` 或测试类启动服务器，监听 8080；  
2. 浏览器访问 `http://localhost:8080/anything`，能打印请求信息并返回“Hello from ReactiveHttpServer”。

---

## 任务2：初步处理请求体与响应体（含简单 JSON） + 基础背压

**场景**：WebFlux 通常需要处理 JSON，请求体可能很大，需要考虑基本背压。你先实现文本和 JSON 的读写，后续在高并发或长流场景下可扩展背压策略。

- [ ] **Body 读取**  
  - 在 `ReactiveServerRequest` 中增加 `Mono<String> bodyToString()` 用于读取文本；  
  - （可选）添加 `Mono<T> bodyToObject(Class<T> clazz)` 用于 JSON 解析，依赖 **Jackson** 或自己实现简单 JSON parser；  
  - **背压提示**：如果要处理大文本/流式数据，可使用 `Flux<ByteBuffer>` 并配合 Reactor 的 `request(n)` 机制，或 `onBackpressureBuffer()` 等做限流。

- [ ] **Body 写出**  
  - 在 `ReactiveServerResponse` 中增加类似 `writeJson(Object obj)` 方法  
  - 内部用 Jackson (`ObjectMapper`) 序列化为字符串后再写入响应  
  - 注意 `ObjectMapper` 的**线程安全**，可单例缓存。

- [ ] **测试 & 背压试验**  
  - 用 `curl` 或 Postman 测试一个 JSON `POST` 请求，解析 body 并返回 JSON；  
  - 可写个小用例或用 Reactor 的 `StepVerifier` 做**异步测试**；  
  - 如果想模拟简单背压，可限制消费者读取速率，看是否能正常处理大文本。

**产出要求**：  
1. 支持接收并读取 JSON 请求；  
2. 支持返回 JSON 响应（`Content-Type: application/json`）；  
3. 提示用户如何在大文件/流式传输中使用背压防止内存溢出。

---

## 任务3：基于注解的 Controller 映射 + 路由冲突检测

**场景**：实现类似 Spring MVC / WebFlux 的注解驱动控制器：
```java
@RestController
public class HelloController {
    @RequestMapping(method=GET, value="/flux/hello")
    public Mono<String> hello() {
        return Mono.just("Hello, WebFlux!");
    }
}
```
并自动把 `"/flux/hello"` 映射到该方法里。

- [ ] **定义注解**  
  - `@RestController` 或 `@Controller`  
  - `@RequestMapping(value="/somePath", method=GET/POST, ...)` 等

- [ ] **扫描注解并注册映射**  
  - 在 MicroSpring IoC 容器启动后，遍历所有 Bean  
  - 找到带 `@RestController` 的类，并收集类和方法上的 `@RequestMapping` 路径  
  - 把路径(可含 HttpMethod) -> `HandlerMethod(bean, method)` 存到 `ReactiveHandlerMapping`
  - **冲突检测**：如 `/users` 重复出现在两个 Controller 方法，打印警告或抛异常。

- [ ] **反射调用 HandlerMethod**  
  - 匹配到时，通过反射执行方法；  
  - 如果返回 `Mono<?>` / `Flux<?>`，则异步写出；若是普通对象，包装成 `Mono.just(...)` 再写。  
  - 可缓存 `Method`、`MethodHandle` 等提升性能。

**产出要求**：  
1. `[HandlerMapping] /flux/hello -> HelloController.hello()` 日志可见；  
2. 访问 `http://localhost:8080/flux/hello` 返回 `"Hello, WebFlux!"`；  
3. 路径冲突时有检测提醒；匹配不到时返回 404。

---

## 任务4：函数式路由（RouterFunction）[可选]

**场景**：Spring WebFlux 的另一种编程模型是函数式路由，你可以提供一个 DSL：

```java
RouterFunction route =
    RouterFunctionBuilder
       .GET("/flux/hello", req -> Mono.just("Hello Router!"))
       .POST("/users", req -> ...)
       .build();
```

- [ ] **定义 `RouterFunction` / `HandlerFunction`**  
  - `HandlerFunction`: `handle(ReactiveServerRequest req) -> Mono<ServerResponse>`  
  - `RouterFunction`: 根据 `(method, path)` 查找对应的 `HandlerFunction`  
  - 底层维护 `Map<(HttpMethod, String), HandlerFunction>`；可检测冲突

- [ ] **与服务器集成**  
  - 在请求进来时，先尝试 `RouterFunction.route(req)` 找到 `HandlerFunction` 并执行  
  - 若找不到，就进入下一个处理（比如注解路由），或返回 404  
- [ ] **优先级**  
  - 定义：如果同时使用注解路由和函数式路由，**谁**优先？可在 `DispatcherHandler` 里决定顺序

**产出要求**：  
1. 可以使用函数式 DSL 声明路由，访问相应地址时返回正确结果；  
2. 当与注解路由冲突时，按你定义的顺序处理或报冲突错误；  
3. 无匹配返回 404。

---

## 任务5：编写核心分发器（`ReactiveDispatcherHandler`）并支持 Filter/Interceptor

**场景**：不管是注解路由还是函数式路由，你都需要一个**统一调度器**来完成请求分发；并在这里加入**Filter**机制，以实现鉴权、日志等通用逻辑。

- [ ] **`ReactiveDispatcherHandler`**  
  - 对外提供 `Mono<Void> handle(req, resp)`；  
  - 内部做：  
    1. 依次查询 **RouterFunction** 和 **HandlerMapping**；  
    2. 找到对应的处理器后，执行并写出响应（`Mono`/`Flux`）；  
    3. 未匹配则返回 404

- [ ] **Filter / Interceptor**  
  - 定义 `WebFilter` 接口：  
    ```java
    Mono<Void> filter(ReactiveServerRequest req, ReactiveServerResponse resp, FilterChain chain);
    ```  
  - `FilterChain` 封装调用下一个过滤器或最终 Handler 的逻辑；  
  - 在请求前可做鉴权、跨域，响应后做日志等。  
  - **异步提示**：通过 `Mono.defer()` 或 `flatMap` 控制异步调用，确保 filter 链有序执行。

- [ ] **异步返回 & 背压**  
  - 如果返回 `Flux<T>`, 要考虑客户端消费速度。可在写响应时使用 Netty 原生的 backpressure / Reactor 自带机制。  
  - 若数据量大，可探讨 `chunked` 传输或 SSE。 

**产出要求**：  
1. `DispatcherHandler` 主导请求分发；  
2. 可以注册多个 `WebFilter`，分别做日志/权限控制；  
3. 无匹配时 404，控制台或日志中记录相应信息。

---

## 任务6：细粒度异常处理

**场景**：在生产中，需要全局或局部地捕获异常并返回自定义错误响应。希望支持类似 Spring 的 `@ExceptionHandler`。

- [ ] **全局异常处理**  
  - 在 `DispatcherHandler` 或单独的“全局异常处理器”中捕获所有运行时异常；  
  - 返回通用错误响应，比如 JSON：`{"errorCode":500, "message":"Internal Error"}`；设置 HTTP 状态码 5xx

- [ ] **注解驱动异常**（可选）  
  - 在扫描阶段收集带 `@ExceptionHandler` 注解的方法；  
  - 若某个 Controller / @ControllerAdvice 类上有如：  
    ```java
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<String> handleIAE(IllegalArgumentException ex) { ... }
    ```  
    则在遇到该异常时，自动调用该方法。  
  - 支持 `@ResponseStatus` 等特性可更贴近 Spring。

- [ ] **测试**  
  - 编写一个会抛异常的 Controller 方法；  
  - 确认全局或局部异常处理器能返回正确 HTTP 状态码 + 自定义 JSON。

**产出要求**：  
1. 所有运行时异常能被捕获，返回合理的 4xx/5xx；  
2. 可对不同异常类型做不同处理；  
3. 有注解示例（如 `@ExceptionHandler`) 并通过测试。

---

## 任务7：与 MicroSpring IoC 整合 & 一键启动

**场景**：把前面做的 Reactive 服务器、注解/函数式路由、核心分发器、Filter、异常处理都整合到一个 `MicroSpringWebFluxApplication` 主入口，**一键启动**。

- [ ] **编写 Main 类**  
  - `public static void main(String[] args) { ... }`  
  - 启动 IoC 容器（`AnnotationConfigApplicationContext("com.example")`）；  
  - 获取 `ReactiveHandlerMapping` / `RouterFunction` / `DispatcherHandler` / Filter 等 Bean；  
  - 启动 `ReactiveHttpServer`，传入 `DispatcherHandler::handle`

- [ ] **打包成可执行 JAR**  
  - 在 `pom.xml` 用 `maven-shade-plugin` 或类似工具，把 `MicroSpringWebFluxApplication` 设为 `<mainClass>`；  
  - 运行 `mvn clean package` 后，可 `java -jar xxxxx.jar` 启动

**产出要求**：  
1. 一个可执行 .jar，一键监听 8080；  
2. 访问 `/flux/hello` 或函数式 `/router/xxx` 等路由时，正常返回；  
3. Filter、异常处理都生效。

---

## 任务8：可选高级扩展（含测试与性能优化）

**场景**：为你的 mini WebFlux 增加更多功能，与真实项目更接近，并做好测试与性能验证。

- [ ] **SSE (Server-Sent Events)**  
  - 对 `Flux<?>` 响应加 `Content-Type: text/event-stream`；  
  - 每次 `onNext` 写出 `data: ...\n\n` 并保持连接不断。

- [ ] **WebSocket**  
  - 实现协议升级到 WebSocket；  
  - 使用 Netty + Reactor 双向流式读写。

- [ ] **更多 Body 解析 / 消息转换**  
  - 如 Form Data、Multipart 文件上传、XML 等；  
  - 建立类似 `HttpMessageReader` / `HttpMessageWriter` 机制，动态选择解析器。

- [ ] **更多路由特性**  
  - 支持带 `@PathVariable` 路径 `/users/{id}`；  
  - `@RequestParam`, `@RequestHeader` 等。

- [ ] **监控 / Metrics**  
  - 输出请求量、响应时间、错误数等；  
  - 可对接 Prometheus/Grafana。

- [ ] **压力 & 异步测试**  
  - 使用 JMeter / Gatling 压测；  
  - 检查内存、线程无泄漏；  
  - 测试背压：如果客户端消费速度慢，服务器是否还能稳定？

- [ ] **性能优化**  
  - 缓存 `MethodHandle`，减小反射开销；  
  - Netty writeQueue 高水位线 / Reactor Schedulers 调优等。

---

### 小结

本任务列表在原有基础上，增加了以下关键要点：

1. **背压（Backpressure）提示**：在任务2、5 开始介入，让你逐步认识异步流和限流机制。  
2. **路由冲突检测 & 优先级**：在任务3、4 中明确，如果注解路由与函数式路由并存，如何处理冲突。  
3. **异常处理的颗粒度**：在任务6 中引入 `@ExceptionHandler`，支持对特定异常定制响应。  
4. **测试覆盖**：建议使用 StepVerifier 或 JMeter 等工具进行异步和压力测试，避免单纯依赖浏览器测试。  
5. **性能和线程模型**：在每个阶段都注意 Netty/I/O 线程与背压机制，不要把阻塞操作放在 IO 线程中。

这样，你能更好地**对标** Spring WebFlux 的常见功能，同时兼顾**生产环境的痛点**（异常处理、背压、路由冲突检测、性能优化等）。祝你开发顺利，学习愉快！