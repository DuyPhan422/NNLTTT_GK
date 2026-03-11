# Kế hoạch Tối ưu UI — SOLID Toàn Diện

> **Trạng thái hiện tại:** Bước 1 & 2 đã hoàn thành.
> Tài liệu này là roadmap từ bước 3 đến hết.

---

## Tổng quan kiến trúc đích

```
ui/
├── common/                 ← Design System (DONE ✅)
│   ├── Theme.java
│   ├── ComponentFactory.java
│   └── TableStyler.java
│
├── components/             ← Shared layout components
│   ├── HeaderPanel.java
│   └── SidebarPanel.java
│
├── panels/
│   ├── base/               ← Abstract base classes (MỚI)
│   │   └── BaseCrudPanel.java   (DONE ✅)
│   │
│   ├── admin/              ← Panels dành riêng cho Admin/Staff
│   │   ├── StudentPanel.java         (DONE ✅)
│   │   ├── TeacherPanel.java         (DONE ✅)
│   │   ├── StaffPanel.java           (DONE ✅)
│   │   ├── CoursePanel.java          (DONE ✅)
│   │   ├── ClassPanel.java           (DONE ✅)
│   │   ├── RoomPanel.java            (DONE ✅)
│   │   ├── EnrollmentPanel.java
│   │   ├── TuitionPanel.java
│   │   ├── UserAccountPanel.java     (DONE ✅)
│   │   ├── ResultAdminPanel.java
│   │   ├── ScheduleManagerPanel.java
│   │   └── RevenueDashboardPanel.java
│   │
│   ├── teacher/            ← Panels dành riêng cho Teacher
│   │   ├── ResultTeacherPanel.java
│   │   ├── ScheduleTeacherPanel.java
│   │   └── AttendanceTeacherPanel.java
│   │
│   ├── student/            ← Panels dành riêng cho Student (sub-package đã có)
│   │   ├── StudentClassPanel.java
│   │   ├── StudentTuitionPanel.java
│   │   ├── ResultStudentPanel.java
│   │   ├── ScheduleStudentPanel.java
│   │   ├── AttendanceStudentPanel.java
│   │   └── BankTransferDialog.java
│   │
│   ├── attendance/         ← Đã có sub-package
│   │   └── AttendanceAdminPanel.java
│   │
│   └── dialogs/            ← Tất cả FormDialog (MỚI — tách khỏi panels/)
│       ├── StudentFormDialog.java
│       ├── TeacherFormDialog.java
│       ├── StaffFormDialog.java
│       ├── CourseFormDialog.java
│       ├── ClassFormDialog.java
│       ├── RoomFormDialog.java
│       ├── ScheduleFormDialog.java
│       ├── EnrollmentFormDialog.java
│       └── RescheduleDialog.java
│
├── LoginFrame.java
├── MainFrame.java
├── StaffMainFrame.java
├── TeacherMainFrame.java
├── StudentMainFrame.java
└── UI.java
```

---

## BƯỚC 3 — Dọn Design Tokens trong các Panel chưa chạm tới
**Mục tiêu:** Xoá toàn bộ `private static final Color/Font` khai báo cục bộ, thay bằng `Theme.*`

**Nguyên lý:** DRY + OCP — màu sắc có một nguồn sự thật duy nhất là `Theme.java`

| File | Vấn đề | Hành động |
|---|---|---|
| `EnrollmentPanel.java` | ~15 dòng design tokens + `createPrimaryButton()` thủ công | Xoá tokens → dùng `Theme.*` + `ComponentFactory.*` + `TableStyler.*` |
| `TuitionPanel.java` | ~14 dòng design tokens + button factory trùng | Như trên |
| `RevenueDashboardPanel.java` | ~12 dòng design tokens | Xoá tokens → dùng `Theme.*` |
| `ResultAdminPanel.java` | ~10 dòng design tokens | Như trên |
| `ResultTeacherPanel.java` | ~10 dòng design tokens | Như trên |
| `SchedulePanel.java` | ~10 dòng design tokens | Như trên |
| `ScheduleManagerPanel.java` | ~8 dòng design tokens | Như trên |
| `ScheduleTeacherPanel.java` | ~10 dòng design tokens | Như trên |
| `AttendanceAdminPanel.java` | Cần kiểm tra | Như trên |
| `AttendanceTeacherPanel.java` | Cần kiểm tra | Như trên |
| `AttendanceStudentPanel.java` | Cần kiểm tra | Như trên |

**Kết quả:** Xoá ~100 dòng trùng lặp, toàn bộ màu/font tập trung 1 chỗ.

---

