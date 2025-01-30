下面给你一个**去除 Reactive 内容**、适用于“普通同步 Web MVC 或类似模式”的 **Micro-Spring-Security** 任务列表。整体依旧保持之前的“**循序渐进、分步骤**”风格，并覆盖从最简单的认证到多种认证策略、再到方法级安全与高级特性。

---

## 任务1：基础认证 + 过滤器链

- [ ] **编写 `SecurityFilter`**  
  - 一个实现 Servlet `Filter`（或你自定义的过滤器接口），在请求到来时检查 `Authorization` 头是否有效。  
  - 先支持最简单的 **Basic Auth**（`Authorization: Basic base64(username:password)`）。  
  - 如果未认证或凭证错误，返回 `401 Unauthorized`；否则将用户信息放入 `SecurityContextHolder`（可用 `ThreadLocal`）后继续调用下一个过滤器。  

- [ ] **定义 `SecurityContextHolder`**  
  - 采用 `ThreadLocal<SecurityContext>` 存储当前线程的用户信息；  
  - 在 Filter 中解析出用户并设置 `SecurityContextHolder.setContext(...)`；  
  - 请求处理结束后记得清理，避免线程复用带来数据污染。

**产出要求**：  
1. 启动后能拦截所有请求，若无或错误的 `Basic Auth` 头则 401；  
2. 认证成功后，在后续 Controller 或其他业务代码里可通过 `SecurityContextHolder.getContext()` 获取用户身份。

---

## 任务2：用户管理与密码加密

- [ ] **编写 `UserDetails` & `UserDetailsService`**  
  - `UserDetails`：包含用户名、加密密码、角色/权限等信息；  
  - `UserDetailsService`：提供 `loadUserByUsername(String username)` 方法；  
  - 在初步阶段可用内存 `Map<String, UserDetails>` 做简单演示。

- [ ] **密码加密与校验**  
  - 编写 `PasswordEncoder` 接口：`String encode(String rawPassword)`, `boolean matches(String rawPassword, String encodedPassword)`  
  - **强烈推荐**使用 `BCrypt` 或类似安全算法；在注释中明确**不能**在生产使用 MD5 等弱加密。  

- [ ] **整合到 `SecurityFilter`**  
  - 当请求携带用户名/密码时，从 `UserDetailsService` 加载用户；  
  - 用 `PasswordEncoder.matches()` 校验密码；  
  - 校验通过后设置 `SecurityContextHolder`，否则 401。

**产出要求**：  
1. 可以配置多个用户、对应加密密码；  
2. 正确密码能够匹配通过，错误密码直接拒绝；  
3. 在日志或控制台上可看到是哪位用户通过了认证。

---

## 任务3：角色与权限控制

- [ ] **引入角色/权限**  
  - 在 `UserDetails` 中维护 `List<String> roles` 或 `List<String> authorities`；  
  - 例如 `"ROLE_ADMIN"`, `"ROLE_USER"` 等。

- [ ] **编写 `AuthorizationFilter`**（可与认证分离）  
  - 检查 `SecurityContextHolder.getContext()` 是否有对应角色；  
  - 对特定 URL（如 `/admin/**`）要求 `ROLE_ADMIN`；若不符合则 `403 Forbidden`。  
  - 可以定义一个简易的配置，如：  
    ```java
    securityConfig
        .antMatchers("/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated();
    ```  

- [ ] **异常处理**  
  - 若角色不满足，直接返回 `403`；  
  - 记录日志标明“访问被拒绝”原因。

**产出要求**：  
1. 不同角色用户访问 `/admin/**` 时看到不同结果（200 / 403）；  
2. 日志中能看到对应的检查和拒绝信息。

---

## 任务4：Session / Token / JWT 认证模式

> 如果只想演示一种方式，也可以挑选 Session 或 JWT 来做。

- [ ] **Session 认证**  
  - 认证通过后在服务端生成 `Session`，返回给客户端 `Set-Cookie: JSESSIONID=xxx`；  
  - 后续请求从 Cookie 解析 `SessionId` 并恢复用户信息；  
  - 需注意 Session 过期、Session 存储（内存 / Redis）等。

- [ ] **Token 认证**  
  - 认证通过后生成一个随机 Token（UUID 等），存储于服务端 `token->User` 映射；  
  - 客户端后续请求带 `Authorization: Bearer <token>`；  
  - 服务端解析后对照存储，若无此 token 或已过期则 401。

