package com.yxz.mymall.member.dao;

import com.yxz.mymall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author yuxinze
 * @email xinzeyu@seu.edu.cn
 * @date 2022-05-06 14:30:05
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
