package org.microspring.test.configuration;

public class Person {
    private final String message;
    private Integer age;
    
    public Person(String message, Integer age) {
        this.message = message;
        this.age = age;
    }
    
    public String getMessage() {
        return message;
    }

    public Integer getAge() {
        return age;
    }
} 