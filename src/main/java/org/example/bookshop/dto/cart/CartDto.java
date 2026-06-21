package org.example.bookshop.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class CartDto {
    private List<CartItemDto> items;
    private Integer totalQuantity;
    private BigDecimal totalPrice;
}