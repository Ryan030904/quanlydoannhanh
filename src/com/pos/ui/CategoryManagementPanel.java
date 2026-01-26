package com.pos.ui;

import com.pos.dao.CategoryDAO;
import com.pos.model.Category;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryManagementPanel extends JPanel {
    private final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private final Runnable onDataChanged;
    private final DefaultTableModel model;
    private final JTable table;

    private final JTextField searchField;
    private final JCheckBox showInactive;

    public CategoryManagementPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        top.setBorder(new EmptyBorder(0, 0, 8, 0));

        searchField = new JTextField();
        searchField.setFont(NORMAL_FONT);
        searchField.setPreferredSize(new Dimension(260, 34));

        showInactive = new JCheckBox("Hiện danh mục ngừng dùng");
        showInactive.setFont(NORMAL_FONT);

        JButton addBtn = new JButton("Thêm");
        JButton editBtn = new JButton("Sửa");
        JButton disableBtn = new JButton("Ngừng dùng");
        JButton enableBtn = new JButton("Bật lại");

        top.add(new JLabel("Tìm:"));
        top.add(searchField);
        top.add(showInactive);
        top.add(addBtn);
        top.add(editBtn);
        top.add(disableBtn);
        top.add(enableBtn);

        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{"ID", "Tên", "Trạng thái", "Mô tả"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        searchField.addActionListener(e -> refreshTable());
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { refreshTable(); }
        });
        showInactive.addActionListener(e -> refreshTable());

        addBtn.addActionListener(e -> {
            Category c = showCategoryDialog(null);
            if (c != null) {
                if (CategoryDAO.create(c)) {
                    refreshTable();
                    if (onDataChanged != null) onDataChanged.run();
                }
            }
        });

        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Chọn một danh mục để sửa");
                return;
            }
            Category c = new Category((int) model.getValueAt(row, 0), (String) model.getValueAt(row, 1));
            c.setActive("Đang dùng".equals(model.getValueAt(row, 2)));
            c.setDescription((String) model.getValueAt(row, 3));
            Category edited = showCategoryDialog(c);
            if (edited != null) {
                if (CategoryDAO.update(edited)) {
                    refreshTable();
                    if (onDataChanged != null) onDataChanged.run();
                }
            }
        });

        disableBtn.addActionListener(e -> setSelectedStatus(false));
        enableBtn.addActionListener(e -> setSelectedStatus(true));

        refreshTable();
    }

    public void refreshTable() {
        model.setRowCount(0);
        boolean includeInactive = showInactive.isSelected();
        List<Category> list = CategoryDAO.findAll(includeInactive);
        String kw = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<Category> filtered = new ArrayList<>();
        for (Category c : list) {
            if (kw.isEmpty() || (c.getName() != null && c.getName().toLowerCase().contains(kw))) {
                filtered.add(c);
            }
        }
        for (Category c : filtered) {
            model.addRow(new Object[]{c.getId(), c.getName(), c.isActive() ? "Đang dùng" : "Ngừng dùng", c.getDescription()});
        }
    }

    private void setSelectedStatus(boolean active) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Chọn một danh mục");
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        if (CategoryDAO.setStatus(id, active)) {
            refreshTable();
            if (onDataChanged != null) onDataChanged.run();
        }
    }

    private Category showCategoryDialog(Category existing) {
        JTextField name = new JTextField();
        JTextArea desc = new JTextArea(3, 20);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        JCheckBox active = new JCheckBox("Đang dùng");
        active.setSelected(true);

        if (existing != null) {
            name.setText(existing.getName());
            desc.setText(existing.getDescription());
            active.setSelected(existing.isActive());
        }

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(new JLabel("Tên danh mục:"));
        p.add(name);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(new JLabel("Mô tả:"));
        p.add(new JScrollPane(desc));
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(active);

        int res = JOptionPane.showConfirmDialog(this, p, existing == null ? "Thêm danh mục" : "Sửa danh mục",
                JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String n = name.getText() == null ? "" : name.getText().trim();
            if (n.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên danh mục không được để trống");
                return null;
            }
            Category c = existing == null ? new Category(0, n) : existing;
            c.setName(n);
            c.setDescription(desc.getText() == null ? null : desc.getText().trim());
            c.setActive(active.isSelected());
            return c;
        }
        return null;
    }
}
