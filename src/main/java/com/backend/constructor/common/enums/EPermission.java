package com.backend.constructor.common.enums;

import lombok.Getter;

@Getter
public enum EPermission {
    PROJECT_VIEW("Xem danh sách và chi tiết dự án", "PROJECT"),
    PROJECT_CREATE("Tạo mới dự án", "PROJECT"),
    PROJECT_UPDATE("Cập nhật thông tin dự án", "PROJECT"),
    PROJECT_DELETE("Xóa dự án", "PROJECT"),

    PROGRESS_VIEW("Xem tiến độ công trình", "PROGRESS"),
    PROGRESS_UPDATE("Cập nhật tiến độ thi công", "PROGRESS"),

    TASK_VIEW("Xem công việc", "TASK"),
    TASK_CREATE("Giao và tạo công việc", "TASK"),
    TASK_UPDATE("Cập nhật và báo cáo tiến độ công việc", "TASK"),
    TASK_DELETE("Xóa công việc", "TASK"),

    PAYMENT_VIEW("Xem hồ sơ thanh toán", "FINANCE"),
    PAYMENT_CREATE("Tạo đề nghị thanh toán", "FINANCE"),
    PAYMENT_APPROVE("Phê duyệt thanh toán và chi phí", "FINANCE"),
    FINANCE_MANAGE("Quản lý tài chính dòng tiền dự án", "FINANCE"),

    ESTIMATE_VIEW("Xem hồ sơ dự toán chi phí", "ESTIMATE"),
    ESTIMATE_CREATE("Lập dự toán công trình", "ESTIMATE"),
    ESTIMATE_UPDATE("Chỉnh sửa định mức và dự toán", "ESTIMATE"),
    ESTIMATE_APPROVE("Phê duyệt bảng dự toán", "ESTIMATE"),

    MATERIAL_VIEW("Xem vật tư và kho kết cấu thép", "MATERIAL"),
    MATERIAL_MANAGE("Quản lý định mức và xuất nhập vật tư", "MATERIAL"),

    CUSTOMER_VIEW("Xem thông tin khách hàng", "CUSTOMER"),
    CUSTOMER_MANAGE("Thêm và quản lý khách hàng", "CUSTOMER"),

    USER_VIEW("Xem danh sách tài khoản và nhân viên", "USER"),
    USER_MANAGE("Quản lý tài khoản, nhân viên và phân quyền", "USER");

    private final String description;
    private final String module;

    EPermission(String description, String module) {
        this.description = description;
        this.module = module;
    }
}
