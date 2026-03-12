# Phân tích: Giảm Trùng Lặp & Tối Ưu Code giữa `StudentPanel` và `TeacherPanel`

---

## 1. Bức Tranh Tổng Quát — Hai File Đang Trùng Lặp Gì?

Khi đặt `StudentPanel.java` và `TeacherPanel.java` cạnh nhau, ta thấy **~85% code là giống hệt nhau**, chỉ khác tên entity và vài chi tiết nhỏ:

| Phần code | StudentPanel | TeacherPanel | Có trùng? |
|---|---|---|---|
| Khai báo fields (`tableModel`, `table`, `statusLabel`, `searchField`, `sorter`, `onDataChanged`) | ✅ | ✅ | **Giống 100%** |
| Constructor (layout, border, add components, `loadData()`) | ✅ | ✅ | **Giống 100%** |
| `buildToolbar()` | ✅ | ✅ | **Giống ~95%** |
| `buildTableCard()` | ✅ | ✅ | **Giống 100%** |
| `buildActionBar()` (Sửa/Xóa buttons) | ✅ | ✅ | **Giống 100%** |
| `buildTableModel()` | ✅ | ✅ | **Giống 100%** |
| `buildTable()` (applyDefaults, hideColumn, sorter, double-click) | ✅ | ✅ | **Giống ~90%** |
| `editSelected()` / `deleteSelected()` | ✅ | ✅ | **Giống ~90%** |
| `notifyChanged()` | ✅ | ✅ | **Giống 100%** |
| `showSuccess/Warning/Error()` | ✅ | ✅ | **Giống 100%** |

---

## 2. Giải Pháp — Tạo Lớp Cha Abstract `BaseCrudPanel<T>`

Thay vì để mỗi Panel tự viết lại toàn bộ, ta trích xuất phần **chung** vào một lớp cha Generic:

```java
// BaseCrudPanel.java  — lớp cha dùng chung cho MỌI panel CRUD
public abstract class BaseCrudPanel<T> extends JPanel {

    // ── Fields dùng chung ─────────────────────────────────────
    protected final DefaultTableModel tableModel;
    protected final JTable            table;
    protected final JLabel            statusLabel;
    protected final JTextField        searchField;
    protected       TableRowSorter<DefaultTableModel> sorter;
    protected       Runnable          onDataChanged;

    // ── Constructor chung ────────────────────────────────────
    protected BaseCrudPanel(String[] columns) {
        this.tableModel  = buildTableModel(columns);
        this.table       = buildTable();
        this.statusLabel = new JLabel();
        this.searchField = new JTextField();

        setLayout(new BorderLayout());
        setBackground(Theme.BG_PAGE);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(buildToolbar(),   BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        loadData();
    }

    // ── Các phương thức mà lớp con BẮT BUỘC phải override ───
    protected abstract void loadData();
    protected abstract void openDialog(T existing);
    protected abstract T    getSelectedEntity();
    protected abstract void deleteEntity(Long id) throws Exception;
    protected abstract String getEntityDisplayName();
    protected abstract int[]  getFilterColumns();
    protected abstract String getSearchPlaceholder();
    protected abstract int[]  getColumnWidths();

    // ── Các phương thức DÙNG CHUNG — lớp con KHÔNG cần viết lại ──

    private JPanel buildToolbar() { /* ... */ }
    private JScrollPane buildTableCard() { /* ... */ }
    private JPanel buildActionBar() { /* ... */ }
    private DefaultTableModel buildTableModel(String[] columns) { /* ... */ }
    private JTable buildTable() { /* ... */ }

    protected void editSelected() { /* ... */ }
    protected void deleteSelected() { /* ... */ }
    protected void notifyChanged() { /* ... */ }

    // Helpers
    protected void showSuccess(String msg) { /* ... */ }
    protected void showWarning(String msg) { /* ... */ }
    protected void showError  (String msg) { /* ... */ }
}
```

Sau đó `StudentPanel` và `TeacherPanel` trở nên **cực kỳ ngắn gọn**:

```java
// StudentPanel.java — sau khi tối ưu
public class StudentPanel extends BaseCrudPanel<Student> {

    private final StudentService studentService;

    public StudentPanel(StudentService studentService) {
        super(new String[]{"ID","STT","Mã HV","Họ và tên","Ngày sinh","Giới tính","Điện thoại","Email","Trạng thái"});
        this.studentService = studentService;
    }

    @Override
    public void loadData() {
        List<Student> list = studentService.findAll();
        // ... chỉ phần logic đặc thù của Student
    }

    @Override
    protected void openDialog(Student existing) {
        // chỉ khác: StudentFormDialog
    }

    // ... vài method abstract ngắn còn lại
}
```

