下面给出一个“**循序渐进、全方位**”的 AOP 学习与实践任务清单，并在此基础上，**额外**加入 **Filter**、**Interpreter** 两个主题的任务，帮助你从零开始搭建并理解它们的原理和应用。该清单会沿用与前面“Micro Spring”任务类似的风格，按阶段、任务编号、场景、需求与产出要求等进行拆分，保证**逻辑连续、难度渐进**。你可以选择性地融合到自己的 Micro-Spring 项目里，也可以当作单独的学习模块来实现。

---

# AOP 专题任务

> **目标**：让你在已有的“动态代理 + BeanPostProcessor”的基础上，逐步拓展到 Pointcut、Advice、Weaving 等更完整的 AOP 概念，并掌握“基于注解配置”或“基于 XML 配置”的切面织入方法。  
> 
> **范围**：从最简单的“拦截所有方法”进阶到“基于表达式匹配的切点”，再到“多切面顺序、环绕通知”等高级功能。

## AOP - 初级阶段

### 任务A1：自定义 `AspectDefinition`（基本的切面结构）
**场景**：需要让开发者能够定义“切点 + 通知”  
1. **`AspectDefinition`**：存放以下信息：  
   - `pointcut`: 表示需要拦截的匹配规则（方法签名、注解、类名等）  
   - `advices`: 存放一个或多个 `Advice`（前置/后置/环绕等）  
2. **`Advice`**：可以定义接口，比如 `beforeMethod(Method, Object[] args)`, `afterMethod(...)`, `aroundMethod(...)` 等  
3. **`Pointcut`**：可以先简化为“类名匹配”或“方法名匹配”，后续再扩展为“表达式匹配”。  

**产出要求**：  
1. 在代码中能写类似：
   ```java
   AspectDefinition aspectDef = new AspectDefinition();
   aspectDef.setPointcut("com.example.service.*Service"); // 简单类名匹配
   aspectDef.addAdvice(new LogAdvice()); // 仅一个前置/后置
   ```
2. 在 `LogAdvice` 里能打印日志：`[LogAdvice] Before method: xxx`  
3. 日志能看到确实拦截了 `xxxService` 的相关方法

---

### 任务A2：与 `BeanPostProcessor` 集成，完成**运行期** Weaving
**场景**：当容器完成某个 Bean 的实例化后，检查它是否符合 `AspectDefinition` 的 `pointcut`；如果符合，就给它**创建代理对象**  
1. **`AopBeanPostProcessor`**：  
   - 实现 `BeanPostProcessor` 接口的 `postProcessAfterInitialization(bean, beanName)`  
   - 遍历你定义的所有 `AspectDefinition`，若其 `pointcut` 匹配该 Bean，则**创建一个代理**。  
2. **`createProxyWithAdvices(...)`**：  
   - 若使用 JDK 动态代理，需要编写一个 `InvocationHandler`，在 `invoke` 内部，按照前置/后置的顺序调度 `Advice`。  
   - 若使用 CGLIB，也要动态生成子类并在方法调用前后调用 `Advice`。  
3. **多个切面的匹配**：如果有多个 `AspectDefinition` 都匹配，可能需要**链式**执行 advices（先 A 再 B）。

**产出要求**：  
1. 使用 JDK/CGLIB 之一，完成**单切面**代理；  
2. 运行后看到**代理对象**替换原始 Bean，调用 Service 方法时能输出 AOP 日志；  
3. 支持**多个切面**时，能按一定顺序执行多个 Advice（顺序可以固定或可自定义）。

---

### 任务A3：引入 Pointcut 表达式与高级 `Advice` 类型
**场景**：现在想支持类似 `execution(* com.example..service.*.*(..))` 或基于注解匹配的切点  
1. **`PointcutExpressionParser`**：  
   - 编写一个解析器，支持 `execution(* com.example..service.*.*(..))` 这种简单表达式；  
   - 或者更简单地先用**正则**匹配；  
   - 也可以只做**注解匹配**（如“任何带 `@Transactional` 的方法”即被切入）。  
