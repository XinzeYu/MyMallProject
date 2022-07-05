package com.yxz.mymall.product.service.impl;

import com.yxz.mymall.product.service.CategoryBrandRelationService;
import com.yxz.mymall.product.vo.Catelog2Vo;
import org.apache.ibatis.ognl.CollectionElementsAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxz.common.utils.PageUtils;
import com.yxz.common.utils.Query;

import com.yxz.mymall.product.dao.CategoryDao;
import com.yxz.mymall.product.entity.CategoryEntity;
import com.yxz.mymall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

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
        //查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //组装成父子的树形结构
        //首先找到所有的一级分类，其parentid为0
        List<CategoryEntity> level1Menus = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu)->{
            menu.setChildren(getChildren(menu, entities));
            return menu;
        }).sorted((menu1, menu2)->{
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());



        return level1Menus;
    }

    /**
     * 递归查找当前菜单的所有子菜单
     *
     * @param root
     * @param entities
     * @return
     */
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> entities) {

        List<CategoryEntity> children = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //递归遍历子菜单
            categoryEntity.setChildren(getChildren(categoryEntity, entities));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {

        baseMapper.deleteBatchIds(asList);

    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> path = new ArrayList<>();
        findParentPath(catelogId, path);
        Collections.reverse(path);
        return (Long[]) path.toArray(new Long[path.size()]);
    }

    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        //级联更新所有的数据
        this.updateById(category);
        if(!StringUtils.isEmpty(category.getName())) {
            //同步更新关联表数据
            categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

            //TODO 更新其他关联表
        }
    }

    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        List<CategoryEntity> entityList = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return entityList;
    }

    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        //查出所有一级分类
        List<CategoryEntity> level1Categorys = getLevel1Categorys();

        //封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //每一个一级分类，查到这个一级分类的二级分类
            List<CategoryEntity> entityList = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
            //封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (entityList != null) {
                catelog2Vos = entityList.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //找当前2级分类的3级分类
                    List<CategoryEntity> level3Catelog = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", l2.getCatId()));
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Category3Vo> category3Vos = level3Catelog.stream().map(l3 -> {
                            Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return category3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(category3Vos);
                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));

        return parent_cid;
    }

    private void findParentPath(Long catelogId, List<Long> path) {
        path.add(catelogId);
        CategoryEntity categoryEntity = this.getById(catelogId);
        if(categoryEntity.getParentCid() != 0) {
            findParentPath(categoryEntity.getParentCid(), path);
        }
        return ;
    }

}