package com.book.bookflow.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信用户表 实体类。
 *
 * @author he
 * @since 2026-02-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("wx_user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 微信openid
     */
    private String openid;

    /**
     * 微信unionid
     */
    private String unionid;

    /**
     * 会话密钥
     */
    private String sessionKey;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatarUrl;

    /**
     * 性别 0-未知 1-男 2-女
     */
    private Integer gender;

    /**
     * 国家
     */
    private String country;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除标记 0-正常 1-删除
     */
    private Integer isDeleted;

}
