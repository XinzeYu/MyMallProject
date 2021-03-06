package com.yxz.mymall.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yxz.mymall.product.entity.BrandEntity;
import com.yxz.mymall.product.service.BrandService;
import com.yxz.mymall.product.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

@SpringBootTest
class MymallProductApplicationTests {

    @Autowired
    BrandService brandService;
    
    @Autowired
    CategoryService categoryService;
    
    @Test
    void test(){
        Long[] catelogPath = categoryService.findCatelogPath(225L);
        for (Long aLong : catelogPath) {
            System.out.println(aLong);
        }
    }

    @Test
    void contextLoads() {

        /*BrandEntity brandEntity = new BrandEntity();
        brandEntity.setBrandId(1L);
        brandEntity.setDescript("novo");
        brandEntity.setName("华为");
        brandService.save(brandEntity);
        System.out.println("保存成功");
        brandService.updateById(brandEntity);*/

        //查询测试
        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id",1L));
        list.forEach((item)->{
            System.out.println(item);
        });
    }

}
