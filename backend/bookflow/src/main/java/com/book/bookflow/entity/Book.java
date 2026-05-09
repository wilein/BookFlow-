package com.book.bookflow.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 书籍表 实体类。
 *
 * @author he
 * @since 2026-02-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("book")
public class Book implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 发布者ID（wx_user.id）
     */
    private Long userId;

    /**
     * ISBN号
     */
    private String isbn;

    /**
     * 书名
     */
    private String title;

    /**
     * 作者
     */
    private String author;

    /**
     * 出版社
     */
    private String publisher;

    /**
     * 出版日期
     */
    private Date publishDate;

    /**
     * 封面图URL数组（JSON格式）
     */
    private String coverImages;

    /**
     * 书籍描述
     */
    private String description;

    /**
     * 分类，如计算机、文学
     */
    private String category;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 原价（可选）
     */
    private BigDecimal originalPrice;

    /**
     * 新旧程度：1-全新 2-9成新 3-8成新 4-7成新 5-6成新及以下
     */
    private Integer condition;

    /**
     * 状态：1-在售 2-交易中 3-已售 4-下架
     */
    private Integer status;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 收藏次数
     */
    private Integer favoriteCount;

    /**
     * 批注数量（冗余字段，方便统计）
     */
    private Integer annotationCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;

}