- [ ] **JWT 认证**  
  - 认证通过后生成 **JWT**（Header+Payload+Signature）；  
  - 无需服务端存储 Session，客户端自持 token，服务端只要校验签名和过期时间即可；  
  - 推荐用 `HS256` 或 `RS256`，看需求选择对称/非对称加密。

**产出要求**：  
1. 至少实现其中一种方式可正常工作；  
2. 若想并行支持多种方式，可设计 `AuthenticationStrategy` 接口，在 Filter 中依次尝试；  
3. 无效/过期 `Session`/Token/JWT 时返回 401 并有相应日志。

---

## 任务5：方法级安全（`@Secured` / `@PreAuthorize`）

- [ ] **定义注解 & 解析**  
  - `@Secured("ROLE_ADMIN")` 或 `@RequiresRole("ADMIN")`；  
  - 解析该注解时可通过**AOP**或**动态代理**在调用目标方法前做校验。

- [ ] **AOP 拦截**  
  - 在目标方法调用前，从 `SecurityContextHolder` 获取当前用户角色/权限；  
  - 若不匹配，抛出 `AccessDeniedException`；在全局异常处理里返回 `403`。

- [ ] **注解扩展**（可选）  
  - 支持 `@PreAuthorize("hasRole('ADMIN') and #userId == principal.id")` 等表达式方式；  
  - 需要一个小型 **SpEL**（Spring Expression Language）或自定义表达式解析器。

**产出要求**：  
1. 在某个业务方法上贴 `@Secured("ROLE_ADMIN")` 后，如果没有 `ROLE_ADMIN` 就不能调用；  
2. 返回 `403` 并在日志中写明 “Method-level access denied”。

---

## 任务6：高级安全特性

- [ ] **CSRF 防护**  
  - 在 **表单提交**场景（通常配合 Session）需要带 CSRF token；  
  - 若检测到无或错误 token，返回 `403`；  
  - 无状态（JWT / Token）模式下可选择跳过或自行设计机制。

- [ ] **XSS 过滤**  
  - 对用户输入做 HTML 转义或过滤恶意脚本；  
  - 可以通过 Servlet Filter 或全局拦截器来处理请求参数/响应。

- [ ] **CORS 配置**  
  - 设置响应头 `Access-Control-Allow-Origin` / `Allow-Methods` / `Allow-Headers` 等；  
  - 使前端或跨域 AJAX 请求时有选择性限制或开放。

- [ ] **日志与审计**  
  - 记录关键操作日志（如删除用户、修改配置）；  
  - 包含操作人、时间、操作结果(成功/失败)。

**产出要求**：  
1. 演示一个需要 CSRF token 的接口，若不带则返回 403；  
2. 携带恶意 `<script>` 注入时被过滤或转义；  
3. CORS 只有指定域名可访问，其他域名被拒绝。

---

## 任务7：与 Micro-Spring-Web / MVC 整合 & 全局异常处理

- [ ] **Filter 链注册**  
  - 在你已有的 `MicroSpringWeb` 或 MVC 核心启动过程里，注册 `SecurityFilter` / `AuthorizationFilter` / 等；  
  - 保证在请求到达 Controller 前先经过安全过滤器。

- [ ] **全局异常处理**  
  - 在你原先的 “细粒度异常处理” 或 `@ControllerAdvice` 里捕获 `AccessDeniedException`, `AuthenticationException`；  
  - 返回统一的 JSON 或错误页面，带正确的 `4xx/5xx` 状态码。

- [ ] **启动类 & 配置**  
  - `MicroSpringSecurityApplication` / `SecurityConfig`：让用户一键加载安全模块；  
  - 启动后，访问受保护资源时自动进行认证 / 授权。

**产出要求**：  
1. 一个可执行程序/主类能正常监听端口；  
2. 安全过滤器生效，未登录/无权限直接被拦截；  
3. 全局异常处理捕获安全相关异常并返回合适的 HTTP 状态码。

---

## 任务8：测试 & 性能调优

- [ ] **单元测试**  
  - 针对 `SecurityFilter`, `UserDetailsService`, `PasswordEncoder`, `AuthorizationFilter` 等核心组件写单测；  
  - 验证各种场景（正确用户名密码、错误密码、无权限角色等）。

