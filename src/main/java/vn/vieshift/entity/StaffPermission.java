package vn.vieshift.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_permissions")
public class StaffPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 150)
    private String department = "Ban Vận Hành & CSKH";

    // Phân hệ được phép truy cập
    @Column(nullable = false)
    private boolean canManageUsers = true;

    @Column(nullable = false)
    private boolean canManageServices = true;

    @Column(nullable = false)
    private boolean canApproveVieker = true;

    @Column(nullable = false)
    private boolean canResolveDispute = true;

    @Column(nullable = false)
    private boolean canManageShop = true;

    @Column(nullable = false)
    private boolean canManageFinance = false;

    @Column(nullable = false)
    private boolean canManageMinigame = true;

    // Giới hạn Thêm - Sửa - Xóa
    @Column(nullable = false)
    private boolean canCreate = true;

    @Column(nullable = false)
    private boolean canEdit = true;

    @Column(nullable = false)
    private boolean canDelete = false; // Mặc định Nhân viên KHÔNG có quyền xóa dữ liệu

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public StaffPermission() {
    }

    public StaffPermission(User user) {
        this.user = user;
        this.department = "Ban Vận Hành & CSKH";
        this.canManageUsers = true;
        this.canManageServices = true;
        this.canApproveVieker = true;
        this.canResolveDispute = true;
        this.canManageShop = true;
        this.canManageFinance = false;
        this.canManageMinigame = true;
        this.canCreate = true;
        this.canEdit = true;
        this.canDelete = false;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean isCanManageUsers() {
        return canManageUsers;
    }

    public void setCanManageUsers(boolean canManageUsers) {
        this.canManageUsers = canManageUsers;
    }

    public boolean isCanManageServices() {
        return canManageServices;
    }

    public void setCanManageServices(boolean canManageServices) {
        this.canManageServices = canManageServices;
    }

    public boolean isCanApproveVieker() {
        return canApproveVieker;
    }

    public void setCanApproveVieker(boolean canApproveVieker) {
        this.canApproveVieker = canApproveVieker;
    }

    public boolean isCanResolveDispute() {
        return canResolveDispute;
    }

    public void setCanResolveDispute(boolean canResolveDispute) {
        this.canResolveDispute = canResolveDispute;
    }

    public boolean isCanManageShop() {
        return canManageShop;
    }

    public void setCanManageShop(boolean canManageShop) {
        this.canManageShop = canManageShop;
    }

    public boolean isCanManageFinance() {
        return canManageFinance;
    }

    public void setCanManageFinance(boolean canManageFinance) {
        this.canManageFinance = canManageFinance;
    }

    public boolean isCanManageMinigame() {
        return canManageMinigame;
    }

    public void setCanManageMinigame(boolean canManageMinigame) {
        this.canManageMinigame = canManageMinigame;
    }

    public boolean isCanCreate() {
        return canCreate;
    }

    public void setCanCreate(boolean canCreate) {
        this.canCreate = canCreate;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }

    public boolean isCanUsers() { return canManageUsers; }
    public boolean isCanServices() { return canManageServices; }
    public boolean isCanVieker() { return canApproveVieker; }
    public boolean isCanDispute() { return canResolveDispute; }
    public boolean isCanShop() { return canManageShop; }
    public boolean isCanFinance() { return canManageFinance; }
    public boolean isCanMinigame() { return canManageMinigame; }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
