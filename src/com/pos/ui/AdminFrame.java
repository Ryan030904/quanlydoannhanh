package com.pos.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminFrame extends JFrame {
    private final Color PRIMARY = new Color(11, 92, 101);
    private final Color ACCENT = new Color(10, 140, 160);
    private final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);

    private final ItemManagementPanel itemPanel;
    private final CategoryManagementPanel categoryPanel;

    public AdminFrame() {
        setTitle("Admin - Quản trị hệ thống");
        setSize(1050, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        JPanel sidebar = new JPanel();
        sidebar.setBackground(PRIMARY);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(14, 12, 14, 12));

        JLabel title = new JLabel("QUẢN TRỊ");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(title);
        sidebar.add(Box.createRigidArea(new Dimension(0, 14)));

        JButton itemsBtn = new JButton("Quản lý món ăn");
        styleMenuButton(itemsBtn);
        JButton categoriesBtn = new JButton("Quản lý danh mục");
        styleMenuButton(categoriesBtn);
        JButton closeBtn = new JButton("Đóng");
        styleMenuButton(closeBtn);

        sidebar.add(itemsBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(categoriesBtn);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(closeBtn);

        root.add(sidebar, BorderLayout.WEST);

        this.itemPanel = new ItemManagementPanel(this::onDataChanged);
        this.categoryPanel = new CategoryManagementPanel(this::onDataChanged);

        content.add(itemPanel, "items");
        content.add(categoryPanel, "categories");
        root.add(content, BorderLayout.CENTER);

        itemsBtn.addActionListener(e -> cardLayout.show(content, "items"));
        categoriesBtn.addActionListener(e -> cardLayout.show(content, "categories"));
        closeBtn.addActionListener(e -> dispose());

        cardLayout.show(content, "items");

        setVisible(true);
    }

    private void styleMenuButton(JButton btn) {
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setBackground(Color.WHITE);
        btn.setForeground(PRIMARY);
        btn.setFont(NORMAL_FONT);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 10, 8, 10));
    }

    private void onDataChanged() {
        try {
            if (itemPanel != null) itemPanel.reloadCategories();
            if (categoryPanel != null) categoryPanel.refreshTable();
        } catch (Exception ignored) {
        }
        try {
            if (AppFrame.getInstance() != null) {
                AppFrame.getInstance().reloadCategories();
                AppFrame.getInstance().refreshMenu();
            }
        } catch (Exception ignored) {
        }
    }
}