---

## 3. Chi Tiết Từng Điểm Tối Ưu

### 3.1 — Loại Bỏ Khai Báo Fields Trùng Lặp

**Trước (lặp ở cả 2 file):**
```java
// StudentPanel.java
private final DefaultTableModel tableModel;
private final JTable            table;
private final JLabel            statusLabel;
private final JTextField        searchField;
private       TableRowSorter<DefaultTableModel> sorter;
private       Runnable          onDataChanged;

// TeacherPanel.java — y hệt 6 dòng trên
private final DefaultTableModel tableModel;
...
```

**Sau (chỉ khai báo 1 lần trong `BaseCrudPanel`):**
```java
// BaseCrudPanel.java
protected final DefaultTableModel tableModel;
protected final JTable            table;
// ...
```
> ✅ **Lý do:** Mỗi lần thêm 1 field mới (ví dụ `refreshBtn`) chỉ cần sửa 1 chỗ, không sợ bỏ sót.

---

### 3.2 — Hợp Nhất `buildToolbar()` / `buildActionBar()` / `buildTableCard()`

**Trước — 3 method, mỗi cái lặp lại 2 lần (6 lần viết tổng cộng):**
```java
// StudentPanel.java
private JPanel buildToolbar() {
    JButton addBtn = ComponentFactory.primaryButton("+ Thêm mới");
    addBtn.addActionListener(e -> openDialog(null));
    // ...
}

// TeacherPanel.java — sao chép 100%
private JPanel buildToolbar() {
    JButton addBtn = ComponentFactory.primaryButton("+ Thêm mới");
    addBtn.addActionListener(e -> openDialog(null));
    // ...
}
```

**Sau — chỉ 1 lần trong `BaseCrudPanel`, dùng `getSearchPlaceholder()` để tùy biến:**
```java
// BaseCrudPanel.java
private JPanel buildToolbar() {
    searchField.putClientProperty("JTextField.placeholderText", getSearchPlaceholder());
    // ... code còn lại chỉ viết 1 lần
}
```
> ✅ **Lý do:** Nếu muốn đổi màu nút "Thêm mới" hay thêm tooltip, chỉ sửa 1 chỗ, tất cả panel đều cập nhật.

---

### 3.3 — Hợp Nhất `buildTableModel()` và `buildTable()`

**Trước — logic ẩn cột ID, căn trái header, double-click lặp ở cả 2 file:**
```java
// Lặp trong StudentPanel và TeacherPanel
t.getTableHeader().setDefaultRenderer((tbl, val, sel, focus, row, col) -> {
    Component c = baseRenderer.getTableCellRendererComponent(...);
    if (c instanceof JLabel lbl) lbl.setHorizontalAlignment(SwingConstants.LEFT);
    return c;
});
TableStyler.hideColumn(t, 0);
t.addMouseListener(new java.awt.event.MouseAdapter() {
    @Override public void mouseClicked(...) {
        if (e.getClickCount() == 2 ...) editSelected();
    }
});
```

**Sau — chỉ viết 1 lần trong `BaseCrudPanel.buildTable()`, lớp con chỉ override `getColumnWidths()`:**
```java
// BaseCrudPanel.java
private JTable buildTable() {
    // Header căn trái — 1 lần duy nhất
    // hideColumn(0)    — 1 lần duy nhất
    // double-click     — 1 lần duy nhất
    int[] widths = getColumnWidths(); // lớp con cung cấp kích thước cột
    for (int i = 0; i < widths.length; i++) {
        cm.getColumn(i + 1).setPreferredWidth(widths[i]);
    }
}
```
> ✅ **Lý do:** Logic styling table chỉ cần kiểm thử 1 lần. Sửa 1 bug (ví dụ: lỗi sort) là fix cho tất cả.

---

### 3.4 — Hợp Nhất `deleteSelected()` — Template Method Pattern

**Trước — logic xác nhận xóa lặp lại:**
```java
// StudentPanel
int ok = JOptionPane.showConfirmDialog(this,
    "Bạn có chắc muốn xóa học viên \"" + name + "\"?", ...);
if (ok != JOptionPane.YES_OPTION) return;
studentService.delete(id);
showSuccess("Đã xóa học viên \"" + name + "\" thành công.");

// TeacherPanel — y hệt, chỉ đổi "học viên" → "giáo viên"
int ok = JOptionPane.showConfirmDialog(this,
    "Bạn có chắc muốn xóa giáo viên \"" + name + "\"?", ...);
```