2. **增加 Advice 类型**：  
   - `BeforeAdvice`, `AfterReturningAdvice`, `AfterThrowingAdvice`, `AroundAdvice` 等；  
   - `AroundAdvice` 可以在方法执行前后做额外处理，并决定是否调用原方法；  
3. **方法级别切点**：  
   - `AspectDefinition` 里记录“只对哪些方法注入 Advice”；  
   - 你需要在 `InvocationHandler` 的 `invoke` 里，根据当前调用的方法签名，判定是否符合 Pointcut，再决定是否调用 Advice。

**产出要求**：  
1. 能在 XML/注解里书写类似：
   ```xml
   <aspect>
     <pointcut expression="execution(* com.example..service.*.*(..))" />
     <advice type="before" class="com.example.LogAdvice"/>
     <advice type="around" class="com.example.TransactionAdvice"/>
   </aspect>
   ```
   或者用注解 `@Aspect`, `@Pointcut(...)`, `@Before(...)`, `@Around(...)`  
2. 在实际调用时，能看到**方法级别**的拦截差异，比如只拦截 `public void save()`，而不拦截 `private` 或者 `query()`。

---

### 任务A4：多切面顺序 & 异常处理
**场景**：Bean 上可能有多个切面，如何控制顺序？出现异常怎么办？  
1. **顺序**：  
   - `@Order(1)`, `@Order(2)` 等注解来标注优先级；  
   - 或者 XML 配置 `<advice order="1" />`；  
   - 在创建代理时，对 advices 按优先级排序。  
2. **异常处理**：  
   - `AfterThrowingAdvice`：当目标方法抛异常时执行；  
   - 需在 `InvocationHandler` 的 `invoke` 中捕获异常，并再调用 `AfterThrowingAdvice`。  
3. **测试**：  
   - 有 2~3 个切面，写不同 `order`；  
   - 人为抛出异常，看 `AfterThrowingAdvice` 是否生效。

**产出要求**：  
1. 日志能看出多切面的执行顺序。  
2. 目标方法抛异常时，能看到自定义的报错日志或回滚逻辑。

---

### 任务A5：编译期/类加载期 Weaving（可选高级）
**场景**：基于 AspectJ 的编译期织入、或者类加载期织入  
- 这部分相对复杂，需要借助 AspectJ Plugin 或者自定义 ClassLoader。  
- 如果想在你的 Micro-Spring 中**模拟**编译期/类加载期织入，可以写一个简化版本，通过 Javassist 或 ASM 在字节码层做修改。  

> 这超出了普通 “动态代理” 的范围，如想深入可另做专题研究。  
> 或者你也可以保留到这里做**扩展**，不列为强制任务。

---

## 总结

### 1. 任务结构与目标

- **AOP 专题任务**围绕“切点(Pointcut)、通知(Advice)、织入(Weaving)、表达式解析”等展开，从最简代理增强 → 多切面 → 表达式切点 → 复杂特性（顺序、异常、编译期织入），让你逐步构建出一个“近似 Spring AOP”的原型。
- **Filter 专题任务**帮助你在 Web 或框架内部实现“责任链式”的前置处理或拦截机制，可用于请求/响应的校验、鉴权、日志、限流等场景。
- **Interpreter 专题任务**让你掌握解释器模式，理解如何为自定义语言或 DSL 做解析和执行，并可与 IoC 结合实现“简单版 SpEL”。

### 2. 实践与进阶

1. **与之前 IoC 容器整合**：  
   - 在 Micro-Spring 里添加一个“切面注册”流程，在 `refresh()` 时加载 `AspectDefinition`，再把 `AopBeanPostProcessor` 注册进去；  
2. **引入高级特性**（可选）：  
   - 各种注解驱动 `@Aspect, @Pointcut, @Before, @After, @AfterThrowing, @Around`；  
   - Condition-Based Filter；  
   - Interpreter 的自定义脚本语言等。  
3. **性能测试与对比**：  
   - AOP 代理对方法调用性能的影响；  
