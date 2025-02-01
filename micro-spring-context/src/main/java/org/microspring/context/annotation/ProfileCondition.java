package org.microspring.context.annotation;

import org.microspring.core.env.Environment;
import org.microspring.core.type.AnnotatedTypeMetadata;

import java.util.Arrays;

/**
 * Profile 条件处理器
 */
public class ProfileCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if (!metadata.isAnnotated(Profile.class.getName())) {
            return true;
        }

        Environment environment = context.getEnvironment();
        String[] activeProfiles = environment.getActiveProfiles();
        String[] requiredProfiles = (String[]) metadata.getAnnotationAttributes(Profile.class.getName()).get("value");

        // 如果没有激活的 profile，只有显式声明为 "default" 的组件才会被注册
        if (activeProfiles == null || activeProfiles.length == 0) {
            return Arrays.asList(requiredProfiles).contains("default");
        }

        // 只要有一个 profile 匹配即可
        for (String requiredProfile : requiredProfiles) {
            for (String activeProfile : activeProfiles) {
                if (requiredProfile.equals(activeProfile)) {
                    return true;
                }
            }
        }

        return false;
    }
} 