## BƯỚC 4 — Dọn Design Tokens trong các FormDialog
**Mục tiêu:** Các dialog đang tự khai báo `Color`, `Font` riêng — thay bằng `Theme.*` + `ComponentFactory.*`

**Nguyên lý:** DRY — dialog là nơi trùng lặp nặng nhất vì mỗi dialog có ~8-10 dòng token

| File | Hành động |
|---|---|
| `StaffFormDialog.java` | Xoá 9 dòng Color/Font → `Theme.*`, nút Lưu/Hủy → `ComponentFactory.*` |
| `TeacherFormDialog.java` | Như trên |
| `StudentFormDialog.java` | Như trên |
| `CourseFormDialog.java` | Như trên |
| `ClassFormDialog.java` | Như trên |
| `RoomFormDialog.java` | Như trên |
| `ScheduleFormDialog.java` | Như trên |
| `EnrollmentFormDialog.java` | Như trên |
| `RescheduleDialog.java` | Như trên |

**Kết quả:** Xoá ~80 dòng trùng lặp. Mỗi dialog chỉ còn layout + validation logic.

---

## BƯỚC 5 — Tạo `BaseFormDialog<T>` Abstract Class
**Mục tiêu:** Các dialog có chung: modal=true, pack(), setLocationRelativeTo(), `isSaved()`, `lblError`, nút Lưu/Hủy → trích xuất vào lớp cha.

**Nguyên lý:** Template Method Pattern + DRY + LSP

```java
// dialogs/BaseFormDialog.java
public abstract class BaseFormDialog<T> extends JDialog {

    protected final JLabel   lblError;
    protected boolean        saved = false;

    protected BaseFormDialog(Frame owner, String title) {
        super(owner, title, true);
        this.lblError = new JLabel(" ");
        lblError.setForeground(Theme.DANGER);
        lblError.setFont(Theme.FONT_SMALL);
    }

    // Template method — lớp con implement
    protected abstract JPanel buildForm();
    protected abstract boolean validate();
    protected abstract void    commitToEntity();

    // Flow cố định — lớp con KHÔNG override
    protected final void initUI(int minWidth) {
        JPanel content = buildContentPanel(); // form + error + buttons
        setContentPane(content);
        pack();
        setMinimumSize(new Dimension(minWidth, 0));
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    protected final void doSave() {
        if (!validate()) return;
        commitToEntity();
        saved = true;
        dispose();
    }

    public boolean isSaved() { return saved; }
}
```

**Kết quả:** Mỗi FormDialog giảm từ ~180 dòng xuống ~80 dòng, chỉ còn phần form fields + validate + commit.

---

## BƯỚC 6 — Tách `SchedulePanel` thành sub-package `schedule/`
**Mục tiêu:** `ScheduleManagerPanel`, `SchedulePanel`, `ScheduleTeacherPanel`, `ScheduleFormDialog`, `RescheduleDialog` đang nằm lẫn lộn trong `panels/` → gom vào `panels/schedule/`

**Nguyên lý:** SRP — package là đơn vị tổ chức trách nhiệm, mỗi domain nằm 1 chỗ

```
panels/schedule/
├── ScheduleManagerPanel.java   ← Admin: quản lý lịch
├── SchedulePanel.java          ← Admin: lịch của 1 lớp
├── ScheduleTeacherPanel.java   ← Teacher: xem lịch dạy
├── ScheduleFormDialog.java
└── RescheduleDialog.java
```

---

## BƯỚC 7 — Di chuyển FormDialog vào `panels/dialogs/`
**Mục tiêu:** Hiện tại `panels/` có 9 FormDialog trộn lẫn với Panel — gây khó navigate

**Nguyên lý:** SRP + Package-by-Feature

```
Trước: panels/StaffFormDialog.java   (nằm lẫn với StaffPanel, CoursePanel, ...)
Sau:   panels/dialogs/StaffFormDialog.java
```

**Các bước thực hiện:**
1. Tạo thư mục `panels/dialogs/`
2. Di chuyển 9 file `*FormDialog.java` + `RescheduleDialog.java` vào đó
3. Cập nhật `package` declaration từ `com.company.ems.ui.panels` → `com.company.ems.ui.panels.dialogs`
4. Cập nhật tất cả `import` trong Panel files tham chiếu đến các dialog
5. Cập nhật `import` trong MainFrame nếu cần

---

## BƯỚC 8 — Tổ chức lại `panels/admin/` và `panels/teacher/`
**Mục tiêu:** Gom panel theo vai trò người dùng

**Nguyên lý:** Package-by-Feature — dễ tìm, dễ phân quyền, dễ xoá 1 tính năng

