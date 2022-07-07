package com.yxz.mymall.product.web;

import com.yxz.mymall.product.entity.CategoryEntity;
import com.yxz.mymall.product.service.CategoryService;
import com.yxz.mymall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redissonClient;

    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model) {

        //1、查出所有的一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();
        model.addAttribute("categories", categoryEntities);

        return "index";
    }

    //index/json/catalog.json
    @GetMapping(value = "/index/catalog.json")
    @ResponseBody
    public Map<String, List<Catelog2Vo>> getCatalogJson() {

        Map<String, List<Catelog2Vo>> catalogJson = categoryService.getCatalogJson();

        return catalogJson;

    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        //获取一把锁，只要锁的名字一样，就是同一把锁
        RLock lock = redissonClient.getLock("my_lock");

        //加锁，阻塞式等待，有自动续期机制
        //lock.lock();
        lock.lock(10, TimeUnit.SECONDS); //在锁时间到了以后不会自动续期，自动解锁时间一定要大于业务的执行时间

        //最佳实战
        /**
         * 1. lock.lock(10, TimeUnit.SECONDS); 省掉续期操作，手动解锁
         * 2. 读写锁。读锁是共享锁，写锁是排他锁，互斥锁。
         * 3. 闭锁。计数解锁。
         * 4. 信号量 tryAcquire()，无需阻塞式等待，可以用作限流
         */
        try{
            System.out.println("枷锁成功，执行。。");
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return "hello";

    }



}
