package cn.tangtj.clouddisk.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author tang
 */
@Controller
public class MainController {

    @RequestMapping()
    public String index(){
        return "login";
    }
}
