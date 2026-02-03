package com.pos.ui;

import com.pos.Session;
import com.pos.dao.EmployeeDAO;
import com.pos.dao.IngredientDAO;
import com.pos.dao.SupplierDAO;
import com.pos.db.DBConnection;
import com.pos.model.Employee;
import com.pos.model.Ingredient;
import com.pos.model.Supplier;
import com.pos.model.User;
import com.pos.util.CurrencyUtil;
import com.pos.ui.components.CardPanel;
import com.pos.ui.components.DatePicker;
import com.pos.ui.components.ModernButton;
import com.pos.ui.components.ModernTableStyle;
import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel Nhập hàng - Layout cố định, không header, hiển thị đầy đủ
 */
public class ImportGoodsPanel extends JPanel {
    private final AppFrame parent;

    // Form fields
    private JTextField invoiceNoField;
    private JComboBox<Object> supplierCombo;

    private DatePicker importDatePicker;
    private JTextField employeeField;
    private JTextArea noteArea;
    private Integer selectedEmployeeId;

    // Summary labels
    private JLabel itemCountLabel;
    private JLabel totalQtyLabel;
    private JLabel totalAmountLabel;

    // Ingredient picker
    private JTextField searchIngredientField;
    private DefaultTableModel ingredientModel;
    private JTable ingredientTable;
    private List<Ingredient> allIngredients = new ArrayList<>();

    // Import details table
    private DefaultTableModel detailModel;
    private JTable detailTable;
    private boolean recalculating;

