package com.pos.ui;

import com.pos.dao.ItemDAO;
import com.pos.model.Item;
import com.pos.ui.components.ModernTableStyle;
import com.pos.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ItemManagementFrame extends JFrame {
    private DefaultTableModel model;

    public ItemManagementFrame() {
        setTitle("Quản lý món ăn");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8,8));

        model = new DefaultTableModel(new Object[]{"ID","Mã","Tên","Danh mục","Giá (VND)","Mô tả"},0) {
            public boolean isCellEditable(int row, int col){ return false; }
        };
        JTable table = new JTable(model);
        ModernTableStyle.apply(table, true);
        refreshTable();

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Thêm");
        JButton editBtn = new JButton("Sửa");
        JButton delBtn = new JButton("Xóa");
        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(delBtn);
        add(actions, BorderLayout.NORTH);

        addBtn.addActionListener(e -> {
            Item item = showItemDialog(null);
            if (item != null) {
                if (ItemDAO.create(item)) {
                    refreshTable();
                    if (AppFrame.getInstance() != null) AppFrame.getInstance().refreshMenu();
                }
            }
        });

        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this,"Chọn một hàng để sửa"); return; }
            Item item = new Item();
            item.setId((int)model.getValueAt(row,0));
            item.setCode((String)model.getValueAt(row,1));
            item.setName((String)model.getValueAt(row,2));
            item.setCategoryId(Integer.parseInt(model.getValueAt(row,3).toString()));
            // parse price remove non-digits
            String priceStr = model.getValueAt(row,4).toString().replaceAll("[^0-9\\.]", "");
            item.setPrice(Double.parseDouble(priceStr));
            item.setDescription((String)model.getValueAt(row,5));
            Item edited = showItemDialog(item);
            if (edited != null) {
                if (ItemDAO.update(edited)) {
                    refreshTable();
                    if (AppFrame.getInstance() != null) AppFrame.getInstance().refreshMenu();
                }
            }
        });

        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this,"Chọn một hàng để xóa"); return; }
            int id = (int)model.getValueAt(row,0);
            int ok = JOptionPane.showConfirmDialog(this,"Xác nhận xóa món này?","Xác nhận",JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                if (ItemDAO.delete(id)) {
                    refreshTable();
                    if (AppFrame.getInstance() != null) AppFrame.getInstance().refreshMenu();
                } else {
                    JOptionPane.showMessageDialog(this, "Không thể xóa món này", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        setVisible(true);
    }

    private void refreshTable() {
        model.setRowCount(0);
        List<Item> items = ItemDAO.findAll();
        for (Item it : items) {
            model.addRow(new Object[]{it.getId(), it.getCode(), it.getName(), it.getCategoryId(), CurrencyUtil.format(it.getPrice()), it.getDescription()});
        }
    }

    private Item showItemDialog(Item existing) {
        JTextField code = new JTextField();
        JTextField name = new JTextField();
        JTextField cat = new JTextField();
        JTextField price = new JTextField();
        JTextField desc = new JTextField();
        final File[] selectedImage = new File[1];
        JButton chooseImgBtn = new JButton("Chọn ảnh...");
        JLabel imgLabel = new JLabel("Chưa chọn");
        if (existing != null) {
            code.setText(existing.getCode());
            name.setText(existing.getName());
            cat.setText(String.valueOf(existing.getCategoryId()));
            price.setText(String.valueOf((long) existing.getPrice()));
            desc.setText(existing.getDescription());
            if (existing.getImagePath() != null) {
                imgLabel.setText(existing.getImagePath());
                // do not set selectedImage; leave null unless user chooses new file
            }
        }
        JPanel p = new JPanel(new GridLayout(0,1));
        p.add(new JLabel("Mã:")); p.add(code);
        p.add(new JLabel("Tên:")); p.add(name);
        p.add(new JLabel("Danh mục (id):")); p.add(cat);
        p.add(new JLabel("Giá (VND):")); p.add(price);
        JPanel imgRow = new JPanel(new BorderLayout(6,6));
        imgRow.add(chooseImgBtn, BorderLayout.WEST);
        imgRow.add(imgLabel, BorderLayout.CENTER);
        p.add(new JLabel("Ảnh:")); p.add(imgRow);
        p.add(new JLabel("Mô tả:")); p.add(desc);
        chooseImgBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            int res = fc.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                selectedImage[0] = f;
                imgLabel.setText(f.getName());
            }
        });
        int res = JOptionPane.showConfirmDialog(this, p, existing == null ? "Thêm món" : "Sửa món", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                Item it = existing == null ? new Item() : existing;
                it.setCode(code.getText().trim());
                it.setName(name.getText().trim());
                it.setCategoryId(Integer.parseInt(cat.getText().trim()));
                it.setPrice(Double.parseDouble(price.getText().trim()));
                it.setDescription(desc.getText().trim());
                // handle image copy
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
                JOptionPane.showMessageDialog(this,"Dữ liệu không hợp lệ: "+ex.getMessage());
            }
        }
        return null;
    }
}


