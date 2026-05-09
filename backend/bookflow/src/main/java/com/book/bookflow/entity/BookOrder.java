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
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("order")
public class BookOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String orderNo;

    private Long bookId;

    private Long buyerId;

    private Long sellerId;

    private BigDecimal totalAmount;

    private Integer status;

    private Integer paymentMethod;

    private LocalDateTime paymentTime;

    private LocalDateTime deliveryTime;

    private LocalDateTime receiveTime;

    private LocalDateTime closeTime;

    private String buyerMessage;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;
}