```
Trước: panels/StudentPanel.java, panels/TeacherPanel.java, ... (15+ files flat)
Sau:
  panels/admin/StudentPanel.java
  panels/admin/TeacherPanel.java
  panels/admin/StaffPanel.java
  ...
  panels/teacher/ResultTeacherPanel.java
  panels/teacher/ScheduleTeacherPanel.java
  ...
```

**Các bước:**
1. Tạo thư mục `panels/admin/` và `panels/teacher/`
2. Di chuyển file, cập nhật `package` declaration
3. Cập nhật tất cả `import` trong `MainFrame`, `StaffMainFrame`, `TeacherMainFrame`
4. Cập nhật `import` trong `BaseCrudPanel` nếu cần

---

## BƯỚC 9 — Di chuyển `BaseCrudPanel` vào `panels/base/`
**Mục tiêu:** Base class nên tách biệt khỏi các concrete class

**Nguyên lý:** SRP — `base/` là foundation, không phải feature

```
Trước: panels/BaseCrudPanel.java
Sau:   panels/base/BaseCrudPanel.java
       package com.company.ems.ui.panels.base;
```

**Cập nhật `extends` trong toàn bộ Panel subclass.**

---

## BƯỚC 10 — Chuẩn hóa `ComponentFactory` bổ sung `formField()`
**Mục tiêu:** Các dialog đang tự tạo `JTextField` với border/font thủ công → đưa vào factory

**Nguyên lý:** DRY + OCP

```java
// Thêm vào ComponentFactory.java
public static JTextField formField(String placeholder) {
    JTextField tf = new JTextField();
    tf.setFont(Theme.FONT_PLAIN);
    tf.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Theme.BORDER),
        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    tf.putClientProperty("JTextField.placeholderText", placeholder);
    return tf;
}

public static JLabel formLabel(String text) {
    JLabel lbl = new JLabel(text);
    lbl.setFont(Theme.FONT_SMALL_BOLD);  // thêm constant này vào Theme
    lbl.setForeground(Theme.TEXT_MUTED);
    return lbl;
}
```

---

## BƯỚC 11 — Kiểm tra và dọn `LoginFrame`, `MainFrame`, `StaffMainFrame`, `TeacherMainFrame`, `StudentMainFrame`
**Mục tiêu:** Đảm bảo các Frame không còn hardcode Color/Font

**Nguyên lý:** DRY

| File | Hành động |
|---|---|
| `LoginFrame.java` | Kiểm tra — thay Color/Font thủ công bằng `Theme.*` |
| `MainFrame.java` | Đã dùng `Theme` chưa? Nếu chưa thì cập nhật |
| `StaffMainFrame.java` | Như trên |
| `TeacherMainFrame.java` | Như trên |
| `StudentMainFrame.java` | Như trên |

---

## BƯỚC 12 — Dọn `UI.java` — tách `initLookAndFeel` ra `AppConfig`
**Mục tiêu:** `UI.java` đang làm 2 việc: khởi tạo LAF + tiện ích table → vi phạm SRP

**Nguyên lý:** SRP

```
Trước: UI.java  { initLookAndFeel() + alignColumn() + autoResizeColumns() }
Sau:
  AppConfig.java  { initLookAndFeel() }   ← cấu hình app
  UI.java         { alignColumn() + autoResizeColumns() }  ← chỉ tiện ích UI
```

---

## Tổng kết thay đổi sau khi hoàn thành

| Chỉ số | Trước | Sau |
|---|---|---|
| Dòng code trùng lặp (Color/Font) | ~300 dòng | 0 |
| Dòng code trùng lặp (buildToolbar/buildTable/...) | ~800 dòng | 0 |
| Số file trong `panels/` (flat) | 26 files | 0 (tất cả vào sub-package) |
| FormDialog dài trung bình | ~180 dòng | ~80 dòng |
| Panel CRUD dài trung bình | ~280 dòng | ~90 dòng |
| Thêm Panel CRUD mới tốn bao nhiêu dòng | ~280 dòng | ~60 dòng |

## Thứ tự ưu tiên thực hiện

```
Bước 3  → Bước 4  (dọn tokens — không đổi cấu trúc, ít rủi ro nhất)
Bước 5             (tạo BaseFormDialog — giảm mạnh code dialog)
Bước 10            (bổ sung ComponentFactory — cần có trước khi refactor dialog)
Bước 6  → Bước 7  (gom schedule + dialog — đổi package)
Bước 8  → Bước 9  (gom theo role — đổi package lớn nhất)
Bước 11 → Bước 12 (dọn frames + tách AppConfig — cuối cùng, ít rủi ro)
```

