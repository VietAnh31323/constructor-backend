package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.domain.converter.LongListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "steel_project_assembly_map")
public class SteelProjectAssemblyMapEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String assemblyName;

    private Long steelProjectId;

    private Long sameQuantity;

    @Convert(converter = LongListConverter.class)
    private List<Long> steelIds;
}