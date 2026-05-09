package com.book.bookflow.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("learning_path")
public class LearningPath implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long userId;

    private Long bookId;

    private Long sourcePathId;

    private String title;

    private String description;

    private String coverImage;

    /**
     * 封面审核状态：0-无需审核 1-待审核 2-已通过 3-已驳回
     */
    private Integer coverImageStatus;

    private Integer difficulty;

    private Integer estimatedHours;

    /**
     * 0-草稿 1-已发布 2-审核中 3-下架
     */
    private Integer status;

    private Integer viewCount;

    private Integer favoriteCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;
}
