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
 * 用户扩展信息表 实体类。
 *
 * @author he
 * @since 2026-03-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_profile")
public class UserProfile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 关联wx_user.id
     */
    private Long userId;

    /**
     * 学号
     */
    private String studentId;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 学校
     */
    private String school;

    /**
     * 院系
     */
    private String department;

    /**
     * 认证状态 0-未提交 1-待审核 2-已通过 3-已驳回
     */
    private Integer authStatus;

    /**
     * 信誉分，初始80
     */
    private Integer creditScore;

    /**
     * 个人简介
     */
    private String intro;

    /**
     * 学生证图片地址
     */
    private String studentCardImageUrl;

    /**
     * 认证方式：student_card / edu_email
     */
    private String verifyType;

    /**
     * 认证提交时间
     */
    private LocalDateTime verifySubmitTime;

    /**
     * 审核备注
     */
    private String auditRemark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;

}
