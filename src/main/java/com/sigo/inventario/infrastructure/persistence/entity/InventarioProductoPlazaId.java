package com.sigo.inventario.infrastructure.persistence.entity;
import lombok.*;
import java.io.Serializable;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class InventarioProductoPlazaId implements Serializable { private Long producto; private Long plaza; }
