package com.pos.ui;

import com.pos.Session;
import com.pos.dao.SupplierDAO;
import com.pos.model.Supplier;
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

public class SuppliersManagementPanel extends JPanel {
    private final Runnable onDataChanged;

    private final boolean supportsSuppliers;

    private JTextField searchField;

    private DefaultTableModel model;
    private JTable table;

    private List<Supplier> current = new ArrayList<>();

    public SuppliersManagementPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        this.supportsSuppliers = SupplierDAO.supportsSuppliers();

        setLayout(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        setOpaque(false);
        setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM));

        if (!supportsSuppliers) {
            JPanel p = new JPanel(new GridBagLayout());
            p.setOpaque(false);
            JLabel msg = new JLabel("Không tìm thấy bảng suppliers");
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

        JLabel searchLabel = new JLabel("Tìm");
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

        ModernButton filterBtn = new ModernButton("Lọc", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        ModernButton resetBtn = new ModernButton("Reset", ModernButton.ButtonType.GHOST, ModernButton.ButtonSize.SMALL);
        filterBtn.setPreferredSize(new Dimension(80, 32));
        resetBtn.setPreferredSize(new Dimension(80, 32));

        top.add(filterBtn);
        top.add(resetBtn);

        filterBtn.addActionListener(e -> refreshTable());
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });

        searchField.addActionListener(e -> refreshTable());
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { refreshTable(); }
        });

        return top;
    }

    private JPanel buildTable() {
        CardPanel panel = new CardPanel(new BorderLayout());
        panel.setShadowSize(2);
        panel.setRadius(UIConstants.RADIUS_LG);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

        model = new DefaultTableModel(new Object[]{
                "STT", "Tên", "SĐT", "Email", "Địa chỉ", "Ghi chú"
        }, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(model);
        ModernTableStyle.apply(table, true);

        // Resize columns to favor name/email/address visibility
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);   // Mã
        table.getColumnModel().getColumn(1).setPreferredWidth(200);  // Tên
        table.getColumnModel().getColumn(2).setPreferredWidth(120);  // SĐT
        table.getColumnModel().getColumn(3).setPreferredWidth(200);  // Email
        table.getColumnModel().getColumn(4).setPreferredWidth(200);  // Địa chỉ
        table.getColumnModel().getColumn(5).setPreferredWidth(160);  // Ghi chú

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActions() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_SM, 0, 0, 0));

        // Empty spacer
        panel.add(new JPanel() {{ setOpaque(false); }}, BorderLayout.WEST);

        // Right: Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SPACING_SM, 0));
        btns.setOpaque(false);

        ModernButton addBtn = new ModernButton("+ Thêm NCC", ModernButton.ButtonType.SUCCESS, ModernButton.ButtonSize.MEDIUM);
        ModernButton editBtn = new ModernButton("Sửa", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.MEDIUM);
        ModernButton deleteBtn = new ModernButton("Xóa", ModernButton.ButtonType.DANGER, ModernButton.ButtonSize.MEDIUM);
        addBtn.setPreferredSize(new Dimension(120, 36));
        editBtn.setPreferredSize(new Dimension(80, 36));
        deleteBtn.setPreferredSize(new Dimension(80, 36));

		boolean canMutate = PermissionService.canMutateTab(Session.getCurrentUser(), "Nhà cung cấp");
		if (!canMutate) {
			addBtn.setVisible(false);
			editBtn.setVisible(false);
			deleteBtn.setVisible(false);
		}

        btns.add(addBtn);
        btns.add(editBtn);
        btns.add(deleteBtn);
        panel.add(btns, BorderLayout.EAST);

        addBtn.addActionListener(e -> {
            Supplier s = showSupplierDialog(null);
            if (s != null) {
                if (SupplierDAO.create(s)) {
                    refreshTable();
                    if (onDataChanged != null) onDataChanged.run();
                }
            }
        });

        editBtn.addActionListener(e -> {
            Supplier s = getSelectedSupplier();
            if (s == null) {
                JOptionPane.showMessageDialog(this, "Chọn một nhà cung cấp để sửa");
                return;
            }
            Supplier edited = showSupplierDialog(s);
            if (edited != null) {
                if (SupplierDAO.update(edited)) {
                    refreshTable();
                    if (onDataChanged != null) onDataChanged.run();
                }
            }
        });

        deleteBtn.addActionListener(e -> {
            Supplier s = getSelectedSupplier();
            if (s == null) {
                JOptionPane.showMessageDialog(this, "Chọn một nhà cung cấp để xóa");
                return;
            }
            
            int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa nhà cung cấp \"" + s.getName() + "\"?\nLưu ý: Xóa NCC có thể ảnh hưởng đến dữ liệu nguyên liệu.",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (confirm != JOptionPane.YES_OPTION) return;
            
            if (SupplierDAO.delete(s.getId())) {
                JOptionPane.showMessageDialog(this, "Đã xóa nhà cung cấp");
                refreshTable();
                if (onDataChanged != null) onDataChanged.run();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể xóa nhà cung cấp này", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private void refreshTable() {
        String keyword = searchField.getText() == null ? null : searchField.getText().trim();
        boolean includeInactive = false;

        current = SupplierDAO.findByFilter(keyword, includeInactive);

        model.setRowCount(0);
        int stt = 1;
        for (Supplier s : current) {
            model.addRow(new Object[]{
                    stt++,
                    s.getName(),
                    s.getPhone(),
                    s.getEmail(),
                    s.getAddress(),
                    s.getNotes()
            });
        }

        if (model.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private Supplier getSelectedSupplier() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int row = table.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= current.size()) return null;
        Supplier src = current.get(row);
        return new Supplier(src.getId(), src.getName(), src.getPhone(), src.getEmail(), src.getAddress(), src.getNotes(), src.isActive());
    }

    private Supplier showSupplierDialog(Supplier existing) {
        JTextField name = new JTextField(25);
        JTextField phone = new JTextField(15);
        JTextField email = new JTextField(25);
        JTextField address = new JTextField(30);
        JTextArea notes = new JTextArea(3, 30);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);

        // Style fields
        name.setFont(UIConstants.FONT_BODY);
        phone.setFont(UIConstants.FONT_BODY);
        email.setFont(UIConstants.FONT_BODY);
        address.setFont(UIConstants.FONT_BODY);
        notes.setFont(UIConstants.FONT_BODY);

        if (existing != null) {
            name.setText(existing.getName());
            phone.setText(existing.getPhone());
            email.setText(existing.getEmail());
            address.setText(existing.getAddress());
            notes.setText(existing.getNotes());
        }

        // Build form panel with GridBagLayout for professional look
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Name (required)
        gbc.gridx = 0; gbc.gridy = row;
        JLabel nameLabel = new JLabel("Tên NCC (*):");
        nameLabel.setFont(UIConstants.FONT_BODY_BOLD);
        nameLabel.setForeground(UIConstants.NEUTRAL_700);
        p.add(nameLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        p.add(name, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;

        // Phone
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        JLabel phoneLabel = new JLabel("Số điện thoại:");
        phoneLabel.setFont(UIConstants.FONT_BODY);
        phoneLabel.setForeground(UIConstants.NEUTRAL_700);
        p.add(phoneLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        p.add(phone, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;

        // Email
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(UIConstants.FONT_BODY);
        emailLabel.setForeground(UIConstants.NEUTRAL_700);
        p.add(emailLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        p.add(email, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;

        // Address
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        JLabel addrLabel = new JLabel("Địa chỉ:");
        addrLabel.setFont(UIConstants.FONT_BODY);
        addrLabel.setForeground(UIConstants.NEUTRAL_700);
        p.add(addrLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        p.add(address, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;

        // Notes
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel notesLabel = new JLabel("Ghi chú:");
        notesLabel.setFont(UIConstants.FONT_BODY);
        notesLabel.setForeground(UIConstants.NEUTRAL_700);
        p.add(notesLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        JScrollPane notesScroll = new JScrollPane(notes);
        notesScroll.setPreferredSize(new Dimension(300, 70));
        p.add(notesScroll, gbc);

        String title = existing == null ? "➕ Thêm Nhà Cung Cấp" : "✏️ Sửa Nhà Cung Cấp";
        int res = JOptionPane.showConfirmDialog(this, p, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return null;

        String n = name.getText() == null ? "" : name.getText().trim();
        if (n.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên nhà cung cấp không được để trống!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String ph = phone.getText() == null ? "" : phone.getText().trim();
        if (!ph.isEmpty()) {
            String digits = ph.replaceAll("[^0-9]", "");
            if (digits.length() < 9 || digits.length() > 11) {
                JOptionPane.showMessageDialog(this, "Số điện thoại không hợp lệ (9-11 số)", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return null;
            }
        }

        String em = email.getText() == null ? "" : email.getText().trim();
        if (!em.isEmpty() && !em.contains("@")) {
            JOptionPane.showMessageDialog(this, "Email không hợp lệ", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        Supplier out = existing == null ? new Supplier() : existing;
        out.setName(n);
        out.setPhone(ph.isEmpty() ? null : ph);
        out.setEmail(em.isEmpty() ? null : em);
        out.setAddress(address.getText() == null ? null : address.getText().trim());
        out.setNotes(notes.getText() == null ? null : notes.getText().trim());

        return out;
    }
}
