package org.example.bookshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.bookshop.dto.auth.AuthResponse;
import org.example.bookshop.dto.auth.LoginRequest;
import org.example.bookshop.dto.auth.RegisterRequest;
import org.example.bookshop.dto.user.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.example.bookshop.constant.ApiPath.AUTH_LOGIN_URL;
import static org.example.bookshop.constant.ApiPath.AUTH_REGISTER_URL;

@Tag(name = "Аутентификация", description = "API для регистрации и входа")
@RequestMapping("/api/auth")
public interface AuthApi {

    @PostMapping(AUTH_REGISTER_URL)
    @Operation(
        summary = "Регистрация нового пользователя",
        description = "Создаёт нового пользователя с указанным username, password и role. " +
            "Возвращает данные созданного пользователя без токена — для получения токена используйте /login."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Пользователь успешно создан"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        @ApiResponse(responseCode = "409", description = "Username уже занят")
    })
    ResponseEntity<UserDto> register(@RequestBody @Valid RegisterRequest request);

    @PostMapping(AUTH_LOGIN_URL)
    @Operation(
        summary = "Вход в систему",
        description = "Аутентификация пользователя по username и password. " +
            "Возвращает JWT токен и срок его действия в секундах."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Успешный вход"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации (пустые поля)"),
        @ApiResponse(responseCode = "401", description = "Неверные учётные данные")
    })
    ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request);
}
