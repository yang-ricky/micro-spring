package org.microspring.webflux;

import io.netty.handler.codec.http.HttpMethod;
import org.microspring.web.annotation.RestController;
import org.microspring.web.annotation.RequestMapping;
import org.microspring.web.annotation.RequestMethod;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Handler mapping for reactive controllers
 */
public class ReactiveHandlerMapping {
    
    // Key: path + method, Value: HandlerMethod
    private final Map<RouteKey, HandlerMethod> handlerMethods = new HashMap<>();

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
        RouteKey routeKey = new RouteKey(path, httpMethod);
        if (handlerMethods.containsKey(routeKey)) {
            HandlerMethod existing = handlerMethods.get(routeKey);
            throw new IllegalStateException(
                String.format("Duplicate route found: %s %s, between %s and %s",
                    httpMethod, path,
                    existing.getMethod(),
                    method)
            );
        }

        // Verify return type is Mono
        if (!method.getReturnType().equals(Mono.class)) {
            throw new IllegalStateException(
                String.format("Controller method must return Mono: %s.%s",
                    controller.getClass().getSimpleName(),
                    method.getName())
            );
        }

        handlerMethods.put(routeKey, new HandlerMethod(controller, method));
    }

    /**
     * Find handler method for given path and HTTP method
     */
    public HandlerMethod getHandler(String path, HttpMethod method) {
        return handlerMethods.get(new RouteKey(path, method));
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

    /**
     * Key class for route mapping
     */
    private static class RouteKey {
        private final String path;
        private final HttpMethod method;

        public RouteKey(String path, HttpMethod method) {
            this.path = path;
            this.method = method;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RouteKey routeKey = (RouteKey) o;
            return Objects.equals(path, routeKey.path) && method == routeKey.method;
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, method);
        }
    }
} 