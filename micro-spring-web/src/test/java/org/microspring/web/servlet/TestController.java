package org.microspring.web.servlet;

import org.microspring.web.annotation.Controller;
import org.microspring.web.annotation.RequestMapping;
import org.microspring.web.annotation.GetMapping;

@Controller
@RequestMapping("/test")
public class TestController {
    @RequestMapping("/hello")
    public String hello() {
        return "Hello, MVC!";
    }

    @GetMapping("/error")
    public String throwError() {
        throw new RuntimeException("Test error");
    }
} 