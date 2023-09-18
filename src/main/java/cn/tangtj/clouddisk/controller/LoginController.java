package cn.tangtj.clouddisk.controller;

import cn.tangtj.clouddisk.entity.User;
import cn.tangtj.clouddisk.utils.UserUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import javax.servlet.http.HttpServletRequest;

/**
 * @author tang
 * 用户登录 和 退出
 */
@Controller
public class LoginController {

    private static Logger logger = LogManager.getLogger(LoginController.class.getName());

    @GetMapping(value = "/login")
    public String login(){
        logger.error("用户通过get请求访问");
        return "login";
    }

    @PostMapping(value = "/login")
    public String userLogin(HttpServletRequest request, RedirectAttributesModelMap modelMap){
        User user = UserUtil.getPrincipal();
        if (user != null) {
            return "redirect:/file";
        }
        String message = (String) request.getAttribute("message");
        if (!"".equals(message)) {
            message = "帐号或密码错误，请重试。";

        }
        modelMap.addFlashAttribute("message", message);
        return "redirect:/login";
    }

}
