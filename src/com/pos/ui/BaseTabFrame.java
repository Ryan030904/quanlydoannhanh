package com.pos.ui;

import com.pos.ui.components.HeaderPanel;
import com.pos.ui.components.ModernButton;
import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Base class for all tab frames - ensures consistent styling
 */
public abstract class BaseTabFrame extends JFrame {
    protected final AppFrame parent;
    
    public BaseTabFrame(AppFrame parent, String title) {
        this.parent = parent;
        
        setTitle(title);
		try {
			setIconImages(AppFrame.getAppIconImages());
		} catch (Exception ignored) {
		}
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1300, 800);
        setLocationRelativeTo(parent);
        
        if (parent != null) {
            parent.setVisible(false);
        }
        
        // Root panel với theme background
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UIConstants.BG_SECONDARY);
        // Add minimal padding around the content
        root.setBorder(new EmptyBorder(0, 0, 0, 0));
        setContentPane(root);
        
        // Modern header
        HeaderPanel header = new HeaderPanel(title.toUpperCase(), getSubtitle());
        // Header height handles its own padding
        root.add(header, BorderLayout.NORTH);
        
        // Content panel wrapper with padding
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.setBorder(new EmptyBorder(
            0, 
            UIConstants.SPACING_MD, 
            UIConstants.SPACING_MD, 
            UIConstants.SPACING_MD
        ));
        
        JPanel content = createContentPanel();
        if (content != null) {
            contentWrapper.add(content, BorderLayout.CENTER);
        }
        
        // Footer with actions
        JPanel footer = createFooterPanel();
        if (footer != null) {
            contentWrapper.add(footer, BorderLayout.SOUTH);
        }
        
        root.add(contentWrapper, BorderLayout.CENTER);
        
        // Window close handler
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (BaseTabFrame.this.parent != null) {
                    BaseTabFrame.this.parent.setVisible(true);
                }
            }
        });
        
        // setVisible(true) should be called by the subclass or after initialization
    }
    
    /** Override to provide subtitle for header */
    protected String getSubtitle() {
        return "";
    }
    
    /** Override to create main content panel */
    protected abstract JPanel createContentPanel();
    
    /** Override to create footer with action buttons */
    protected JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 
                                                   UIConstants.SPACING_SM, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(UIConstants.SPACING_MD, 0, 0, 0));
        
        ModernButton closeBtn = new ModernButton("Đóng", 
                                                  ModernButton.ButtonType.GHOST,
                                                  ModernButton.ButtonSize.MEDIUM);
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        
        return footer;
    }
}
