下面给你更**细化**的两大任务拆解，以帮助你在现有 **Micro Spring**（IoC 容器）和 **Micro Tomcat**（精简版 Servlet 容器）基础上，完成一个**最小可用的 MVC 框架**并能“**一键打包、java -jar 启动**”。

---

## 一、实现简易的 MicroSpringMVC

### 目标概述

1. **定义注解**：`@Controller`, `@RequestMapping`，模拟 Spring MVC 的最基础特性。  
2. **扫描这些注解**，自动收集“映射信息”：\[请求路径] → \[对应的控制器方法]。  
3. **实现核心类 `DispatcherServlet`**，在收到 HTTP 请求后：  
   - 根据请求路径，找到对应的控制器方法；  
   - 反射调用方法，获取结果；  
   - 将结果写回响应。  
4. **实现 HandlerMapping/HandlerAdapter** 等简化组件，保证代码结构清晰，可扩展。

### 详细任务清单

#### 1. 定义注解

1. **`@Controller`**  
   - 标记一个类为 Controller（可以放在类上）。  
   - 类似 `@Component`，不过语义是用于 MVC 层。  

2. **`@RequestMapping`**  
   - 用于标记类或方法，保存请求路径、请求方法（GET/POST）等信息。  
   - 例如：  
     ```java
     @Controller
     @RequestMapping("/demo")
     public class DemoController {

         @RequestMapping("/hello")
         public String hello(...) {
             return "hello page";
         }
     }
     ```
   - 可以先只支持 `value` 属性（请求路径），忽略其它复杂功能（如 `method=GET`、`produces=`等）。

**产出**：  
- 一个或两个注解类 `.java` 文件，结构类似：
  ```java
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Controller {}

  @Target({ElementType.TYPE, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface RequestMapping {
      String value() default "";
  }
  ```

---

#### 2. 扫描 Controller 类并收集映射信息

1. **包扫描**  
   - 在 `ApplicationContext` 或一个单独的类（如 `ControllerScanner`）里，对指定包进行**Class**扫描。  
   - 判断哪些类含有 `@Controller` 注解，如果有则将其纳入“候选 Controller”。  

2. **读取方法上的 `@RequestMapping`**  
   - 对于每个 Controller，找到它所有的方法；若方法上有 `@RequestMapping`，则记录“请求路径 -> 方法信息”。  
   - 可能需要组合类上的 `@RequestMapping("/demo")` 和方法上的 `@RequestMapping("/hello")`，得到完整路径 `/demo/hello`。  

3. **存储映射信息**  
   - 可以设计一个简单的 **`HandlerMapping`** 类/接口：  
     - 内部维护一个 **`Map<String, HandlerMethod>`**，key 为完整 URL 路径，value 为一个对象（包含 controller实例、Method反射对象 等）。  
   - 在容器刷新完（或在 DispatcherServlet init 时）做这一步，保证 Controller 已经被 IoC 容器创建好。

**产出**：  
- `HandlerMapping` 内部有一张映射表，如：  
  ```plaintext
  "/demo/hello" -> { controllerBean=DemoController@1234, method=DemoController.hello() }
  "/user/add"   -> { controllerBean=UserController@5678, method=UserController.addUser() }
  ```
- 能打印日志：`[HandlerMapping] Mapped /demo/hello -> DemoController.hello()`

---

#### 3. 实现 `DispatcherServlet`

1. **Servlet 的职责**  
   - 解析请求路径，如 `"/demo/hello"`；  
   - 从 `HandlerMapping` 中找到对应的 `HandlerMethod`；  
   - 通过反射调用 method，并把结果写入 `Response`。  

2. **初始化**  
   - 在 `init()` 里获取 **`ApplicationContext`**（或 `BeanFactory`），拿到 `HandlerMapping`（或者自行扫描？看你设计）。  
   - 将 `HandlerMapping` 存到 DispatcherServlet 的成员变量里。  

