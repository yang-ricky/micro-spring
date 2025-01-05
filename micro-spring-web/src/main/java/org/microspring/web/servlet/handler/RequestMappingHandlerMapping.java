package org.microspring.web.servlet.handler;

import org.microspring.context.ApplicationContext;
import org.microspring.web.annotation.Controller;
import org.microspring.web.annotation.GetMapping;
import org.microspring.web.annotation.RequestMapping;
import org.microspring.web.annotation.RestController;
import org.microspring.web.context.WebApplicationContext;
import org.microspring.web.method.HandlerMethod;
import org.microspring.web.servlet.HandlerMapping;
import org.microspring.web.servlet.MethodNotAllowedException;
import org.microspring.web.annotation.RequestMethod;
import org.microspring.web.annotation.PostMapping;
import org.microspring.web.annotation.PutMapping;
import org.microspring.web.annotation.DeleteMapping;
import org.microspring.web.annotation.PatchMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

public class RequestMappingHandlerMapping implements HandlerMapping {
    
    private final Map<RequestMappingInfo, HandlerMethod> handlerMethods = new HashMap<>();
    
    public RequestMappingHandlerMapping(ApplicationContext applicationContext) {
        if (!(applicationContext instanceof WebApplicationContext)) {
            throw new IllegalArgumentException(
                "ApplicationContext must be an instance of WebApplicationContext");
        }
        initHandlerMethods((WebApplicationContext) applicationContext);
    }
    
    private void initHandlerMethods(WebApplicationContext applicationContext) {
        // 获取所有带有 @Controller 或 @RestController 注解的 bean
        String[] controllerBeans = applicationContext.getBeanNamesForAnnotation(Controller.class);
        String[] restControllerBeans = applicationContext.getBeanNamesForAnnotation(RestController.class);
        
        // 合并两个数组
        String[] allBeans = new String[controllerBeans.length + restControllerBeans.length];
        System.arraycopy(controllerBeans, 0, allBeans, 0, controllerBeans.length);
        System.arraycopy(restControllerBeans, 0, allBeans, controllerBeans.length, restControllerBeans.length);
        
        for (String beanName : allBeans) {
            Object controller = applicationContext.getBean(beanName);
            Class<?> controllerClass = controller.getClass();
            
            // Get class-level mapping
            String baseUrl = "";
            if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
                baseUrl = controllerClass.getAnnotation(RequestMapping.class).value();
            }
            
            // Get method-level mapping
            for (Method method : controllerClass.getMethods()) {
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                GetMapping getMapping = method.getAnnotation(GetMapping.class);
                PostMapping postMapping = method.getAnnotation(PostMapping.class);
                PutMapping putMapping = method.getAnnotation(PutMapping.class);
                DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
                PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
                
                if (requestMapping != null || getMapping != null || 
                    postMapping != null || putMapping != null ||
                    deleteMapping != null || patchMapping != null) {
                    
                    String methodUrl = "";
                    RequestMethod[] methods = {};
                    
                    if (requestMapping != null) {
                        methodUrl = requestMapping.value();
                        methods = requestMapping.method();
                    } else if (getMapping != null) {
                        methodUrl = getMapping.value();
                        methods = new RequestMethod[]{RequestMethod.GET};
                    } else if (postMapping != null) {
                        methodUrl = postMapping.value();
                        methods = new RequestMethod[]{RequestMethod.POST};
                    } else if (putMapping != null) {
                        methodUrl = putMapping.value();
                        methods = new RequestMethod[]{RequestMethod.PUT};
                    } else if (deleteMapping != null) {
                        methodUrl = deleteMapping.value();
                        methods = new RequestMethod[]{RequestMethod.DELETE};
                    } else if (patchMapping != null) {
                        methodUrl = patchMapping.value();
                        methods = new RequestMethod[]{RequestMethod.PATCH};
                    }
                    
                    String fullUrl = baseUrl + methodUrl;
                    RequestMappingInfo mappingInfo = new RequestMappingInfo(fullUrl, methods);
                    
                    handlerMethods.put(mappingInfo, new HandlerMethod(controller, method));
                    System.out.println("[HandlerMapping] Mapped " + mappingInfo + 
                        " -> " + controllerClass.getSimpleName() + "." + method.getName());
                }
            }
        }
    }
    
    @Override
    public HandlerMethod getHandler(HttpServletRequest request) {
        String lookupPath = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        
        // 先找到匹配的路径
        HandlerMethod handlerMethod = null;
        RequestMappingInfo matchedInfo = null;
        
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            if (pathMatch(mappingInfo.getPath(), lookupPath)) {
                matchedInfo = mappingInfo;
                handlerMethod = entry.getValue();
                // 如果方法也匹配，直接返回
                if (isMethodMatch(mappingInfo.methods, method)) {
                    return handlerMethod;
                }
            }
        }
        
        // 如果找到了路径但方法不匹配，抛出 405 Method Not Allowed
        if (matchedInfo != null) {
            throw new MethodNotAllowedException(method, matchedInfo.getAllowedMethods());
        }
        
        return null;
    }
    
    private boolean isMethodMatch(RequestMethod[] configuredMethods, String requestMethod) {
        if (configuredMethods.length == 0) {
            return true;  // 没有配置方法表示接受所有方法
        }
        for (RequestMethod method : configuredMethods) {
            if (method.name().equals(requestMethod)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean pathMatch(String pattern, String lookupPath) {
        // 完全相等的情况
        if (pattern.equals(lookupPath)) {
            return true;
        }
        
        // 将 URL 模板中的 {xxx} 转换为正则表达式
        String[] patternParts = pattern.split("/");
        String[] lookupParts = lookupPath.split("/");
        
        if (patternParts.length != lookupParts.length) {
            return false;
        }
        
        for (int i = 0; i < patternParts.length; i++) {
            String patternPart = patternParts[i];
            String lookupPart = lookupParts[i];
            
            if (patternPart.isEmpty() && lookupPart.isEmpty()) {
                continue;
            }
            
            if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                continue;  // 路径参数部分，跳过比较
            }
            
            if (!patternPart.equals(lookupPart)) {
                return false;
            }
        }
        
        return true;
    }
    
    private static class RequestMappingInfo {
        private final String path;
        private final RequestMethod[] methods;
        
        public RequestMappingInfo(String path, RequestMethod[] methods) {
            this.path = path;
            this.methods = methods.length == 0 ? 
                RequestMethod.values() : methods;  // 如果没指定方法，匹配所有方法
        }
        
        public boolean matches(String lookupPath, String httpMethod) {
            if (!path.equals(lookupPath)) {
                return false;
            }
            
            if (methods.length == 0) {
                return true;
            }
            
            for (RequestMethod method : methods) {
                if (method.name().equals(httpMethod)) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public String toString() {
            return String.format("%s %s", Arrays.toString(methods), path);
        }
        
        // 需要实现 equals 和 hashCode 方法，因为这个类作为 Map 的 key
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RequestMappingInfo that = (RequestMappingInfo) o;
            return Objects.equals(path, that.path) && 
                   Arrays.equals(methods, that.methods);
        }
        
        @Override
        public int hashCode() {
            int result = Objects.hash(path);
            result = 31 * result + Arrays.hashCode(methods);
            return result;
        }
        
        public String getPath() {
            return path;
        }
        
        public Set<String> getAllowedMethods() {
            Set<String> allowed = new HashSet<>();
            for (RequestMethod method : methods) {
                allowed.add(method.name());
            }
            return allowed;
        }
    }
} 