package org.example.bookshop.mapper;

import org.example.bookshop.dto.cart.CartItemDto;
import org.example.bookshop.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartItemMapper {

    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "coverUrl", source = "book.coverUrl")
    @Mapping(target = "price", source = "book.price")
    @Mapping(target = "subtotal",
        expression = "java(item.getBook().getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))")
    CartItemDto toDto(CartItem item);
}