3. **处理请求** (`service()` / `doGet()` / `doPost()` 等)  
   - 获取请求URI，假定形如 `/demo/hello`。  
   - 从 `handlerMapping.getHandler("/demo/hello")` 得到 `HandlerMethod`。  
   - 如果找不到，返回 404。  
   - 如果找到，则通过反射执行 `method.invoke(controllerBean, ...)`。  
     - **入参解析**：可以先只支持无参或 (HttpServletRequest, HttpServletResponse) 两个参数。  
     - **返回值**：可以先只支持 `String`，表示要返回的“视图名”或“文本内容”。  

4. **返回响应**  
   - 如果方法返回一个 `String`，你可以直接 `response.getWriter().write(returnValue)`；  
   - 或者可以区分视图名、JSON 等，本阶段先做最简化：直接输出。  

**产出**：  
- 一个 `DispatcherServlet.java`，有 `init()`、`service()` 等方法；  
- 当调用浏览器访问 `/demo/hello` 时，能看到你的 controller 返回的字符串。

---

#### 4. 注册 `DispatcherServlet` 到 MicroTomcat

1. **在 MicroTomcat 的 `Context` 或 `Wrapper`** 中，把 `DispatcherServlet` 当作 Servlet 类进行注册；  
2. 或者在 `web.xml` 模式下写：  
   ```xml
   <servlet>
     <servlet-name>dispatcher</servlet-name>
     <servlet-class>com.microtomcat.example.DispatcherServlet</servlet-class>
   </servlet>
   <servlet-mapping>
     <servlet-name>dispatcher</servlet-name>
     <url-pattern>/</url-pattern>
   </servlet-mapping>
   ```
   不过你现在很可能手动注册即可。

3. **确保 `DispatcherServlet` 在 `init()` 时** 能拿到 `ApplicationContext`（你可以通过某种全局单例或在初始化顺序里先启动 MicroSpring，再把它传给 DispatcherServlet）。  

**产出**：  
- 访问 `http://localhost:8080/demo/hello`，能够进入 `DispatcherServlet`，调用 `DemoController.hello()` 并输出返回内容。  

---

#### 5. 进一步扩展

- 支持更多注解：`@RequestParam("id")` 来获取请求参数、`@ResponseBody` 返回 JSON 等。  
- 支持视图解析：如果 `@Controller` 方法返回 `"hello.jsp"`，则 DispatcherServlet 去 `WEB-INF/views/hello.jsp` 找资源。  
- 添加一个 **`HandlerAdapter`** 接口，把 “handler method 调用” 的具体逻辑抽离出来，以便支持多种类型的处理器（类似 Spring MVC 的结构）。

若只要简易版本，可暂时忽略这些高级特性。

---

## 二、与 MicroTomcat 整合为可执行 Jar

### 目标概述

1. **在同一个项目/同一个打包**里包含：MicroTomcat + MicroSpring。  
2. **启动入口**：`java -jar micro-spring-application.jar`，在 `main()` 里：  
   - 初始化 MicroSpring 容器；  
   - 启动 MicroTomcat（port=8080）；  
   - 注册 `DispatcherServlet` or 其它 Servlet；  
   - 一切准备就绪后，等待请求。  
3. 访问 `http://localhost:8080/` 时，由 `DispatcherServlet` 把请求转发到对应的 Controller。  

### 详细任务清单

#### 1. 定义一个 Main 类（或 MicroSpringApplication）

```java
public class MicroSpringApplication {
    public static void main(String[] args) {
        // 1. 启动 MicroSpring 容器
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        
        // 2. 初始化 MicroTomcat
        ServerConfig config = new ServerConfig();
        config.setPort(8080);
        config.setWebRoot("webroot"); // 如果用到了静态资源
        
        AbstractHttpServer tomcatServer = HttpServerFactory.createServer(config);
        tomcatServer.init();

        // 3. 注册 DispatcherServlet
        //    这里可能要看 micro-tomcat 的容器结构，找到 rootContext = Host->Context->Wrapper
        //    Wrapper wrapper = new Wrapper("DispatcherServlet", "xx.xx.DispatcherServlet");
        //    ...
        //    context->addChild(wrapper);
        
        // 4. 启动 Tomcat
        tomcatServer.start();
        
        // 5. Log/Wait
        System.out.println("MicroSpring + MicroTomcat started on port 8080");
    }
}
```

