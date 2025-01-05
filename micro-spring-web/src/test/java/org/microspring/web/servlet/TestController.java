package org.microspring.web.servlet;

import org.microspring.web.annotation.Controller;
import org.microspring.web.annotation.RequestMapping;

@Controller
@RequestMapping("/test")
public class TestController {
    @RequestMapping("/hello")
    public String hello() {
        return "Hello, MVC!";
    }
} 