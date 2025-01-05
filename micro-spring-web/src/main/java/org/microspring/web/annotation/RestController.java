package org.microspring.web.annotation;

import java.lang.annotation.*;
import org.microspring.stereotype.Component;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@ResponseBody
public @interface RestController {
    String value() default "";
} 