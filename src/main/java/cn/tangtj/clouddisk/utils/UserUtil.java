package cn.tangtj.clouddisk.utils;

import cn.tangtj.clouddisk.entity.User;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

/**
 * @author tang
 */
public class UserUtil {

    public static User getPrincipal() {
        // 获取当前用户的主体
        Subject subject = SecurityUtils.getSubject();

        // 从主体中获取用户信息
        User user = (User) subject.getPrincipal();

        // 如果用户信息不为空，返回用户信息
        if (user != null) {
            return user;
        }

        // 如果用户信息为空，返回 null
        return null;
    }
}