    public ImportGoodsPanel(AppFrame parent) {
        this.parent = parent;

        setLayout(new BorderLayout(UIConstants.SPACING_SM, UIConstants.SPACING_SM));
        setOpaque(false);
        setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM));

        // === TOP: Form + Picker (fixed height 260px) ===
        JPanel topPanel = new JPanel(new BorderLayout(UIConstants.SPACING_SM, 0));
        topPanel.setOpaque(false);
        topPanel.setPreferredSize(new Dimension(0, 260));
        topPanel.setMinimumSize(new Dimension(0, 260));
        topPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        // Left: Form (fixed width 360px)
        JPanel formPanel = buildFormPanel();
        formPanel.setPreferredSize(new Dimension(360, 260));
        formPanel.setMinimumSize(new Dimension(360, 260));

        // Right: Ingredient picker
        JPanel pickerPanel = buildIngredientPicker();

        topPanel.add(formPanel, BorderLayout.WEST);
        topPanel.add(pickerPanel, BorderLayout.CENTER);

        // === CENTER: Detail table ===
        JPanel detailPanel = buildDetailPanel();

        // === BOTTOM: Actions (fixed height 50px) ===
        JPanel actionsPanel = buildActionsPanel();
        actionsPanel.setPreferredSize(new Dimension(0, 50));

        // Main layout - không dùng SplitPane
        add(topPanel, BorderLayout.NORTH);
        add(detailPanel, BorderLayout.CENTER);
        add(actionsPanel, BorderLayout.SOUTH);

        // Load data
        loadSuppliers();
        loadIngredients();
        initDefaultValues();
    }

    /**
     * Panel Form thông tin phiếu - không header
     */
    private JPanel buildFormPanel() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM)
        ));

        // Form content với GridBagLayout
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Mã phiếu
        gbc.gridx = 0; gbc.gridy = row;
        form.add(createLabel("Mã phiếu:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        invoiceNoField = createTextField();
        form.add(invoiceNoField, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;

        // Ngày nhập
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(createLabel("Ngày nhập:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        importDatePicker = new DatePicker(LocalDate.now());
        importDatePicker.setPreferredSize(new Dimension(140, 28));
        form.add(importDatePicker, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;

        // Nhà cung cấp
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(createLabel("Nhà cung cấp:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        supplierCombo = new JComboBox<>();
        supplierCombo.setFont(UIConstants.FONT_BODY);
        supplierCombo.setPreferredSize(new Dimension(180, 28));
        form.add(supplierCombo, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;

        // Nhân viên
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(createLabel("Nhân viên:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JPanel empPanel = new JPanel(new BorderLayout(2, 0));
        empPanel.setOpaque(false);
        employeeField = createTextField();
        employeeField.setEditable(false);
        employeeField.setBackground(UIConstants.NEUTRAL_100);
        employeeField.setFocusable(false);
        employeeField.setRequestFocusEnabled(false);
        ModernButton pickEmpBtn = new ModernButton("...", ModernButton.ButtonType.GHOST, ModernButton.ButtonSize.SMALL);
        pickEmpBtn.setPreferredSize(new Dimension(28, 28));
        pickEmpBtn.setEnabled(false);
        pickEmpBtn.setVisible(false);
        empPanel.add(employeeField, BorderLayout.CENTER);
        empPanel.add(pickEmpBtn, BorderLayout.EAST);
        form.add(empPanel, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;

        // Ghi chú
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(createLabel("Ghi chú:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        noteArea = new JTextArea(2, 15);
        noteArea.setFont(UIConstants.FONT_CAPTION);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_300));
        JScrollPane noteScroll = new JScrollPane(noteArea);
        noteScroll.setBorder(null);
        noteScroll.setPreferredSize(new Dimension(180, 40));
        form.add(noteScroll, gbc);
        gbc.weighty = 0;

        card.add(form, BorderLayout.CENTER);

        // Summary panel ở dưới
        JPanel summary = buildSummaryPanel();
        card.add(summary, BorderLayout.SOUTH);

        return card;
    }

    /**
     * Panel tổng kết nhỏ gọn
     */
    private JPanel buildSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 4, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_XS, 0, 0, 0));

        itemCountLabel = new JLabel("0 mặt hàng", SwingConstants.CENTER);
        totalQtyLabel = new JLabel("SL: 0", SwingConstants.CENTER);
        totalAmountLabel = new JLabel("0 ₫", SwingConstants.CENTER);

        itemCountLabel.setFont(UIConstants.FONT_CAPTION);
        totalQtyLabel.setFont(UIConstants.FONT_CAPTION);
        totalAmountLabel.setFont(UIConstants.FONT_BODY_BOLD);

        itemCountLabel.setForeground(UIConstants.INFO);
        totalQtyLabel.setForeground(UIConstants.WARNING_DARK);
        totalAmountLabel.setForeground(UIConstants.SUCCESS);

        JPanel box1 = createMiniBox(itemCountLabel);
        JPanel box2 = createMiniBox(totalQtyLabel);
        JPanel box3 = createMiniBox(totalAmountLabel);

        panel.add(box1);
        panel.add(box2);
        panel.add(box3);

        return panel;
    }

    private JPanel createMiniBox(JLabel label) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(UIConstants.NEUTRAL_100);
        box.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_300));
        box.add(label, BorderLayout.CENTER);
        return box;
    }

    /**
     * Panel chọn nguyên liệu - không header
     */
    private JPanel buildIngredientPicker() {
        JPanel card = new JPanel(new BorderLayout(0, UIConstants.SPACING_XS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM)
        ));

        // Search bar
        JPanel searchPanel = new JPanel(new BorderLayout(4, 0));
        searchPanel.setOpaque(false);
        JLabel searchLabel = new JLabel("Tìm NL:");
        searchLabel.setFont(UIConstants.FONT_CAPTION);
        searchIngredientField = new JTextField();
        searchIngredientField.setFont(UIConstants.FONT_BODY);
        searchIngredientField.setPreferredSize(new Dimension(150, 26));
        searchIngredientField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300),
            new EmptyBorder(2, 6, 2, 6)
        ));
        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchIngredientField, BorderLayout.CENTER);
        card.add(searchPanel, BorderLayout.NORTH);

        // Table nguyên liệu
        ingredientModel = new DefaultTableModel(new Object[]{"Mã", "Tên nguyên liệu", "ĐV", "Tồn", "Giá"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        ingredientTable = new JTable(ingredientModel);
        ModernTableStyle.apply(ingredientTable, true);
        ingredientTable.setRowHeight(24);

        ingredientTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        ingredientTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        ingredientTable.getColumnModel().getColumn(2).setPreferredWidth(45);
        ingredientTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        ingredientTable.getColumnModel().getColumn(4).setPreferredWidth(70);

        JScrollPane scroll = new JScrollPane(ingredientTable);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        btnPanel.setOpaque(false);

        ModernButton addBtn = new ModernButton("+ Thêm", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        ModernButton addAllBtn = new ModernButton("Thêm tất cả", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.SMALL);
        addBtn.setPreferredSize(new Dimension(80, 28));
        addAllBtn.setPreferredSize(new Dimension(90, 28));

        addBtn.addActionListener(e -> addSelectedIngredient());
        addAllBtn.addActionListener(e -> addAllFilteredIngredients());

        btnPanel.add(addBtn);
        btnPanel.add(addAllBtn);
        card.add(btnPanel, BorderLayout.SOUTH);

        // Double click to add
        ingredientTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) addSelectedIngredient();
            }
        });

        // Search filter
        searchIngredientField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterIngredients(); }
            public void removeUpdate(DocumentEvent e) { filterIngredients(); }
            public void changedUpdate(DocumentEvent e) { filterIngredients(); }
        });

        return card;
    }

    /**
     * Panel chi tiết phiếu nhập - không header
     */
    private JPanel buildDetailPanel() {
        JPanel card = new JPanel(new BorderLayout(0, UIConstants.SPACING_XS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM)
        ));

        // Actions bar
        JPanel actionsBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actionsBar.setOpaque(false);

        ModernButton removeBtn = new ModernButton("Xóa dòng", ModernButton.ButtonType.DANGER, ModernButton.ButtonSize.SMALL);
        ModernButton clearBtn = new ModernButton("Xóa tất cả", ModernButton.ButtonType.WARNING, ModernButton.ButtonSize.SMALL);
        removeBtn.setPreferredSize(new Dimension(85, 26));
        clearBtn.setPreferredSize(new Dimension(85, 26));

        removeBtn.addActionListener(e -> removeSelectedDetail());
        clearBtn.addActionListener(e -> clearAllDetails());

        actionsBar.add(removeBtn);
        actionsBar.add(clearBtn);
        card.add(actionsBar, BorderLayout.NORTH);

        // Table chi tiết
        detailModel = new DefaultTableModel(new Object[]{
            "Mã", "Tên nguyên liệu", "ĐV", "Giá nhập", "Số lượng", "Thành tiền"
        }, 0) {
            public boolean isCellEditable(int row, int col) {
                return col == 4; // Chỉ cho phép sửa cột Số lượng
            }
        };

        detailTable = new JTable(detailModel);
        ModernTableStyle.apply(detailTable, true);
        detailTable.setDefaultEditor(Object.class, null);
        detailTable.getTableHeader().setReorderingAllowed(false);
        detailTable.setRowSelectionAllowed(true);
        detailTable.setColumnSelectionAllowed(false);
        detailTable.setCellSelectionEnabled(false);
        detailTable.setRowHeight(26);

        detailTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        detailTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        detailTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        detailTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        detailTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        detailTable.getColumnModel().getColumn(5).setPreferredWidth(110);

        // Renderer hiển thị VND cho cột Giá nhập (3) và Thành tiền (5)
        javax.swing.table.DefaultTableCellRenderer vndRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                double val = parseDouble(value);
                setText(CurrencyUtil.format(val));
                setHorizontalAlignment(SwingConstants.RIGHT);
                return c;
            }
        };
        detailTable.getColumnModel().getColumn(3).setCellRenderer(vndRenderer);
        detailTable.getColumnModel().getColumn(5).setCellRenderer(vndRenderer);

        // Renderer cho cột Số lượng (4) - căn giữa
        javax.swing.table.DefaultTableCellRenderer qtyRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                double val = parseDouble(value);
                setText(String.valueOf((int) val));
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        detailTable.getColumnModel().getColumn(4).setCellRenderer(qtyRenderer);

        // Custom editor cho cột Số lượng - chỉ cho phép nhập số
        JTextField qtyEditor = new JTextField();
        qtyEditor.setHorizontalAlignment(JTextField.CENTER);
        qtyEditor.setFont(UIConstants.FONT_BODY);
        // Chỉ cho phép nhập số
        qtyEditor.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != java.awt.event.KeyEvent.VK_BACK_SPACE && c != java.awt.event.KeyEvent.VK_DELETE) {
                    e.consume(); // Không cho nhập ký tự không phải số
                    return;
                }

                // Không cho nhập số 0 ở đầu (nhưng vẫn cho 10, 20...)
                if (c == '0') {
                    String text = qtyEditor.getText() == null ? "" : qtyEditor.getText();
                    int selStart = qtyEditor.getSelectionStart();
                    int selEnd = qtyEditor.getSelectionEnd();
                    boolean replacingAll = !text.isEmpty() && selStart == 0 && selEnd == text.length();
                    boolean emptyAfterReplace = text.isEmpty() || replacingAll;
                    if (emptyAfterReplace) {
                        e.consume();
                    }
                }
            }
        });
        DefaultCellEditor qtyCellEditor = new DefaultCellEditor(qtyEditor) {
            @Override
            public boolean stopCellEditing() {
                Object v = getCellEditorValue();
                String s = v == null ? "" : String.valueOf(v).trim();
                int n;
                try {
                    n = s.isEmpty() ? 1 : Integer.parseInt(s);
                } catch (NumberFormatException ex) {
                    n = 1;
                }
                if (n <= 0) n = 1;
                qtyEditor.setText(String.valueOf(n));
                return super.stopCellEditing();
            }
        };
        qtyCellEditor.setClickCountToStart(1); // 1 click để edit
        detailTable.getColumnModel().getColumn(4).setCellEditor(qtyCellEditor);

        JScrollPane scroll = new JScrollPane(detailTable);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);

        // Table listener
        detailModel.addTableModelListener(e -> {
            if (!recalculating) recalcTotal();
        });

        return card;
    }

    /**
     * Panel actions - cố định chiều cao
     */
    private JPanel buildActionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_XS, 0, 0, 0));

        // Tips
        JLabel tips = new JLabel("Double-click để thêm nhanh | Click vào ô Số lượng để nhập số trực tiếp");
        tips.setFont(UIConstants.FONT_CAPTION);
        tips.setForeground(UIConstants.NEUTRAL_500);
        panel.add(tips, BorderLayout.WEST);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SPACING_SM, 0));
        btnPanel.setOpaque(false);

        ModernButton cancelBtn = new ModernButton("Hủy", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.MEDIUM);
        ModernButton saveBtn = new ModernButton("Lưu phiếu nhập", ModernButton.ButtonType.SUCCESS, ModernButton.ButtonSize.MEDIUM);
        cancelBtn.setPreferredSize(new Dimension(90, 36));
        saveBtn.setPreferredSize(new Dimension(130, 36));

        cancelBtn.addActionListener(e -> {
            if (parent != null) parent.navigateToTab("Nguyên liệu");
        });
        saveBtn.addActionListener(e -> doSave());

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        panel.add(btnPanel, BorderLayout.EAST);

        return panel;
    }

    // ========== Helper methods ==========

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_CAPTION);
        l.setForeground(UIConstants.NEUTRAL_700);
        return l;
    }

    private JTextField createTextField() {
        JTextField tf = new JTextField();
        tf.setFont(UIConstants.FONT_BODY);
        tf.setPreferredSize(new Dimension(180, 26));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300),
            new EmptyBorder(2, 6, 2, 6)
        ));
        return tf;
    }

    private void initDefaultValues() {
        String defaultNo = "HDN-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + (System.currentTimeMillis() % 1000);
        invoiceNoField.setText(defaultNo);

        User u = Session.getCurrentUser();
        if (u != null) {
            employeeField.setText(u.getUsername());
            selectedEmployeeId = u.getId();
        }
        updateSummary();
    }

    private void loadSuppliers() {
        supplierCombo.removeAllItems();
        supplierCombo.addItem("-- Chọn NCC --");
        if (SupplierDAO.supportsSuppliers()) {
            for (Supplier s : SupplierDAO.findByFilter("", false)) {
                supplierCombo.addItem(s);
            }
        }
        // Khi thay đổi NCC thì lọc lại nguyên liệu theo NCC đó
        supplierCombo.addActionListener(e -> filterIngredients());
    }

    private void loadIngredients() {
        allIngredients = IngredientDAO.findAll();
        filterIngredients();
    }

    private void filterIngredients() {
        String keyword = searchIngredientField.getText() == null ? "" : searchIngredientField.getText().toLowerCase().trim();
        ingredientModel.setRowCount(0);

        String selectedSupplierName = null;
        Object selectedSupplier = supplierCombo == null ? null : supplierCombo.getSelectedItem();
        if (selectedSupplier instanceof Supplier) {
            String n = ((Supplier) selectedSupplier).getName();
            if (n != null && !n.trim().isEmpty()) selectedSupplierName = n.trim();
        }

        for (Ingredient ing : allIngredients) {
            boolean matchSupplier = true;
            if (selectedSupplierName != null) {
                String ingSup = ing == null ? null : ing.getSupplier();
                matchSupplier = ingSup != null && ingSup.trim().equalsIgnoreCase(selectedSupplierName);
            }
            boolean matchKeyword = keyword.isEmpty() ||
                (ing.getName() != null && ing.getName().toLowerCase().contains(keyword)) ||
                String.valueOf(ing.getId()).contains(keyword);

            if (matchSupplier && matchKeyword) {
                ingredientModel.addRow(new Object[]{
                    ing.getId(),
                    ing.getName(),
                    ing.getUnit(),
                    CurrencyUtil.formatQuantity(ing.getCurrentStock()),
                    ing.getUnitPrice() != null ? CurrencyUtil.format(ing.getUnitPrice()) : "0"
                });
            }
        }
    }

    private boolean isSupplierSelected() {
        return supplierCombo.getSelectedIndex() > 0 && supplierCombo.getSelectedItem() instanceof Supplier;
    }

    private void addSelectedIngredient() {
        // Dừng editing trước khi thêm để commit giá trị hiện tại
        if (detailTable.isEditing()) {
            detailTable.getCellEditor().stopCellEditing();
        }
        
        int row = ingredientTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn nguyên liệu cần thêm");
            return;
        }

        int ingId = (int) ingredientModel.getValueAt(row, 0);
        String name = (String) ingredientModel.getValueAt(row, 1);
        String unit = (String) ingredientModel.getValueAt(row, 2);
        double price = parsePrice(String.valueOf(ingredientModel.getValueAt(row, 4)));

        // Check exists
        for (int i = 0; i < detailModel.getRowCount(); i++) {
            if (String.valueOf(detailModel.getValueAt(i, 0)).equals(String.valueOf(ingId))) {
                double qty = parseDouble(detailModel.getValueAt(i, 4)) + 1;
                detailModel.setValueAt((int) qty, i, 4);
                recalcTotal();
                return;
            }
        }

        detailModel.addRow(new Object[]{ingId, name, unit, price, 1, price});
        recalcTotal();
    }

    private void addAllFilteredIngredients() {
        if (ingredientModel.getRowCount() == 0) return;

        int confirm = JOptionPane.showConfirmDialog(this,
            "Thêm " + ingredientModel.getRowCount() + " nguyên liệu?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        for (int i = 0; i < ingredientModel.getRowCount(); i++) {
            int ingId = (int) ingredientModel.getValueAt(i, 0);
            String name = (String) ingredientModel.getValueAt(i, 1);
            String unit = (String) ingredientModel.getValueAt(i, 2);
            double price = parsePrice(String.valueOf(ingredientModel.getValueAt(i, 4)));

            boolean exists = false;
            for (int j = 0; j < detailModel.getRowCount(); j++) {
                if (String.valueOf(detailModel.getValueAt(j, 0)).equals(String.valueOf(ingId))) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                detailModel.addRow(new Object[]{ingId, name, unit, price, 1, price});
            }
        }
        recalcTotal();
    }

    private void removeSelectedDetail() {
        // Hủy editing trước khi xóa để tránh lỗi hiển thị
        if (detailTable.isEditing()) {
            detailTable.getCellEditor().cancelCellEditing();
        }
        int row = detailTable.getSelectedRow();
        if (row >= 0) {
            detailModel.removeRow(row);
            recalcTotal();
        }
    }

    private void clearAllDetails() {
        if (detailModel.getRowCount() == 0) return;
        // Hủy editing trước khi xóa
        if (detailTable.isEditing()) {
            detailTable.getCellEditor().cancelCellEditing();
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Xóa tất cả?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            detailModel.setRowCount(0);
            recalcTotal();
        }
    }

    private void pickEmployee() {
        List<Employee> employees = EmployeeDAO.findAllActive();
        if (employees.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có nhân viên");
            return;
        }
        Object selected = JOptionPane.showInputDialog(this, "Chọn nhân viên", "Nhân viên",
                JOptionPane.PLAIN_MESSAGE, null, employees.toArray(), employees.get(0));
        if (selected instanceof Employee) {
            Employee e = (Employee) selected;
            employeeField.setText(e.getUsername() != null ? e.getUsername() : e.getFullName());
            selectedEmployeeId = e.getId();
        }
    }

    private void recalcTotal() {
        if (recalculating) return;
        recalculating = true;

        try {
            double total = 0;
            double totalQty = 0;
            int itemCount = detailModel.getRowCount();

            for (int r = 0; r < detailModel.getRowCount(); r++) {
                double price = parseDouble(detailModel.getValueAt(r, 3));
                double qty = parseDouble(detailModel.getValueAt(r, 4));
                double line = price * qty;

                double currentLine = parseDouble(detailModel.getValueAt(r, 5));
                if (Math.abs(currentLine - line) > 0.001) {
                    detailModel.setValueAt(line, r, 5);
                }

                total += line;
                totalQty += qty;
            }

            updateSummaryValues(itemCount, totalQty, total);
        } finally {
            recalculating = false;
        }
    }

    private void updateSummary() {
        updateSummaryValues(0, 0, 0);
    }

    private void updateSummaryValues(int items, double qty, double total) {
        itemCountLabel.setText(items + " mặt hàng");
        totalQtyLabel.setText("SL: " + String.format("%.0f", qty));
        totalAmountLabel.setText(CurrencyUtil.format(total));
    }

    private void doSave() {
        String supplierName = "";
        int supplierId = 0;
        if (supplierCombo.getSelectedIndex() > 0 && supplierCombo.getSelectedItem() instanceof Supplier) {
            Supplier sup = (Supplier) supplierCombo.getSelectedItem();
            supplierName = sup.getName();
            supplierId = sup.getId();
        }

        // NCC không bắt buộc - chỉ dùng để lọc nguyên liệu
        // Nếu không chọn NCC thì ghi nhận là "Không xác định"
        if (supplierName.isEmpty()) {
            supplierName = "Không xác định";
        }

        int employeeId = selectedEmployeeId != null ? selectedEmployeeId : 0;
        if (employeeId <= 0) {
            JOptionPane.showMessageDialog(this, "Không xác định được nhân viên", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (detailModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Chưa có nguyên liệu nào", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean hasValidLine = false;
        for (int r = 0; r < detailModel.getRowCount(); r++) {
            if (parseDouble(detailModel.getValueAt(r, 4)) > 0) {
                hasValidLine = true;
                break;
            }
        }

        if (!hasValidLine) {
            JOptionPane.showMessageDialog(this, "Cần ít nhất 1 dòng với SL > 0", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Lưu phiếu nhập?\n• NCC: " + supplierName + "\n• " + detailModel.getRowCount() + " mặt hàng\n• Tổng: " + totalAmountLabel.getText(),
            "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String invoiceNo = invoiceNoField.getText().trim();
        String importDate = importDatePicker.getText();
        String note = noteArea.getText().trim();
        String reason = "Invoice:" + invoiceNo + " | Supplier:" + supplierName +
                (importDate.isEmpty() ? "" : " | Date:" + importDate) +
                (note.isEmpty() ? "" : " | Note:" + note);

        final int finalSupplierId = supplierId;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                boolean canLog = hasTable(c, "inventory_transactions");

                for (int r = 0; r < detailModel.getRowCount(); r++) {
                    String idStr = String.valueOf(detailModel.getValueAt(r, 0)).trim();
                    if (idStr.isEmpty()) continue;

                    int ingredientId = Integer.parseInt(idStr);
                    double unitPrice = parseDouble(detailModel.getValueAt(r, 3));
                    double qty = parseDouble(detailModel.getValueAt(r, 4));
                    if (qty <= 0) continue;

                    if (canLog) {
                        insertInventoryTransaction(c, ingredientId, qty, unitPrice, unitPrice * qty, reason, employeeId, finalSupplierId);
                    }

                    if (!IngredientDAO.addStock(c, ingredientId, qty)) {
                        throw new SQLException("Không cập nhật được tồn kho id=" + ingredientId);
                    }
                }

                c.commit();
                
                // Hiển thị hóa đơn nhập hàng
                showImportInvoice(invoiceNo, supplierName, importDate, note, employeeId);
                
                // Reset form và chuyển về tab Nguyên liệu
                resetForm();
                if (parent != null) parent.navigateToTab("Hóa đơn nhập");
            } catch (Exception ex) {
                c.rollback();
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean hasTable(Connection c, String tableName) {
        try {
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getTables(c.getCatalog(), null, tableName, new String[]{"TABLE"})) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = md.getTables(c.getCatalog(), null, tableName.toUpperCase(), new String[]{"TABLE"})) {
                if (rs.next()) return true;
            }
        } catch (SQLException ignored) {}
        return false;
    }

    private void insertInventoryTransaction(Connection c, int ingredientId, double qty, double unitPrice, double totalCost,
                                           String reason, int employeeId, int supplierId) throws SQLException {
        String sql = "INSERT INTO inventory_transactions (ingredient_id, transaction_type, quantity, unit_price, total_cost, reason, employee_id, supplier_id) VALUES (?, 'import', ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            ps.setDouble(2, qty);
            ps.setDouble(3, unitPrice);
            ps.setDouble(4, totalCost);
            ps.setString(5, reason);
            ps.setInt(6, employeeId);
            if (supplierId > 0) {
                ps.setInt(7, supplierId);
            } else {
                ps.setNull(7, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
        }
    }

    private double parseDouble(Object v) {
        if (v == null) return 0;
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            String cleaned = s.replaceAll("[^0-9.\\-]", "");
            try { return Double.parseDouble(cleaned); } catch (NumberFormatException e) { return 0; }
        }
    }

    private double parsePrice(String s) {
        if (s == null) return 0;
        String cleaned = s.replaceAll("[^0-9.\\-]", "");
        try { return Double.parseDouble(cleaned); } catch (NumberFormatException e) { return 0; }
    }

    /**
     * Hiển thị hóa đơn nhập hàng sau khi lưu thành công
     */
    private void showImportInvoice(String invoiceNo, String supplierName, String importDate, String note, int employeeId) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Hóa đơn nhập hàng", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(550, 600);
        dialog.setLocationRelativeTo(this);

        JPanel invoicePanel = new JPanel();
        invoicePanel.setLayout(new BoxLayout(invoicePanel, BoxLayout.Y_AXIS));
        invoicePanel.setBackground(Color.WHITE);
        invoicePanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Header
        JLabel storeName = new JLabel("CỬA HÀNG ĐỒ ĂN NHANH");
        storeName.setFont(new Font("SansSerif", Font.BOLD, 18));
        storeName.setAlignmentX(Component.CENTER_ALIGNMENT);
        invoicePanel.add(storeName);

        JLabel storeAddress = new JLabel("Địa chỉ: 123 Đường ABC, Quận XYZ");
        storeAddress.setFont(new Font("SansSerif", Font.PLAIN, 11));
        storeAddress.setAlignmentX(Component.CENTER_ALIGNMENT);
        invoicePanel.add(storeAddress);

        invoicePanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Title
        JLabel title = new JLabel("PHIẾU NHẬP HÀNG");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        invoicePanel.add(title);

        invoicePanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Info
        JPanel infoPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        infoPanel.setOpaque(false);
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        
        infoPanel.add(new JLabel("Số phiếu:")); infoPanel.add(new JLabel(invoiceNo));
        infoPanel.add(new JLabel("Ngày nhập:")); infoPanel.add(new JLabel(importDate));
        infoPanel.add(new JLabel("Nhà cung cấp:")); infoPanel.add(new JLabel(supplierName));
        infoPanel.add(new JLabel("Nhân viên:")); infoPanel.add(new JLabel(employeeField.getText()));
        if (note != null && !note.isEmpty()) {
            infoPanel.add(new JLabel("Ghi chú:")); infoPanel.add(new JLabel(note));
        }
        invoicePanel.add(infoPanel);

        invoicePanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Separator
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        invoicePanel.add(sep);

        invoicePanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Table
        String[] cols = {"STT", "Nguyên liệu", "ĐV", "SL", "Đơn giá", "Thành tiền"};
        DefaultTableModel invoiceModel = new DefaultTableModel(cols, 0);
        double total = 0;
        for (int i = 0; i < detailModel.getRowCount(); i++) {
            double qty = parseDouble(detailModel.getValueAt(i, 4));
            double price = parseDouble(detailModel.getValueAt(i, 3));
            double lineTotal = parseDouble(detailModel.getValueAt(i, 5));
            total += lineTotal;
            invoiceModel.addRow(new Object[]{
                i + 1,
                detailModel.getValueAt(i, 1),
                detailModel.getValueAt(i, 2),
                (int) qty,
                CurrencyUtil.format(price),
                CurrencyUtil.format(lineTotal)
            });
        }
        JTable invoiceTable = new JTable(invoiceModel);
        invoiceTable.setRowHeight(25);
        invoiceTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        invoiceTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        
        JScrollPane tableScroll = new JScrollPane(invoiceTable);
        tableScroll.setPreferredSize(new Dimension(500, 200));
        tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        invoicePanel.add(tableScroll);

        invoicePanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Total
        final double grandTotal = total;
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalPanel.setOpaque(false);
        JLabel totalLabel = new JLabel("TỔNG CỘNG: " + CurrencyUtil.format(grandTotal));
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalLabel.setForeground(UIConstants.SUCCESS);
        totalPanel.add(totalLabel);
        invoicePanel.add(totalPanel);

        invoicePanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Footer
        JLabel thanks = new JLabel("Đã nhập kho thành công!");
        thanks.setFont(new Font("SansSerif", Font.BOLD, 14));
        thanks.setForeground(UIConstants.SUCCESS);
        thanks.setAlignmentX(Component.CENTER_ALIGNMENT);
        invoicePanel.add(thanks);

        JScrollPane scroll = new JScrollPane(invoicePanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        dialog.add(scroll, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBackground(Color.WHITE);
        
        ModernButton printBtn = new ModernButton("In phiếu", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.MEDIUM);
        ModernButton closeBtn = new ModernButton("Đóng", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.MEDIUM);
        
        printBtn.addActionListener(e -> {
            // Xuất PDF demo
            com.pos.util.PdfExportUtil.exportTableToPdfDemo(invoiceTable, "PHIẾU NHẬP HÀNG",
                "Số phiếu: " + invoiceNo + "\nNgày: " + importDate + "\nNCC: " + supplierName,
                "TỔNG CỘNG: " + CurrencyUtil.format(grandTotal),
                "PhieuNhap_" + invoiceNo, dialog);
        });
        closeBtn.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(printBtn);
        btnPanel.add(closeBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    /**
     * Reset form sau khi lưu thành công
     */
    private void resetForm() {
        // Clear detail table
        detailModel.setRowCount(0);
        
        // Generate new invoice number
        String newNo = "HDN-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + (System.currentTimeMillis() % 1000);
        invoiceNoField.setText(newNo);
        
        // Reset supplier
        supplierCombo.setSelectedIndex(0);
        
        // Clear note
        noteArea.setText("");
        
        // Update summary
        updateSummary();
    }
}