- [ ] **集成测试**  
  - 用 JUnit + `MockMvc` / RestAssured / Postman 测试端到端；  
  - 包括 Session / JWT / BasicAuth 等多种模式，检测请求头、Cookie 是否正确处理。

- [ ] **压力测试 & 性能优化**  
  - 用 JMeter / Gatling 对认证环节高并发测试，观察是否有瓶颈；  
  - 若使用数据库或外部系统查询用户信息，考虑**连接池**、**缓存**；  
  - 若使用 JWT，观察签名/验签算法对性能的影响。

- [ ] **日志监控 & 报警**  
  - 记录大量 401 / 403 时输出警告日志或触发报警；  
  - 可对接 Prometheus / Grafana 做可视化监控。

**产出要求**：  
1. 单元测试覆盖核心安全流程，集成测试覆盖主要用例；  
2. 压测报告展示 QPS、响应时间，并进行针对性优化；  
3. 安全日志或监控平台中能看见攻击被拦截、异常请求数量等统计信息。

---

## 任务9（可选附加）：基于 ACL (对象级权限)

- [ ] **设计 ACL 数据模型**  
  - 需要一个数据结构/表，记录：  
    - 资源标识（如 `document_id`）、主体标识（用户/角色），以及**权限位**（READ, WRITE, DELETE 等）。  
  - 类似 Spring Security ACL，使用 `Sid`、`ObjectIdentity`、`Permission` 的概念。

- [ ] **编写 `AclService`**  
  - 提供方法：`boolean canRead(Object resource, Authentication auth)`, `boolean canWrite(...)` 等；  
  - 从存储中查询该 resource 的 ACL，再根据当前用户/角色判断是否满足权限。

- [ ] **集成到授权逻辑**  
  - 在需要做“对象级权限检查”的地方调用 `AclService.canRead(resource, auth)`；  
  - 若返回 false，则抛出 `AccessDeniedException`。

**产出要求**：  
1. 针对同一个资源对象，不同用户可能有不同的权限；  
2. 测试用例中演示：用户 A 拥有文件1读写权限，但没有文件2的写权限，访问文件2写接口时被拒绝。

---

## 任务10（可选附加）：基于 ABAC (属性级权限)

- [ ] **定义属性获取和策略解析**  
  - 在 **ABAC** 中，权限判定可能需要：  
    - 用户属性（部门、职位、地域……）  
    - 环境属性（时间、IP……）  
    - 资源属性（资源类型、机密级别……）  
  - 你需要一个 `PolicyEngine` 或类似服务来综合判断这些属性是否满足规则。

- [ ] **属性注入和匹配**  
  - 在 Controller/Service 调用前，搜集用户、资源、环境等属性；  
  - 将之交给 `PolicyEngine`：如 “user.department == 'sales' AND resource.type == 'report' AND time in [9:00-18:00]”。

- [ ] **表达式示例**  
  - `@PreAuthorize("hasAttribute('department', 'sales') and hasAttribute('resourceType', 'report')")`  
  - 内部由自定义 ExpressionHandler 或 AOP 解析这些属性表达式。

**产出要求**：  
1. 能针对更多动态条件做权限判断，而非仅仅角色；  
2. 测试用例演示：同一个用户在**工作时间**可访问报表，下班后则禁止；  
3. 日志中可打印出“ABAC策略匹配失败（department=sales, time=20:00）”。

---

### 小结

通过以上 8 个阶段性任务，你可以在 **普通同步 Web MVC** 场景下打造一个简化版的 “**Micro-Spring-Security**”，涵盖：

1. **认证**（Basic / Session / Token / JWT 等）；  
2. **授权**（URL 级、方法级、角色/权限）；  
3. **高级安全防护**（CSRF、XSS、CORS 等）；  
4. **与现有框架整合**（Filter 链 & 全局异常处理）；  
5. **测试与性能**（单元测试、集成测试、压力测试、日志监控）。

这样既能保证**循序渐进**，又兼顾生产环境常见的**安全需求**与**最佳实践**，且没有任何“Reactive”或非阻塞线程模型的依赖，适合在传统（阻塞）MVC 项目或你的 `MicroSpringWeb` 同步式框架中落地。祝你开发顺利！