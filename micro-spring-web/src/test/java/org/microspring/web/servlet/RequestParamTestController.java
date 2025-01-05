package org.microspring.web.servlet;

import org.microspring.web.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/params")
public class RequestParamTestController {
    
    @GetMapping("/required")
    public Map<String, Object> testRequired(@RequestParam("id") Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        return result;
    }
    
    @GetMapping("/optional")
    public Map<String, Object> testOptional(
            @RequestParam(value = "name", required = false) String name) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        return result;
    }
    
    @GetMapping("/default")
    public Map<String, Object> testDefault(
            @RequestParam(value = "age", defaultValue = "18") Integer age) {
        Map<String, Object> result = new HashMap<>();
        result.put("age", age);
        return result;
    }
    
    @GetMapping("/multiple")
    public Map<String, Object> testMultiple(
            @RequestParam("id") Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "age", defaultValue = "18") Integer age) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("name", name);
        result.put("age", age);
        return result;
    }
} 