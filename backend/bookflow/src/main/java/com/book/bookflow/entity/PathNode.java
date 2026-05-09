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
 * 学习路径节点表 实体类。
 *
 * @author he
 * @since 2026-03-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("path_node")
public class PathNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 所属路径ID
     */
    private Long pathId;

    /**
     * 父节点ID（支持嵌套）
     */
    private Long parentId;

    /**
     * 节点标题
     */
    private String title;

    /**
     * 节点描述/学习内容
     */
    private String description;

    /**
     * 本节点要掌握的学习目标
     */
    private String learningGoal;

    /**
     * 本节点推荐的学习方法
     */
    private String learningMethod;

    /**
     * 本节点完成标准
     */
    private String deliverable;

    /**
     * 学习步骤 JSON 数组
     */
    private String stepsJson;

    /**
     * 排序序号
     */
    private Integer orderNum;

    /**
     * 预计学习分钟数
     */
    private Integer estimatedMinutes;

    /**
     * 关联的资源ID列表（JSON数组）
     */
    private String resourceIds;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;

}
