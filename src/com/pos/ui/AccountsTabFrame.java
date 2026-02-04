package com.pos.ui;

import com.pos.ui.components.ModernButton;
import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AccountsTabFrame extends JFrame {
    private final AppFrame parent;

    public AccountsTabFrame(AppFrame parent) {
        this.parent = parent;

        setTitle("Tài khoản");
		try {
			setIconImages(AppFrame.getAppIconImages());
		} catch (Exception ignored) {
		}
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 760);
        setLocationRelativeTo(parent);

        if (this.parent != null) {
            this.parent.setVisible(false);
        }

        JPanel root = new JPanel(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        root.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        setContentPane(root);

        AccountsManagementPanel panel = new AccountsManagementPanel(() -> {
            try {
                if (AppFrame.getInstance() != null) {
                    AppFrame.getInstance().refreshMenu();
                }
            } catch (Exception ignored) {
            }
        });
        root.add(panel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SPACING_SM, 0));
        actions.setOpaque(false);
        ModernButton closeBtn = new ModernButton("Đóng", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.MEDIUM);
        closeBtn.setPreferredSize(new Dimension(100, 36));
        actions.add(closeBtn);
        root.add(actions, BorderLayout.SOUTH);

        closeBtn.addActionListener(e -> dispose());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (AccountsTabFrame.this.parent != null) {
                    AccountsTabFrame.this.parent.setVisible(true);
                }
            }
        });

        setVisible(true);
    }
}
