package com.yupi.springbootinit.model.dto.user;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 用户更新个人信息请求
 *
 * @author <a href="https://github.com/kkkano">kkkano</a>
 * @from <a href=“https://github.com/kkkano/BI”</a>
 */
@Data
public class UserUpdateMyRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;
    private int usageCount; // 使用次数
    private Integer points; // 积分
    private LocalDateTime lastCheckIn;//最后签到时间
    private static final long serialVersionUID = 1L;
}