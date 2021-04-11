package com.yao.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.yao.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.IntStream;

@SpringBootTest
public class GulimallSearchApplicationTests {
	@Autowired
	private RestHighLevelClient client;
	public static String name="noob";
	final String noob="11";
	private final String age="12";
	public final String user="yao";
	private String password="123456";
	@Test
	public void Map(){
		List list = new ArrayList();

		Product product1 = new Product("noob",14);
		Product product2 = new Product("noob",20);
		Product product3 = new Product("noob",44);
		Product product4 = new Product("noob",122);

		list.add(product1);
		list.add(product2);
		list.add(product3);
		list.add(product4);

		for (Object item : list) {
			System.out.println("product: "+item);
		}
		System.out.println("--------------------------------------");
		Map<Integer,Object> map = new HashMap();

		map.put(1,product2);
		map.put(1,product1);



		for (Map.Entry<Integer, Object> item:map.entrySet()){
			System.out.println("key: "+item.getKey()+"values: "+item.getValue());
		}

		String s = new String("noob");
		if (s.equals("noob1")){
			System.out.println(".............");
		}else {
			System.out.println(",,,,,,,,,,,,,,");
		}
		System.out.println(s);
		char c = s.charAt(0);
		System.out.println("char="+c);

		Date date = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
		String format1 = format.format(date);
		System.out.println(format1);


		
		int l,h,m;
		int a=1,b=1,f=1;
		int[] k={1,2,3,4,5,6};
		Integer[] v={22,33,44,55,55};
		String[] vvv={"noooobb","ssss","wwwwwwww"};
		String vv="bbbbbbnllllllllll";
		int sum = Arrays.stream(k).sum();
		int length = k.length;
		System.out.println("sum:"+sum+"长度"+length);
		String[] ns = vv.split("n");
		int i = vv.hashCode();
		System.out.println("hashCode"+i);
		String n = ns[0];
		String bb = ns[1];
		System.out.println("0:"+n+"1:"+bb);


		System.out.println(Integer.SIZE);
		System.out.println(Integer.MAX_VALUE);

		Integer password2 = null;
		password2= Integer.valueOf(this.password);
		int i1 = password2 + 2;
		System.out.println("password2: "+i1);

		Integer[] noob ={1,2,3,4,5,6,7,8,9,10};
		int ifor;
		int length1 = k.length;
		for (Integer integer : noob) {
			System.out.println(integer);
		}



	}
	@Data
	static class Product {
		private String name;
		private Integer age;

		public Product(String name, Integer age) {
			this.name = name;
			this.age = age;
		}

	}

	@Test
	public void contextLoads() {
		System.out.println(client);
	}

	@ToString
	@Data
    static class Accout {
        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;
        }

    /**
     *
     * {
     *     skuid:1
     *     skuTitle:苹果
     *     price:9999
     *     saleCount:99
     *     attrs{
     *         {尺寸：5寸},
     *         {CPU:高通886},
     *         {分辨率:2k}
     *     }
     * }
     * 冗余:
     *  100万*20=1000000*2kb=2000mb=2G
     *
     *  {sku索引
     *      skuid:1
     *      spuid:11
     *      xxxx
     *  }
     *  attr索引{
     *      spuid:11,
     *      attrs:[{
     *          {尺寸：5寸},
     *          {CPU:高通886},
     *          {分辨率:2k}
     *      }]
     *  }
     *
      * @throws IOException
     */
    @Test
	public void searchData() throws IOException {
		//创建一个检索请求
		SearchRequest searchRequest = new SearchRequest();
		//指定索引
		searchRequest.indices("bank");
		//指定DSl检索条件
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		//构造检索条件
		searchSourceBuilder.query(QueryBuilders.matchQuery("address","mill"));
		//按照年龄进行聚合
		TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
		searchSourceBuilder.aggregation(ageAgg);
		//计算平均工资
		AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
		searchSourceBuilder.aggregation(balanceAvg);

		System.out.printf("检索条件"+searchSourceBuilder.toString());
		searchRequest.source(searchSourceBuilder);

		//执行检索
		SearchResponse searchResponse = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
		//分析结果
		System.out.printf(searchResponse.toString());
		//Map map = JSON.parseObject(searchResponse.toString(), Map.class);
		//获取所有查询的数据
            /**
             * "account_number": 970,
             * 				"balance": 19648,
             * 				"firstname": "Forbes",
             * 				"lastname": "Wallace",
             * 				"age": 28,
             * 				"gender": "M",
             * 				"address": "990 Mill Road",
             * 				"employer": "Pheast",
             * 				"email": "forbeswallace@pheast.com",
             * 				"city": "Lopezo",
             * 				"state": "AK"
             * }
             */
		SearchHits hits = searchResponse.getHits();
		SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            //hit.getIndex();hit.getType();hit.getId();
            String string = hit.getSourceAsString();
            Accout accout = JSON.parseObject(string, Accout.class);
            System.out.println("accout: "+accout);
        }
        //获取这次检索的分析信息
            Aggregations aggregations = searchResponse.getAggregations();
//            for (Aggregation aggregation : aggregations.asList()) {
//                //System.out.printf("当前聚合的名字:"+aggregation.getName());
//            }
            Terms ageAgg1 = aggregations.get("ageAgg");
            for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
                String keyAsString = bucket.getKeyAsString();
                System.out.printf("年龄: "+keyAsString);
            }
            Avg balanceAvg1 = aggregations.get("balanceAvg");
            System.out.printf("平均工资: "+balanceAvg1.getValue());

        }


	@Test
	public void IndexData() throws IOException {
		IndexRequest indexRequest =new IndexRequest("users");
		indexRequest.id("1");
	//	indexRequest.source("userName","zhanshan",18,"gender","男");
		User user = new User();
		user.setUserName("fffff");
		user.setAge(18);
		user.setGender("女");
		String jsonString = JSON.toJSONString(user);
		indexRequest.source(jsonString, XContentType.JSON);//要保存的内容

		//执行操作
		IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
		//提取有用的响应数据
		System.out.println(index);
	}
	@Data
	class User{
		private String userName;
		private String gender;
		private Integer age;
	}
}
