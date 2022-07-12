package com.yxz.mymall.member.vo;

import lombok.Data;

@Data
public class SocialUser {

    private String access_token;

    private String remind_in;

    private long expires_in;

    private String uid;

    private long id;

    private String isRealName;

}
