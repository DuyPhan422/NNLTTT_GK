package com.company.ems.ui;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;

/**
 * Cấu hình khởi động ứng dụng — Look & Feel, UIManager defaults.
 */
public final class AppConfig {
    private AppConfig() {}

    public static void initLookAndFeel() {
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 6);
            UIManager.put("Component.arc", 6);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.width", 8);
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
        }
    }
}

