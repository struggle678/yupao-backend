package com.struggle.yupao.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户登录请求体
 * @author Mr.Chen
 */
@Data
public class TeamUpdateRequest implements Serializable {

    private static final long serialVersionUID = -3630710365564735954L;
    /**
     * id
     */
    private Long id;

    /**
     * 队伍头像
     */
    private String teamAvatarUrl;


    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;


    /**
     * 过期时间
     */
    private Date expireTime;
    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;




}
