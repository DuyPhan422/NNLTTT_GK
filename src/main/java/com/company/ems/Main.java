package com.company.ems;

import com.company.ems.repo.jpa.*;
import com.company.ems.service.*;
import com.company.ems.ui.UI;
import com.company.ems.ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        UI.initLookAndFeel();

        // Manual DI: tạo toàn bộ dependencies tại đây, truyền vào constructor
        StudentService studentService = new StudentService(new JpaStudentRepository());
        TeacherService teacherService = new TeacherService(new JpaTeacherRepository());
        CourseService  courseService  = new CourseService(new JpaCourseRepository());
        RoomService    roomService    = new RoomService(new JpaRoomRepository());
        StaffService   staffService   = new StaffService(new JpaStaffRepository());

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(
                    studentService, teacherService,
                    courseService, roomService, staffService
            );
            frame.setVisible(true);
        });
    }
}