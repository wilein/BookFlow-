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
@Table("resource")
public class Resource implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long userId;

    private Long bookId;

    private String bindType;

    private Long bindId;

    private String title;

    /**
     * 1-课件 2-习题 3-笔记 4-拓展阅读 5-其他
     */
    private Integer type;

    private String fileUrl;

    private Long fileSize;

    private String fileFormat;

    private Integer downloadCount;

    private String description;

    /**
     * 1-公开 2-仅买家/可见用户 3-私密
     */
    private Integer visibility;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;
}
