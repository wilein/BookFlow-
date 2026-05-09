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
@Table("order_issue")
public class OrderIssue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long orderId;

    private Long userId;

    /**
     * 1-疑问 2-售后 3-举报
     */
    private Integer type;

    private String content;

    private String replyContent;

    private Long replyUserId;

    /**
     * 0-待处理 1-已回复 2-已关闭
     */
    private Integer status;

    private LocalDateTime replyTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;
}
