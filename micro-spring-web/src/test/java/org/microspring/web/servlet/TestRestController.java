package org.microspring.web.servlet;

import org.microspring.web.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class TestRestController {
    
    @GetMapping("/user")
    public User getUser() {
        return new User("John", 25);
    }
    
    @PostMapping("/user")
    public User createUser() {
        return new User("New User", 30);
    }
    
    @PutMapping("/user")
    public User updateUser() {
        return new User("Updated User", 35);
    }
    
    @RequestMapping("/users")
    public List<User> getUsers() {
        return Arrays.asList(
            new User("John", 25),
            new User("Jane", 24)
        );
    }
    
    @DeleteMapping("/user/{id}")
    public Map<String, String> deleteUser() {
        Map<String, String> result = new HashMap<>();
        result.put("message", "User deleted successfully");
        return result;
    }
    
    @PatchMapping("/user/{id}")
    public User patchUser() {
        return new User("Partially Updated User", 28);
    }
    
    @PostMapping("/address")
    public Map<String, String> save(@Valid @RequestBody AddressVO vo) {
        Map<String, String> result = new HashMap<>();
        result.put("message", "Address saved: " + vo.getStreet());
        return result;
    }
    
    public static class User {
        private String name;
        private int age;
        
        public User() {} // Jackson需要无参构造函数
        
        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public int getAge() {
            return age;
        }
        
        public void setAge(int age) {
            this.age = age;
        }
        
        @Override
        public String toString() {
            return "{\"name\":\"" + name + "\",\"age\":" + age + "}";
        }
    }
    
    // 包装类，用于正确输出 JSON 数组
    public static class UserList extends ArrayList<User> {
        private final String jsonString;
        
        public UserList(String jsonString) {
            this.jsonString = jsonString;
        }
        
        @Override
        public String toString() {
            return jsonString;
        }
    }
    
    public static class AddressVO {
        private String street;
        private String city;
        
        // Getters and setters
        public String getStreet() {
            return street;
        }
        
        public void setStreet(String street) {
            this.street = street;
        }
        
        public String getCity() {
            return city;
        }
        
        public void setCity(String city) {
            this.city = city;
        }
    }
} 