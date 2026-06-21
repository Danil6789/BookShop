package org.example.bookshop.mapper;

import org.example.bookshop.dto.order.OrderItemDto;
import org.example.bookshop.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderItemMapper {

    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "subtotal",
        expression = "java(orderItem.getPriceAtPurchase().multiply(BigDecimal.valueOf(orderItem.getQuantity())))")
    OrderItemDto toDto(OrderItem orderItem);
}
