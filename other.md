下面给出两个全新创建的任务模块，分别覆盖 **Spring 事件机制（ApplicationListener）** 和 **JDBC 整合**。在设计时，依旧保持“小步迭代、循序渐进”的原则，并尽量拆分成较小的可执行任务，以便初学者能逐步完成并理解其中原理。

---

# 进阶任务列表（新增）

## 任务15：Spring 事件机制（ApplicationEvent & ApplicationListener）

### 学习目标
1. 理解 Spring 事件模型中 **ApplicationEvent** 与 **ApplicationListener** 的基本概念  
2. 掌握如何定义自定义事件，并在容器内分发与监听  
3. 体会事件驱动开发的思想，以及与 **Bean 生命周期**、**容器刷新流程** 的关联

### 任务15.1：支持 ApplicationEvent 与 ApplicationListener
**场景**：手写最简版事件模型，在容器启动或手动触发时，分发事件给 Listener

- [ ] **定义 `ApplicationEvent`**  
  - 类似 `public class ApplicationEvent { private final long timestamp; ... }`  
  - 提供构造方法，记录事件产生时间、事件源对象等信息

- [ ] **定义 `ApplicationListener`**  
  - 接口：`public interface ApplicationListener<E extends ApplicationEvent> { void onApplicationEvent(E event); }`  
  - 用来监听特定类型的事件

- [ ] **在 `ApplicationContext` 中增加事件发布功能**  
  - 创建一个 `ApplicationEventPublisher` 接口，提供 `publishEvent(ApplicationEvent event)` 方法  
  - `ApplicationContext` 默认实现此接口，内部维护一组 `ApplicationListener`  
  - 当调用 `publishEvent(...)` 时，遍历并调用对应的 `listener.onApplicationEvent(...)`

#### 产出要求
1. 定义一个简单的事件类（如 `ContextRefreshedEvent`）  
2. 定义一个监听器（如 `ContextRefreshedListener` 实现 `ApplicationListener<ContextRefreshedEvent>`）  
3. 在 `ApplicationContext.refresh()` 方法结束时，调用 `publishEvent(new ContextRefreshedEvent(this))`  
4. 监听器收到事件后，输出日志 “容器刷新完毕事件已接收” 或类似提示  

---

### 任务15.2：自定义事件与监听器
**场景**：除了容器内部事件外，用户可能需要发布自定义业务事件，进行解耦

- [ ] **自定义事件**  
  - 比如 `UserRegisteredEvent`，包含一个 `User` 对象或必要的用户信息

- [ ] **编写自定义监听器**  
  - `UserRegisterListener` 实现 `ApplicationListener<UserRegisteredEvent>`  
  - 在 `onApplicationEvent(...)` 内进行后续操作，如发送欢迎邮件、打印日志等

- [ ] **事件触发**  
  - 在任何地方，拿到 `ApplicationEventPublisher` 后，调用 `publisher.publishEvent(new UserRegisteredEvent(user))`

#### 产出要求
1. 在示例测试中，模拟用户注册流程，触发 `UserRegisteredEvent`  
2. 日志中可看到“已接收用户注册事件”的输出  
3. 演示事件驱动的好处：监听器与业务逻辑解耦，后续可轻松添加/替换监听器  

---

### 任务15.3：事件层次结构与高级玩法（可选）
**场景**：扩展事件模型，如让一个监听器可以监听多种事件；或事件之间存在继承关系

- [ ] **事件继承结构**  
  - `ApplicationContextEvent` → `ContextRefreshedEvent` / `ContextClosedEvent` / `ContextStartedEvent` 等  
  - 监听器也可以监听父类事件

- [ ] **一次发布，多层监听**  
  - 当发布一个 `ContextRefreshedEvent` 时，监听 `ContextRefreshedEvent` 的监听器和监听 `ApplicationContextEvent` 的监听器都能收到

- [ ] **组合监听器**  
  - 一个监听器实现多个事件类型的处理；或使用 `SmartApplicationListener`（如果实现更高级功能）

#### 产出要求
1. 观察事件继承带来的监听行为差异  
2. 在日志中观察到同一个事件被多个兼容监听器捕获  
3. 自行决定是否要引入更复杂的事件层次（可选）

---

## 任务16：JDBC 整合

### 学习目标
1. 学习如何在 **Micro-Spring** 中使用 JDBC 访问数据库  
2. 掌握在 IoC 容器中管理 **DataSource**、使用自动配置或手动配置方式  
3. 基础的数据库操作（增、删、改、查），体会与 IoC 注入的结合

### 任务16.1：引入 DataSource 并手动配置
**场景**：在 XML 或注解中声明数据库连接参数，并在容器启动时创建一个 DataSource

