package org.example.bookshop.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BookDto {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String coverUrl;
    private Long categoryId;
    private String categoryName;
}