- 由于你的 MicroTomcat 与官方 Tomcat 不同，怎么“注册 Servlet” 需要看你自己的实现。有的示例是：
  ```java
  Context rootContext = new Context("", "webroot");
  rootContext.addChild(new Wrapper("dispatcher", "com.example.DispatcherServlet"));
  host.addChild(rootContext);
  engine.addChild(host);
  ```
- 核心思路：**把 `DispatcherServlet` 当成一个 Wrapper/Servlet** 放到 MicroTomcat 里即可。  

---

#### 2. 在 `pom.xml` 中引入依赖

- 依赖 **micro-tomcat-core** + **micro-spring-core**（如果你把这两个做成 separate modules）。  
- 若都在同一个多模块项目里，也可以用 `<module>` + `<dependency>` 方式。  
- 例如：

```xml
<dependencies>
    <dependency>
        <groupId>com.microtomcat</groupId>
        <artifactId>micro-tomcat-core</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>com.microspring</groupId>
        <artifactId>micro-spring-core</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    <!-- 其他需要的依赖，如 logging, JSON 等 -->
</dependencies>
```

---

#### 3. 打包成可执行 Jar

1. **使用 Maven 的 `maven-shade-plugin` 或 `spring-boot-maven-plugin`**：  
   - `maven-shade-plugin`：可以把所有依赖打入一个大 jar；并指定 `Main-Class` 即 `MicroSpringApplication`。  
   - `spring-boot-maven-plugin`：也能把项目打包成可执行 jar，但这通常和 Spring Boot 强绑定，不一定适合你自己的 micro-spring。  

2. **在 `pom.xml` 里配置**（以 `maven-shade-plugin` 为例）：
   ```xml
   <build>
     <plugins>
       <plugin>
         <groupId>org.apache.maven.plugins</groupId>
         <artifactId>maven-shade-plugin</artifactId>
         <version>3.2.4</version>
         <executions>
           <execution>
             <phase>package</phase>
             <goals>
               <goal>shade</goal>
             </goals>
             <configuration>
               <createDependencyReducedPom>true</createDependencyReducedPom>
               <transformers>
                 <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                   <mainClass>com.microspring.MicroSpringApplication</mainClass>
                 </transformer>
               </transformers>
             </configuration>
           </execution>
         </executions>
       </plugin>
     </plugins>
   </build>
   ```

3. **打包命令**：  
   - `mvn clean package -DskipTests`  
   - 生成的 jar 通常在 `target/micro-spring-application-1.0-SNAPSHOT-shaded.jar` 之类。

**产出**：  
- 一个可执行 jar，如 `micro-spring-application.jar`，能**直接执行**：  
  ```bash
  java -jar micro-spring-application.jar
  ```
- 启动后自动**MicroTomcat** 在 8080 端口监听，**MicroSpring** 容器也就绪，能访问你的 Controller。  

---

#### 4. 测试验证

- 启动后访问：`http://localhost:8080/xxx`  
- 如果配置了 `DispatcherServlet` 拦截所有路径 `/`，可以访问 `http://localhost:8080/demo/hello` 试试。  
- 查看控制台日志确认：  
  1. MicroTomcat 已启动；  
  2. MicroSpring 容器已加载 Bean；  
  3. 调用了 `DispatcherServlet` 的 `init()` 方法；  
  4. 收到了 HTTP 请求，调用了对应的 Controller 方法。  

---

## 总结

1. **MicroSpringMVC** 部分：让你以 **注解驱动** 的方式，实现一套最简的 MVC 架构：  
   - `@Controller` 标记类；  
   - `@RequestMapping` 标记路由；  
   - `DispatcherServlet` + `HandlerMapping` 做运行时分发。  
2. **与 MicroTomcat 整合**：最终打包成一个可执行 jar，把 **IoC 容器** + **Servlet 容器** 合并在一起；通过一个 `main()` 方法统一**启动**，外部只需执行 `java -jar` 即可访问。

以上细化的步骤，能让你清晰地把 **MicroSpring** 和 **MicroTomcat** 融合成一个**一键可运行**的简易 Web 框架。祝你开发顺利、学习愉快!