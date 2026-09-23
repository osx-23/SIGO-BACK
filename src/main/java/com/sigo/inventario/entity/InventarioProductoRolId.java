package com.sigo.inventario.entity;
import lombok.*;
import java.io.Serializable;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class InventarioProductoRolId implements Serializable { private Long producto; private Long rol; }
