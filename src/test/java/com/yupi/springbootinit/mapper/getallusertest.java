package com.yupi.springbootinit.mapper;

import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class getallusertest {
    @Autowired
    private UserService userService;

    @Test
    public void testGetAllUsers() {
        List<User> userList = userService.getAllUsers();
        for (User user : userList) {
            System.out.println(user);
        }
    }
}