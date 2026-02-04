package com.pos.ui;

import com.pos.Session;
import com.pos.dao.CategoryDAO;
import com.pos.model.Category;
import com.pos.service.PermissionService;
import com.pos.ui.components.CardPanel;
import com.pos.ui.components.ModernButton;
import com.pos.ui.components.ModernTableStyle;
import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryManagementPanel extends JPanel {
    private final Runnable onDataChanged;
    private final DefaultTableModel model;
    private final JTable table;

    private final JTextField searchField;

    private List<Category> currentCategories = new ArrayList<>();

    public CategoryManagementPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        setLayout(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        setOpaque(false);
        setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM));

        CardPanel top = new CardPanel(new FlowLayout(FlowLayout.LEFT, UIConstants.SPACING_SM, UIConstants.SPACING_SM));
        top.setShadowSize(2);
        top.setRadius(UIConstants.RADIUS_MD);
        top.setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_MD, UIConstants.SPACING_SM, UIConstants.SPACING_MD));

        JLabel searchLabel = new JLabel("Tìm:");
        searchLabel.setFont(UIConstants.FONT_BODY);
        searchLabel.setForeground(UIConstants.NEUTRAL_700);
        top.add(searchLabel);

        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setPreferredSize(new Dimension(260, UIConstants.INPUT_HEIGHT_SM));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        top.add(searchField);

        ModernButton addBtn = new ModernButton("Thêm", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        ModernButton editBtn = new ModernButton("Sửa", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.SMALL);
        ModernButton deleteBtn = new ModernButton("Xóa", ModernButton.ButtonType.DANGER, ModernButton.ButtonSize.SMALL);
        addBtn.setPreferredSize(new Dimension(80, 32));
        editBtn.setPreferredSize(new Dimension(70, 32));
        deleteBtn.setPreferredSize(new Dimension(70, 32));

        boolean canAdd = PermissionService.canAddTab(Session.getCurrentUser(), "Danh mục");
        boolean canEdit = PermissionService.canEditTab(Session.getCurrentUser(), "Danh mục");
        boolean canDelete = PermissionService.canDeleteTab(Session.getCurrentUser(), "Danh mục");
        addBtn.setVisible(canAdd);
        editBtn.setVisible(canEdit);
        deleteBtn.setVisible(canDelete);

        top.add(addBtn);
        top.add(editBtn);
        top.add(deleteBtn);

        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{"STT", "Tên", "Mô tả"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        ModernTableStyle.apply(table, true);

        CardPanel tableCard = new CardPanel(new BorderLayout());
        tableCard.setShadowSize(2);
        tableCard.setRadius(UIConstants.RADIUS_MD);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        tableCard.add(scroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        CategoryDAO.ensureSequentialIdsIfNeeded();

        searchField.addActionListener(e -> refreshTable());
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { refreshTable(); }
        });

        addBtn.addActionListener(e -> {
            Category c = showCategoryDialog(null);
            if (c != null) {
                if (CategoryDAO.create(c)) {
                    refreshTable();
                    if (this.onDataChanged != null) this.onDataChanged.run();
                }
            }
        });

        editBtn.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow == -1) {
                JOptionPane.showMessageDialog(this, "Chọn một danh mục để sửa");
                return;
            }
            int row = table.convertRowIndexToModel(viewRow);
            if (row < 0 || row >= currentCategories.size()) return;
            Category src = currentCategories.get(row);
            Category c = new Category(src.getId(), src.getName());
            c.setActive(src.isActive());
            c.setDescription(src.getDescription());
            Category edited = showCategoryDialog(c);
            if (edited != null) {
                if (CategoryDAO.update(edited)) {
                    refreshTable();
                    if (this.onDataChanged != null) this.onDataChanged.run();
                }
            }
        });

        deleteBtn.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow == -1) {
                JOptionPane.showMessageDialog(this, "Chọn một danh mục để xóa");
                return;
            }
            int row = table.convertRowIndexToModel(viewRow);
            if (row < 0 || row >= currentCategories.size()) return;
            Category c = currentCategories.get(row);
            int id = c.getId();
            boolean inUse = CategoryDAO.isInUse(id);
            String msg = inUse
                    ? "Danh mục đang được dùng trong Món ăn. Nếu xóa, các món ăn thuộc danh mục này sẽ bị trống danh mục. Bạn có chắc muốn xóa?"
                    : "Bạn có chắc muốn xóa danh mục này?";
            int res = JOptionPane.showConfirmDialog(this, msg, "Xác nhận xóa",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (res == JOptionPane.YES_OPTION) {
                if (CategoryDAO.delete(id)) {
                    refreshTable();
                    if (this.onDataChanged != null) this.onDataChanged.run();
                }
            }
        });

        refreshTable();
    }

    public void refreshTable() {
        model.setRowCount(0);
        List<Category> list = CategoryDAO.findAll(false);
        String kw = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        currentCategories = new ArrayList<>();
        for (Category c : list) {
            if (kw.isEmpty() || (c.getName() != null && c.getName().toLowerCase().contains(kw))) {
                currentCategories.add(c);
            }
        }
        int stt = 1;
        for (Category c : currentCategories) {
            model.addRow(new Object[]{stt++, c.getName(), c.getDescription()});
        }
    }

    private Category showCategoryDialog(Category existing) {
        JTextField name = new JTextField();
        JTextArea desc = new JTextArea(3, 20);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);

        if (existing != null) {
            name.setText(existing.getName());
            desc.setText(existing.getDescription());
        }

        JScrollPane descScroll = new JScrollPane(desc);
        descScroll.setPreferredSize(new Dimension(320, 90));

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row;
        p.add(new JLabel("Tên danh mục:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        p.add(name, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.NORTHWEST;
        p.add(new JLabel("Mô tả:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        p.add(descScroll, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.weighty = 0; gbc.anchor = GridBagConstraints.WEST;

        int res = JOptionPane.showConfirmDialog(this, p, existing == null ? "Thêm danh mục" : "Sửa danh mục",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String n = name.getText() == null ? "" : name.getText().trim();
            if (n.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên danh mục không được để trống");
                return null;
            }
            Category c = existing == null ? new Category(0, n) : existing;
            c.setName(n);
            c.setDescription(desc.getText() == null ? null : desc.getText().trim());
            c.setActive(true);
            return c;
        }
        return null;
    }
}