**Sau — Template Method: flow xóa nằm ở lớp cha, lớp con chỉ cung cấp tên:**
```java
// BaseCrudPanel.java
protected void deleteSelected() {
    // ...
    int ok = JOptionPane.showConfirmDialog(this,
        "Bạn có chắc muốn xóa " + getEntityDisplayName() + " \"" + name + "\"?", ...);
    if (ok != JOptionPane.YES_OPTION) return;
    try {
        deleteEntity(id);                          // ← lớp con implement
        showSuccess("Đã xóa " + getEntityDisplayName() + " \"" + name + "\" thành công.");
        notifyChanged();
    } catch (Exception e) { showError(...); }
}

// StudentPanel.java — chỉ cần:
@Override protected String getEntityDisplayName() { return "học viên"; }
@Override protected void   deleteEntity(Long id)  { studentService.delete(id); }

// TeacherPanel.java — chỉ cần:
@Override protected String getEntityDisplayName() { return "giáo viên"; }
@Override protected void   deleteEntity(Long id)  { teacherService.delete(id); }
```
> ✅ **Lý do:** Áp dụng **Template Method Pattern** — flow cố định ở lớp cha, điểm biến đổi được delegate xuống lớp con.

---

### 3.5 — Hợp Nhất `showSuccess` / `showWarning` / `showError`

**Trước — 3 method helper copy-paste vào mỗi file:**
```java
// Lặp trong StudentPanel, TeacherPanel, StaffPanel, RoomPanel, ...
private void showSuccess(String msg) {
    JOptionPane.showMessageDialog(this, msg, "Thành công", JOptionPane.INFORMATION_MESSAGE);
}
private void showWarning(String msg) { ... }
private void showError  (String msg) { ... }
```

**Sau — chỉ 1 lần trong `BaseCrudPanel`:**
```java
protected void showSuccess(String msg) { JOptionPane.showMessageDialog(this, msg, "Thành công", JOptionPane.INFORMATION_MESSAGE); }
protected void showWarning(String msg) { JOptionPane.showMessageDialog(this, msg, "Cảnh báo",   JOptionPane.WARNING_MESSAGE); }
protected void showError  (String msg) { JOptionPane.showMessageDialog(this, msg, "Lỗi",        JOptionPane.ERROR_MESSAGE); }
```
> ✅ **Lý do:** Muốn thêm logging, âm thanh, hay thay bằng toast notification — sửa 1 chỗ là xong.

---

## 4. Kết Quả Đo Lường

| Chỉ số | Trước | Sau | Cải thiện |
|---|---|---|---|
| Tổng dòng code (`StudentPanel` + `TeacherPanel`) | ~280 dòng | ~80 dòng | **-71%** |
| Code trùng lặp | ~240 dòng | 0 dòng | **-100%** |
| Thêm `StaffPanel` mới | ~130 dòng | ~35 dòng | **-73%** |
| Sửa bug 1 chỗ ảnh hưởng tất cả panel | ❌ Phải sửa N file | ✅ Sửa 1 file | |

---

## 5. Các Nguyên Lý Được Áp Dụng

| Nguyên lý | Ứng dụng trong code |
|---|---|
| **DRY** *(Don't Repeat Yourself)* | Toàn bộ phần 3 ở trên — không viết lại logic đã tồn tại |
| **Template Method Pattern** | `deleteSelected()` định nghĩa flow, lớp con fill vào "chỗ trống" |
| **Open/Closed Principle** | `BaseCrudPanel` đóng với sửa đổi, mở để mở rộng qua subclass |
| **Single Responsibility Principle** | `BaseCrudPanel` lo layout/CRUD-flow; lớp con lo data-specific |
| **Generics `<T>`** | Đảm bảo type-safety khi truyền entity giữa panel và dialog |

---

## 6. Tóm Tắt — Tại Sao Phải Làm Vậy?

> **"Mỗi mảnh kiến thức phải có một đại diện duy nhất, rõ ràng, có thẩm quyền trong một hệ thống."**
> — *Andrew Hunt & David Thomas, The Pragmatic Programmer*

1. **Bảo trì dễ hơn:** Sửa bug lỗi styling table? Sửa 1 chỗ trong `BaseCrudPanel`, tất cả panel đều được fix.
2. **Mở rộng nhanh hơn:** Thêm `CoursePanel`, `RoomPanel`... chỉ cần override ~5 method abstract, không phải viết lại 130 dòng.
3. **Giảm rủi ro:** Khi copy-paste, rất dễ quên cập nhật 1 chỗ → sinh ra bug âm thầm (ví dụ: fix sort ở `StudentPanel` nhưng quên `TeacherPanel`).
4. **Dễ đọc hơn:** Lớp con chỉ còn phần logic đặc thù → người đọc code biết ngay điểm khác biệt là gì.

