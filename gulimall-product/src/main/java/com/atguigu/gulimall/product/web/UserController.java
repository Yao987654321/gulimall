package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.dao.UserDao;
import com.atguigu.gulimall.product.entity.UserEntity;
import com.atguigu.gulimall.product.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UserController {

    @Autowired
    UserDao userDao;
    @Autowired
    UserService userService;

    @RequestMapping("/login")
    public String login(@RequestParam("username") String username, @RequestParam("password") String password){
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setPassword(password);
       UserEntity user= userDao.select(userEntity);
       if (user==null){
           return "error";
       }
       return "ok";
    }

    @RequestMapping("/regiest")
    public String regiest(@RequestParam("username") String username, @RequestParam("password") String password,
                        Model model){
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setPassword(password);
        if (username==null||password==null){
            String error ="用户为空";
            model.addAttribute("error",error);
            return "error";
        }else {
           String userCheck = userDao.selectByname(username);
           if (userCheck==null){
               userDao.insert(userEntity);
               return "ok";
           }else {
               return "error";
           }
        }
    }
    @GetMapping("intologin")
    public String into(){
        return "login";
    }
    @GetMapping("intoregiest")
    public String into2(){
        return "insert";
    }
    @RequestMapping("/test")
    public ModelAndView test(@RequestParam("username") String username, @RequestParam("password") String password){
        ModelAndView modelAndView = new ModelAndView("ok");
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setPassword(password);
        userDao.test(userEntity);
        return modelAndView;
    }
    @RequestMapping("/delete")
    public ModelAndView tset2(@RequestParam("username") String username){
        ModelAndView modelAndView = new ModelAndView("ok");
        userService.delete(username);

        return modelAndView;
    }

}
