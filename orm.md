下面是一个**“spring-orm”** 相关的进阶任务列表示例，模仿你的格式和风格，以便让初学者在 **micro-spring** 框架中逐步实现或整合 ORM 功能，并与 **IoC**、**AOP**、**JDBC**、**事务** 等结合使用。示例里主要以 **JPA/Hibernate** 整合为核心，亦可扩展到 **MyBatis** 等其他 ORM 工具。

---

# 进阶任务列表（新增）

## 任务17：引入 Spring ORM 思想 - JPA/Hibernate 整合

### 学习目标
1. 了解 **ORM**（对象关系映射）的基本概念以及 **JPA** 标准  
2. 初步掌握在 **micro-spring** 环境下进行 **Hibernate** 或 **JPA Provider** 的整合流程  
3. 体会与 **JdbcTemplate**、**DataSource**、**事务管理** 等基础组件的互相配合

---

### 任务17.1：创建 `micro-spring-orm` 子模块，并声明基础结构

**场景**：我们需要一个专门的模块来处理 ORM 整合逻辑，就像 Spring 官方的 *spring-orm* 一样。  

- [ ] **创建 `micro-spring-orm` 模块**  
  - 在项目结构中新增一个子项目/模块 `micro-spring-orm`  
  - 保持与核心 **micro-spring-core**、**micro-spring-context**、**micro-spring-aop** 等解耦，便于独立迭代

- [ ] **编写初始类：`OrmConfiguration`**  
  - 用于加载 ORM 相关配置，例如指定数据库方言（dialect）、实体类扫描、自动建表策略等  
  - 暂时可以先留空或提供示例 `application.properties` 读取

- [ ] **引入 `EntityManagerFactory` / `SessionFactory`**  
  - 若是 JPA 方式：定义简易版 `EntityManagerFactory` 封装，里面持有 Hibernate 的原生 `SessionFactory`  
  - 若是直接用 Hibernate：则直接管理 `org.hibernate.SessionFactory`  
  - 通过 BeanDefinition & BeanFactory 注册，让容器能生成并管理它

#### 产出要求
1. 在 `micro-spring-orm` 中，有一个核心配置类/组件能够读取 ORM 配置，并创建 `EntityManagerFactory` 或 `SessionFactory` 对象  
2. 日志输出 “micro-spring-orm: EntityManagerFactory created successfully” 或类似提示  
3. 在 `pom.xml`（或 build.gradle）中添加相应的依赖（Hibernate / JPA API），确保能编译运行

---

### 任务17.2：扫描并注册 JPA/Hibernate 实体

**场景**：通常我们需要扫描带有 `@Entity` 或 `@Table` 注解的实体类，然后让 Hibernate/JPA 为其生成数据表与映射信息。  

- [ ] **支持 `@Entity` 注解**  
  - 在 `OrmConfiguration` / `EntityManagerFactoryBuilder` 等处，扫描应用中的包，找到所有含 `@Entity` 的类  
  - 将这些类注册到 Hibernate 的配置（`MetadataSources` 或类似 API）里

- [ ] **实体示例**  
  - 定义一个简单实体 `User`，带有 `@Entity`、`@Table(name="users")` 注解，属性如 `id`, `username`, `email` 等  
  - 在数据库中让 Hibernate 自动建表或更新表结构

- [ ] **自动表生成**（可选）  
  - 可通过 Hibernate 配置 `hibernate.hbm2ddl.auto` = `update` / `create-drop` 等选项，自动建表

#### 产出要求
1. 在日志中看到扫描到的实体类列表，如 “Found entity class: com.example.User”  
2. 启动时，自动/手动在数据库里生成对应表结构 `users`  
3. 若生成失败或未配置数据源，抛出友好提示，如 “No DataSource found for ORM initialization”  

---

### 任务17.3：集成简单的 **Session / EntityManager** 操作

**场景**：在 Spring ORM 中，你可以通过 `HibernateTemplate`、`JpaTemplate`（或更现代的 `EntityManager`）来进行增删改查；在 micro-spring-orm 中也需要一个简易封装。  

- [ ] **定义 `OrmTemplate`**（类似 JdbcTemplate）  
  - 持有 `SessionFactory` 或 `EntityManagerFactory`  
  - 提供常用操作：`save(Object entity)`, `find(Class<T> entityClass, Object id)`, `update(...)`, `delete(...)` 等  
  - 内部简化 `Session` / `EntityManager` 的获取和关闭流程

- [ ] **注入与使用**  
  - 在用户的业务层代码中，通过 IoC 容器注入 `OrmTemplate`  
  - 演示基本 `save`、`find`、`delete` 操作

