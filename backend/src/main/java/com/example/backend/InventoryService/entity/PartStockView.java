package com.example.backend.InventoryService.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Immutable
@Table(name = "v_part_stock")
public class PartStockView {

    @EmbeddedId
    private PartStockViewId id;

    @Column(
            name = "on_hand",
            precision = 12,
            scale = 3
    )
    private BigDecimal onHand;

    // getters setters


    public PartStockViewId getId() {
        return id;
    }

    public void setId(PartStockViewId id) {
        this.id = id;
    }

    public BigDecimal getOnHand() {
        return onHand;
    }

    public void setOnHand(BigDecimal onHand) {
        this.onHand = onHand;
    }
}