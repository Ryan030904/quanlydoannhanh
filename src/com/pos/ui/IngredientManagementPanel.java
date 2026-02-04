package com.pos.ui;

import com.pos.dao.IngredientDAO;
import com.pos.dao.SupplierDAO;
import com.pos.db.DBConnection;
import com.pos.model.Ingredient;
import com.pos.model.Supplier;
import com.pos.ui.components.CardPanel;
import com.pos.ui.components.ModernButton;
import com.pos.ui.components.ModernTableStyle;
import com.pos.ui.theme.UIConstants;
import com.pos.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class IngredientManagementPanel extends JPanel {
    private final Runnable onDataChanged;

    private final DefaultTableModel model;
    private final JTable table;

    private final JTextField searchField;
    private final JComboBox<String> supplierCombo;
    private final JCheckBox lowStockOnly;

    private List<Ingredient> currentIngredients = new ArrayList<>();

    private final boolean supportsMin;
    private final boolean supportsActive;

    public IngredientManagementPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        this.supportsMin = IngredientDAO.supportsMinStockLevel();
        this.supportsActive = IngredientDAO.supportsIsActive();

        setLayout(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        setOpaque(false);
        setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM));

        // === FILTER BAR ===
        CardPanel top = new CardPanel(new BorderLayout(UIConstants.SPACING_MD, 0));
        top.setShadowSize(2);
        top.setRadius(UIConstants.RADIUS_MD);
        top.setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_MD, UIConstants.SPACING_SM, UIConstants.SPACING_MD));

        JPanel filters = new JPanel(new WrapLayout(FlowLayout.LEFT, UIConstants.SPACING_SM, UIConstants.SPACING_XS));
        filters.setOpaque(false);

        JLabel filterIcon = new JLabel("Bộ lọc");
        filterIcon.setFont(UIConstants.FONT_BODY_BOLD);
        filterIcon.setForeground(UIConstants.PRIMARY_700);
        filters.add(filterIcon);

        JLabel searchLabel = new JLabel("Tìm:");
        searchLabel.setFont(UIConstants.FONT_BODY);
        searchLabel.setForeground(UIConstants.NEUTRAL_700);
        filters.add(searchLabel);

        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setPreferredSize(new Dimension(200, UIConstants.INPUT_HEIGHT_SM));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        filters.add(searchField);

        JLabel supplierLabel = new JLabel("Nhà cung cấp:");
        supplierLabel.setFont(UIConstants.FONT_BODY);
        supplierLabel.setForeground(UIConstants.NEUTRAL_700);
        filters.add(supplierLabel);

        supplierCombo = new JComboBox<>();
        supplierCombo.setFont(UIConstants.FONT_BODY);
        supplierCombo.setPreferredSize(new Dimension(180, UIConstants.INPUT_HEIGHT_SM));
        filters.add(supplierCombo);

        lowStockOnly = new JCheckBox("Chỉ hiển thị sắp hết");
        lowStockOnly.setFont(UIConstants.FONT_BODY);
        lowStockOnly.setForeground(UIConstants.NEUTRAL_600);
        lowStockOnly.setOpaque(false);
        filters.add(lowStockOnly);

        ModernButton addBtn = new ModernButton("Thêm", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        ModernButton editBtn = new ModernButton("Sửa", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.SMALL);
        ModernButton deleteBtn = new ModernButton("Xóa", ModernButton.ButtonType.DANGER, ModernButton.ButtonSize.SMALL);
        ModernButton importBtn = new ModernButton("Nhập kho", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        addBtn.setPreferredSize(new Dimension(84, 32));
        editBtn.setPreferredSize(new Dimension(74, 32));
        deleteBtn.setPreferredSize(new Dimension(74, 32));
        importBtn.setPreferredSize(new Dimension(92, 32));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SPACING_SM, 0));
        actions.setOpaque(false);
        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(deleteBtn);
        actions.add(importBtn);

        top.add(filters, BorderLayout.CENTER);
        top.add(actions, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // === TABLE ===
        CardPanel tableCard = new CardPanel(new BorderLayout());
        tableCard.setShadowSize(2);
        tableCard.setRadius(UIConstants.RADIUS_LG);
        tableCard.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

        model = new DefaultTableModel(new Object[]{
                "STT", "Mã", "Tên", "Đơn vị", "Tồn hiện tại", "Tồn tối thiểu", "Giá nhập", "Nhà cung cấp"
        }, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(model);
        ModernTableStyle.apply(table, true);

        // Tối ưu column widths
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);   // STT
        table.getColumnModel().getColumn(1).setPreferredWidth(60);   // Mã
        table.getColumnModel().getColumn(2).setPreferredWidth(200);  // Tên
        table.getColumnModel().getColumn(3).setPreferredWidth(70);   // Đơn vị
        table.getColumnModel().getColumn(4).setPreferredWidth(100);  // Tồn hiện tại
        table.getColumnModel().getColumn(5).setPreferredWidth(100);  // Tồn tối thiểu
        table.getColumnModel().getColumn(6).setPreferredWidth(100);  // Giá nhập
        table.getColumnModel().getColumn(7).setPreferredWidth(180);  // Nhà cung cấp

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        tableCard.add(scroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        reloadSuppliers();
        refreshTable();

        supplierCombo.addActionListener(e -> refreshTable());
        lowStockOnly.addActionListener(e -> refreshTable());

        searchField.addActionListener(e -> refreshTable());
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { refreshTable(); }
        });

        addBtn.addActionListener(e -> {
            Ingredient ing = showIngredientDialog(null);
            if (ing != null) {
                if (IngredientDAO.create(ing)) {
                    reloadSuppliers();
                    refreshTable();
                    if (onDataChanged != null) onDataChanged.run();
                } else {
                    JOptionPane.showMessageDialog(this, "Không thể thêm nguyên liệu. Tên nguyên liệu có thể đã tồn tại.");
                }
            }
        });

        editBtn.addActionListener(e -> {
            Ingredient ing = getSelectedIngredientSnapshot();
            if (ing == null) {
                JOptionPane.showMessageDialog(this, "Chọn một nguyên liệu để sửa");
                return;
            }
            Ingredient edited = showIngredientDialog(ing);
            if (edited != null) {
                if (IngredientDAO.update(edited)) {
                    reloadSuppliers();
                    refreshTable();
                    if (onDataChanged != null) onDataChanged.run();
                }
            }
        });

        deleteBtn.addActionListener(e -> deleteSelectedIngredient());

        importBtn.addActionListener(e -> openImport());
    }

    private void reloadSuppliers() {
        Object prevSelObj = supplierCombo.getSelectedItem();
        String prevSel = prevSelObj == null ? null : String.valueOf(prevSelObj);
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
        m.addElement("Tất cả");
		if (SupplierDAO.supportsSuppliers()) {
			for (Supplier s : SupplierDAO.findByFilter("", false)) {
				if (s != null && s.getName() != null && !s.getName().trim().isEmpty()) {
					m.addElement(s.getName().trim());
				}
			}
		} else {
			for (String s : IngredientDAO.findAllSuppliers()) {
				m.addElement(s);
			}
		}
        supplierCombo.setModel(m);
        supplierCombo.setMaximumRowCount(20);
		if (prevSel != null && m.getIndexOf(prevSel) >= 0) {
			supplierCombo.setSelectedItem(prevSel);
		} else {
			supplierCombo.setSelectedIndex(0);
		}
    }

	public void onSuppliersChanged() {
		reloadSuppliers();
		refreshTable();
	}

    public void refreshTable() {
        model.setRowCount(0);

        String keyword = searchField.getText() == null ? null : searchField.getText().trim();
        Object selSupplier = supplierCombo.getSelectedItem();
        String supplier = selSupplier == null ? null : String.valueOf(selSupplier);
        boolean low = lowStockOnly.isSelected();

        currentIngredients = IngredientDAO.findByFilter(keyword, supplier, low, false);

        int stt = 1;
        for (Ingredient ing : currentIngredients) {
            String currentStockStr = CurrencyUtil.formatQuantity(ing.getCurrentStock());
            String minObj = supportsMin ? CurrencyUtil.formatQuantity(ing.getMinStockLevel()) : "-";
            String unitPriceStr = ing.getUnitPrice() == null ? "" : CurrencyUtil.format(ing.getUnitPrice());

            model.addRow(new Object[]{
                    stt++,
                    ing.getId(),
                    ing.getName(),
                    ing.getUnit(),
                    currentStockStr,
                    minObj,
                    unitPriceStr,
                    ing.getSupplier()
            });
        }
    }

    private Ingredient getSelectedIngredientSnapshot() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;

        int row = table.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= currentIngredients.size()) return null;
        Ingredient src = currentIngredients.get(row);
        return new Ingredient(src.getId(), src.getName(), src.getUnit(), src.getCurrentStock(), src.getMinStockLevel(), src.getUnitPrice(), src.getSupplierId(), src.getSupplier());
    }

    private Ingredient showIngredientDialog(Ingredient existing) {
        JTextField name = new JTextField();
        JTextField unit = new JTextField();
        JTextField min = new JTextField("0");
        JTextField unitPrice = new JTextField();
        JTextField supplier = new JTextField();

        if (existing != null) {
            name.setText(existing.getName());
            unit.setText(existing.getUnit());
            if (supportsMin) min.setText(CurrencyUtil.formatQuantity(existing.getMinStockLevel()));
            if (existing.getUnitPrice() != null) unitPrice.setText(String.valueOf(existing.getUnitPrice().longValue()));
            if (existing.getSupplier() != null) supplier.setText(existing.getSupplier());
        }

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row;
        p.add(new JLabel("Tên nguyên liệu:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        p.add(name, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        p.add(new JLabel("Đơn vị:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        p.add(unit, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;

        if (supportsMin) {
            row++;
            gbc.gridx = 0; gbc.gridy = row;
            p.add(new JLabel("Tồn tối thiểu:"), gbc);
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            p.add(min, gbc);
            gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        }

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        p.add(new JLabel("Giá nhập tham chiếu:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        p.add(unitPrice, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        p.add(new JLabel("Nhà cung cấp:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        p.add(supplier, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;

        int res = JOptionPane.showConfirmDialog(this, p, existing == null ? "Thêm nguyên liệu" : "Sửa nguyên liệu",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return null;

        String n = name.getText() == null ? "" : name.getText().trim();
        String u = unit.getText() == null ? "" : unit.getText().trim();
        if (n.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên nguyên liệu không được để trống");
            return null;
        }
        if (u.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Đơn vị không được để trống");
            return null;
        }

        double minVal = 0;
        if (supportsMin) {
            minVal = parseDouble(min.getText());
            if (minVal < 0) minVal = 0;
        }

        Double priceVal = null;
        String ps = unitPrice.getText() == null ? "" : unitPrice.getText().trim();
        if (!ps.isEmpty()) {
            try { priceVal = Double.parseDouble(ps.replaceAll("[^0-9\\.\\-]", "")); } catch (NumberFormatException ignored) {}
        }

        String sup = supplier.getText() == null ? null : supplier.getText().trim();
        if (sup != null && sup.isEmpty()) sup = null;

        Ingredient ing = existing == null ? new Ingredient(0, n, u, 0, minVal, priceVal, sup) : existing;
        ing.setName(n);
        ing.setUnit(u);
        ing.setMinStockLevel(minVal);
        ing.setUnitPrice(priceVal);
        ing.setSupplier(sup);
        ing.setSupplierId(null);

        return ing;
    }

    private void deleteSelectedIngredient() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một nguyên liệu để xóa");
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= currentIngredients.size()) return;
        int id = currentIngredients.get(row).getId();
        String name = currentIngredients.get(row).getName();
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Bạn có chắc muốn xóa nguyên liệu \"" + name + "\"?\nLưu ý: Xóa nguyên liệu có thể ảnh hưởng đến các công thức món ăn.",
            "Xác nhận xóa",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (confirm != JOptionPane.YES_OPTION) return;
        
        if (IngredientDAO.delete(id)) {
            JOptionPane.showMessageDialog(this, "Đã xóa nguyên liệu");
            refreshTable();
            if (onDataChanged != null) onDataChanged.run();
        } else {
            JOptionPane.showMessageDialog(this, "Không thể xóa nguyên liệu này");
        }
    }

    private void setSelectedActive(boolean active) {
        if (!supportsActive) return;
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một nguyên liệu");
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= currentIngredients.size()) return;
        int id = currentIngredients.get(row).getId();
        if (IngredientDAO.setStatus(id, active)) {
            refreshTable();
            if (onDataChanged != null) onDataChanged.run();
        }
    }

    private void adjustSelectedStock() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một nguyên liệu để điều chỉnh");
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= currentIngredients.size()) return;
        int id = currentIngredients.get(row).getId();
        String name = currentIngredients.get(row).getName();

        String s = JOptionPane.showInputDialog(this, "Nhập số lượng điều chỉnh (+/-)", "0");
        if (s == null) return;
        double delta;
        try {
            delta = Double.parseDouble(s.trim().replaceAll("[^0-9\\.\\-]", ""));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số lượng không hợp lệ");
            return;
        }
        if (delta == 0) return;

        int ok = JOptionPane.showConfirmDialog(this,
                "Xác nhận điều chỉnh tồn cho '" + name + "' (" + (delta > 0 ? "+" : "") + delta + ") ?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                boolean canLog = hasTable(c, "inventory_transactions");
                if (canLog) {
                    insertInventoryTransactionAdjustment(c, id, delta, "Manual adjustment");
                }
                IngredientDAO.adjustStock(c, id, delta);
                c.commit();
                refreshTable();
                if (onDataChanged != null) onDataChanged.run();
            } catch (Exception ex) {
                try { c.rollback(); } catch (SQLException ignored) {}
                JOptionPane.showMessageDialog(this, "Không thể điều chỉnh tồn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            } finally {
                try { c.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openImport() {
        java.awt.Window w = SwingUtilities.getWindowAncestor(this);
        AppFrame parent = w instanceof AppFrame ? (AppFrame) w : AppFrame.getInstance();
        if (parent != null) {
            parent.navigateToTab("Nhập hàng");
        }
    }

    private double parseDouble(Object v) {
        if (v == null) return 0;
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            String cleaned = s.replaceAll("[^0-9\\.\\-]", "");
            if (cleaned.isEmpty()) return 0;
            try {
                return Double.parseDouble(cleaned);
            } catch (NumberFormatException ex2) {
                return 0;
            }
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
        } catch (SQLException ignored) {
        }
        return false;
    }

    private void insertInventoryTransactionAdjustment(Connection c, int ingredientId, double delta, String reason) throws SQLException {
        String sql = "INSERT INTO inventory_transactions (ingredient_id, transaction_type, quantity, unit_price, total_cost, reason, employee_id) " +
                "VALUES (?, 'adjustment', ?, NULL, NULL, ?, NULL)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            ps.setDouble(2, delta);
            ps.setString(3, reason);
            ps.executeUpdate();
        }
    }

    private String getIsActiveLabelSafe(int ingredientId) {
        if (!supportsActive) return "Đang dùng";
        String sql = "SELECT is_active FROM ingredients WHERE ingredient_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 1 ? "Đang dùng" : "Ngừng dùng";
                }
            }
        } catch (SQLException ex) {
            return "";
        }
        return "";
    }
}