- [ ] **事务管理**  
  - 如果你已经在 **micro-spring-tx** 或 **micro-spring-jdbc** 里做了事务管理，可复用：让 `OrmTemplate` 内部在同一个事务里操作  
  - 测试：一个事务里先 `save()`, 再 `delete()`, 最后 `commit`；或在中间抛异常，触发 `rollback`

#### 产出要求
1. 在单元测试或示例中，用 `OrmTemplate.save(new User(...))` 把数据插入数据库  
2. 用 `OrmTemplate.find(User.class, userId)` 查到数据并打印，验证插入成功  
3. 日志或控制台输出 “Hibernate Session opened / closed” 之类的提示，确认事务被正常管理  

---

### 任务17.4：DAO / Repository 层封装（可选高级）

**场景**：效仿 Spring Data JPA 的思路，让用户仅仅声明一个接口（如 `UserRepository`），就能自动生成基本的 CRUD 方法，简化 DAO 层编写。  

- [ ] **定义 `Repository` 接口**  
  - 比如 `CrudRepository<T, ID>`，带有 `save`, `findById`, `delete`, `findAll` 等方法签名  
  - 让 `micro-spring-orm` 里自动帮它生成实现类（可以借助动态代理或 APT 机制）

- [ ] **扫描 `@Repository` 或 `@OrmRepository`**（自定义注解）  
  - 在容器启动时，扫描带有该注解的接口  
  - 为其创建动态代理，调用底层 `OrmTemplate` 或 `SessionFactory` 完成实际操作

- [ ] **方法名解析**（可选进阶）  
  - 学习 Spring Data 里“根据方法名推断查询”的简易思路，比如 `findByUsername(String username)` 自动生成对应查询  
  - 这里实现一个简化版即可

#### 产出要求
1. 在示例中，只写一个 `interface UserRepository extends CrudRepository<User, Long>`  
2. 容器初始化后，即可 `context.getBean(UserRepository.class)` 拿到代理对象，直接 `save(...)`、`findById(...)`  
3. 若要支持 “方法名解析”，可试验 `User findByUsername(String username)` 等方法自动生成查询语句  

---

### 任务17.5：MyBatis 整合（可选并行任务）

**场景**：除 JPA/Hibernate 外，很多人还会用 **MyBatis** 这类半自动 ORM 工具。也可以放到同一个 “micro-spring-orm” 模块里，或另开一个 “micro-spring-mybatis” 模块。  

- [ ] **创建 MyBatis 集成**  
  - 参考 Spring 官方 “spring-orm” 对 MyBatis 的支持（或 “mybatis-spring”），实现一次性加载 Mapper 配置文件、Mapper 接口

- [ ] **注入 SqlSessionFactory**  
  - 与 JPA 的 `EntityManagerFactory` 类似，需要定义 `SqlSessionFactory` 并放入 IoC  
  - 配置数据库连接、Mapper.xml 路径、别名等

- [ ] **Mapper 扫描**  
  - 自动扫描带有 `@Mapper` 注解的接口  
  - 使用动态代理生成实现类，注入到容器中

- [ ] **事务支持**  
  - 如果在 micro-spring 中已经搭建了事务框架，则 MyBatis 同样可以借助 `DataSource`、`TransactionManager` 来管理

#### 产出要求
1. 编写一个简单的 Mapper 接口 `UserMapper`，含 `@Mapper` 注解，声明方法 `insertUser(User user)`, `selectUserById(Long id)` 等  
2. XML 或注解方式编写 SQL，测试插入、查询、更新等  
3. 验证事务回滚是否正常  

---

## 总结

通过这些**spring-orm** 相关任务，初学者能快速上手 **ORM** 在微型 Spring 框架中的应用场景，了解以下内容：

1. **SessionFactory / EntityManagerFactory** 的创建与管理  
2. 如何扫描实体、自动生成/更新数据库表结构  
3. `OrmTemplate` 之类的简易工具封装，减少样板代码  
4. 事务结合：通过已有的 **micro-spring-tx** 或 **AOP** 代理，为 ORM 操作赋能  
5. 进一步可选：动态代理为用户创建 `Repository` / `Mapper` 实现类，体验 **Spring Data** 或 **mybatis-spring** 的自动化思路  

这些功能的实现有助于理解 Spring ORM 模块的原理与使用模式，也为今后集成更多第三方框架（如 MyBatis、JOOQ、Kotlin Exposed 等）奠定基础。记得多写日志和单元测试，随时记录思路与遇到的问题，持续迭代完善。祝学习愉快！