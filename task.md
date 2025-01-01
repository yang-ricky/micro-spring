# Micro Spring 任务列表

## 基础阶段（核心功能构建）

### 学习目标
1. 掌握 **IoC**（Inverse of Control）和 **DI**（Dependency Injection）的基本概念  
2. 能够使用反射机制**创建对象并完成注入**  
3. 实现 **BeanFactory** 的基础功能：Bean 注册、加载与获取  
4. 了解最基本的 **回调注入** 与 **复杂类型注入** 思想（初步尝试）

---

### 任务1：搭建项目结构与环境
**场景**：初始化一个最基础的项目框架

- [x] **创建 Maven 项目**  
  - 目录结构：`src/main/java` + `src/test/java`  
  - 在 `pom.xml` 中定义 `groupId`, `artifactId`, `version`, 并确保能正常编译

- [x] **定义核心类**：
  1. `BeanFactory`：用于管理和获取 Bean 的核心容器类  
  2. `BeanDefinition`：用于描述 Bean 的类、作用域、初始化方法等元信息  

- [x] **编写简单测试** 或 main 方法，验证工程能正常运行

**产出要求**：
1. 控制台可打印 “Micro Spring 启动”，或类似欢迎语  
2. `BeanFactory` 能加载最简单的 `BeanDefinition`（哪怕方法体是空的也行）  

---

### 任务2：实现最基本的 IoC 容器
**场景**：手动注册和获取 Bean

- [x] **完善 `BeanFactory`**：  
  - `registerBeanDefinition(String beanName, BeanDefinition bd)`  
  - `getBean(String beanName)`

- [x] **编写 Bean 创建逻辑**：  
  - 使用 `Class.forName(...)` 加载 Class  
  - `clazz.getDeclaredConstructor().newInstance()` 实例化

- [x] **支持单例模式**：  
  - 在 `BeanFactory` 中维护一个 `singletonObjects: Map<String,Object>` 用于单例缓存  
  - 如果 scope = singleton，则只创建一次

**产出要求**：
1. `getBean("someBean")` 多次调用，拿到的**是同一个实例**  
2. 控制台打印 `[BeanFactory] Creating bean: someBean` 等日志  

---

### 任务3：支持 XML 配置加载
**场景**：在早期 Spring 中，通过 XML 定义所有 Bean

- [x] **`XmlBeanDefinitionReader`** 类：  
  - `loadBeanDefinitions(String xmlPath)`  
  - 使用 DOM/SAX/Pull 解析 `<bean>` 标签，读出 `id`, `class`, `scope`, `init-method` 等

- [x] **`BeanFactory.readBeanDefinitions(xmlPath)`**  
  - 内部调用 `XmlBeanDefinitionReader` 返回若干 `BeanDefinition`  
  - 逐个 `registerBeanDefinition(beanName, bd)`

**产出要求**：
1. 在 `applicationContext.xml` 中写多个 `<bean>`  
2. `BeanFactory.readBeanDefinitions("applicationContext.xml")` 一次性加载  
3. 测试/日志里能看到解析到的 Bean 信息  

---

### 任务4：实现简单的依赖注入（构造器注入 & 属性注入）
**场景**：Bean A 依赖 Bean B，需要容器帮忙注入

- [x] **构造器注入**  
  - 在 `BeanDefinition` 增加 `constructorArgs`  
  - 在 Bean 创建时，通过 `clazz.getConstructor(...)` + `newInstance(...)` 传入依赖

- [x] **属性注入（Setter 注入）**  
  - 在 `BeanDefinition` 增加 `PropertyValues`  
  - 在 Bean 完成实例化后，用反射调用 `setXxx(...)`

- [x] **回调注入**（简单体验）  
  - 如果某个 Bean 需要在注入后做回调（如 `Aware` 接口思想），可以在“属性注入”后调用它的回调方法  
  - 比如自定义一个 `BeanNameAware`，让 Bean 可以感知自己的 `beanName`

- [x] **复杂类型注入**  
  - 在 XML 中配置 `<property name="address"><value>xxxxx</value></property>` 或 `<list>`, `<map>` 等集合类型  
  - 在加载时转换为对应的集合或对象

