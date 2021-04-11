package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    UserDao userDao;
    public void delete(String username) {
        
    }
}
