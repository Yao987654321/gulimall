package com.yao.gulimall.search.controller;

import com.yao.gulimall.search.service.MallSearchService;
import com.yao.gulimall.search.vo.SearchParam;
import com.yao.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    /**
     * 自动将页面提交的所有查询参数封装成对象
     * @param param
     * @return
     */
    @GetMapping("/list.html")
    public String listPage(SearchParam param, Model model){
        //1,根据页面来的参数，去es中检索商品
        SearchResult result =  mallSearchService.search(param);
        List<SearchResult.AttrVo> attrs = result.getAttrs();
//        attrs.remove(0);
//        attrs.remove(1);
        model.addAttribute("result",result);
        return "list";
    }

}