**产出要求**：
1. `Bean A` 通过构造器/Setter 拿到 `Bean B`  
2. 支持注入普通属性(字符串、整数) + 简单集合(list/map)  
3. 有能力做一个回调，比如 `setBeanName()` 或 `afterPropertiesSet()`  

---

## 中级阶段（增强功能与优化设计）

### 学习目标
1. 学习 **ApplicationContext** 分层设计  
2. 掌握 **组件扫描**、**注解驱动 IoC**  
3. 支持 **延迟注入**（lazy-init）、**SpEL 表达式注入** 等更灵活的注入方式  
4. 继续丰富**Bean 生命周期**（Init/Destroy 回调），并学习 **BeanPostProcessor**  
5. 初步接触 **AOP**（动态代理 + CGLIB），掌握**原生动态代理**与 **CGLIB** 的区别

---

### 任务5：分层设计 + `ApplicationContext`
**场景**：类似 Spring 的 `BeanFactory` + `ApplicationContext`

- [x] **拆分接口与实现**：  
  - `BeanFactory` 只定义获取 Bean、注册 BeanDefinition 等核心方法  
  - `DefaultBeanFactory` 作为基本实现

- [x] **引入 `ApplicationContext`**  
  - `ApplicationContext` 封装更多高级功能：资源加载、事件发布  
  - `ClassPathXmlApplicationContext`：一次性加载指定路径下的 XML 并执行 `refresh()`  
  - 在 `refresh()` 中统一完成**BeanDefinition 注册** + **Bean 创建**

**产出要求**：
1. `new ClassPathXmlApplicationContext("appContext.xml")` 即可自动加载和初始化所有 Bean  
2. 容器分层更清晰，后续功能方便接入  

---

### 任务6：注解驱动 IoC（组件扫描）
**场景**：支持 `@Component`, `@Autowired` 等注解，减少 XML 配置

- [ ] **组件扫描**  
  - 在 `ApplicationContext` 里添加 “包扫描” 功能  
  - 遍历目标包的所有类，凡是带 `@Component` 就自动注册为 BeanDefinition  
  - Bean 的 `scope`、`init-method` 等可在注解或默认值中指定（如 `@Scope("prototype")`）

- [ ] **自动装配**  
  - 对字段或构造器参数带 `@Autowired` 时，自动根据类型查找候选 Bean  
  - 如果有多个同类型 Bean，可用 `@Qualifier` 区分

- [ ] **属性注入 + SpEL 支持**  
  - 在字段或 `@Value("#{systemProperties['user.home']}")` 之类的场景  
  - 先只需实现简单表达式，如 `@Value("${db.url}")` 从配置文件中读取；或 `@Value("#{1+2}")` 进行简单运算

**产出要求**：
1. 仅使用注解（`@Component`, `@Autowired`, `@Value`），无需在 XML 里声明 Bean  
2. 包扫描能自动加载并创建 Bean  
3. 测试中能看到 SpEL 表达式成功注入属性  

---

### 任务7：延迟注入（lazy-init）
**场景**：有些 Bean 不在容器启动时立即创建，而是在第一次被使用时才创建

- [ ] **BeanDefinition 增加 `lazyInit` 标识**  
  - 若 `lazyInit = true`，则在容器启动时**不**创建该 Bean  
  - 只有当 `getBean(...)` 第一次调用时，再真正实例化

- [ ] **在创建容器时**：  
  - 仍扫描/注册所有 Bean，但只实例化非 lazy-init 的 Bean  
  - 对于 lazy-init 的 Bean，往往需要**动态代理**或**记录**以延迟加载

- [ ] **测试**  
  - 在 XML/注解配置里标记一个 Bean 为 `lazy-init`  
  - 启动容器后，查看日志，确认它**尚未创建**  
  - 当第一次 `getBean("lazyBean")` 时，才看到日志 “Creating lazyBean ...”

**产出要求**：
1. 能区分“普通 Bean 立即创建”和“lazy-init Bean 延迟创建”  
2. 通过日志能验证延迟注入过程  

