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
 * 用户收藏表 实体类。
 *
 * @author he
 * @since 2026-03-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_favorite")
public class UserFavorite implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long userId;

    /**
     * 收藏类型：1-书籍 2-学习路径
     */
    private Integer targetType;

    /**
     * 对应的ID
     */
    private Long targetId;

    private LocalDateTime createTime;

}
