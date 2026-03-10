package com.company.ems.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Thanh header phía trên cùng của content area.
 * Hiển thị tiêu đề trang hiện tại + thông tin người dùng.
 */
public class HeaderPanel extends JPanel {

    private static final Color BG_COLOR   = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color TITLE_COLOR  = new Color(15, 23, 42);
    private static final Color SUB_COLOR    = new Color(100, 116, 139);

    private final JLabel titleLabel;
    private final JLabel subtitleLabel;
    private final JLabel userLabel;

    public HeaderPanel(String title, String subtitle) {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)
        ));

        // Phần tiêu đề bên trái
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(TITLE_COLOR);

        subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(SUB_COLOR);

        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);
        add(titlePanel, BorderLayout.WEST);

        // Avatar người dùng bên phải
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        userPanel.setOpaque(false);

        userLabel = new JLabel("");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userLabel.setForeground(SUB_COLOR);

        JLabel avatarLabel = new JLabel("👤");
        avatarLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));

        userPanel.add(userLabel);
        userPanel.add(avatarLabel);
        add(userPanel, BorderLayout.EAST);
    }

    /** Cập nhật tiêu đề khi chuyển trang */
    public void setTitle(String title, String subtitle) {
        titleLabel.setText(title);
        subtitleLabel.setText(subtitle);
    }

    /** Cập nhật tên / role người dùng hiển thị góc phải */
    public void setUserInfo(String displayName) {
        userLabel.setText(displayName);
    }
}

