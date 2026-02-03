package com.pos.ui;

import com.pos.dao.CustomerDAO;
import com.pos.model.Customer;
import com.pos.ui.theme.UIConstants;
import com.pos.ui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CustomersManagementPanel extends JPanel {
    private final AppFrame parent;
    private final Runnable onDataChanged;

    private final boolean supportsCustomers;

    private JTextField searchField;

    private DefaultTableModel model;
    private JTable table;

    private List<Customer> current = new ArrayList<>();

    public CustomersManagementPanel(AppFrame parent, Runnable onDataChanged) {
        this.parent = parent;
        this.onDataChanged = onDataChanged;
        this.supportsCustomers = CustomerDAO.supportsCustomers();

        setLayout(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        setOpaque(false);
        setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM));

        if (!supportsCustomers) {
            JPanel p = new JPanel(new GridBagLayout());
            p.setOpaque(false);
            JLabel msg = new JLabel("Không tìm thấy bảng customers");
            msg.setFont(UIConstants.FONT_BODY);
            msg.setForeground(UIConstants.WARNING_DARK);
            p.add(msg);
            add(p, BorderLayout.CENTER);
            return;
        }

        add(buildFilterBar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);

        refreshTable();
    }

    private JPanel buildFilterBar() {
        CardPanel top = new CardPanel(new WrapLayout(FlowLayout.LEFT, UIConstants.SPACING_SM, UIConstants.SPACING_SM));
        top.setShadowSize(2);
        top.setRadius(UIConstants.RADIUS_MD);
        top.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

        // Tìm kiếm
        JLabel searchLabel = new JLabel("Tìm kiếm");
        searchLabel.setFont(UIConstants.FONT_BODY_BOLD);
        searchLabel.setForeground(UIConstants.NEUTRAL_700);
        top.add(searchLabel);

        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setPreferredSize(new Dimension(250, UIConstants.INPUT_HEIGHT_SM));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        searchField.setToolTipText("Nhập tên, SĐT để tìm kiếm");
        top.add(searchField);

        // Nút lọc và reset
        ModernButton filterBtn = new ModernButton("Lọc", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        ModernButton resetBtn = new ModernButton("Reset", ModernButton.ButtonType.GHOST, ModernButton.ButtonSize.SMALL);
        filterBtn.setPreferredSize(new Dimension(70, 32));
        resetBtn.setPreferredSize(new Dimension(70, 32));
        top.add(filterBtn);
        top.add(resetBtn);

        // Xử lý sự kiện
        filterBtn.addActionListener(e -> refreshTable());
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });

        searchField.addActionListener(e -> refreshTable());

        return top;
    }

    private JPanel buildTable() {
        CardPanel panel = new CardPanel(new BorderLayout());
        panel.setShadowSize(2);
        panel.setRadius(UIConstants.RADIUS_LG);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

        // Bảng 3 cột: Mã, Tên, SĐT
        model = new DefaultTableModel(new Object[]{
                "Mã", "Tên", "SĐT"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(model);
        ModernTableStyle.apply(table, true);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SPACING_SM, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_SM, 0, 0, 0));

        ModernButton addBtn = new ModernButton("Thêm", ModernButton.ButtonType.PRIMARY);
        ModernButton editBtn = new ModernButton("Sửa", ModernButton.ButtonType.SECONDARY);
        ModernButton deleteBtn = new ModernButton("Xóa", ModernButton.ButtonType.DANGER);

        panel.add(addBtn);
        panel.add(editBtn);
        panel.add(deleteBtn);

        addBtn.addActionListener(e -> {
            Customer c = showCustomerDialog(null);
            if (c != null) {
                if (CustomerDAO.create(c)) {
                    refreshTable();
                    if (onDataChanged != null) onDataChanged.run();
                }
            }
        });

        editBtn.addActionListener(e -> {
            Customer c = getSelectedCustomer();
            if (c == null) {
                JOptionPane.showMessageDialog(this, "Chọn một khách hàng để sửa");
                return;
            }
            Customer edited = showCustomerDialog(c);
            if (edited != null) {
                if (CustomerDAO.update(edited)) {
                    refreshTable();
                    if (onDataChanged != null) onDataChanged.run();
                }
            }
        });

        deleteBtn.addActionListener(e -> {
            Customer c = getSelectedCustomer();
            if (c == null) {
                JOptionPane.showMessageDialog(this, "Chọn một khách hàng để xóa");
                return;
            }
            
            int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa khách hàng \"" + c.getFullName() + "\"?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (confirm != JOptionPane.YES_OPTION) return;
            
            if (CustomerDAO.delete(c.getId())) {
                JOptionPane.showMessageDialog(this, "Đã xóa khách hàng");
                refreshTable();
                if (onDataChanged != null) onDataChanged.run();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể xóa khách hàng này", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private void refreshTable() {
        String keyword = searchField.getText() == null ? null : searchField.getText().trim();

        current = CustomerDAO.findByFilter(keyword, null, false);

        model.setRowCount(0);
        for (Customer c : current) {
            model.addRow(new Object[]{
                c.getId(),
                c.getFullName(),
                c.getPhone()
            });
        }

        if (model.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private Customer getSelectedCustomer() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= current.size()) return null;
        return current.get(row);
    }

    private Customer showCustomerDialog(Customer existing) {
        JTextField name = new JTextField();
        JTextField phone = new JTextField();

        if (existing != null) {
            name.setText(existing.getFullName());
            phone.setText(existing.getPhone());
        }

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.add(new JLabel("Tên khách hàng:"));
        panel.add(name);
        panel.add(new JLabel("Số điện thoại:"));
        panel.add(phone);

        int res = JOptionPane.showConfirmDialog(this, panel,
            existing == null ? "Thêm khách hàng" : "Sửa khách hàng",
            JOptionPane.OK_CANCEL_OPTION);
        
        if (res != JOptionPane.OK_OPTION) return null;

        String n = name.getText() == null ? "" : name.getText().trim();
        String p = phone.getText() == null ? "" : phone.getText().trim();

        if (n.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên khách hàng không được để trống");
            return null;
        }

        Customer c = existing == null ? new Customer() : existing;
        c.setFullName(n);
        c.setPhone(p);
        c.setAddress(""); // Không dùng nữa
        c.setMembershipLevel("bronze"); // Mặc định
        
        return c;
    }
}
