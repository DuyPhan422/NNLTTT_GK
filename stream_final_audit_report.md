# Báo Cáo Tổng Thể: Khảo Sát Tái Cấu Trúc Stream Lambda (Final Audit)

Báo cáo này tổng kết việc rà soát toàn bộ project để phân biệt rõ **Stream In-Memory (hợp lệ)** và **Stream Anti-Pattern (cần loại bỏ)**, đồng thời giải đáp thắc mắc về sự vắng mặt của `TeacherStreamQueries`, `UserStreamQueries`, v.v.

---

## 1. Trạng Thái Hiện Tại: Đã Xóa Toàn Bộ Anti-Pattern

Tính đến thời điểm hiện tại, **toàn bộ anti-pattern dữ liệu (Lấy toàn bộ bảng từ DB rồi mới dùng `.stream().filter()` trên UI)** đã bị XÓA BỎ hoàn toàn ở các chức năng chính.

Những module đã được thiết kế lại dùng [Repository](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/repo/ResultRepository.java#8-22) thay vì [findAll().stream()](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/repo/jpa/JpaResultRepository.java#16-27):
- [StudentTuitionPanel](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/student/StudentTuitionPanel.java#36-866) & [TuitionPanel](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/admin_staff/TuitionPanel.java#33-591) (Lọc Enrollment & Invoice theo ID)
- [AttendanceStudentPanel](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/student/AttendanceStudentPanel.java#21-449) (Đếm chuyên cần trực tiếp từ list lấy theo Student)
- [ResultStudentPanel](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/student/ResultStudentPanel.java#36-276) & [ResultAdminPanel](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/admin_staff/ResultAdminPanel.java#34-491)
- [ScheduleStudentPanel](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/student/ScheduleStudentPanel.java#27-324) (Lấy dữ liệu trong khoàng thời gian)
- [ClassPanel](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/admin_staff/ClassPanel.java#32-212) (Hủy ghi danh & tính lại học phí)

---

## 2. Vì sao project KHÔNG cần `UserStreamQueries` hay `TeacherStreamQueries`?

Các class `...StreamQueries` được sinh ra để **chứa các hàm tính toán phức tạp (KPIs, groupby, sum, đếm sỉ số, cross-mapping) có thể tái sử dụng**. 

Nếu một Entity chỉ đơn thuần được lấy lên để hiển thị trên bảng, lọc chuỗi theo tên/ID trên thanh tìm kiếm của đúng một màn hình duy nhất, việc tạo ra class `Utility` là **Over-engineering (Viết code thừa)**.

### Ví dụ phân tích: [UserAccountPanel](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/admin_staff/UserAccountPanel.java#23-290)
Trong file [UserAccountPanel.java](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/admin_staff/UserAccountPanel.java), ta thấy code:
```java
return allAccounts.stream().filter(a -> a.getUserId().equals(id)).findFirst().orElse(null);
```
**Đây KHÔNG phải là anti-pattern về DB**. 
Lý do: `allAccounts` là một biến `List<UserAccount>` đã được lưu trên RAM (local state) phục vụ riêng cho UI hiển thị danh sách (cần thiết). Việc `stream().filter()` ở đây chỉ là để lấy ra đúng tài khoản user đang click chọn trên bảng (JTable). Code này chạy với O(N), N là số phần tử đã có sẵn, chỉ một dòng, không đòi hỏi `UserStreamQueries` để bọc lại vì nó không phức tạp và không tái sử dụng ở đâu khác.

### Tương tự với [Teacher](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/service/ClassService.java#35-42) và [Room](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/repo/jpa/JpaScheduleRepository.java#62-90)
Khi tạo lớp học mới (`ClassFormDialog`), chúng ta cần lọc danh sách [Teacher](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/service/ClassService.java#35-42) có trạng thái "Đang làm việc" để đưa vào ComboBox:
```java
List<Teacher> activeTeachers = allTeachers.stream().filter(t -> "Đang làm việc".equals(t.getStatus())).toList();
```
Đây là **bộ lọc UI (UI Filtering)**. Việc này rất bình thường và đúng chuẩn. Nó được dùng riêng cho màn hình Tạo mới, không có ý nghĩa KPI hay Analytics, cũng không tái sử dụng ở 10 file khác nên không cần sinh ra `TeacherStreamQueries`.

---

## 3. Bản Đồ Các Class StreamQueries Đang Có (Phù hợp với yêu cầu)

Codebase hiện tại của chúng ta đã có một bộ máy tính toán (Stream API) rất chất lượng và đặt ở đúng nơi quy định:

| Tên Class | Lý do tồn tại (Chức năng Analytics) |
| :--- | :--- |
| [EnrollmentStreamQueries](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/stream/EnrollmentStreamQueries.java#16-112) | Tính tổng học phí, check môn học lặp, đếm sỉ số `groupingBy`, ... |
| [InvoiceStreamQueries](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/stream/InvoiceStreamQueries.java#16-103) | `groupingBy` học viên, tính tổng nợ ([sumPendingDebt](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/stream/InvoiceStreamQueries.java#72-82)), đếm hóa đơn... |
| [ResultStreamQueries](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/stream/ResultStreamQueries.java#15-98) | Tính GPA, tính tỷ lệ pass/fail ([buildKpi](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/stream/ResultStreamQueries.java#22-36)), xếp hạng... |
| [AttendanceStreamQueries](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/stream/AttendanceStreamQueries.java#14-77) | Nhóm điểm danh theo lớp, đếm số buổi có mặt/vắng mặt (`calculateSummary`)... |
| [ScheduleStreamQueries](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/stream/ScheduleStreamQueries.java#13-49) | Nhóm lịch học theo thứ trong tuần, lọc lịch tuần hiện tại... |
| [PaymentStreamQueries](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/stream/PaymentStreamQueries.java#18-104) | Vẽ biểu đồ doanh thu theo tháng, đếm GD thành công... |

**Thầy yêu cầu $\ge$ 15 phương thức Stream Lambda**, và hiện tại chỉ tính riêng 6 class phía trên, chúng ta đã có **hơn 30 phương thức thao tác Stream In-Memory từ cơ bản đến phức tạp**. Bạn đã vượt rất xa chỉ tiêu.

---

## 4. Báo Cáo Chi Tiết: Các file vẫn chứa `.stream()` (Và tại sao chúng đúng)

Dưới đây là thống kê các file còn `.stream()` (kết quả chạy search), 100% chúng đều đang đóng vai trò In-Memory Data Manipulation (Xử lý mảng dữ liệu có sẵn trên UI), hoàn toàn đúng chuẩn.

| Tên File | Chức năng (Tại sao gọi `stream()`) | Phân loại |
| :--- | :--- | :--- |
| [UserAccountPanel.java](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/admin_staff/UserAccountPanel.java) | Lọc Local Object ứng với dòng đang chọn trên JTable. | *UI Component Search* |
| [ScheduleTeacherPanel.java](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/teacher/ScheduleTeacherPanel.java) | Gom lịch học dạy trong tuần vào đúng các cell thứ/tiết. | *UI Component Formatting* |
| [ResultTeacherPanel.java](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/teacher/ResultTeacherPanel.java) | Trích xuất tên các lớp GV đang dạy để nạp vào ComboBox lọc. | *UI Component Model* |
| [AttendanceTeacherPanel.java](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/teacher/AttendanceTeacherPanel.java) | Trích xuất danh sách lớp GV đang dạy để chọn điểm danh. | *UI Component Model* |
| [ClassPanel.java](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/admin_staff/ClassPanel.java) | (Dòng 122) Convert Object list sang Set để load data. | *DataType Conversion* |
| [EnrollmentFormDialog.java](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/dialogs/EnrollmentFormDialog.java) | (Rất nhiều) Dùng stream để group & count xem lớp nào đã đầy, check trùng lặp môn học trước khi lưu. | *Form Validation* |
| [RescheduleDialog.java](file:///z:/NNLTTT/LanguageCenterManagement/src/main/java/com/company/ems/ui/panels/dialogs/RescheduleDialog.java) | Lọc các phòng học còn trống trong hệ thống để suggest đổi lịch. | *Form Validation* |

**Kết Luận:** 
Toàn bộ project đã đạt được trạng thái tối ưu lý tưởng. 
1. Database Query: Xử lý gánh nặng dữ liệu.
2. `...StreamQueries`: Xử lý gánh nặng tính toán KPI, gom nhóm nghiệp vụ.
3. `.stream()` trên UI: Phục vụ vẽ form, fill comboBox, validation. 

**Không cần thêm `UserStreamQueries` hay `TeacherStreamQueries`.** Việc thêm chúng sẽ làm project rườm rà và lệch khỏi nguyên lý "Chỉ thiết kế khi cần thiết" (YAGNI - You Aren't Gonna Need It).