- [x] **定义 `DataSource` Bean**  
  - 可以使用最简单的 `DriverManagerDataSource`（自行编写或参考 Spring 同名类），持有：  
    - `url`, `username`, `password`, `driverClassName` 等属性  
  - 在 `BeanFactory` / `ApplicationContext` 初始化时，加载并创建这个数据源实例

- [x] **连接测试**  
  - 在初始化完成后，用 `DataSource.getConnection()` 简单测试能否成功连接数据库  
  - 如果失败，抛出异常或打印错误日志，提示连接信息

#### 产出要求
1. 在 XML / 注解里配置 `dataSource` Bean，指定数据库连接参数  
2. 容器启动时，能够连接到测试数据库，并打印 “DataSource Connected Successfully”  
3. 如果无法连接，提供友好异常消息，如 “Failed to connect to DB: url=xxx”  

---

### 任务16.2：创建简易版 JDBC Template 工具类
**场景**：简化 JDBC 常规操作（增删改查）

- [x] **定义 `JdbcTemplate`** 类  
  - 持有 `DataSource`  
  - 封装常见的 `executeUpdate(sql, params...)` / `executeQuery(sql, rowMapper, params...)` 等方法  
  - 内部要处理 `Connection → PreparedStatement → ResultSet → 关闭` 流程

- [x] **注入 `JdbcTemplate`**  
  - 通过 IoC 容器，`JdbcTemplate` 构造器或 Setter 注入 `dataSource`  
  - 在 XML / 注解中声明 `JdbcTemplate` Bean，让容器自动装配

- [x] **RowMapper 机制**  
  - 定义 `RowMapper<T>` 接口，`T mapRow(ResultSet rs, int rowNum)`  
  - 业务层通过实现 `RowMapper` 将结果集转换为对象

#### 产出要求
1. 在测试类中使用 `JdbcTemplate` 调用 `executeUpdate("INSERT ...")` 或 `executeQuery("SELECT ...", rowMapper)`  
2. 看到数据库里确实写入/读取了数据  
3. 能通过日志或断言验证 `RowMapper` 是否正确工作  

---

### 任务16.3：事务管理（可选简单版本）
**场景**：在数据库操作中，如果有多条 SQL，需要保证他们在一个事务中执行

- [x] **定义 `TransactionManager`**  
  - 可用最简单的 JDBC 事务：`conn.setAutoCommit(false); conn.commit(); conn.rollback();`

- [x] **AOP + 注解方式**（或手动方式）  
  - 可以先用手动方式：在业务方法里拿到 `TransactionManager`，调用 `begin`、`commit`、`rollback`  
  - 若想集成 AOP + `@Transactional`，可扩展 `BeanPostProcessor` 生成代理

#### 产出要求
1. 编写一个示例：插入多条数据，一条失败后整体回滚  
2. 验证事务确实生效：出现异常后，数据库里不应出现残余数据  
3. 提供日志或测试报告，说明事务执行过程  

---

### 任务16.4：容错与连接池（可选进阶）
**场景**：如果数据库连接不稳定，需要重试；或者通过连接池提高效率

- [x] **连接重试**  
  - 如果连接失败，尝试 X 次重试  
  - 记录或报警，以排查网络问题

- [x] **使用连接池**  
  - 简易实现或引入第三方连接池（如 HikariCP、Druid）  
  - 在 `dataSource` Bean 中初始化连接池参数

#### 产出要求
1. 当数据库短暂不可用时，能在日志中看到重试过程  
2. 使用连接池后，观察多线程场景下的性能提升（可做简单对比测试）  
3. 记录关键参数（最大连接数、超时时间等），并给出调优建议  

---

## 总结

这两个新增的任务（**Spring 事件机制** 和 **JDBC 整合**）能够让初学者在已有的 Micro-Spring 框架基础上，进一步体验到 Spring 生态中常见的功能点：

1. **事件驱动开发**：通过 ApplicationEvent + ApplicationListener，感受容器如何在内部与外部事件之间架设“桥梁”；在解耦的同时也能将业务流程做得更灵活、可扩展。  
2. **数据库访问**：整合 JDBC 并包装成一个简易版 `JdbcTemplate`，让初学者熟悉数据库交互的常规流程，也能对后续 MyBatis / JPA 等高级 ORM 工具有更清晰的理解。

和之前的任务一样，务必在实现过程中多写日志、多做单元测试，并随时记录遇到的问题和心得。通过这些小步的任务拆分，初学者能在较短的周期内完成可工作的功能，且对框架内部原理有更加深刻的认识。祝大家学习愉快！