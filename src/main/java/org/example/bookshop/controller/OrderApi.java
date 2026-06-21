package org.example.bookshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.bookshop.dto.order.OrderDto;
import org.example.bookshop.dto.order.UpdateStatusRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static org.example.bookshop.constant.ApiPath.ORDERS_BY_ID_URL;
import static org.example.bookshop.constant.ApiPath.ORDERS_STATUS_URL;
import static org.example.bookshop.constant.ApiPath.ORDERS_URL;

@Tag(name = "Заказы", description = "Оформление заказов из корзины и управление ими")
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

    @GetMapping
    @Operation(summary = "Список заказов",
        description = "USER получает список своих заказов, ADMIN — всех заказов в системе. Сортировка по id DESC.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список заказов"),
        @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    ResponseEntity<List<OrderDto>> getOrders(@Parameter(hidden = true) Authentication authentication);

    @GetMapping(ORDERS_BY_ID_URL)
    @Operation(summary = "Детали заказа",
        description = "USER видит только свои заказы, ADMIN — любые. При попытке чужого заказа — 403.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Заказ найден"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Чужой заказ"),
        @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    ResponseEntity<OrderDto> getById(@Parameter(hidden = true) Authentication authentication,
                                     @PathVariable Long id);

    @PatchMapping(ORDERS_STATUS_URL)
    @Operation(summary = "Сменить статус заказа (ADMIN)",
        description = "Устанавливает произвольный статус заказа. Доступно только администратору.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Статус обновлён"),
        @ApiResponse(responseCode = "400", description = "Не указан статус"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
        @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    ResponseEntity<OrderDto> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody UpdateStatusRequest request);

    @DeleteMapping(ORDERS_BY_ID_URL)
    @Operation(summary = "Отменить заказ",
        description = "USER может отменить только свой заказ в статусе PENDING; ADMIN — любой PENDING. "
            + "При отмене товар возвращается на склад.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Заказ отменён, товар возвращён на склад"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Чужой заказ"),
        @ApiResponse(responseCode = "404", description = "Заказ не найден"),
        @ApiResponse(responseCode = "409", description = "Заказ в статусе, который нельзя отменить")
    })
    ResponseEntity<OrderDto> cancel(@Parameter(hidden = true) Authentication authentication,
                                     @PathVariable Long id);
}
