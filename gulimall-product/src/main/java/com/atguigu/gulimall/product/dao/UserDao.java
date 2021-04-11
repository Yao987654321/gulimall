package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.UserEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserDao {

    @Select("SELECT * FROM user WHERE username = #{username} and password=#{password}")
    UserEntity select(UserEntity userEntity);
    @Insert("INSERT INTO user (username,password) VALUES(#{username},#{password})")
    void insert(UserEntity userEntity);
    @Select("SELECT * FROM user WHERE username = #{username}")
    String selectByname(String username);

    void test(UserEntity userEntity);
}
