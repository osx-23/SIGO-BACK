package com.sigo.inventario.api.dto.request;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record GuardarConteoItemRequest(@NotNull Long productoId,@NotNull @DecimalMin("0.0") BigDecimal cantidad){}
