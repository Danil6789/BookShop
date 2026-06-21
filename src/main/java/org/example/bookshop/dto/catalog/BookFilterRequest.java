package org.example.bookshop.dto.catalog;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookFilterRequest {
    private String q;
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}