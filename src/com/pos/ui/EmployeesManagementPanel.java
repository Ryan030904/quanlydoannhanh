package com.pos.ui;

import com.pos.dao.EmployeeDAO;
import com.pos.model.Employee;
import com.pos.ui.theme.UIConstants;
import com.pos.ui.components.*;
import com.pos.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class EmployeesManagementPanel extends JPanel {
    private final Runnable onDataChanged;

    private final boolean supportsPhone;
    private final boolean supportsUsername;
    private final boolean supportsSalary;
    private final boolean supportsHireDate;

    private JTextField searchField;
    private JComboBox<String> positionCombo;

    private DefaultTableModel model;
    private JTable table;

    private List<Employee> current = new ArrayList<>();

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public EmployeesManagementPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        this.supportsPhone = EmployeeDAO.supportsPhone();
        this.supportsUsername = EmployeeDAO.supportsUsername();
        this.supportsSalary = EmployeeDAO.supportsSalary();
        this.supportsHireDate = EmployeeDAO.supportsHireDate();

        setLayout(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        setOpaque(false);
        setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM));

        if (!EmployeeDAO.supportsEmployees()) {
            JPanel p = new JPanel(new GridBagLayout());
            p.setOpaque(false);
            JLabel msg = new JLabel("Không tìm thấy bảng employees");
            msg.setFont(UIConstants.FONT_BODY);
            msg.setForeground(UIConstants.WARNING_DARK);
            p.add(msg);
            add(p, BorderLayout.CENTER);
            return;
        }

        add(buildTop(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);

        refreshTable();
    }

    private JPanel buildTop() {
        CardPanel top = new CardPanel(new WrapLayout(FlowLayout.LEFT, UIConstants.SPACING_SM, UIConstants.SPACING_XS));
        top.setShadowSize(2);
        top.setRadius(UIConstants.RADIUS_MD);
        top.setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_MD, UIConstants.SPACING_SM, UIConstants.SPACING_MD));

        JLabel filterIcon = new JLabel("Bộ lọc");
        filterIcon.setFont(UIConstants.FONT_BODY_BOLD);
        filterIcon.setForeground(UIConstants.PRIMARY_700);
        top.add(filterIcon);

        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setPreferredSize(new Dimension(220, UIConstants.INPUT_HEIGHT_SM));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));

        positionCombo = new JComboBox<>(new String[]{"Tất cả", "staff", "cashier", "chef", "waiter"});
        positionCombo.setFont(UIConstants.FONT_BODY);
        positionCombo.setPreferredSize(new Dimension(140, UIConstants.INPUT_HEIGHT_SM));

        ModernButton filterBtn = new ModernButton("Lọc", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        ModernButton resetBtn = new ModernButton("Reset", ModernButton.ButtonType.GHOST, ModernButton.ButtonSize.SMALL);
        filterBtn.setPreferredSize(new Dimension(80, 32));
        resetBtn.setPreferredSize(new Dimension(80, 32));
        

        JLabel lbl1 = new JLabel("Tìm:");
        lbl1.setFont(UIConstants.FONT_BODY);
        lbl1.setForeground(UIConstants.NEUTRAL_700);
        top.add(lbl1);
        top.add(searchField);
        JLabel lbl2 = new JLabel("Vị trí:");
        lbl2.setFont(UIConstants.FONT_BODY);
        lbl2.setForeground(UIConstants.NEUTRAL_700);
        top.add(lbl2);
        top.add(positionCombo);
        top.add(filterBtn);
        top.add(resetBtn);

        filterBtn.addActionListener(e -> refreshTable());
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            positionCombo.setSelectedIndex(0);
            refreshTable();
        });

        searchField.addActionListener(e -> refreshTable());
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { refreshTable(); }
        });
        positionCombo.addActionListener(e -> refreshTable());

        return top;
    }

    private JPanel buildTable() {
        CardPanel panel = new CardPanel(new BorderLayout());
        panel.setShadowSize(2);
        panel.setRadius(UIConstants.RADIUS_LG);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

        model = new DefaultTableModel(new Object[]{
                "STT", "Ho ten", "Email", "SDT", "Chuc vu"
        }, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(model);
        ModernTableStyle.apply(table, true);

        // Allocate more space for full names and trim technical columns
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);   // STT
        table.getColumnModel().getColumn(1).setPreferredWidth(280);  // Ho ten
        table.getColumnModel().getColumn(2).setPreferredWidth(260);  // Email
        table.getColumnModel().getColumn(3).setPreferredWidth(140);  // SDT
        table.getColumnModel().getColumn(4).setPreferredWidth(130);  // Chuc vu

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
            Employee emp = showEmployeeDialog(null);
            if (emp != null) {
                if (EmployeeDAO.create(emp)) {
                    refreshTable();
                    if (onDataChanged != null) onDataChanged.run();
                } else {
                    JOptionPane.showMessageDialog(this, "Không thể thêm nhân viên");
                }
            }
        });

        editBtn.addActionListener(e -> {
            Employee emp = getSelectedEmployee();
            if (emp == null) {
                JOptionPane.showMessageDialog(this, "Chọn một nhân viên để sửa");
                return;
            }
            Employee edited = showEmployeeDialog(emp);
            if (edited != null) {
                if (EmployeeDAO.update(edited)) {
                    refreshTable();
                    if (onDataChanged != null) onDataChanged.run();
                } else {
                    JOptionPane.showMessageDialog(this, "Không thể cập nhật nhân viên");
                }
            }
        });

        deleteBtn.addActionListener(e -> {
            Employee emp = getSelectedEmployee();
            if (emp == null) {
                JOptionPane.showMessageDialog(this, "Chọn một nhân viên để xóa");
                return;
            }
            if (emp.getPosition() != null && emp.getPosition().trim().equalsIgnoreCase("manager")) {
                JOptionPane.showMessageDialog(this, "Không thể xóa nhân viên vị trí manager. Chỉ tài khoản admin là quản lý.");
                return;
            }
            
            int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa nhân viên \"" + emp.getFullName() + "\"?\nLưu ý: Xóa nhân viên có thể ảnh hưởng đến dữ liệu hóa đơn.",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (confirm != JOptionPane.YES_OPTION) return;
            
            if (EmployeeDAO.delete(emp.getId())) {
                JOptionPane.showMessageDialog(this, "Đã xóa nhân viên");
                refreshTable();
                if (onDataChanged != null) onDataChanged.run();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể xóa nhân viên này", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private void refreshTable() {
        String keyword = searchField.getText() == null ? null : searchField.getText().trim();
        String pos = positionCombo.getSelectedItem() == null ? null : String.valueOf(positionCombo.getSelectedItem());
        boolean includeInactive = false;

        current = EmployeeDAO.findByFilter(keyword, pos, includeInactive);

        model.setRowCount(0);
        int stt = 1;
        for (Employee e : current) {
            String phone = supportsPhone ? safe(e.getPhone()) : "-";

            model.addRow(new Object[]{
                    stt++,
                    e.getFullName(),
                    safe(e.getEmail()),
                    phone,
                    e.getPosition()
            });
        }

        if (model.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private Employee getSelectedEmployee() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= current.size()) return null;
        Employee src = current.get(row);
        Employee e = new Employee(src.getId(), src.getFullName(), src.getUsername(), src.getEmail(), src.getPhone(), src.getPosition(), src.getSalary(), src.getHireDate(), src.isActive());
        return e;
    }

    private Employee showEmployeeDialog(Employee existing) {
        JTextField fullName = new JTextField();
        JTextField email = new JTextField();
        JTextField phone = new JTextField();
        JTextField username = new JTextField();
        boolean lockManagerPosition = existing != null
                && existing.getPosition() != null
                && existing.getPosition().trim().equalsIgnoreCase("manager");
        JComboBox<String> position = lockManagerPosition
                ? new JComboBox<>(new String[]{"manager"})
                : new JComboBox<>(new String[]{"staff", "cashier", "chef", "waiter"});
        JCheckBox active = new JCheckBox("Đang hoạt động");
        active.setSelected(true);

        if (existing != null) {
            fullName.setText(existing.getFullName());
            email.setText(existing.getEmail());
            phone.setText(existing.getPhone());
            username.setText(existing.getUsername());
            if (existing.getPosition() != null) position.setSelectedItem(existing.getPosition());
            active.setSelected(existing.isActive());
        }
        position.setEnabled(!lockManagerPosition);

        username.setEnabled(supportsUsername);
        phone.setEnabled(supportsPhone);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(new JLabel("Họ tên:"));
        p.add(fullName);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(new JLabel("Email:"));
        p.add(email);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        if (supportsPhone) {
            p.add(new JLabel("SĐT:"));
            p.add(phone);
            p.add(Box.createRigidArea(new Dimension(0, 6)));
        }
        if (supportsUsername) {
            p.add(new JLabel("Username:"));
            p.add(username);
            p.add(Box.createRigidArea(new Dimension(0, 6)));
        }
        p.add(new JLabel("Vị trí:"));
        p.add(position);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(active);

        int res = JOptionPane.showConfirmDialog(this, p, existing == null ? "Thêm nhân viên" : "Sửa nhân viên",
                JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return null;

        String fn = fullName.getText() == null ? "" : fullName.getText().trim();
        String em = email.getText() == null ? "" : email.getText().trim();
        String pos = position.getSelectedItem() == null ? "" : String.valueOf(position.getSelectedItem());

        if (fn.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Họ tên không được để trống");
            return null;
        }
        if (em.isEmpty() || !em.contains("@")) {
            JOptionPane.showMessageDialog(this, "Email không hợp lệ");
            return null;
        }
        if (pos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vị trí bắt buộc");
            return null;
        }

        Integer ignoreId = existing == null ? null : existing.getId();
        if (EmployeeDAO.emailExists(em, ignoreId)) {
            JOptionPane.showMessageDialog(this, "Email đã tồn tại");
            return null;
        }

        String un = null;
        if (supportsUsername) {
            un = username.getText() == null ? null : username.getText().trim();
            if (un != null && !un.isEmpty()) {
                if (EmployeeDAO.usernameExists(un, ignoreId)) {
                    JOptionPane.showMessageDialog(this, "Username đã tồn tại");
                    return null;
                }
            } else {
                un = null;
            }
        }

        String ph = null;
        if (supportsPhone) {
            ph = phone.getText() == null ? null : phone.getText().trim();
            if (ph != null && !ph.isEmpty()) {
                String digits = ph.replaceAll("[^0-9]", "");
                if (digits.length() < 9 || digits.length() > 11) {
                    JOptionPane.showMessageDialog(this, "SĐT không hợp lệ");
                    return null;
                }
            } else {
                ph = null;
            }
        }

        // Mặc định lương = null và ngày vào = null
        Double sal = null;
        LocalDate hd = null;

        Employee out = existing == null ? new Employee(0, fn, un, em, ph, pos, sal, hd, active.isSelected()) : existing;
        out.setFullName(fn);
        out.setEmail(em);
        out.setPhone(ph);
        out.setUsername(un);
        out.setPosition(pos);
        out.setSalary(sal);
        out.setHireDate(hd);
        out.setActive(active.isSelected());

        return out;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
