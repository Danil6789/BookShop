package org.example.bookshop.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItemDto> items;
}
