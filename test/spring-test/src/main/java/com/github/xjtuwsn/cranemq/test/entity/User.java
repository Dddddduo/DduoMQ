package com.github.xjtuwsn.cranemq.test.entity;

import java.io.Serializable;

/**
 * @project:dduomq
 * @file:User
 * @author:dduo
 * @create:2023/10/14-21:43
 */
public class User implements Serializable {
    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
