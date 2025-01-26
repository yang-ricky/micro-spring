package org.microspring.mybatis.annotation;

import org.microspring.context.annotation.Import;
import org.microspring.mybatis.config.MapperAnnotationRegistrar;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(MapperAnnotationRegistrar.class)
public @interface Mapper {
} 