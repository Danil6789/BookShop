package org.example.bookshop.mapper;

import org.example.bookshop.dto.user.UserDto;
import org.example.bookshop.dto.auth.AuthResponse;
import org.example.bookshop.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserDto toDto(User user);

    default AuthResponse toAuthResponse(User user, String token, long expiresIn) {
        return new AuthResponse(token, expiresIn, user.getUsername(), user.getRole().name());
    }
}
