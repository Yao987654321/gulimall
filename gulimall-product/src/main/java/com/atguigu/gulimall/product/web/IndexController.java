package com.atguigu.gulimall.product.web;

import com.alibaba.nacos.api.config.filter.IFilterConfig;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.CateLog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redisson;
    @Autowired
    StringRedisTemplate redisTemplate;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){

        //TODO 1,查出所有一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLeve1Category();


        //classpath:/templates/0 +返回值+ .html
        model.addAttribute("category",categoryEntities);
        return "index";
    }

    @ResponseBody
    @GetMapping("index/catalog.json")
    public Map<String, List<CateLog2Vo>> getCatalogJson(){
        Map<String, List<CateLog2Vo>> catalogJson = categoryService.getCatalogJson();
        return catalogJson;
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        //获取到锁
        RLock lock = redisson.getLock("my-lock");

        //加锁
        lock.lock();//阻塞式锁
        //锁的自动续期，如果业务过长运行期间自动续上30s，不用担心业务过长，锁自动过期
        //2加锁的业务只要运行完成即使不手动解锁，默认在30s后自动解锁
        try{
            System.out.println("加锁成功"+Thread.currentThread().getId());
            Thread.sleep(30000);
        }catch (Exception e){

        } finally{
            //解锁
            System.out.println("释放锁"+Thread.currentThread().getId());
            lock.unlock();
        }
        return "hello!world";
    }

    //保证能获取到最新数据
    @GetMapping("/write")
    @ResponseBody
    public String writeValue(){
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        String s="";
        RLock rLock = lock.writeLock();
        try {
            //改数据加写锁,读数据加读锁
            rLock.lock();
            System.out.println("写锁加锁成功"+Thread.currentThread().getId());
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
            redisTemplate.opsForValue().set("writeValue",s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
             rLock.unlock();
            System.out.println("写锁释放"+Thread.currentThread().getId());
        }
        return s;
    }

    @GetMapping("/read")
    @ResponseBody
    public String readValue(){
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        //加读锁
        RLock rLock = lock.readLock();
        rLock.lock();
        System.out.println("读锁加锁成功"+Thread.currentThread().getId());
        String s="";
        try {
            Thread.sleep(30000);
            s = redisTemplate.opsForValue().get("writeValue");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            rLock.unlock();
            System.out.println("读锁释放"+Thread.currentThread().getId());
        }
        return s;
    }

    @GetMapping("/park")
    @ResponseBody
    public String park(){
        RSemaphore park = redisson.getSemaphore("park");
        boolean b = park.tryAcquire();
        if (b){
            //执行业务
        }else {
            return "error";
        }
        return "ok=>"+b;
    }

    @GetMapping("/go")
    @ResponseBody
    public String go(){
        RSemaphore park = redisson.getSemaphore("park");
        park.release();
        return "ok";
    }


    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.trySetCount(5);
        door.await();//等待锁都完成

        return "放假了";
    }

    @GetMapping("/gogogo/{id}")
    public String gogogo(@PathVariable("id") Long id){
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.countDown();//计算减一
        return id+"班的走了";
    }
}
