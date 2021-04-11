package com.yao.gulimall.search.vo;

import lombok.Data;
import org.elasticsearch.action.search.SearchResponse;

import java.util.List;

/**
 * 封装所有页面可能传递的查询条件
 */
@Data
public class SearchParam {

    private String keyword;//页面传递过来的全文匹配关键字
    private Long  catalog3Id; //3级分类id

    /**
     * sort=saleCount_asc/desc
     *sort=skuPrice_asc/desc
     * sort=hostScore_asc/desc
     */
    private String sort;//排序条件
    /**
     * 更多的过滤条件
     * hasStock(是否有货),skuPrice区间,brandId,cataLog3Id,attrs,
     * hasStock=0/1
     * skuPrice=1_500/_500/500_
     * brandId=1
     */
    private Integer hasStock;//显示是否有货
    private String skuPrice;//价格区间
    private List<Long> brandId;//按照品牌id查询（可以多选）
    private List<String> attrs;//按照属性筛选
    private Integer pageNum=1;//页码
}
