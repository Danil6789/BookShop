package org.example.bookshop.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.bookshop.entity.OrderStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStatusRequest {
    @NotNull
    private OrderStatus status;
}