---

### 任务8：增强 Bean 生命周期管理（包括回调销毁、回调注入）
**场景**：丰富初始化和销毁过程；学习回调注入细节

- [ ] **BeanDefinition** 增加 `initMethodName`, `destroyMethodName`  
- [ ] **在容器刷新完毕**：对所有**非 lazy** 的单例 Bean 调用 init 方法  
- [ ] **在容器关闭时**：对所有单例 Bean 调用 destroy 方法  
- [ ] **回调注入**：如实现 `BeanNameAware`, `BeanFactoryAware`，在创建 Bean 时回调

**产出要求**：
1. 测试添加一个 `DemoBean`，其中 `initMethodName="initMe"`，`destroyMethodName="cleanup"`  
2. 关闭容器时，能看到 “cleanup ...” 的日志  
3. 若实现自定义回调接口 `BeanNameAware` 等，能在 Bean 内部拿到 beanName  

---

### 任务9：BeanPostProcessor 扩展 + AOP（动态代理）
**场景**：为所有 Bean 的创建过程插入钩子；并尝试 AOP 原理

- [ ] **`BeanPostProcessor`**  
  - 在容器创建完一个 Bean（但在 initMethod 之前），依次调用已注册的 `BeanPostProcessor`  
  - 可以在 `postProcessBeforeInitialization` 做属性检查；在 `postProcessAfterInitialization` 返回代理对象

- [ ] **原生动态代理 (JDK)**  
  - 当某个 Bean 需要 AOP（比如带 `@Aspect` 或配置文件指示），用 JDK Proxy 包装  
  - 仅支持接口代理，若 Bean 没有实现接口，则无能为力

- [ ] **CGLIB**  
  - 当需要代理无接口的类，就尝试使用 CGLIB  
  - 需在 `pom.xml` 中添加 CGLIB 依赖

- [ ] **日志切面 / 性能切面**  
  - 写一个简单的 `LogAdvice`，在方法执行前后打印日志  
  - 测试调用 Bean 的方法，看是否有拦截效果

**产出要求**：
1. `BeanPostProcessor` 能**统一**拦截所有 Bean 的初始化过程  
2. JDK/CGLIB 动态代理能让 Bean 方法添加日志输出  
3. 观察**原生动态代理** vs **CGLIB** 的区别（可在日志或文档中说明代理类型）  

---

## 高级阶段（扩展与容错）

### 学习目标
1. 进一步掌握 **条件装配**（@Conditional）、**模块装配**  
2. 解决 **循环依赖** 问题（如 A ↔ B 互相注入）  
3. 处理复杂的异常，提升容器容错能力  

---

### 任务10：条件装配（Conditional）与模块装配
**场景**：只有在满足某些条件下，才装配特定 Bean 或模块

- [ ] **定义 `@Conditional`** 注解  
  - 接收一个“条件判断类”参数，如 `MyCondition.class`  
  - `MyCondition` 实现一个 `matches(Context context)` 方法，用来判定是否装配

- [ ] **在加载 BeanDefinition 时**  
  - 若某个类有 `@Conditional(WindowsCondition.class)`，则先判断 `WindowsCondition.matches(...)` 是否为 true  
  - true → 注册 BeanDefinition，false → 不注册

- [ ] **模块装配**  
  - 在 XML 或注解中，根据不同环境（dev / prod）加载不同数据库配置 Bean  
  - 也可根据 `System.getProperty("os.name")` 判断是 Windows / Linux

**产出要求**：
1. 配置若干 “条件 Bean”，只有在 `os.name=Windows` 时才加载  
2. 日志/测试里能看到“BeanX is skipped due to condition” 或 “BeanX is loaded”  

---

### 任务11：解决循环依赖问题（三级缓存）
**场景**：A ↔ B 相互注入

- [ ] **三级缓存**：  
  - 一级缓存：完整创建的单例 Bean  
  - 二级缓存：半成品 Bean（未完成初始化）  
  - 三级缓存：ObjectFactory 用于生成代理对象时，避免循环依赖

