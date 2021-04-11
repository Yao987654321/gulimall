package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//2级分类
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CateLog2Vo {
    private String catalog1Id; //一级分类
    private List<catalog3Vo>  catalog3List; //三级分类
    private String id;
    private String name;

    //三级分类
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class catalog3Vo{
        private String catalog2Id;
        private String id;
        private String name;
    }
}
