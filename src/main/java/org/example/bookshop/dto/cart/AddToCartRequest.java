package org.example.bookshop.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToCartRequest {
    @NotNull
    private Long bookId;

    @NotNull
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;
}