- [ ] **修改注入逻辑**：  
  - 如果注入 B 时发现 B 也在创建中，就从二/三级缓存取“早期 Bean”  
  - 最终都初始化完后，放到一级缓存

- [ ] **测试**：  
  - `ClassA` 有 `ClassB b;`  
  - `ClassB` 有 `ClassA a;`  
  - `getBean("A")` 不抛 `BeanCurrentlyInCreationException`

**产出要求**：
1. 可以成功创建有循环依赖的 Bean  
2. 确认里面字段确实都互相持有对方  
3. 如果出现代理场景，需要三级缓存来解决  

---

### 任务12：属性注入（SpEL 表达式增强）
**场景**：更复杂的表达式注入

- [ ] **拓展 SpEL**  
  - 支持 `#{beanB.someValue + 100}` 这类表达式  
  - 或 `@Value("#{'Hello ' + systemProperties['user.name']}")`  
  - 需要自己做一个小的解析器，或引入 Spring Expression Engine

- [ ] **与循环依赖 / 条件装配配合**  
  - 复杂表达式可能依赖其他 Bean 的属性  
  - 如果依赖的 Bean 还没初始化，可能就会出现先后顺序的问题

**产出要求**：
1. 在注解/XML 里写 `@Value("#{beanB.price * 1.1}")`  
2. 成功给某个属性赋值 = B 的 price * 1.1  
3. 测试中能打印结果，确保 SpEL 工作正常  

---

### 任务13：增强异常体系与容错处理
**场景**：捕获更多错误，提供友好提示

- [ ] **自定义异常**：`BeanCreationException`, `NoSuchBeanDefinitionException`, `CircularDependencyException` 等  
- [ ] **在创建、注入、销毁各阶段** 捕获异常并包装成自定义异常  
- [ ] **日志与提示**：打印清晰的“错误上下文”，如当前 Bean 名称、依赖栈等

**产出要求**：
1. 当 Bean 类不存在或构造器参数不匹配时，抛 `BeanCreationException`  
2. 当出现循环依赖未能解决时，抛 `CircularDependencyException` 并打印相关信息  
3. 方便使用者排查错误  

---

## 附加任务（可选拓展）

1. **集成 JDBC 或其他外部资源**  
   - 提供简易版 `DataSource` 或 `JdbcTemplate`，在容器启动时初始化  
   - `@Value("${jdbc.url}")` 读配置，或 SpEL 来拼装字符串  

2. **实现简单的 MicroSpringMVC**  
   - 定义 `@Controller`, `@RequestMapping` 注解  
   - 在内部维护一个 `HandlerMapping` 将请求路径映射到方法  
   - 与 **MicroTomcat** 集成时，把 `DispatcherServlet` 注册成 Servlet  

3. **基于注解的事务管理**  
   - `@Transactional` + AOP：进入方法前开启事务，结束后 commit/rollback  
   - 配合数据库层，能演示最小事务案例  

4. **配置文件热更新**  
   - 监控 XML/注解文件变化，自动 `refresh()` 容器  
   - 注意已存在 Bean 的重载逻辑  

5. **与 MicroTomcat 整合**  
   - 打包成可执行 JAR，包括 `MicroTomcat + MicroSpring`  
   - `java -jar micro-spring-application.jar` 即可启动  
   - 访问 `http://localhost:8080/` 由 `DispatcherServlet` 分发到 Controller  

6. **性能调优**  
   - 压测 Bean 创建速度、AOP 代理开销  
   - 对比 JDK 动态代理 vs CGLIB  

7. **安全框架**  
   - 定义 `@Secured("ROLE_USER")` 注解  
   - 在容器或 MVC 层做认证与授权逻辑  

8. **原生动态代理 vs CGLIB 深入**  
   - 比较二者在**有无接口**场景下的可用性、性能差别  
   - Logging，Benchmark 结果写在文档中  

9. **条件装配 + 多环境配置**  
   - 针对 dev / test / prod 多环境，通过 `@Profile` 或 `@Conditional` 控制 Bean 是否注入  
   - 在容器启动时判断当前激活环境变量  
