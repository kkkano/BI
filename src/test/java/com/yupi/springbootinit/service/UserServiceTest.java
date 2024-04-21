package com.yupi.springbootinit.service;

import javax.annotation.Resource;

import com.yupi.springbootinit.model.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 用户服务测试
 *
 * @author <a href="https://github.com/kkkano">kkkano</a>
 * @from <a href=“https://github.com/kkkano/BI”</a>
 */
@SpringBootTest
public class UserServiceTest {

    @Resource
    private UserService userService;
    @Test
    void usersearch() {
        String userName = "阿滋以";
        User user = new User();
        user.setUserName(userName);
        List<User> result = userService.userSearch(user);
        for (User u : result) {
            System.out.println(u);
        }
    }
    @Test
    void userRegister() {
        String userAccount = "yupi";
        String userPassword = "";
        String checkPassword = "123456";
        try {
            long result = userService.userRegister(userAccount, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userAccount = "yu";
            result = userService.userRegister(userAccount, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
        } catch (Exception e) {

        }
    }
}
