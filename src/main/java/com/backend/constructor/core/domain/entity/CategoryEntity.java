package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.common.validator.Unique;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "category")
public class CategoryEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Unique(err = "ERROR_0005")
    private String code;

    @Unique(err = "ERROR_0006")
    private String name;

    private String description;

    private Boolean isActive;

    // 2.18: Hạng mục dạng cây
    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "default_unit_price", precision = 18, scale = 2)
    private BigDecimal defaultUnitPrice;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
