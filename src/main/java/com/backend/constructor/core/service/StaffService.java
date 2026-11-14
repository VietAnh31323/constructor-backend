package com.backend.constructor.core.service;

import com.backend.constructor.app.api.StaffApi;
import com.backend.constructor.app.dto.account.RoleDto;
import com.backend.constructor.app.dto.staff.*;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.common.enums.ERole;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.GenerateCodeService;
import com.backend.constructor.common.service.MailService;
import com.backend.constructor.common.validator.UniqueValidationService;
import com.backend.constructor.config.languages.Translator;
import com.backend.constructor.core.domain.constant.Constants;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.domain.enums.*;
import com.backend.constructor.core.port.mapper.StaffCertificateMapper;
import com.backend.constructor.core.port.mapper.StaffMapper;
import com.backend.constructor.core.port.repository.*;
import com.backend.constructor.core.service.internal.InternalAccountStaffMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.backend.constructor.core.service.HelperService.addIfNotNull;
import static com.backend.constructor.core.service.HelperService.joinName;
import static com.backend.constructor.core.service.PasswordGenerator.generateDefaultPassword;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffService implements StaffApi {
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{9}(\\d{3})?$");

    private final StaffRepository staffRepository;
    private final StaffCertificateRepository staffCertificateRepository;
    private final StaffMapper staffMapper;
    private final StaffCertificateMapper staffCertificateMapper;
    private final InternalAccountStaffMapService internalAccountStaffMapService;
    private final AccountStaffMapRepository accountStaffMapRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final AccountRoleMapRepository accountRoleMapRepository;
    private final ProjectRepository projectRepository;
    private final ProjectProgressRepository projectProgressRepository;
    private final ProjectProgressTaskMapRepository projectProgressTaskMapRepository;
    private final ProgressRepository progressRepository;
    private final TaskRepository taskRepository;
    private final TaskStaffMapRepository taskStaffMapRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final Translator translator;
    private final AuditLogService auditLogService;
    private final GenerateCodeService generateCodeService;
    private final UniqueValidationService uniqueValidationService;

    @Override
    @Transactional
    public IdResponse create(StaffDto input) {
        input.trimData();
        validateStaffHrInfo(input);
        validateCertificatesForPosition(input.getPosition(), input.getCertificates());
        validateAccountSetupInput(input);

        generateCodeService.generateCode(input, Constants.NS, StaffEntity.class);
        input.setId(null);

        StaffEntity staffEntity = staffMapper.toEntity(input);
        if (staffEntity.getStatus() == null) {
            staffEntity.setStatus(StaffStatus.WORKING);
        }
        staffEntity.setName(joinName(input.getFirstName(), input.getLastName()));
        uniqueValidationService.validate(staffEntity);
        staffRepository.save(staffEntity);

        syncCertificates(staffEntity.getId(), input.getCertificates());

        if (input.getAssignAccountId() != null) {
            assignExistingAccountToStaff(staffEntity, input.getAssignAccountId(), input);
        } else if (input.getCreateAccount() == null || Boolean.TRUE.equals(input.getCreateAccount())) {
            createNewAccountForStaff(staffEntity, input);
            staffRepository.save(staffEntity);
        }

        return IdResponse.builder().id(staffEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(StaffDto input) {
        if (input.getId() == null) {
            throw BusinessException.exception("CST002");
        }
        input.trimData();
        validateStaffHrInfo(input);
        validateAccountSetupInput(input);

        StaffEntity staffEntity = staffRepository.getStaffById(input.getId());

        // Nếu payload không truyền certificates (null), giữ danh sách chứng chỉ hiện có để kiểm tra ràng buộc chức vụ
        List<StaffCertificateDto> effectiveCertificates = input.getCertificates();
        if (effectiveCertificates == null) {
            effectiveCertificates = getCertificates(staffEntity.getId());
        }
        validateCertificatesForPosition(input.getPosition(), effectiveCertificates);

        generateCodeService.generateCode(input, Constants.NS, StaffEntity.class);
        StaffStatus previousStatus = staffEntity.getStatus();
        staffMapper.update(input, staffEntity);
        if (staffEntity.getStatus() == null) {
            staffEntity.setStatus(previousStatus != null ? previousStatus : StaffStatus.WORKING);
        }
        staffEntity.setName(joinName(input.getFirstName(), input.getLastName()));
        uniqueValidationService.validate(staffEntity);
        staffRepository.save(staffEntity);

        if (input.getCertificates() != null) {
            syncCertificates(staffEntity.getId(), input.getCertificates());
        }

        // 2.03: Đồng bộ tài khoản liên kết hoặc tạo/gán mới ngay trên form nhân sự
        Optional<AccountEntity> linkedAccountOpt = findLinkedAccount(staffEntity.getId());
        if (linkedAccountOpt.isPresent()) {
            AccountEntity account = linkedAccountOpt.get();
            account.setFirstName(staffEntity.getFirstName());
            account.setLastName(staffEntity.getLastName());
            if (input.getMustChangePassword() != null) {
                account.setMustChangePassword(input.getMustChangePassword());
            }
            accountRepository.save(account);
            if (input.getRoles() != null) {
                syncAccountRoles(account, input.getRoles(), true);
            }
        } else {
            if (input.getAssignAccountId() != null) {
                assignExistingAccountToStaff(staffEntity, input.getAssignAccountId(), input);
            } else if (Boolean.TRUE.equals(input.getCreateAccount())) {
                createNewAccountForStaff(staffEntity, input);
                staffRepository.save(staffEntity);
            }
        }

        return IdResponse.builder().id(staffEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse delete(Long id) {
        StaffEntity staffEntity = staffRepository.getStaffById(id);
        List<StaffCertificateEntity> certificates = staffCertificateRepository.getListByStaffId(id);
        if (!certificates.isEmpty()) {
            staffCertificateRepository.deleteAll(certificates);
        }
        staffRepository.delete(staffEntity);
        deleteAccountOfStaff(id);
        return IdResponse.builder().id(staffEntity.getId()).build();
    }

    @Override
    public StaffDto getDetail(Long id) {
        StaffEntity staffEntity = staffRepository.getStaffById(id);
        StaffDto dto = staffMapper.toDto(staffEntity);
        if (dto.getStatus() == null) {
            dto.setStatus(StaffStatus.WORKING);
        }
        dto.setCertificates(getCertificates(id));
        enrichStaffDetailWithAccount(dto, id);
        return dto;
    }

    @Override
    public Page<StaffOutput> getListStaff(StaffFilterParam param, Pageable pageable) {
        Page<StaffEntity> staffEntities = staffRepository.getPageStaff(param, pageable);
        List<Long> staffIds = staffEntities.getContent().stream()
                .map(StaffEntity::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, List<StaffCertificateEntity>> certMap = staffCertificateRepository.getListByStaffIds(staffIds)
                .stream()
                .collect(Collectors.groupingBy(StaffCertificateEntity::getStaffId));

        // 2.03: Batch-load thông tin tài khoản & vai trò
        List<AccountStaffMapEntity> staffAccountMaps = accountStaffMapRepository.getListByStaffIds(staffIds);
        Map<Long, Long> accountIdByStaffId = new HashMap<>();
        Set<Long> accountIds = new HashSet<>();
        for (AccountStaffMapEntity asm : staffAccountMaps) {
            if (asm.getStaffId() != null && asm.getAccountId() != null) {
                accountIdByStaffId.putIfAbsent(asm.getStaffId(), asm.getAccountId());
                accountIds.add(asm.getAccountId());
            }
        }
        Map<Long, AccountEntity> accountMap = accountIds.isEmpty()
                ? Collections.emptyMap()
                : accountRepository.findAllByIds(accountIds).stream()
                .collect(Collectors.toMap(AccountEntity::getId, Function.identity()));
        Map<Long, List<RoleDto>> roleMapByAccountId = accountIds.isEmpty()
                ? Collections.emptyMap()
                : roleRepository.getMapRoleDtoByAccountIds(accountIds);

        // 2.04: Batch-load thống kê tải dự án & công việc
        LocalDate today = LocalDate.now();
        Map<Long, WorkloadSummaryCount> workloadMap = computeBatchWorkloadSummary(staffIds, today);

        return staffEntities.map(entity -> {
            StaffOutput output = staffMapper.toOutput(entity);
            if (output.getStatus() == null) {
                output.setStatus(StaffStatus.WORKING);
            }

            List<StaffCertificateEntity> certs = certMap.getOrDefault(entity.getId(), Collections.emptyList());
            int expiredCount = 0;
            int expiringSoonCount = 0;
            for (StaffCertificateEntity cert : certs) {
                if (cert.getExpiryDate() == null) {
                    continue;
                }
                if (cert.getExpiryDate().isBefore(today)) {
                    expiredCount++;
                } else if (!cert.getExpiryDate().isAfter(today.plusDays(30))) {
                    expiringSoonCount++;
                }
            }
            output.setCertificateCount(certs.size());
            output.setExpiredCertificateCount(expiredCount);
            output.setExpiringSoonCertificateCount(expiringSoonCount);

            Long accId = accountIdByStaffId.get(entity.getId());
            AccountEntity acc = accId != null ? accountMap.get(accId) : null;
            if (acc != null) {
                output.setHasAccount(true);
                output.setAccountId(acc.getId());
                output.setAccountUsername(acc.getUsername());
                output.setAccountStatus(acc.getStatus());
                output.setRoles(roleMapByAccountId.getOrDefault(acc.getId(), Collections.emptyList()));
            } else {
                output.setHasAccount(false);
                output.setRoles(Collections.emptyList());
            }

            WorkloadSummaryCount wl = workloadMap.get(entity.getId());
            if (wl != null) {
                output.setActiveProjectCount(wl.activeProjects);
                output.setActiveTaskCount(wl.activeTasks);
                output.setOverdueTaskCount(wl.overdueTasks);
                output.setWorkloadLevel(determineWorkloadLevel(wl.activeProjects, wl.activeTasks, wl.overdueTasks));
            } else {
                output.setActiveProjectCount(0);
                output.setActiveTaskCount(0);
                output.setOverdueTaskCount(0);
                output.setWorkloadLevel(WorkloadLevel.NORMAL);
            }

            return output;
        });
    }

    @Override
    public StaffWorkloadDto getStaffWorkload(Long staffId) {
        StaffEntity staff = staffRepository.getStaffById(staffId);
        LocalDate today = LocalDate.now();

        // 1. Tải toàn bộ công việc nhân sự tham gia (thực hiện hoặc duyệt)
        List<TaskStaffMapEntity> assigneeMaps = taskStaffMapRepository.getListByStaffId(staffId);
        Set<Long> assignedTaskIds = assigneeMaps.stream()
                .map(TaskStaffMapEntity::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<TaskEntity> assignedTasks = assignedTaskIds.isEmpty()
                ? Collections.emptyList()
                : taskRepository.findAllByIds(assignedTaskIds);
        List<TaskEntity> reviewedTasks = taskRepository.getListByReviewerId(staffId);

        Map<Long, TaskEntity> taskById = new LinkedHashMap<>();
        Map<Long, Set<String>> rolesByTaskId = new HashMap<>();

        for (TaskEntity t : assignedTasks) {
            taskById.put(t.getId(), t);
            rolesByTaskId.computeIfAbsent(t.getId(), k -> new LinkedHashSet<>()).add("ASSIGNEE");
        }
        for (TaskEntity t : reviewedTasks) {
            taskById.putIfAbsent(t.getId(), t);
            rolesByTaskId.computeIfAbsent(t.getId(), k -> new LinkedHashSet<>()).add("REVIEWER");
        }

        // 2. Nạp công việc cha (nếu là việc con) để suy ra dự án & hạng mục tiến độ
        Set<Long> parentTaskIds = taskById.values().stream()
                .map(TaskEntity::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, TaskEntity> parentTaskMap = parentTaskIds.isEmpty()
                ? Collections.emptyMap()
                : taskRepository.getTaskMapByIds(parentTaskIds);

        Set<Long> lookupTaskIdsForProgress = new HashSet<>(taskById.keySet());
        lookupTaskIdsForProgress.addAll(parentTaskIds);

        List<ProjectProgressTaskMapEntity> progressTaskMaps = lookupTaskIdsForProgress.isEmpty()
                ? Collections.emptyList()
                : projectProgressTaskMapRepository.getListByTaskIds(lookupTaskIdsForProgress);

        Map<Long, Long> projectProgressIdByTaskId = new HashMap<>();
        Set<Long> projectProgressIds = new HashSet<>();
        for (ProjectProgressTaskMapEntity pptm : progressTaskMaps) {
            if (pptm.getTaskId() != null && pptm.getProjectProgressId() != null) {
                projectProgressIdByTaskId.putIfAbsent(pptm.getTaskId(), pptm.getProjectProgressId());
                projectProgressIds.add(pptm.getProjectProgressId());
            }
        }

        Map<Long, ProjectProgressEntity> projectProgressMap = projectProgressIds.isEmpty()
                ? Collections.emptyMap()
                : projectProgressRepository.findAllByIds(projectProgressIds).stream()
                .collect(Collectors.toMap(ProjectProgressEntity::getId, Function.identity()));

        Set<Long> progressIds = projectProgressMap.values().stream()
                .map(ProjectProgressEntity::getProgressId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CodeNameResponse> progressInfoMap = progressIds.isEmpty()
                ? Collections.emptyMap()
                : progressRepository.getMapProgressSimpleByIds(progressIds);

        // 3. Tải tất cả dự án liên quan (trực tiếp làm Manager/Creator/Supporter hoặc gián tiếp qua Task)
        List<ProjectEntity> directProjects = projectRepository.getListByParticipantStaffId(staffId);
        Map<Long, ProjectEntity> projectById = new LinkedHashMap<>();
        for (ProjectEntity p : directProjects) {
            projectById.put(p.getId(), p);
        }

        Set<Long> taskLinkedProjectIds = projectProgressMap.values().stream()
                .map(ProjectProgressEntity::getProjectId)
                .filter(pid -> pid != null && !projectById.containsKey(pid))
                .collect(Collectors.toSet());
        if (!taskLinkedProjectIds.isEmpty()) {
            for (ProjectEntity p : projectRepository.findAllByIds(taskLinkedProjectIds)) {
                projectById.putIfAbsent(p.getId(), p);
            }
        }

        // 4. Xây dựng danh sách Task DTO và gom số lượng task theo từng dự án
        Map<Long, Integer> totalTaskCountByProject = new HashMap<>();
        Map<Long, Integer> activeTaskCountByProject = new HashMap<>();

        int notStartedTasks = 0;
        int inProgressTasks = 0;
        int completedTasks = 0;
        int overdueTasks = 0;
        int dueSoonTasks = 0;
        int highPriorityActiveTasks = 0;

        List<StaffTaskItemDto> taskItems = new ArrayList<>();
        for (TaskEntity task : taskById.values()) {
            TaskState state = task.getState() != null ? task.getState() : TaskState.NOT_STARTED;
            boolean isCompleted = state == TaskState.COMPLETED;
            if (state == TaskState.COMPLETED) {
                completedTasks++;
            } else if (state == TaskState.IN_PROGRESS) {
                inProgressTasks++;
            } else {
                notStartedTasks++;
            }

            boolean isOverdue = !isCompleted && task.getEndDate() != null && task.getEndDate().isBefore(today);
            boolean isDueSoon = !isCompleted && task.getEndDate() != null
                    && !task.getEndDate().isBefore(today)
                    && !task.getEndDate().isAfter(today.plusDays(7));

            if (isOverdue) {
                overdueTasks++;
            }
            if (isDueSoon) {
                dueSoonTasks++;
            }
            if (!isCompleted && task.getPriorityLevel() == PriorityLevel.HIGH) {
                highPriorityActiveTasks++;
            }

            Long ppId = projectProgressIdByTaskId.get(task.getId());
            if (ppId == null && task.getParentId() != null) {
                ppId = projectProgressIdByTaskId.get(task.getParentId());
            }
            ProjectProgressEntity pp = ppId != null ? projectProgressMap.get(ppId) : null;
            ProjectEntity proj = (pp != null && pp.getProjectId() != null) ? projectById.get(pp.getProjectId()) : null;
            CodeNameResponse prog = (pp != null && pp.getProgressId() != null) ? progressInfoMap.get(pp.getProgressId()) : null;
            TaskEntity parentTask = task.getParentId() != null ? parentTaskMap.get(task.getParentId()) : null;

            if (proj != null) {
                totalTaskCountByProject.merge(proj.getId(), 1, Integer::sum);
                if (!isCompleted) {
                    activeTaskCountByProject.merge(proj.getId(), 1, Integer::sum);
                }
            }

            taskItems.add(StaffTaskItemDto.builder()
                    .id(task.getId())
                    .code(task.getCode())
                    .name(task.getName())
                    .parentId(task.getParentId())
                    .parentTaskCode(parentTask != null ? parentTask.getCode() : null)
                    .parentTaskName(parentTask != null ? parentTask.getName() : null)
                    .projectId(proj != null ? proj.getId() : null)
                    .projectCode(proj != null ? proj.getCode() : null)
                    .projectName(proj != null ? proj.getName() : null)
                    .progressId(prog != null ? prog.getId() : null)
                    .progressName(prog != null ? prog.getName() : null)
                    .rolesInTask(new ArrayList<>(rolesByTaskId.getOrDefault(task.getId(), Collections.emptySet())))
                    .priorityLevel(task.getPriorityLevel())
                    .state(state)
                    .progressPercent(task.getProgressPercent() != null ? task.getProgressPercent() : BigDecimal.ZERO)
                    .startDate(task.getStartDate())
                    .endDate(task.getEndDate())
                    .overdue(isOverdue)
                    .dueSoon(isDueSoon)
                    .build());
        }

        taskItems.sort(Comparator
                .comparing((StaffTaskItemDto t) -> t.getState() == TaskState.COMPLETED ? 1 : 0)
                .thenComparing(t -> t.isOverdue() ? 0 : 1)
                .thenComparing(StaffTaskItemDto::getEndDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StaffTaskItemDto::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        // 5. Xây dựng danh sách Dự án tham gia
        int activeProjects = 0;
        int managedActiveProjects = 0;
        int completedProjects = 0;

        List<StaffProjectItemDto> projectItems = new ArrayList<>();
        for (ProjectEntity proj : projectById.values()) {
            ProjectState pState = proj.getState() != null ? proj.getState() : ProjectState.NOT_STARTED;
            boolean pCompleted = pState == ProjectState.COMPLETED;
            if (pCompleted) {
                completedProjects++;
            } else {
                activeProjects++;
                if (Objects.equals(proj.getManagerId(), staffId)) {
                    managedActiveProjects++;
                }
            }

            List<String> pRoles = new ArrayList<>();
            if (Objects.equals(proj.getManagerId(), staffId)) {
                pRoles.add("MANAGER");
            }
            if (Objects.equals(proj.getCreatorId(), staffId)) {
                pRoles.add("CREATOR");
            }
            if (Objects.equals(proj.getSupporterId(), staffId)) {
                pRoles.add("SUPPORTER");
            }
            int staffTasksInProj = totalTaskCountByProject.getOrDefault(proj.getId(), 0);
            if (staffTasksInProj > 0) {
                pRoles.add("TASK_MEMBER");
            }

            boolean pOverdue = !pCompleted
                    && proj.getDeliveryDate() != null
                    && proj.getDeliveryDate().isBefore(today);

            projectItems.add(StaffProjectItemDto.builder()
                    .id(proj.getId())
                    .code(proj.getCode())
                    .name(proj.getName())
                    .owner(proj.getOwner())
                    .address(proj.getAddress())
                    .state(pState)
                    .progressPercent(proj.getProgressPercent() != null ? proj.getProgressPercent() : BigDecimal.ZERO)
                    .signDate(proj.getSignDate())
                    .deliveryDate(proj.getDeliveryDate())
                    .overdue(pOverdue)
                    .rolesInProject(pRoles)
                    .staffTaskCount(staffTasksInProj)
                    .staffActiveTaskCount(activeTaskCountByProject.getOrDefault(proj.getId(), 0))
                    .build());
        }

        projectItems.sort(Comparator
                .comparing((StaffProjectItemDto p) -> p.getState() == ProjectState.COMPLETED ? 1 : 0)
                .thenComparing(p -> p.isOverdue() ? 0 : 1)
                .thenComparing(StaffProjectItemDto::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        int activeTasks = notStartedTasks + inProgressTasks;
        WorkloadLevel level = determineWorkloadLevel(activeProjects, activeTasks, overdueTasks);

        return StaffWorkloadDto.builder()
                .staffId(staff.getId())
                .staffCode(staff.getCode())
                .staffName(staff.getName())
                .position(staff.getPosition())
                .department(staff.getDepartment())
                .totalProjects(projectItems.size())
                .activeProjects(activeProjects)
                .managedActiveProjects(managedActiveProjects)
                .completedProjects(completedProjects)
                .totalTasks(taskItems.size())
                .notStartedTasks(notStartedTasks)
                .inProgressTasks(inProgressTasks)
                .completedTasks(completedTasks)
                .activeTasks(activeTasks)
                .overdueTasks(overdueTasks)
                .dueSoonTasks(dueSoonTasks)
                .highPriorityActiveTasks(highPriorityActiveTasks)
                .workloadLevel(level)
                .projects(projectItems)
                .tasks(taskItems)
                .build();
    }

    @Override
    public List<StaffCertificateDto> getCertificates(Long staffId) {
        LocalDate today = LocalDate.now();
        return staffCertificateRepository.getListByStaffId(staffId).stream()
                .map(entity -> {
                    StaffCertificateDto dto = staffCertificateMapper.toDto(entity);
                    dto.computeValidity(today);
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional
    public IdResponse addCertificate(Long staffId, StaffCertificateDto input) {
        staffRepository.getStaffById(staffId);
        input.trimData();
        validateSingleCertificate(input);

        StaffCertificateEntity entity = staffCertificateMapper.toEntity(input);
        entity.setStaffId(staffId);
        uniqueValidationService.validate(entity);
        staffCertificateRepository.save(entity);
        return IdResponse.builder().id(entity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse updateCertificate(Long staffId, Long certId, StaffCertificateDto input) {
        StaffEntity staff = staffRepository.getStaffById(staffId);
        StaffCertificateEntity entity = staffCertificateRepository.getCertificateById(certId);
        if (!Objects.equals(entity.getStaffId(), staffId)) {
            throw BusinessException.exception("CST002");
        }
        input.trimData();
        validateSingleCertificate(input);

        staffCertificateMapper.update(input, entity);
        uniqueValidationService.validate(entity);
        staffCertificateRepository.save(entity);

        if (staff.getPosition() != null && staff.getPosition().isRequiresCertificate()) {
            validateCertificatesForPosition(staff.getPosition(), getCertificates(staffId));
        }

        return IdResponse.builder().id(entity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse deleteCertificate(Long staffId, Long certId) {
        StaffEntity staff = staffRepository.getStaffById(staffId);
        StaffCertificateEntity entity = staffCertificateRepository.getCertificateById(certId);
        if (!Objects.equals(entity.getStaffId(), staffId)) {
            throw BusinessException.exception("CST002");
        }

        if (staff.getPosition() != null && staff.getPosition().isRequiresCertificate()) {
            List<StaffCertificateDto> remaining = getCertificates(staffId).stream()
                    .filter(c -> !Objects.equals(c.getId(), certId))
                    .toList();
            validateCertificatesForPosition(staff.getPosition(), remaining);
        }

        staffCertificateRepository.delete(entity);
        return IdResponse.builder().id(entity.getId()).build();
    }

    // ==================== PRIVATE HELPERS ====================

    private void validateStaffHrInfo(StaffDto input) {
        if (input.getPosition() == null) {
            throw BusinessException.exception("CST006");
        }
        if (StringUtils.isNotBlank(input.getIdCardNumber())
                && !ID_CARD_PATTERN.matcher(input.getIdCardNumber()).matches()) {
            throw BusinessException.exception("CST028");
        }
    }

    private void validateAccountSetupInput(StaffDto input) {
        if (StringUtils.isNotBlank(input.getInitialPassword()) && input.getInitialPassword().length() < 6) {
            throw BusinessException.exception("CST032");
        }
    }

    private void validateSingleCertificate(StaffCertificateDto cert) {
        if (cert.getCertificateType() == null
                || StringUtils.isBlank(cert.getCertificateNumber())
                || cert.getIssueDate() == null
                || cert.getExpiryDate() == null) {
            throw BusinessException.exception("ERROR_0001");
        }
        if (cert.getExpiryDate().isBefore(cert.getIssueDate())) {
            throw BusinessException.exception("CST026");
        }
    }

    private void validateCertificatesForPosition(Position position, List<StaffCertificateDto> certificates) {
        List<StaffCertificateDto> certList = certificates != null ? certificates : Collections.emptyList();

        Set<String> seenNumbers = new HashSet<>();
        for (StaffCertificateDto cert : certList) {
            validateSingleCertificate(cert);
            String normalizedNo = cert.getCertificateNumber().trim().toLowerCase(Locale.ROOT);
            if (!seenNumbers.add(normalizedNo)) {
                throw BusinessException.exception("CST029");
            }
        }

        if (position != null && position.isRequiresCertificate()) {
            if (certList.isEmpty()) {
                throw BusinessException.exception("CST025");
            }
            LocalDate today = LocalDate.now();
            boolean hasValidCert = certList.stream()
                    .anyMatch(c -> c.getExpiryDate() != null && !c.getExpiryDate().isBefore(today));
            if (!hasValidCert) {
                throw BusinessException.exception("CST027");
            }
        }
    }

    private void syncCertificates(Long staffId, List<StaffCertificateDto> incomingDtos) {
        List<StaffCertificateDto> dtos = incomingDtos != null ? incomingDtos : Collections.emptyList();
        List<StaffCertificateEntity> existingList = staffCertificateRepository.getListByStaffId(staffId);
        Map<Long, StaffCertificateEntity> existingById = existingList.stream()
                .collect(Collectors.toMap(StaffCertificateEntity::getId, c -> c));

        Set<Long> retainedIds = dtos.stream()
                .map(StaffCertificateDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<StaffCertificateEntity> toDelete = existingList.stream()
                .filter(e -> !retainedIds.contains(e.getId()))
                .toList();
        if (!toDelete.isEmpty()) {
            staffCertificateRepository.deleteAll(toDelete);
            staffCertificateRepository.flush();
        }

        for (StaffCertificateDto dto : dtos) {
            StaffCertificateEntity entity;
            if (dto.getId() != null && existingById.containsKey(dto.getId())) {
                entity = existingById.get(dto.getId());
                staffCertificateMapper.update(dto, entity);
            } else {
                entity = staffCertificateMapper.toEntity(dto);
                entity.setStaffId(staffId);
            }
            uniqueValidationService.validate(entity);
            staffCertificateRepository.save(entity);
        }
    }

    private Optional<AccountEntity> findLinkedAccount(Long staffId) {
        List<AccountStaffMapEntity> maps = accountStaffMapRepository.getListByStaffId(staffId);
        if (maps.isEmpty()) {
            return Optional.empty();
        }
        Long accId = maps.getFirst().getAccountId();
        if (accId == null) {
            return Optional.empty();
        }
        List<AccountEntity> accounts = accountRepository.findAllByIds(Set.of(accId));
        return accounts.isEmpty() ? Optional.empty() : Optional.of(accounts.getFirst());
    }

    private void enrichStaffDetailWithAccount(StaffDto dto, Long staffId) {
        Optional<AccountEntity> accOpt = findLinkedAccount(staffId);
        if (accOpt.isEmpty()) {
            dto.setHasAccount(false);
            dto.setRoles(Collections.emptyList());
            return;
        }
        AccountEntity acc = accOpt.get();
        Map<Long, List<RoleDto>> roleMap = roleRepository.getMapRoleDtoByAccountIds(Set.of(acc.getId()));
        dto.setHasAccount(true);
        dto.setAccountId(acc.getId());
        dto.setAccountUsername(acc.getUsername());
        dto.setAccountStatus(acc.getStatus());
        dto.setMustChangePassword(Boolean.TRUE.equals(acc.getMustChangePassword()));
        dto.setRoles(roleMap.getOrDefault(acc.getId(), Collections.emptyList()));
    }

    private void createNewAccountForStaff(StaffEntity staffEntity, StaffDto input) {
        if (StringUtils.isBlank(staffEntity.getEmail())) {
            throw BusinessException.exception("ERROR_0001");
        }
        if (accountRepository.existsByUsername(staffEntity.getEmail())) {
            throw new BusinessException(
                    String.valueOf(HttpStatus.BAD_REQUEST.value()),
                    translator.toLocale("CST014", staffEntity.getEmail())
            );
        }

        String rawPassword = StringUtils.isNotBlank(input.getInitialPassword())
                ? input.getInitialPassword()
                : generateDefaultPassword(8);
        boolean mustChange = input.getMustChangePassword() == null || Boolean.TRUE.equals(input.getMustChangePassword());

        staffEntity.setGenPassword(rawPassword);

        AccountEntity account = AccountEntity.builder()
                .username(staffEntity.getEmail())
                .password(passwordEncoder.encode(rawPassword))
                .firstName(staffEntity.getFirstName())
                .lastName(staffEntity.getLastName())
                .status(AccountStatus.ACTIVE)
                .mustChangePassword(mustChange)
                .failedLoginAttempts(0)
                .build();
        accountRepository.save(account);

        internalAccountStaffMapService.createAccountMap(staffEntity.getId(), account.getId());

        List<RoleEntity> resolvedRoles = resolveEffectiveRoles(staffEntity.getPosition(), input.getRoles());
        List<AccountRoleMapEntity> roleMaps = resolvedRoles.stream()
                .map(r -> AccountRoleMapEntity.builder()
                        .accountId(account.getId())
                        .roleId(r.getId())
                        .build())
                .toList();
        if (!roleMaps.isEmpty()) {
            accountRoleMapRepository.saveAll(roleMaps);
        }

        String primaryRoleLabel = (!resolvedRoles.isEmpty() && resolvedRoles.getFirst().getName() != null)
                ? resolvedRoles.getFirst().getName().getValue()
                : ERole.STAFF.getValue();
        mailService.sendPasswordEmail(account.getUsername(), rawPassword, primaryRoleLabel);

        List<String> roleNames = resolvedRoles.stream()
                .map(r -> r.getName() != null ? r.getName().name() : "")
                .toList();
        auditLogService.logExplicit(
                "CREATE_STAFF_ACCOUNT",
                "STAFF",
                "AccountEntity",
                String.valueOf(account.getId()),
                null,
                Map.of("staffId", staffEntity.getId(), "username", account.getUsername(), "roles", roleNames),
                "Tạo tài khoản đăng nhập " + account.getUsername() + " cho nhân sự " + staffEntity.getName(),
                null
        );
    }

    private void assignExistingAccountToStaff(StaffEntity staffEntity, Long accountId, StaffDto input) {
        if (!accountStaffMapRepository.getListByStaffId(staffEntity.getId()).isEmpty()) {
            throw BusinessException.exception("CST031");
        }
        Optional<AccountStaffMapEntity> existingByAccount = accountStaffMapRepository.findOptionalByAccountId(accountId);
        if (existingByAccount.isPresent()) {
            throw BusinessException.exception("CST030");
        }

        AccountEntity account = accountRepository.getAccountById(accountId);
        if (StringUtils.isBlank(account.getFirstName())) {
            account.setFirstName(staffEntity.getFirstName());
        }
        if (StringUtils.isBlank(account.getLastName())) {
            account.setLastName(staffEntity.getLastName());
        }
        if (input.getMustChangePassword() != null) {
            account.setMustChangePassword(input.getMustChangePassword());
        }
        accountRepository.save(account);

        internalAccountStaffMapService.createAccountMap(staffEntity.getId(), account.getId());

        if (input.getRoles() != null && !input.getRoles().isEmpty()) {
            syncAccountRoles(account, input.getRoles(), false);
        }

        auditLogService.logExplicit(
                "ASSIGN_ACCOUNT_TO_STAFF",
                "STAFF",
                "AccountEntity",
                String.valueOf(account.getId()),
                null,
                Map.of("staffId", staffEntity.getId(), "username", account.getUsername()),
                "Gán tài khoản có sẵn " + account.getUsername() + " cho nhân sự " + staffEntity.getName(),
                null
        );
    }

    private void syncAccountRoles(AccountEntity account, List<RoleDto> requestedRoles, boolean logAudit) {
        List<RoleEntity> oldRoles = roleRepository.getListRoleByAccountId(account.getId());
        List<String> oldRoleNames = oldRoles.stream()
                .map(r -> r.getName() != null ? r.getName().name() : "")
                .sorted()
                .toList();

        List<AccountRoleMapEntity> existingMaps = accountRoleMapRepository.getListByAccountId(account.getId());
        if (!existingMaps.isEmpty()) {
            accountRoleMapRepository.deleteAllInBatch(existingMaps);
        }

        List<RoleEntity> resolvedRoles = resolveRequestedRoles(requestedRoles);
        List<AccountRoleMapEntity> newMaps = resolvedRoles.stream()
                .map(r -> AccountRoleMapEntity.builder()
                        .accountId(account.getId())
                        .roleId(r.getId())
                        .build())
                .toList();
        if (!newMaps.isEmpty()) {
            accountRoleMapRepository.saveAll(newMaps);
        }

        List<String> newRoleNames = resolvedRoles.stream()
                .map(r -> r.getName() != null ? r.getName().name() : "")
                .sorted()
                .toList();

        if (logAudit && !Objects.equals(oldRoleNames, newRoleNames)) {
            auditLogService.logExplicit(
                    "UPDATE_ROLE",
                    "ACCOUNT",
                    "AccountEntity",
                    String.valueOf(account.getId()),
                    Map.of("roles", oldRoleNames),
                    Map.of("roles", newRoleNames),
                    "Cập nhật phân quyền tài khoản " + account.getUsername() + " từ hồ sơ nhân sự",
                    null
            );
        }
    }

    private List<RoleEntity> resolveRequestedRoles(List<RoleDto> requestedRoles) {
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, RoleEntity> allById = roleRepository.getAllRoles().stream()
                .collect(Collectors.toMap(RoleEntity::getId, Function.identity()));
        Map<ERole, RoleEntity> allByName = allById.values().stream()
                .filter(r -> r.getName() != null)
                .collect(Collectors.toMap(RoleEntity::getName, Function.identity(), (a, b) -> a));

        LinkedHashMap<Long, RoleEntity> resolved = new LinkedHashMap<>();
        for (RoleDto dto : requestedRoles) {
            if (dto == null) continue;
            RoleEntity entity = null;
            if (dto.getId() != null) {
                entity = allById.get(dto.getId());
            }
            if (entity == null && dto.getName() != null) {
                entity = allByName.get(dto.getName());
            }
            if (entity != null) {
                resolved.put(entity.getId(), entity);
            }
        }
        return new ArrayList<>(resolved.values());
    }

    private List<RoleEntity> resolveEffectiveRoles(Position position, List<RoleDto> requestedRoles) {
        List<RoleEntity> explicit = resolveRequestedRoles(requestedRoles);
        if (!explicit.isEmpty()) {
            return explicit;
        }
        ERole defaultRole = mapPositionToDefaultRole(position);
        Optional<RoleEntity> byPosition = roleRepository.getByName(defaultRole);
        if (byPosition.isPresent()) {
            return List.of(byPosition.get());
        }
        return roleRepository.getByName(ERole.STAFF).map(List::of).orElse(Collections.emptyList());
    }

    private ERole mapPositionToDefaultRole(Position position) {
        if (position == null) {
            return ERole.STAFF;
        }
        return switch (position) {
            case PROJECT_MANAGER -> ERole.PROJECT_MANAGER;
            case SITE_MANAGER, SUPERVISOR -> ERole.SITE_ENGINEER;
            case ESTIMATOR -> ERole.ESTIMATOR;
            case ACCOUNTANT -> ERole.ACCOUNTANT;
            case WAREHOUSE_KEEPER -> ERole.WAREHOUSE;
            default -> ERole.STAFF;
        };
    }

    private void deleteAccountOfStaff(Long id) {
        List<AccountStaffMapEntity> accountStaffMapEntities = accountStaffMapRepository.getListByStaffId(id);
        Set<Long> accountIds = new HashSet<>();
        for (AccountStaffMapEntity accountStaffMapEntity : accountStaffMapEntities) {
            addIfNotNull(accountIds, accountStaffMapEntity.getAccountId());
        }
        List<AccountEntity> accountEntities = accountIds.isEmpty()
                ? Collections.emptyList()
                : accountRepository.findAllByIds(accountIds);
        if (!accountStaffMapEntities.isEmpty()) {
            accountStaffMapRepository.deleteAll(accountStaffMapEntities);
        }
        if (!accountEntities.isEmpty()) {
            accountRepository.deleteAll(accountEntities);
        }
    }

    private Map<Long, WorkloadSummaryCount> computeBatchWorkloadSummary(List<Long> staffIds, LocalDate today) {
        if (staffIds == null || staffIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> staffIdSet = new HashSet<>(staffIds);
        Map<Long, Set<Long>> activeProjectsByStaff = new HashMap<>();
        Map<Long, Set<Long>> activeTasksByStaff = new HashMap<>();
        Map<Long, Set<Long>> overdueTasksByStaff = new HashMap<>();

        // 1. Dự án tham gia trực tiếp
        List<ProjectEntity> directProjects = projectRepository.getListByParticipantStaffIds(staffIdSet);
        for (ProjectEntity p : directProjects) {
            if (p.getState() == ProjectState.COMPLETED) {
                continue;
            }
            if (p.getManagerId() != null && staffIdSet.contains(p.getManagerId())) {
                activeProjectsByStaff.computeIfAbsent(p.getManagerId(), k -> new HashSet<>()).add(p.getId());
            }
            if (p.getCreatorId() != null && staffIdSet.contains(p.getCreatorId())) {
                activeProjectsByStaff.computeIfAbsent(p.getCreatorId(), k -> new HashSet<>()).add(p.getId());
            }
            if (p.getSupporterId() != null && staffIdSet.contains(p.getSupporterId())) {
                activeProjectsByStaff.computeIfAbsent(p.getSupporterId(), k -> new HashSet<>()).add(p.getId());
            }
        }

        // 2. Công việc được giao & duyệt
        List<TaskStaffMapEntity> staffTaskMaps = taskStaffMapRepository.getListByStaffIds(staffIdSet);
        Set<Long> assignedTaskIds = staffTaskMaps.stream()
                .map(TaskStaffMapEntity::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, TaskEntity> assignedTaskMap = assignedTaskIds.isEmpty()
                ? Collections.emptyMap()
                : taskRepository.getTaskMapByIds(assignedTaskIds);

        List<TaskEntity> reviewedTasks = taskRepository.getListByReviewerIds(staffIdSet);

        Map<Long, Set<Long>> staffIdsByTaskId = new HashMap<>();
        Map<Long, TaskEntity> allRelevantTasks = new HashMap<>(assignedTaskMap);

        for (TaskStaffMapEntity tsm : staffTaskMaps) {
            if (tsm.getStaffId() == null || tsm.getTaskId() == null) continue;
            TaskEntity task = assignedTaskMap.get(tsm.getTaskId());
            if (task == null) continue;
            staffIdsByTaskId.computeIfAbsent(task.getId(), k -> new HashSet<>()).add(tsm.getStaffId());
            if (task.getState() != TaskState.COMPLETED) {
                activeTasksByStaff.computeIfAbsent(tsm.getStaffId(), k -> new HashSet<>()).add(task.getId());
                if (task.getEndDate() != null && task.getEndDate().isBefore(today)) {
                    overdueTasksByStaff.computeIfAbsent(tsm.getStaffId(), k -> new HashSet<>()).add(task.getId());
                }
            }
        }

        for (TaskEntity task : reviewedTasks) {
            if (task.getReviewerId() == null || !staffIdSet.contains(task.getReviewerId())) continue;
            allRelevantTasks.putIfAbsent(task.getId(), task);
            staffIdsByTaskId.computeIfAbsent(task.getId(), k -> new HashSet<>()).add(task.getReviewerId());
            if (task.getState() != TaskState.COMPLETED) {
                activeTasksByStaff.computeIfAbsent(task.getReviewerId(), k -> new HashSet<>()).add(task.getId());
                if (task.getEndDate() != null && task.getEndDate().isBefore(today)) {
                    overdueTasksByStaff.computeIfAbsent(task.getReviewerId(), k -> new HashSet<>()).add(task.getId());
                }
            }
        }

        // 3. Ánh xạ dự án gián tiếp từ công việc
        if (!allRelevantTasks.isEmpty()) {
            Set<Long> lookupIds = new HashSet<>(allRelevantTasks.keySet());
            for (TaskEntity t : allRelevantTasks.values()) {
                if (t.getParentId() != null) {
                    lookupIds.add(t.getParentId());
                }
            }
            List<ProjectProgressTaskMapEntity> pptms = projectProgressTaskMapRepository.getListByTaskIds(lookupIds);
            Map<Long, Long> ppIdByTask = new HashMap<>();
            Set<Long> ppIds = new HashSet<>();
            for (ProjectProgressTaskMapEntity m : pptms) {
                if (m.getTaskId() != null && m.getProjectProgressId() != null) {
                    ppIdByTask.putIfAbsent(m.getTaskId(), m.getProjectProgressId());
                    ppIds.add(m.getProjectProgressId());
                }
            }
            Map<Long, ProjectProgressEntity> ppMap = ppIds.isEmpty()
                    ? Collections.emptyMap()
                    : projectProgressRepository.findAllByIds(ppIds).stream()
                    .collect(Collectors.toMap(ProjectProgressEntity::getId, Function.identity()));
            Set<Long> projIds = ppMap.values().stream()
                    .map(ProjectProgressEntity::getProjectId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, ProjectEntity> projMap = projIds.isEmpty()
                    ? Collections.emptyMap()
                    : projectRepository.findAllByIds(projIds).stream()
                    .collect(Collectors.toMap(ProjectEntity::getId, Function.identity()));

            for (TaskEntity task : allRelevantTasks.values()) {
                Long ppId = ppIdByTask.get(task.getId());
                if (ppId == null && task.getParentId() != null) {
                    ppId = ppIdByTask.get(task.getParentId());
                }
                ProjectProgressEntity pp = ppId != null ? ppMap.get(ppId) : null;
                ProjectEntity proj = (pp != null && pp.getProjectId() != null) ? projMap.get(pp.getProjectId()) : null;
                if (proj != null && proj.getState() != ProjectState.COMPLETED) {
                    for (Long sId : staffIdsByTaskId.getOrDefault(task.getId(), Collections.emptySet())) {
                        activeProjectsByStaff.computeIfAbsent(sId, k -> new HashSet<>()).add(proj.getId());
                    }
                }
            }
        }

        Map<Long, WorkloadSummaryCount> result = new HashMap<>();
        for (Long sId : staffIdSet) {
            int ap = activeProjectsByStaff.getOrDefault(sId, Collections.emptySet()).size();
            int at = activeTasksByStaff.getOrDefault(sId, Collections.emptySet()).size();
            int ot = overdueTasksByStaff.getOrDefault(sId, Collections.emptySet()).size();
            result.put(sId, new WorkloadSummaryCount(ap, at, ot));
        }
        return result;
    }

    private WorkloadLevel determineWorkloadLevel(int activeProjects, int activeTasks, int overdueTasks) {
        if (activeProjects >= 5 || activeTasks > 8 || overdueTasks >= 3) {
            return WorkloadLevel.OVERLOADED;
        }
        if (activeProjects >= 3 || activeTasks >= 6 || overdueTasks >= 1) {
            return WorkloadLevel.HIGH;
        }
        return WorkloadLevel.NORMAL;
    }

    private record WorkloadSummaryCount(int activeProjects, int activeTasks, int overdueTasks) {
    }
}
