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
 * 轮播图表 实体类。
 *
 * @author he
 * @since 2026-02-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("banner")
public class Banner implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 图片标题/描述
     */
    private String title;

    /**
     * 图片相对路径或完整URL
     */
    private String imageUrl;

    /**
     * 点击跳转链接（小程序页面路径或HTTP链接）
     */
    private String link;

    /**
     * 排序值，越小越靠前
     */
    private Integer sortOrder;

    /**
     * 状态：1启用，0禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
