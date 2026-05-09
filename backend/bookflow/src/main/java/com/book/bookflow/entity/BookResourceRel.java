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
 * 书籍资源关联表 实体类。
 *
 * @author he
 * @since 2026-03-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("book_resource_rel")
public class BookResourceRel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long bookId;

    private Long resourceId;

    private LocalDateTime createTime;

}
