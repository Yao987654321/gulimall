package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.CateLog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    //private Map<String,Object> cache = new HashMap<>();
//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redisson;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2、组装成父子的树形结构

        //2.1）、找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
             categoryEntity.getParentCid() == 0
        ).map((menu)->{
            menu.setChildren(getChildrens(menu,entities));
            return menu;
        }).sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO  1、检查当前删除的菜单，是否被别的地方引用

        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);


        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     * @CacheEvict：失效模式
     * @Caching:同时进行多缓存操作
     * @param category
     */

//    @Caching(evict = {
//            @CacheEvict(value = "category",key = "'getLeve1Category'"),
//            @CacheEvict(value = "category",key = "'getCatalogJson'")
//    })
    @CacheEvict(value = "category",allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
        //同时修改缓存中的数据
        //redis.del("catalogJson")
    }
    //每一个需要缓存的数据我们都要指定放在哪个名字的缓存
    /**
     * 代表当前方法的结果需要缓存，如果有缓存方法,就不调用
     * 如果缓存中没有，会调用方法，最后将方法的结果放入缓存
     * 3，默认行为：
     *  1，如果缓存中有，方法就不调用
     *  2，key默认自动生成，缓存的名字：：SimpleKey[]（自动生成key值）
     *  3，缓存的value的值默认使用jdk序列化机制，将序列化后的数据存到redis
     *  4，默认ttl（存活时间）-1
     *
     *  自定义：
     *  1，指定生成缓存的key key属性指定，接收一个Spel
     *  2，指定缓存的存活时间，配置文件中修改ttl
     *  3，将数据保存为json格式
     *          自定义RedCacheconfiguration即可
     *  4，springcache的不足：
     *          1，读模式：
     *              缓存穿透：查询一个null的数据 解决：spring.cache.redis.cache-null-values=true
     *              缓存击穿：大量的数据并发进来查询一个正好过期的数据 解决：加锁
     *              缓存雪崩：大量的key同时过期 解决：加随机时间。加上过期时间 spring.cache.redis.time-to-live=3600000
     *          2,写模式：
     *              读写加锁
     *              引入canal感知到MySQL的更新去更新数据库
     *              读多写多，直接去数据库查询即可
     *        总结：
     *        常规数据（读多写少，及时性要求不高的）完全可以用springcache
     *        特殊数据：特殊设计.
     *      原理：
     *      CacheManager(RedisCachemanager)->Cache(RedisCache)->Cache负责缓存读写
     *
     */


    @Cacheable(value = {"category"},key = "#root.method.name")
    @Override
    public List<CategoryEntity> getLeve1Category() {
        System.out.println("getLeve1Category....");
        long time = System.currentTimeMillis();
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        System.out.println("消耗时间: "+(System.currentTimeMillis() - time));
        return categoryEntities;
    }
    //TODO 堆外内存溢出：OutOfDirectMemoryError
    //springboot2.0以后默认使用lettuce作为redis的客户端。它使用netty进行网络通信
    //lettuce的bug导致netty堆外内存溢出 -Xmx130m netty如果没有指定堆外内存，默认使用-Xmx130m
    //解决方案： -Dio.netty.maxDirectMemory进行设置
    //不能使用-Dio.netty.maxDirectMemory去调堆外内存
    //升级lettuce客户端 2，切换使用jedis

    @Cacheable(value = "category",key = "#root.method.name")
    @Override
    public  Map<String, List<CateLog2Vo>> getCatalogJson(){
        System.out.println("查询了数据库...... ");
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        List<CategoryEntity> leve1Category = getParent_cid(selectList, 0L);

        //封装数据
        Map<String, List<CateLog2Vo>> parent_cid = leve1Category.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //每一个一级分类，查到到一级分类的2级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            //封装上面的结果
            List<CateLog2Vo> cateLog2Vos = null;
            if (categoryEntities != null) {
                cateLog2Vos = categoryEntities.stream().map(l2 -> {
                    CateLog2Vo cateLog2Vo = new CateLog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //当前2级的3级封装成vo
                    List<CategoryEntity> leve3Category = getParent_cid(selectList, l2.getCatId());

                    if (leve3Category != null) {
                        List<CateLog2Vo.catalog3Vo> collect = leve3Category.stream().map(l3 -> {
                            //封装成指定格式
                            CateLog2Vo.catalog3Vo catalog3Vo = new CateLog2Vo.catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName().toString());
                            return catalog3Vo;
                        }).collect(Collectors.toList());
                        cateLog2Vo.setCatalog3List(collect);
                    }
                    return cateLog2Vo;
                }).collect(Collectors.toList());
            }
            return cateLog2Vos;
        }));
        return parent_cid;
    }



   // @Override
    public Map<String, List<CateLog2Vo>> getCatalogJson2(){
        //给缓存中放json字符串，拿出的json字符串，还要逆转为能用的对象类型{序列化，反序列化}

        /**
         *  1,空结果缓存：解决缓存穿透
         *  2，设置过期时间（加随机数）：解决缓存雪崩
         *  3，加锁：解决缓存击穿
         */

        //1,加入缓存逻辑,缓存中的数据都是json字符串
        //JSON跨语言，跨平台兼容
        String catalogJson = stringRedisTemplate.opsForValue().get("catalogJson");

        if (StringUtils.isEmpty(catalogJson)){
        //2,缓存中没有,查询数据库
        System.out.println("缓存不命中....将要查询数据库..... ");
        Map<String, List<CateLog2Vo>> catalogJsonForDB = getCatalogJsonForDBWithRedisLock();

            return catalogJsonForDB;
        }

        //转为我们指定的对象
        System.out.println("缓存命中....直接返回..... ");
        Map<String, List<CateLog2Vo>> result = JSON.parseObject(catalogJson,new TypeReference<Map<String, List<CateLog2Vo>>>(){});
        return result;
    }

    /**
     * 缓存数据一致性问题
     * @return
     */
    public Map<String, List<CateLog2Vo>> getCatalogJsonForDBWithRedissonLock() {
            //1,占用分布式锁去reids占坑
        //锁的粒度：具体缓存的是某个数据 11-号商品 product-11-Lock product-12-Lock
        RLock lock = redisson.getLock("catalogJson-lock");
        lock.lock();
        Map<String, List<CateLog2Vo>> dataFromDb;
        try {
            dataFromDb = getDataFromDb();
        }finally {
            lock.unlock();
        }
            return dataFromDb;
        }



    public Map<String, List<CateLog2Vo>> getCatalogJsonForDBWithRedisLock() {
        //1,占用分布式锁去reids占坑
        String uuid = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock",uuid,300,TimeUnit.SECONDS);
        if (lock){
            System.out.println("获取分布式锁成功...");
            //加锁成功，返回数据
            //设置过期时间,必须和加锁是同步的，原子的
            //stringRedisTemplate.expire("lock",30,TimeUnit.SECONDS);
            Map<String, List<CateLog2Vo>> dataFromDb;
            try {
                 dataFromDb = getDataFromDb();
            }finally {
                String script="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                stringRedisTemplate.execute(new DefaultRedisScript<Long>(script,Long.class)
                        ,Arrays.asList("lock"),uuid);
            }
            //获取值对比+对比值成功删除=原子操作 Lua脚本解锁
/*            Object lockValue = stringRedisTemplate.opsForValue().get("lock");
            if (uuid.equals(lockValue)){
                //删除我自己的锁
                stringRedisTemplate.delete("lock");//删除锁
            }*/
            return dataFromDb;
        }else {
            System.out.println("获取分布式锁失败...等待重试...");
            //占锁失败重试 synchronized
            //休眠100ms重试
            try {
                Thread.sleep(200);
            } catch (Exception e) {

            }
            return   getCatalogJsonForDBWithRedisLock();//自旋的方式
        }
    }



    private Map<String, List<CateLog2Vo>> getDataFromDb() {
        String catalogJson = stringRedisTemplate.opsForValue().get("catalogJson");
        if (!StringUtils.isEmpty(catalogJson)) {
            //缓存不为null直接返回
            Map<String, List<CateLog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<CateLog2Vo>>>() {
            });
            return result;
        }
        System.out.println("查询了数据库...... ");

        List<CategoryEntity> selectList = baseMapper.selectList(null);


        List<CategoryEntity> leve1Category = getParent_cid(selectList, 0L);

        //封装数据
        Map<String, List<CateLog2Vo>> parent_cid = leve1Category.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //每一个一级分类，查到到一级分类的2级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            //封装上面的结果
            List<CateLog2Vo> cateLog2Vos = null;
            if (categoryEntities != null) {
                cateLog2Vos = categoryEntities.stream().map(l2 -> {
                    CateLog2Vo cateLog2Vo = new CateLog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //当前2级的3级封装成vo
                    List<CategoryEntity> leve3Category = getParent_cid(selectList, l2.getCatId());

                    if (leve3Category != null) {
                        List<CateLog2Vo.catalog3Vo> collect = leve3Category.stream().map(l3 -> {
                            //封装成指定格式
                            CateLog2Vo.catalog3Vo catalog3Vo = new CateLog2Vo.catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName().toString());
                            return catalog3Vo;
                        }).collect(Collectors.toList());
                        cateLog2Vo.setCatalog3List(collect);
                    }
                    return cateLog2Vo;
                }).collect(Collectors.toList());
            }
            return cateLog2Vos;
        }));
        //3,查到的数据放入缓存，将对象转为json放在缓存
        String s = JSON.toJSONString(parent_cid);
        stringRedisTemplate.opsForValue().set("catalogJson", s, 1, TimeUnit.DAYS);
        return parent_cid;
    }

    //从数据库查询并封装数据
    public Map<String, List<CateLog2Vo>> getCatalogJsonForDBWithLocalLock() {
//        1，如果缓存中有就用缓存中的
//        Map<String, List<CateLog2Vo>> catalogJson = (Map<String, List<CateLog2Vo>>) cache.get("catalogJson");
//        if (cache.get("catalogJson") == null) {
//       cache.put("catalogJson",parent_cid);
//  }
//        return catalogJson;

        //只要是同一把锁就能锁住这个锁的所有线程
        //synchronized (this) springboot所有的组件在容器中都是单例的

        //TODO 本地锁 ：synchronized JUC(lOCK)在分布式情况下，想要锁住所有，必须所有分布式锁
        synchronized (this){
            //得到锁以后，我们应该在去缓存确定一次，如果没有才需要继续查询
            return getDataFromDb();
        }

    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList,Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
        return  collect;
        // return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
    }

    //225,25,2
    private List<Long> findParentPath(Long catelogId,List<Long> paths){
        //1、收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid()!=0){
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;

    }


    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root,List<CategoryEntity> all){

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //1、找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity,all));
            return categoryEntity;
        }).sorted((menu1,menu2)->{
            //2、菜单的排序
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }



}