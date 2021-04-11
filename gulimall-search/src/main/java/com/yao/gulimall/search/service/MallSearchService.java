package com.yao.gulimall.search.service;

import com.yao.gulimall.search.vo.SearchParam;
import com.yao.gulimall.search.vo.SearchResult;

public interface MallSearchService {
    /**
     * 检索的所有参数:
     *      返回结果
     * @param param
     * @return
     */
    SearchResult search(SearchParam param);
}
