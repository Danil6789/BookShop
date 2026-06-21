package org.example.bookshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.bookshop.dto.cart.AddToCartRequest;
import org.example.bookshop.dto.cart.CartDto;
import org.example.bookshop.dto.cart.CartItemDto;
import org.example.bookshop.dto.cart.UpdateCartItemRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.example.bookshop.constant.ApiPath.CART_ITEM_BY_BOOK_ID_URL;
import static org.example.bookshop.constant.ApiPath.CART_ITEMS_URL;
import static org.example.bookshop.constant.ApiPath.CART_URL;

@Tag(name = "Корзина", description = "API для управления корзиной покупок")
@RequestMapping(CART_URL)
public interface CartApi {

    @GetMapping
    @Operation(summary = "Получить текущую корзину", description = "Возвращает все позиции корзины с агрегированными суммами")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Корзина пользователя"),
        @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    ResponseEntity<CartDto> getCart(@Parameter(hidden = true) Authentication authentication);

    @PostMapping(CART_ITEMS_URL)
    @Operation(summary = "Добавить книгу в корзину",
        description = "Если книга уже в корзине, её количество увеличивается. Иначе создаётся новая позиция.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Книга добавлена в корзину"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Книга не найдена")
    })
    ResponseEntity<CartItemDto> addItem(@Parameter(hidden = true) Authentication authentication,
                                        @Valid @RequestBody AddToCartRequest request);

    @PatchMapping(CART_ITEM_BY_BOOK_ID_URL)
    @Operation(summary = "Обновить количество книги", description = "Устанавливает точное количество (не инкрементирует)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Количество обновлено"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Позиция с указанным bookId не найдена в корзине")
    })
    ResponseEntity<CartItemDto> updateQuantity(@Parameter(hidden = true) Authentication authentication,
                                               @PathVariable Long bookId,
                                               @Valid @RequestBody UpdateCartItemRequest request);

    @DeleteMapping(CART_ITEM_BY_BOOK_ID_URL)
    @Operation(summary = "Удалить книгу из корзины")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Книга удалена из корзины"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Позиция с указанным bookId не найдена в корзине")
    })
    ResponseEntity<Void> removeItem(@Parameter(hidden = true) Authentication authentication,
                                    @PathVariable Long bookId);

    @DeleteMapping
    @Operation(summary = "Очистить корзину", description = "Удаляет все позиции из корзины текущего пользователя")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Корзина очищена"),
        @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    ResponseEntity<Void> clearCart(@Parameter(hidden = true) Authentication authentication);
}