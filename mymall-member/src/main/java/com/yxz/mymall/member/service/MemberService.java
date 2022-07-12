package com.yxz.mymall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxz.common.utils.PageUtils;
import com.yxz.mymall.member.entity.MemberEntity;
import com.yxz.mymall.member.exception.PhoneException;
import com.yxz.mymall.member.exception.UsernameException;
import com.yxz.mymall.member.vo.MemberUserLoginVo;
import com.yxz.mymall.member.vo.MemberUserRegisterVo;
import com.yxz.mymall.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author yuxinze
 * @email xinzeyu@seu.edu.cn
 * @date 2022-05-06 14:30:05
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void register(MemberUserRegisterVo vo);

    void checkPhoneUnique(String phone) throws PhoneException;

    void checkUserNameUnique(String userName) throws UsernameException;

    MemberEntity login(MemberUserLoginVo vo);

    MemberEntity login(SocialUser socialUser) throws Exception;
}

