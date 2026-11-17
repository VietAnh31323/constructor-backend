package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "project_category_map")
public class ProjectCategoryMapEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId;

    private Long categoryId;

    // 2.19: Khối lượng & đơn giá hạng mục theo từng dự án
    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "quantity", precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
