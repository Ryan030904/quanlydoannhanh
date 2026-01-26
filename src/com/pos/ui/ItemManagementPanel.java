package com.pos.ui;

import com.pos.dao.CategoryDAO;
import com.pos.dao.ItemDAO;
import com.pos.model.Category;
import com.pos.model.Item;
import com.pos.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemManagementPanel extends JPanel {
    private final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private final Runnable onDataChanged;

    private final DefaultTableModel model;
    private final JTable table;

    private final JComboBox<Category> categoryCombo;
    private final JTextField searchField;
    private final JCheckBox showInactive;

    private Map<Integer, Category> categoryMap = new HashMap<>();

    public ItemManagementPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        top.setBorder(new EmptyBorder(0, 0, 8, 0));

        categoryCombo = new JComboBox<>();
        categoryCombo.setFont(NORMAL_FONT);
        categoryCombo.setPreferredSize(new Dimension(220, 34));

        searchField = new JTextField();
        searchField.setFont(NORMAL_FONT);
        searchField.setPreferredSize(new Dimension(240, 34));

        showInactive = new JCheckBox("Hiện món ngừng bán");
        showInactive.setFont(NORMAL_FONT);

        JButton addBtn = new JButton("Thêm");
        JButton editBtn = new JButton("Sửa");
        JButton disableBtn = new JButton("Ngừng bán");
        JButton enableBtn = new JButton("Bán lại");

        top.add(new JLabel("Danh mục:"));
        top.add(categoryCombo);
        top.add(new JLabel("Tìm:"));
        top.add(searchField);
        top.add(showInactive);
        top.add(addBtn);
        top.add(editBtn);
        top.add(disableBtn);
        top.add(enableBtn);

        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{"ID", "Mã", "Tên", "Danh mục", "Giá", "Trạng thái", "Mô tả"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        reloadCategories();
        refreshTable();

        categoryCombo.addActionListener(e -> refreshTable());
        searchField.addActionListener(e -> refreshTable());
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { refreshTable(); }
        });
        showInactive.addActionListener(e -> refreshTable());

        addBtn.addActionListener(e -> {
            Item it = showItemDialog(null);
            if (it != null) {
                if (ItemDAO.create(it)) {
                    refreshTable();
                    if (onDataChanged != null) onDataChanged.run();
                }
            }
        });

        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Chọn một món để sửa");
                return;
            }
            Item it = new Item();
            it.setId((int) model.getValueAt(row, 0));
            it.setCode((String) model.getValueAt(row, 1));
            it.setName((String) model.getValueAt(row, 2));
            Object catObj = model.getValueAt(row, 3);
            if (catObj instanceof Category) {
                it.setCategoryId(((Category) catObj).getId());
            }
            String priceStr = model.getValueAt(row, 4).toString().replaceAll("[^0-9\\.]", "");
            it.setPrice(Double.parseDouble(priceStr.isEmpty() ? "0" : priceStr));
            it.setActive("Đang bán".equals(model.getValueAt(row, 5)));
            it.setDescription((String) model.getValueAt(row, 6));
            Item edited = showItemDialog(it);
            if (edited != null) {
                if (ItemDAO.update(edited)) {
                    refreshTable();
                    if (onDataChanged != null) onDataChanged.run();
                }
            }
        });

        disableBtn.addActionListener(e -> setSelectedActive(false));
        enableBtn.addActionListener(e -> setSelectedActive(true));
    }

    public void reloadCategories() {
        DefaultComboBoxModel<Category> m = new DefaultComboBoxModel<>();
        Category all = new Category(0, "Tất cả");
        m.addElement(all);
        categoryMap.clear();
        List<Category> cats = CategoryDAO.findAll(true);
        for (Category c : cats) {
            categoryMap.put(c.getId(), c);
            m.addElement(c);
        }
        categoryCombo.setModel(m);
    }

    private void refreshTable() {
        model.setRowCount(0);
        String keyword = searchField.getText() == null ? null : searchField.getText().trim();
        Integer categoryId = null;
        Object sel = categoryCombo.getSelectedItem();
        if (sel instanceof Category) {
            Category c = (Category) sel;
            if (c.getId() > 0) categoryId = c.getId();
        }
        boolean includeInactive = showInactive.isSelected();
        List<Item> items = ItemDAO.findByFilterAdmin(keyword, categoryId, includeInactive);
        for (Item it : items) {
            Category cat = categoryMap.get(it.getCategoryId());
            if (cat == null) cat = new Category(it.getCategoryId(), String.valueOf(it.getCategoryId()));
            model.addRow(new Object[]{it.getId(), it.getCode(), it.getName(), cat, CurrencyUtil.formatUSDAsVND(it.getPrice()),
                    it.isActive() ? "Đang bán" : "Ngừng bán", it.getDescription()});
        }
    }

    private void setSelectedActive(boolean active) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Chọn một món");
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        if (ItemDAO.setActive(id, active)) {
            refreshTable();
            if (onDataChanged != null) onDataChanged.run();
        }
    }

    private Item showItemDialog(Item existing) {
        JTextField code = new JTextField();
        JTextField name = new JTextField();
        JComboBox<Category> cat = new JComboBox<>();
        JTextField price = new JTextField();
        JTextField desc = new JTextField();
        JCheckBox active = new JCheckBox("Đang bán");
        active.setSelected(true);

        DefaultComboBoxModel<Category> cm = new DefaultComboBoxModel<>();
        for (Category c : CategoryDAO.findAll(true)) cm.addElement(c);
        cat.setModel(cm);

        final File[] selectedImage = new File[1];
        JButton chooseImgBtn = new JButton("Chọn ảnh...");
        JLabel imgLabel = new JLabel("Chưa chọn");

        if (existing != null) {
            code.setText(existing.getCode());
            name.setText(existing.getName());
            price.setText(String.valueOf(existing.getPrice()));
            desc.setText(existing.getDescription());
            active.setSelected(existing.isActive());
            for (int i = 0; i < cat.getItemCount(); i++) {
                if (cat.getItemAt(i).getId() == existing.getCategoryId()) {
                    cat.setSelectedIndex(i);
                    break;
                }
            }
            if (existing.getImagePath() != null) {
                imgLabel.setText(existing.getImagePath());
            }
        }

        chooseImgBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            int res = fc.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                selectedImage[0] = f;
                imgLabel.setText(f.getName());
            }
        });

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(new JLabel("Mã:"));
        p.add(code);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(new JLabel("Tên:"));
        p.add(name);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(new JLabel("Danh mục:"));
        p.add(cat);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(new JLabel("Giá:"));
        p.add(price);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        JPanel imgRow = new JPanel(new BorderLayout(6, 6));
        imgRow.add(chooseImgBtn, BorderLayout.WEST);
        imgRow.add(imgLabel, BorderLayout.CENTER);
        p.add(new JLabel("Ảnh:"));
        p.add(imgRow);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(new JLabel("Mô tả:"));
        p.add(desc);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(active);

        int res = JOptionPane.showConfirmDialog(this, p, existing == null ? "Thêm món" : "Sửa món",
                JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                String n = name.getText() == null ? "" : name.getText().trim();
                if (n.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Tên món không được để trống");
                    return null;
                }
                Item it = existing == null ? new Item() : existing;
                it.setCode(code.getText() == null ? null : code.getText().trim());
                it.setName(n);
                Object selectedCat = cat.getSelectedItem();
                if (selectedCat instanceof Category) {
                    it.setCategoryId(((Category) selectedCat).getId());
                }
                it.setPrice(Double.parseDouble(price.getText() == null ? "0" : price.getText().trim()));
                it.setDescription(desc.getText() == null ? null : desc.getText().trim());
                it.setActive(active.isSelected());

                if (selectedImage[0] != null) {
                    File imgDir = new File("img");
                    if (!imgDir.exists()) imgDir.mkdirs();
                    String newName = System.currentTimeMillis() + "_" + selectedImage[0].getName();
                    File dest = new File(imgDir, newName);
                    try {
                        Files.copy(selectedImage[0].toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        it.setImagePath(dest.getPath());
                    } catch (IOException ioe) {
                        JOptionPane.showMessageDialog(this, "Không thể lưu ảnh: " + ioe.getMessage());
                    }
                }
                return it;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Dữ liệu không hợp lệ: " + ex.getMessage());
            }
        }
        return null;
    }
}
