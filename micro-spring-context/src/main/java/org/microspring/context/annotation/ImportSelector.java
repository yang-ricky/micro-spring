package org.microspring.context.annotation;

/**
 * 接口定义了一个选择导入类的策略
 * 实现类可以根据条件动态决定要导入哪些类
 */
public interface ImportSelector {
    /**
     * 选择要导入的类
     * @param importingClass 导入该ImportSelector的类
     * @return 要导入的类的全限定名数组
     */
    String[] selectImports(Class<?> importingClass);
} 