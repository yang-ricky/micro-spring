package org.microspring.web.servlet;

import org.microspring.web.annotation.*;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;

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
    
    @GetMapping("/users/{id}")
    public SimpleUser getUserById(@PathVariable("id") Long id) {
        return new SimpleUser(id, "User " + id);
    }
    
    @GetMapping("/users/{userId}/posts/{postId}")
    public Post getUserPost(
        @PathVariable("userId") Long userId,
        @PathVariable("postId") Long postId
    ) {
        return new Post(userId, postId, "Post " + postId + " by User " + userId);
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class User {
        private String name;
        private int age;
        
        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SimpleUser {
        private Long id;
        private String name;
        
        public SimpleUser(Long id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    public static class Post {
        private Long userId;
        private Long postId;
        private String title;
        
        public Post(Long userId, Long postId, String title) {
            this.userId = userId;
            this.postId = postId;
            this.title = title;
        }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getPostId() { return postId; }
        public void setPostId(Long postId) { this.postId = postId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }
    
    public static class AddressVO {
        private String street;
        private String city;
        
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, String> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("type", "validation_error");
        return response;
    }
} 