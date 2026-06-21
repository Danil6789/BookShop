package org.example.bookshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.bookshop.dto.order.OrderDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.example.bookshop.constant.ApiPath.ORDERS_URL;

@Tag(name = "Заказы", description = "Оформление заказов из корзины")
@RequestMapping(ORDERS_URL)
public interface OrderApi {

    @PostMapping
    @Operation(summary = "Оформить заказ из корзины",
        description = "Создаёт заказ из всех позиций корзины текущего пользователя, "
            + "списывает товар со склада и очищает корзину. Атомарно: при ошибке ни одно изменение не сохраняется.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Заказ успешно оформлен"),
        @ApiResponse(responseCode = "400", description = "Корзина пуста"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "409", description = "Недостаточно товара на складе")
    })
    ResponseEntity<OrderDto> checkout(@Parameter(hidden = true) Authentication authentication);
}
