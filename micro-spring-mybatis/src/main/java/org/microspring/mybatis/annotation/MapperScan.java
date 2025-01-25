package org.microspring.mybatis.annotation;

import java.lang.annotation.*;
import org.microspring.context.annotation.Import;
import org.microspring.mybatis.config.MapperScannerRegistrar;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(MapperScannerRegistrar.class)
public @interface MapperScan {
    /**
     * 扫描的包名
     */
    String value() default "";
    
    /**
     * 扫描的包名，与value作用相同
     */
    String[] basePackages() default {};
    
    /**
     * SqlSessionFactory bean的名称
     */
    String sqlSessionFactoryRef() default "";
} 