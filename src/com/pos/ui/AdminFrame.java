package com.pos.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import com.pos.ui.theme.UIConstants;
import com.pos.ui.components.*;

public class AdminFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);

    private final ItemManagementPanel itemPanel;
    private final CategoryManagementPanel categoryPanel;
    
    private SidebarButton itemsBtn;
    private SidebarButton categoriesBtn;

    public AdminFrame() {
        setTitle("Admin - Quản trị hệ thống");
        setSize(1050, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UIConstants.BG_SECONDARY);
        setContentPane(root);

        // === SIDEBAR ===
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BorderLayout());
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIConstants.NEUTRAL_200));

        // Header
        JPanel sidebarHeader = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(
                    0, 0, UIConstants.PRIMARY_700,
                    getWidth(), getHeight(), UIConstants.PRIMARY_600
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebarHeader.setPreferredSize(new Dimension(0, 60));
        sidebarHeader.setLayout(new BorderLayout());
        sidebarHeader.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_LG, UIConstants.SPACING_MD, UIConstants.SPACING_LG));

        JLabel title = new JLabel("⚙️ QUẢN TRỊ");
        title.setForeground(Color.WHITE);
        title.setFont(UIConstants.FONT_HEADING_3);
        sidebarHeader.add(title, BorderLayout.CENTER);
        sidebar.add(sidebarHeader, BorderLayout.NORTH);

        // Menu
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(Color.WHITE);
        menuPanel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_SM, UIConstants.SPACING_MD, UIConstants.SPACING_SM));

        ButtonGroup menuGroup = new ButtonGroup();
        
        itemsBtn = new SidebarButton("Quản lý món ăn", "");
        categoriesBtn = new SidebarButton("Quản lý danh mục", "");
        
        menuGroup.add(itemsBtn);
        menuGroup.add(categoriesBtn);
        
        menuPanel.add(itemsBtn);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        menuPanel.add(categoriesBtn);
        menuPanel.add(Box.createVerticalGlue());
        
        // Close button
        ModernButton closeBtn = new ModernButton("Đóng cửa sổ", ModernButton.ButtonType.GHOST);
        
        closeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, UIConstants.BUTTON_HEIGHT));
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        menuPanel.add(closeBtn);
        
        sidebar.add(menuPanel, BorderLayout.CENTER);
        root.add(sidebar, BorderLayout.WEST);

        // === CONTENT ===
        content.setBackground(UIConstants.BG_SECONDARY);
        content.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        
        this.itemPanel = new ItemManagementPanel(this::onDataChanged);
        this.categoryPanel = new CategoryManagementPanel(this::onDataChanged);

        content.add(wrapContent("Quản lý món ăn", itemPanel), "items");
        content.add(wrapContent("Quản lý danh mục", categoryPanel), "categories");
        root.add(content, BorderLayout.CENTER);

        // Event listeners
        itemsBtn.addActionListener(e -> {
            cardLayout.show(content, "items");
        });
        categoriesBtn.addActionListener(e -> {
            cardLayout.show(content, "categories");
        });
        closeBtn.addActionListener(e -> dispose());

        cardLayout.show(content, "items");
        itemsBtn.setSelected(true);

        setVisible(true);
    }
    
    private JPanel wrapContent(String title, JPanel panel) {
        JPanel wrapper = new JPanel(new BorderLayout(0, UIConstants.SPACING_SM));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM));
        
        // Content fills the entire space - no header
        wrapper.add(panel, BorderLayout.CENTER);
        
        return wrapper;
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
