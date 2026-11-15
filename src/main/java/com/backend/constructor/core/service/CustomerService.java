package com.backend.constructor.core.service;

import com.backend.constructor.app.api.CustomerApi;
import com.backend.constructor.app.api.PublicCustomerLeadApi;
import com.backend.constructor.app.dto.customer.*;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.GenerateCodeService;
import com.backend.constructor.common.validator.UniqueValidationService;
import com.backend.constructor.core.domain.constant.Constants;
import com.backend.constructor.core.domain.entity.CustomerCareLogEntity;
import com.backend.constructor.core.domain.entity.CustomerEntity;
import com.backend.constructor.core.domain.entity.ProjectEntity;
import com.backend.constructor.core.domain.entity.StaffEntity;
import com.backend.constructor.core.domain.enums.*;
import com.backend.constructor.core.port.mapper.CustomerCareLogMapper;
import com.backend.constructor.core.port.mapper.CustomerMapper;
import com.backend.constructor.core.port.repository.CustomerCareLogRepository;
import com.backend.constructor.core.port.repository.CustomerRepository;
import com.backend.constructor.core.port.repository.ProjectRepository;
import com.backend.constructor.core.port.repository.StaffRepository;
import com.backend.constructor.user.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService implements CustomerApi, PublicCustomerLeadApi {
    private static final Pattern TAX_CODE_PATTERN = Pattern.compile("^\\d{10}(-?\\d{3})?$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{9}(\\d{3})?$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{9,15}$");

    private final CustomerRepository customerRepository;
    private final CustomerCareLogRepository customerCareLogRepository;
    private final ProjectRepository projectRepository;
    private final StaffRepository staffRepository;
    private final CustomerMapper customerMapper;
    private final CustomerCareLogMapper customerCareLogMapper;
    private final GenerateCodeService generateCodeService;
    private final UniqueValidationService uniqueValidationService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public IdResponse create(CustomerDto input) {
        input.trimData();
        validateCustomerClassification(input);
        Long resolvedStaffId = resolveAssignedStaffId(input);

        generateCodeService.generateCode(input, Constants.KH, CustomerEntity.class);
        input.setId(null);

        CustomerEntity entity = customerMapper.toEntity(input);
        applyCustomerDefaults(entity, resolvedStaffId);
        uniqueValidationService.validate(entity);
        customerRepository.save(entity);
        return IdResponse.builder().id(entity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(CustomerDto input) {
        if (input.getId() == null) {
            throw BusinessException.exception("CST003");
        }
        input.trimData();
        validateCustomerClassification(input);
        Long resolvedStaffId = resolveAssignedStaffId(input);

        generateCodeService.generateCode(input, Constants.KH, CustomerEntity.class);
        CustomerEntity entity = customerRepository.getCustomerById(input.getId());
        String previousName = entity.getName();
        CustomerPipelineStage previousStage = entity.getPipelineStage();

        customerMapper.update(input, entity);
        applyCustomerDefaults(entity, resolvedStaffId);
        uniqueValidationService.validate(entity);
        customerRepository.save(entity);

        // Đồng bộ tên chủ đầu tư (project.owner) nếu khách hàng đổi tên
        if (!Objects.equals(previousName, entity.getName())) {
            List<ProjectEntity> linkedProjects = projectRepository.getListByCustomerId(entity.getId());
            if (!linkedProjects.isEmpty()) {
                for (ProjectEntity project : linkedProjects) {
                    project.setOwner(entity.getName());
                }
                projectRepository.saveAll(linkedProjects);
            }
        }

        if (previousStage != null && previousStage != entity.getPipelineStage()) {
            auditLogService.logExplicit(
                    "CHANGE_PIPELINE_STAGE",
                    "CUSTOMER",
                    "CustomerEntity",
                    String.valueOf(entity.getId()),
                    Map.of("pipelineStage", previousStage.name()),
                    Map.of("pipelineStage", entity.getPipelineStage().name()),
                    "Chuyển trạng thái pipeline khách hàng " + entity.getName()
                            + " từ " + previousStage.getLabel()
                            + " sang " + entity.getPipelineStage().getLabel(),
                    null
            );
        }

        return IdResponse.builder().id(entity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse delete(Long id) {
        CustomerEntity customerEntity = customerRepository.getCustomerById(id);
        if (projectRepository.existsByCustomerId(id)) {
            throw BusinessException.exception("CST035");
        }
        List<CustomerCareLogEntity> careLogs = customerCareLogRepository.getListByCustomerId(id);
        if (!careLogs.isEmpty()) {
            customerCareLogRepository.deleteAll(careLogs);
        }
        customerRepository.delete(customerEntity);
        return IdResponse.builder().id(customerEntity.getId()).build();
    }

    @Override
    public CustomerDto getDetail(Long id) {
        CustomerEntity entity = customerRepository.getCustomerById(id);
        CustomerDto dto = customerMapper.toDto(entity);
        normalizeDtoDefaults(dto);

        if (entity.getAssignedStaffId() != null) {
            Map<Long, CodeNameResponse> staffMap = staffRepository.getMapSimpleStaffByIds(Set.of(entity.getAssignedStaffId()));
            dto.setAssignedStaff(staffMap.get(entity.getAssignedStaffId()));
        }

        // 2.06: Tải danh sách dự án liên kết của khách hàng
        List<ProjectEntity> projects = projectRepository.getListByCustomerId(id);
        Set<Long> managerIds = projects.stream()
                .map(ProjectEntity::getManagerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CodeNameResponse> managerMap = managerIds.isEmpty()
                ? Collections.emptyMap()
                : staffRepository.getMapSimpleStaffByIds(managerIds);

        BigDecimal totalValue = BigDecimal.ZERO;
        List<CustomerProjectItemDto> projectItems = new ArrayList<>();
        for (ProjectEntity proj : projects) {
            BigDecimal contractVal = proj.getContractValue() != null ? proj.getContractValue() : BigDecimal.ZERO;
            totalValue = totalValue.add(contractVal);
            BigDecimal rem = proj.getRemainingAmount() != null ? proj.getRemainingAmount() : BigDecimal.ZERO;
            PaymentStatus payStatus = rem.compareTo(BigDecimal.ZERO) > 0
                    ? PaymentStatus.NOT_COMPLETED
                    : PaymentStatus.COMPLETED;

            projectItems.add(CustomerProjectItemDto.builder()
                    .id(proj.getId())
                    .code(proj.getCode())
                    .name(proj.getName())
                    .address(proj.getAddress())
                    .contractValue(contractVal)
                    .contractAdvance(proj.getContractAdvance())
                    .remainingAmount(rem)
                    .signDate(proj.getSignDate())
                    .deliveryDate(proj.getDeliveryDate())
                    .progressPercent(proj.getProgressPercent() != null ? proj.getProgressPercent() : BigDecimal.ZERO)
                    .state(proj.getState() != null ? proj.getState() : ProjectState.RECEIVED)
                    .paymentStatus(payStatus)
                    .manager(proj.getManagerId() != null ? managerMap.get(proj.getManagerId()) : null)
                    .build());
        }

        dto.setProjects(projectItems);
        dto.setProjectCount(projectItems.size());
        dto.setTotalContractValue(totalValue);

        // 2.07: Tóm tắt lịch sử chăm sóc khách hàng
        List<CustomerCareLogEntity> careLogs = customerCareLogRepository.getListByCustomerId(id);
        enrichCareSummaryOnCustomerDto(dto, careLogs, LocalDateTime.now());

        return dto;
    }

    @Override
    public Page<CustomerDto> getListStaff(CustomerFilterParam param, Pageable pageable) {
        Page<CustomerEntity> customerEntities = customerRepository.getPageCustomer(param, pageable);
        List<Long> customerIds = customerEntities.getContent().stream()
                .map(CustomerEntity::getId)
                .filter(Objects::nonNull)
                .toList();

        Set<Long> assignedStaffIds = customerEntities.getContent().stream()
                .map(CustomerEntity::getAssignedStaffId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CodeNameResponse> staffMap = assignedStaffIds.isEmpty()
                ? Collections.emptyMap()
                : staffRepository.getMapSimpleStaffByIds(assignedStaffIds);

        Map<Long, List<ProjectEntity>> projectsByCustomerId = customerIds.isEmpty()
                ? Collections.emptyMap()
                : projectRepository.getListByCustomerIds(customerIds).stream()
                .filter(p -> p.getCustomerId() != null)
                .collect(Collectors.groupingBy(ProjectEntity::getCustomerId));

        Map<Long, List<CustomerCareLogEntity>> careLogsByCustomerId = customerIds.isEmpty()
                ? Collections.emptyMap()
                : customerCareLogRepository.getListByCustomerIds(customerIds).stream()
                .collect(Collectors.groupingBy(CustomerCareLogEntity::getCustomerId));

        LocalDateTime now = LocalDateTime.now();

        return customerEntities.map(entity -> {
            CustomerDto dto = customerMapper.toDto(entity);
            normalizeDtoDefaults(dto);
            if (entity.getAssignedStaffId() != null) {
                dto.setAssignedStaff(staffMap.get(entity.getAssignedStaffId()));
            }

            List<ProjectEntity> linkedProjects = projectsByCustomerId.getOrDefault(entity.getId(), Collections.emptyList());
            BigDecimal sumValue = linkedProjects.stream()
                    .map(ProjectEntity::getContractValue)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setProjectCount(linkedProjects.size());
            dto.setTotalContractValue(sumValue);

            List<CustomerCareLogEntity> logs = careLogsByCustomerId.getOrDefault(entity.getId(), Collections.emptyList());
            enrichCareSummaryOnCustomerDto(dto, logs, now);

            return dto;
        });
    }

    // ==================== 2.08: PIPELINE ====================

    @Override
    public CustomerPipelineSummaryDto getPipelineSummary() {
        List<CustomerEntity> allCustomers = customerRepository.findAll();
        List<Long> customerIds = allCustomers.stream()
                .map(CustomerEntity::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, List<ProjectEntity>> projectsByCustomer = customerIds.isEmpty()
                ? Collections.emptyMap()
                : projectRepository.getListByCustomerIds(customerIds).stream()
                .filter(p -> p.getCustomerId() != null)
                .collect(Collectors.groupingBy(ProjectEntity::getCustomerId));

        Map<Long, List<CustomerCareLogEntity>> careLogsByCustomer = customerIds.isEmpty()
                ? Collections.emptyMap()
                : customerCareLogRepository.getListByCustomerIds(customerIds).stream()
                .collect(Collectors.groupingBy(CustomerCareLogEntity::getCustomerId));

        Map<CustomerPipelineStage, Long> stageCustomerCounts = new EnumMap<>(CustomerPipelineStage.class);
        Map<CustomerPipelineStage, Long> stageProjectCounts = new EnumMap<>(CustomerPipelineStage.class);
        Map<CustomerPipelineStage, BigDecimal> stageContractTotals = new EnumMap<>(CustomerPipelineStage.class);

        long individualCount = 0;
        long businessCount = 0;
        long upcomingFollowUps = 0;
        long overdueFollowUps = 0;
        BigDecimal grandTotalValue = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (CustomerEntity c : allCustomers) {
            CustomerType type = c.getCustomerType() != null ? c.getCustomerType() : CustomerType.INDIVIDUAL;
            if (type == CustomerType.BUSINESS) {
                businessCount++;
            } else {
                individualCount++;
            }

            CustomerPipelineStage stage = c.getPipelineStage() != null
                    ? c.getPipelineStage()
                    : CustomerPipelineStage.LEAD;
            stageCustomerCounts.merge(stage, 1L, Long::sum);

            List<ProjectEntity> cProjects = projectsByCustomer.getOrDefault(c.getId(), Collections.emptyList());
            stageProjectCounts.merge(stage, (long) cProjects.size(), Long::sum);
            BigDecimal cVal = cProjects.stream()
                    .map(ProjectEntity::getContractValue)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stageContractTotals.merge(stage, cVal, BigDecimal::add);
            grandTotalValue = grandTotalValue.add(cVal);

            List<CustomerCareLogEntity> cLogs = careLogsByCustomer.getOrDefault(c.getId(), Collections.emptyList());
            if (!cLogs.isEmpty()) {
                CustomerCareLogEntity latest = cLogs.getFirst();
                if (latest.getNextActionDate() != null) {
                    if (latest.getNextActionDate().isBefore(now)) {
                        overdueFollowUps++;
                    } else if (!latest.getNextActionDate().isAfter(now.plusDays(7))) {
                        upcomingFollowUps++;
                    }
                }
            }
        }

        List<CustomerPipelineSummaryDto.StageItem> stageItems = new ArrayList<>();
        for (CustomerPipelineStage stage : CustomerPipelineStage.values()) {
            stageItems.add(CustomerPipelineSummaryDto.StageItem.builder()
                    .stage(stage)
                    .label(stage.getLabel())
                    .stepOrder(stage.getStepOrder())
                    .customerCount(stageCustomerCounts.getOrDefault(stage, 0L))
                    .projectCount(stageProjectCounts.getOrDefault(stage, 0L))
                    .totalContractValue(stageContractTotals.getOrDefault(stage, BigDecimal.ZERO))
                    .build());
        }

        return CustomerPipelineSummaryDto.builder()
                .totalCustomers(allCustomers.size())
                .individualCustomers(individualCount)
                .businessCustomers(businessCount)
                .upcomingFollowUps(upcomingFollowUps)
                .overdueFollowUps(overdueFollowUps)
                .totalContractValue(grandTotalValue)
                .stages(stageItems)
                .build();
    }

    @Override
    @Transactional
    public IdResponse updatePipelineStage(Long customerId, CustomerPipelineStageUpdateDto input) {
        CustomerEntity customer = customerRepository.getCustomerById(customerId);
        CustomerPipelineStage oldStage = customer.getPipelineStage() != null
                ? customer.getPipelineStage()
                : CustomerPipelineStage.LEAD;
        CustomerPipelineStage newStage = input.getPipelineStage();

        if (customer.getCustomerType() == CustomerType.BUSINESS
                && isContractOrLaterStage(newStage)
                && (StringUtils.isBlank(customer.getTaxCode()) || StringUtils.isBlank(customer.getRepresentativeName()))) {
            throw BusinessException.exception("CST033");
        }

        customer.setPipelineStage(newStage);
        if (newStage != CustomerPipelineStage.LOST && customer.getContactStatus() == ContactStatus.NOT_CONTACTED) {
            customer.setContactStatus(ContactStatus.CONTACTED);
        }
        customerRepository.save(customer);

        String trimmedNote = StringUtils.trimToNull(input.getNote());
        if (trimmedNote != null) {
            Long actorStaffId = SecurityUtils.getCurrentUserLogin()
                    .flatMap(staffRepository::findOptionalStaffByUsername)
                    .map(StaffEntity::getId)
                    .orElse(customer.getAssignedStaffId());

            CustomerCareLogEntity autoLog = CustomerCareLogEntity.builder()
                    .customerId(customerId)
                    .staffId(actorStaffId)
                    .careChannel(CareChannel.OTHER)
                    .careTime(LocalDateTime.now())
                    .summary("Chuyển trạng thái Pipeline: " + oldStage.getLabel() + " → " + newStage.getLabel())
                    .content(trimmedNote)
                    .outcome(newStage == CustomerPipelineStage.LOST ? CareOutcome.DECLINED : CareOutcome.POSITIVE)
                    .build();
            customerCareLogRepository.save(autoLog);
        }

        auditLogService.logExplicit(
                "CHANGE_PIPELINE_STAGE",
                "CUSTOMER",
                "CustomerEntity",
                String.valueOf(customer.getId()),
                Map.of("pipelineStage", oldStage.name()),
                Map.of("pipelineStage", newStage.name(), "note", trimmedNote != null ? trimmedNote : ""),
                "Chuyển pipeline khách hàng " + customer.getName()
                        + ": " + oldStage.getLabel() + " → " + newStage.getLabel(),
                null
        );

        return IdResponse.builder().id(customer.getId()).build();
    }

    // ==================== 2.07: LỊCH SỬ CHĂM SÓC KHÁCH HÀNG ====================

    @Override
    public List<CustomerCareLogDto> getCareLogs(Long customerId) {
        customerRepository.getCustomerById(customerId);
        List<CustomerCareLogEntity> entities = customerCareLogRepository.getListByCustomerId(customerId);
        Set<Long> staffIds = entities.stream()
                .map(CustomerCareLogEntity::getStaffId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CodeNameResponse> staffMap = staffIds.isEmpty()
                ? Collections.emptyMap()
                : staffRepository.getMapSimpleStaffByIds(staffIds);

        LocalDateTime now = LocalDateTime.now();
        return entities.stream()
                .map(entity -> {
                    CustomerCareLogDto dto = customerCareLogMapper.toDto(entity);
                    if (entity.getStaffId() != null) {
                        dto.setStaff(staffMap.get(entity.getStaffId()));
                    }
                    if (entity.getNextActionDate() != null) {
                        dto.setOverdueFollowUp(entity.getNextActionDate().isBefore(now));
                        dto.setUpcomingFollowUp(!entity.getNextActionDate().isBefore(now)
                                && !entity.getNextActionDate().isAfter(now.plusDays(7)));
                    }
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional
    public IdResponse addCareLog(Long customerId, CustomerCareLogDto input) {
        CustomerEntity customer = customerRepository.getCustomerById(customerId);
        input.trimData();

        Long staffId = resolveCareStaffId(input, customer.getAssignedStaffId());
        CustomerCareLogEntity entity = customerCareLogMapper.toEntity(input);
        entity.setId(null);
        entity.setCustomerId(customerId);
        entity.setStaffId(staffId);
        if (entity.getCareTime() == null) {
            entity.setCareTime(LocalDateTime.now());
        }
        customerCareLogRepository.save(entity);

        // Đồng bộ trạng thái liên hệ & người phụ trách khách hàng nếu cần
        boolean customerDirty = false;
        if (customer.getAssignedStaffId() == null && staffId != null) {
            customer.setAssignedStaffId(staffId);
            customerDirty = true;
        }
        if (entity.getOutcome() == CareOutcome.POSITIVE || entity.getOutcome() == CareOutcome.NEED_FOLLOW_UP) {
            if (customer.getContactStatus() != ContactStatus.RESPONDED) {
                customer.setContactStatus(ContactStatus.RESPONDED);
                customerDirty = true;
            }
        } else if (entity.getOutcome() == CareOutcome.NO_ANSWER) {
            if (customer.getContactStatus() == ContactStatus.NOT_CONTACTED) {
                customer.setContactStatus(ContactStatus.NO_RESPONSE);
                customerDirty = true;
            }
        } else if (customer.getContactStatus() == null || customer.getContactStatus() == ContactStatus.NOT_CONTACTED) {
            customer.setContactStatus(ContactStatus.CONTACTED);
            customerDirty = true;
        }
        if (customerDirty) {
            customerRepository.save(customer);
        }

        return IdResponse.builder().id(entity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse updateCareLog(Long customerId, Long logId, CustomerCareLogDto input) {
        CustomerEntity customer = customerRepository.getCustomerById(customerId);
        CustomerCareLogEntity entity = customerCareLogRepository.getCareLogById(logId);
        if (!Objects.equals(entity.getCustomerId(), customerId)) {
            throw BusinessException.exception("CST038");
        }
        input.trimData();

        Long staffId = resolveCareStaffId(input, entity.getStaffId() != null ? entity.getStaffId() : customer.getAssignedStaffId());
        LocalDateTime existingTime = entity.getCareTime();
        customerCareLogMapper.update(input, entity);
        entity.setStaffId(staffId);
        if (entity.getCareTime() == null) {
            entity.setCareTime(existingTime != null ? existingTime : LocalDateTime.now());
        }
        customerCareLogRepository.save(entity);
        return IdResponse.builder().id(entity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse deleteCareLog(Long customerId, Long logId) {
        customerRepository.getCustomerById(customerId);
        CustomerCareLogEntity entity = customerCareLogRepository.getCareLogById(logId);
        if (!Objects.equals(entity.getCustomerId(), customerId)) {
            throw BusinessException.exception("CST038");
        }
        customerCareLogRepository.delete(entity);
        return IdResponse.builder().id(entity.getId()).build();
    }

    // ==================== TIẾP NHẬN LEAD TỪ WEBSITE CÔNG KHAI ====================

    @Override
    @Transactional
    public PublicCustomerLeadResponseDto submitPublicLead(PublicCustomerLeadRequestDto input) {
        input.trimData();
        String normalizedPhone = input.getPhone() != null
                ? input.getPhone().replaceAll("[\\s.\\-()]", "")
                : "";
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw BusinessException.exception("CST039");
        }
        if (StringUtils.isNotBlank(input.getTaxCode())
                && !TAX_CODE_PATTERN.matcher(input.getTaxCode()).matches()) {
            throw BusinessException.exception("CST036");
        }

        String sourceLabel = StringUtils.defaultIfBlank(input.getSource(), "Website Landing Page");
        String inquiryContent = buildLeadInquirySummary(input, sourceLabel);
        CustomerType resolvedType = input.getCustomerType() != null
                ? input.getCustomerType()
                : CustomerType.INDIVIDUAL;

        Optional<CustomerEntity> existingOpt = customerRepository.findFirstByPhone(normalizedPhone);
        if (existingOpt.isPresent()) {
            CustomerEntity existing = existingOpt.get();
            if (StringUtils.isBlank(existing.getEmail()) && StringUtils.isNotBlank(input.getEmail())) {
                existing.setEmail(input.getEmail());
            }
            if (StringUtils.isBlank(existing.getAddress()) && StringUtils.isNotBlank(input.getAddress())) {
                existing.setAddress(input.getAddress());
            }
            if (StringUtils.isBlank(existing.getRepresentativeName()) && StringUtils.isNotBlank(input.getRepresentativeName())) {
                existing.setRepresentativeName(input.getRepresentativeName());
            }
            if (StringUtils.isBlank(existing.getTaxCode()) && StringUtils.isNotBlank(input.getTaxCode())) {
                existing.setTaxCode(input.getTaxCode());
                uniqueValidationService.validate(existing);
            }
            existing.setIsPotential(Boolean.TRUE);
            if (existing.getContactStatus() == ContactStatus.NO_RESPONSE || existing.getContactStatus() == null) {
                existing.setContactStatus(ContactStatus.NOT_CONTACTED);
            }
            if (StringUtils.isBlank(existing.getDescription())) {
                existing.setDescription(inquiryContent);
            }
            customerRepository.save(existing);

            CustomerCareLogEntity careLog = CustomerCareLogEntity.builder()
                    .customerId(existing.getId())
                    .staffId(existing.getAssignedStaffId())
                    .careChannel(CareChannel.OTHER)
                    .careTime(LocalDateTime.now())
                    .summary("Khách hàng gửi thêm yêu cầu tư vấn từ " + sourceLabel)
                    .content(inquiryContent)
                    .outcome(CareOutcome.NEED_FOLLOW_UP)
                    .nextActionDate(LocalDateTime.now().plusHours(4))
                    .nextActionNote("Liên hệ phản hồi yêu cầu tư vấn mới của khách hàng từ Website")
                    .build();
            customerCareLogRepository.save(careLog);

            auditLogService.logExplicit(
                    "PUBLIC_LEAD_SUBMIT",
                    "CUSTOMER",
                    "CustomerEntity",
                    String.valueOf(existing.getId()),
                    null,
                    Map.of("phone", normalizedPhone, "source", sourceLabel, "existingCustomer", true),
                    "Khách hàng hiện hữu (" + existing.getCode() + ") gửi yêu cầu tư vấn mới từ " + sourceLabel,
                    null
            );

            return PublicCustomerLeadResponseDto.builder()
                    .id(existing.getId())
                    .code(existing.getCode())
                    .name(existing.getName())
                    .existingCustomer(true)
                    .build();
        }

        CustomerDto seedDto = CustomerDto.builder()
                .name(input.getName())
                .customerType(resolvedType)
                .representativeName(input.getRepresentativeName())
                .taxCode(input.getTaxCode())
                .phone(normalizedPhone)
                .email(input.getEmail())
                .address(input.getAddress())
                .contactStatus(ContactStatus.NOT_CONTACTED)
                .pipelineStage(CustomerPipelineStage.LEAD)
                .isPotential(Boolean.TRUE)
                .description(inquiryContent)
                .note("Nguồn đăng ký: " + sourceLabel + " — Chờ nhân viên CSKH liên hệ và bổ sung hồ sơ chi tiết.")
                .build();

        generateCodeService.generateCode(seedDto, Constants.KH, CustomerEntity.class);
        CustomerEntity newCustomer = customerMapper.toEntity(seedDto);
        applyCustomerDefaults(newCustomer, null);
        uniqueValidationService.validate(newCustomer);
        customerRepository.save(newCustomer);

        CustomerCareLogEntity initialLog = CustomerCareLogEntity.builder()
                .customerId(newCustomer.getId())
                .careChannel(CareChannel.OTHER)
                .careTime(LocalDateTime.now())
                .summary("Tiếp nhận đăng ký tư vấn mới từ " + sourceLabel)
                .content(inquiryContent)
                .outcome(CareOutcome.NEED_FOLLOW_UP)
                .nextActionDate(LocalDateTime.now().plusHours(4))
                .nextActionNote("Gọi tư vấn lần đầu, xác nhận nhu cầu và bổ sung hồ sơ khách hàng")
                .build();
        customerCareLogRepository.save(initialLog);

        auditLogService.logExplicit(
                "PUBLIC_LEAD_SUBMIT",
                "CUSTOMER",
                "CustomerEntity",
                String.valueOf(newCustomer.getId()),
                null,
                Map.of(
                        "code", newCustomer.getCode(),
                        "name", newCustomer.getName(),
                        "phone", normalizedPhone,
                        "customerType", resolvedType.name(),
                        "source", sourceLabel
                ),
                "Tiếp nhận khách hàng tiềm năng mới " + newCustomer.getCode() + " - " + newCustomer.getName() + " từ " + sourceLabel,
                null
        );

        return PublicCustomerLeadResponseDto.builder()
                .id(newCustomer.getId())
                .code(newCustomer.getCode())
                .name(newCustomer.getName())
                .existingCustomer(false)
                .build();
    }

    // ==================== PRIVATE HELPERS ====================

    private void validateCustomerClassification(CustomerDto input) {
        CustomerType type = input.getCustomerType() != null ? input.getCustomerType() : CustomerType.INDIVIDUAL;
        CustomerPipelineStage stage = input.getPipelineStage() != null ? input.getPipelineStage() : CustomerPipelineStage.LEAD;
        if (type == CustomerType.BUSINESS && isContractOrLaterStage(stage)) {
            if (StringUtils.isBlank(input.getTaxCode()) || StringUtils.isBlank(input.getRepresentativeName())) {
                throw BusinessException.exception("CST033");
            }
        }
        if (StringUtils.isNotBlank(input.getTaxCode())
                && !TAX_CODE_PATTERN.matcher(input.getTaxCode()).matches()) {
            throw BusinessException.exception("CST036");
        }
        if (StringUtils.isNotBlank(input.getIdCardNumber())
                && !ID_CARD_PATTERN.matcher(input.getIdCardNumber()).matches()) {
            throw BusinessException.exception("CST028");
        }
    }

    private boolean isContractOrLaterStage(CustomerPipelineStage stage) {
        return stage == CustomerPipelineStage.CONTRACT_SIGNED
                || stage == CustomerPipelineStage.IN_CONSTRUCTION
                || stage == CustomerPipelineStage.WARRANTY;
    }

    private String buildLeadInquirySummary(PublicCustomerLeadRequestDto input, String sourceLabel) {
        List<String> lines = new ArrayList<>();
        lines.add("[Nguồn tiếp nhận: " + sourceLabel + "]");
        if (StringUtils.isNotBlank(input.getServiceInterest())) {
            lines.add("• Dịch vụ quan tâm: " + input.getServiceInterest());
        }
        if (StringUtils.isNotBlank(input.getProjectScale())) {
            lines.add("• Quy mô / diện tích dự kiến: " + input.getProjectScale());
        }
        if (StringUtils.isNotBlank(input.getEstimatedBudget())) {
            lines.add("• Ngân sách dự kiến: " + input.getEstimatedBudget());
        }
        if (StringUtils.isNotBlank(input.getMessage())) {
            lines.add("• Yêu cầu chi tiết từ khách hàng: " + input.getMessage());
        }
        return String.join("\n", lines);
    }

    private Long resolveAssignedStaffId(CustomerDto input) {
        if (input.getAssignedStaff() != null && input.getAssignedStaff().getId() != null) {
            staffRepository.getStaffById(input.getAssignedStaff().getId());
            return input.getAssignedStaff().getId();
        }
        if (input.getAssignedStaffId() != null) {
            staffRepository.getStaffById(input.getAssignedStaffId());
            return input.getAssignedStaffId();
        }
        return null;
    }

    private Long resolveCareStaffId(CustomerCareLogDto input, Long fallbackStaffId) {
        if (input.getStaff() != null && input.getStaff().getId() != null) {
            staffRepository.getStaffById(input.getStaff().getId());
            return input.getStaff().getId();
        }
        if (input.getStaffId() != null) {
            staffRepository.getStaffById(input.getStaffId());
            return input.getStaffId();
        }
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(staffRepository::findOptionalStaffByUsername)
                .map(StaffEntity::getId)
                .orElse(fallbackStaffId);
    }

    private void applyCustomerDefaults(CustomerEntity entity, Long resolvedStaffId) {
        if (entity.getCustomerType() == null) {
            entity.setCustomerType(CustomerType.INDIVIDUAL);
        }
        if (entity.getContactStatus() == null) {
            entity.setContactStatus(ContactStatus.NOT_CONTACTED);
        }
        if (entity.getPipelineStage() == null) {
            entity.setPipelineStage(CustomerPipelineStage.LEAD);
        }
        if (entity.getIsPotential() == null) {
            entity.setIsPotential(Boolean.TRUE);
        }
        entity.setAssignedStaffId(resolvedStaffId);
    }

    private void normalizeDtoDefaults(CustomerDto dto) {
        if (dto.getCustomerType() == null) {
            dto.setCustomerType(CustomerType.INDIVIDUAL);
        }
        if (dto.getContactStatus() == null) {
            dto.setContactStatus(ContactStatus.NOT_CONTACTED);
        }
        if (dto.getPipelineStage() == null) {
            dto.setPipelineStage(CustomerPipelineStage.LEAD);
        }
    }

    private void enrichCareSummaryOnCustomerDto(CustomerDto dto,
                                                List<CustomerCareLogEntity> sortedLogs,
                                                LocalDateTime now) {
        dto.setCareLogCount(sortedLogs.size());
        if (sortedLogs.isEmpty()) {
            dto.setOverdueFollowUp(false);
            return;
        }
        CustomerCareLogEntity latest = sortedLogs.getFirst();
        dto.setLastCareTime(latest.getCareTime());
        if (latest.getNextActionDate() != null) {
            dto.setNextActionDate(latest.getNextActionDate());
            dto.setNextActionNote(latest.getNextActionNote());
            dto.setOverdueFollowUp(latest.getNextActionDate().isBefore(now));
        } else {
            dto.setOverdueFollowUp(false);
        }
    }
}
