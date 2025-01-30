package org.microspring.webflux;

import io.netty.handler.codec.http.HttpMethod;
import org.microspring.web.annotation.RestController;
import org.microspring.web.annotation.RequestMapping;
import org.microspring.web.annotation.RequestMethod;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Handler mapping for reactive controllers
 */
public class ReactiveHandlerMapping {
    
    // List of handler methods to support path pattern matching
    private final List<HandlerMethod> handlerMethods = new ArrayList<>();

    /**
     * Register a controller bean and its methods
     */
    public void registerController(Object controller) {
        Class<?> controllerClass = controller.getClass();
        
        // Check if class has @RestController annotation
        if (!controllerClass.isAnnotationPresent(RestController.class)) {
            return;
        }

        // Get class-level @RequestMapping
        String baseUrl = "";
        if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
            baseUrl = controllerClass.getAnnotation(RequestMapping.class).value();
        }

        // Scan methods
        for (Method method : controllerClass.getMethods()) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping != null) {
                String path = baseUrl + mapping.value();
                RequestMethod[] requestMethods = mapping.method();
                
                // If no method specified, default to GET
                if (requestMethods.length == 0) {
                    registerRoute(path, HttpMethod.GET, method, controller);
                } else {
                    for (RequestMethod requestMethod : requestMethods) {
                        registerRoute(path, convertToNettyMethod(requestMethod), method, controller);
                    }
                }
            }
        }
    }

    private void registerRoute(String path, HttpMethod httpMethod, Method method, Object controller) {
        HandlerMethod handlerMethod = new HandlerMethod(controller, method);
        
        // Check for duplicate routes
        for (HandlerMethod existing : handlerMethods) {
            if (existing.matches(path)) {
                RequestMethod[] existingMethods = existing.getMethod().getAnnotation(RequestMapping.class).method();
                RequestMethod[] newMethods = method.getAnnotation(RequestMapping.class).method();
                
                // 如果两个路由都没有指定方法（默认GET）或者有相同的方法，则冲突
                if ((existingMethods.length == 0 && newMethods.length == 0) ||
                    (existingMethods.length > 0 && newMethods.length > 0 &&
                     convertToNettyMethod(existingMethods[0]).equals(httpMethod))) {
                    throw new IllegalStateException(
                        String.format("Duplicate route found: %s %s, between %s and %s",
                            httpMethod, path,
                            existing.getMethod(),
                            method)
                    );
                }
            }
        }

        // Verify return type is Mono
        if (!method.getReturnType().equals(Mono.class)) {
            throw new IllegalStateException(
                String.format("Controller method must return Mono: %s.%s",
                    controller.getClass().getSimpleName(),
                    method.getName())
            );
        }

        handlerMethods.add(handlerMethod);
    }

    /**
     * Find handler method for given path and HTTP method
     */
    public HandlerMethod getHandler(String path, HttpMethod method) {
        
        for (HandlerMethod handlerMethod : handlerMethods) {
            
            if (handlerMethod.matches(path)) {
                RequestMethod[] methods = handlerMethod.getMethod()
                    .getAnnotation(RequestMapping.class)
                    .method();
                
                // If no methods specified or method matches
                if (methods.length == 0 || 
                    convertToNettyMethod(methods[0]).equals(method)) {
                    return handlerMethod;
                }
            }
        }

        return null;
    }

    private HttpMethod convertToNettyMethod(RequestMethod springMethod) {
        switch (springMethod) {
            case GET:
                return HttpMethod.GET;
            case POST:
                return HttpMethod.POST;
            case PUT:
                return HttpMethod.PUT;
            case DELETE:
                return HttpMethod.DELETE;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + springMethod);
        }
    }
} 