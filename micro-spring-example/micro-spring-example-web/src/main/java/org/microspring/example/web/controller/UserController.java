package org.microspring.example.web.controller;

import org.microspring.web.annotation.Controller;
import org.microspring.web.annotation.GetMapping;
import org.microspring.web.annotation.RequestMapping;
import org.microspring.web.annotation.ResponseBody;
import org.microspring.example.web.model.User;

@Controller
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/current")
    @ResponseBody
    public User getCurrentUser() {
        return new User(1L, "admin", "admin@example.com");
    }
} 