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
 * 评论表 实体类。
 *
 * @author he
 * @since 2026-03-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("comment")
public class Comment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long postId;

    private Long userId;

    private String content;

    private Integer likeCount;

    private LocalDateTime createTime;

    private Integer isDeleted;

}
