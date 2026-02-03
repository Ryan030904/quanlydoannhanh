package com.pos.ui;

import com.pos.Session;
import com.pos.dao.EmployeeDAO;
import com.pos.dao.IngredientDAO;
import com.pos.db.DBConnection;
import com.pos.model.Employee;
import com.pos.model.Ingredient;
import com.pos.model.User;
import com.pos.util.CurrencyUtil;
import com.pos.ui.components.*;
import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ImportGoodsFrame extends JFrame {
	private final AppFrame parent;

	private JTextField invoiceNoField;
	private JTextField supplierField;
	private JTextField importDateField;
	private JTextField totalField;
	private JTextField employeeField;
	private Integer selectedEmployeeId;

	private DefaultTableModel model;
	private JTable table;
	private boolean recalculating;

	public ImportGoodsFrame(AppFrame parent) {
		this.parent = parent;

		setTitle("Nhập hàng");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(1050, 680);
		setLocationRelativeTo(parent);

		if (this.parent != null) {
			this.parent.setVisible(false);
		}

		JPanel root = new JPanel(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
		root.setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM));
		root.setOpaque(false);
		setContentPane(root);

		JPanel form = buildFormPanel();
		JPanel table = buildTablePanel();
		JPanel actions = buildActionsPanel();

		root.add(form, BorderLayout.NORTH);
		root.add(table, BorderLayout.CENTER);
		root.add(actions, BorderLayout.SOUTH);

		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosed(java.awt.event.WindowEvent e) {
				if (ImportGoodsFrame.this.parent != null) {
					ImportGoodsFrame.this.parent.setVisible(true);
				}
			}
		});

		setVisible(true);
	}

	private JPanel buildFormPanel() {
		CardPanel panel = new CardPanel(new GridBagLayout());
		panel.setShadowSize(2);
		panel.setRadius(UIConstants.RADIUS_MD);
		panel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

		invoiceNoField = new JTextField();
		supplierField = new JTextField();
		importDateField = new JTextField();
		totalField = new JTextField();
		employeeField = new JTextField();

		invoiceNoField.setFont(UIConstants.FONT_BODY);
		supplierField.setFont(UIConstants.FONT_BODY);
		importDateField.setFont(UIConstants.FONT_BODY);
		totalField.setFont(UIConstants.FONT_BODY);
		employeeField.setFont(UIConstants.FONT_BODY);

		totalField.setEditable(false);

		String defaultNo = "HDN" + (System.currentTimeMillis() % 100000);
		invoiceNoField.setText(defaultNo);
		importDateField.setText(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
		importDateField.setEditable(false);
		totalField.setText(CurrencyUtil.format(0));

		User u = Session.getCurrentUser();
		if (u != null) {
			employeeField.setText(u.getUsername());
			selectedEmployeeId = u.getId();
		}

		JButton pickSupplierBtn = smallPickerButton();
		JButton pickEmployeeBtn = smallPickerButton();

		pickSupplierBtn.addActionListener(e -> pickSupplier());
		pickEmployeeBtn.addActionListener(e -> pickEmployee());

		GridBagConstraints g = new GridBagConstraints();
		g.insets = new Insets(UIConstants.SPACING_XS, UIConstants.SPACING_SM, UIConstants.SPACING_XS, UIConstants.SPACING_SM);
		g.fill = GridBagConstraints.HORIZONTAL;

		// Left column
		g.gridx = 0;
		g.gridy = 0;
		g.weightx = 0;
		panel.add(label("Mã hóa đơn nhập", UIConstants.FONT_BODY), g);

		g.gridx = 1;
		g.gridy = 0;
		g.weightx = 1;
		panel.add(invoiceNoField, g);

		g.gridx = 0;
		g.gridy = 1;
		g.weightx = 0;
		panel.add(label("Nhà cung cấp", UIConstants.FONT_BODY), g);

		g.gridx = 1;
		g.gridy = 1;
		g.weightx = 1;
		panel.add(wrapFieldWithPicker(supplierField, pickSupplierBtn), g);

		g.gridx = 0;
		g.gridy = 2;
		g.weightx = 0;
		panel.add(label("Ngày nhập", UIConstants.FONT_BODY), g);

		g.gridx = 1;
		g.gridy = 2;
		g.weightx = 1;
		panel.add(importDateField, g);

		// Right column
		g.gridx = 2;
		g.gridy = 0;
		g.weightx = 0;
		panel.add(label("Tổng tiền", UIConstants.FONT_BODY), g);

		g.gridx = 3;
		g.gridy = 0;
		g.weightx = 1;
		panel.add(totalField, g);

		g.gridx = 2;
		g.gridy = 1;
		g.weightx = 0;
		panel.add(label("Nhân viên", UIConstants.FONT_BODY), g);

		g.gridx = 3;
		g.gridy = 1;
		g.weightx = 1;
		panel.add(wrapFieldWithPicker(employeeField, pickEmployeeBtn), g);

		// Spacing row
		g.gridx = 0;
		g.gridy = 3;
		g.weightx = 0;
		g.weighty = 1;
		g.gridwidth = 4;
		g.fill = GridBagConstraints.BOTH;
		panel.add(Box.createVerticalStrut(1), g);

		return panel;
	}

	private JPanel buildTablePanel() {
		JPanel panel = new JPanel(new BorderLayout(8, 8));
		panel.setOpaque(false);

		model = new DefaultTableModel(new Object[]{"Mã NL", "Tên nguyên liệu", "Đơn vị", "Giá nhập", "Số lượng", "Thành tiền"}, 0) {
			public boolean isCellEditable(int row, int col) {
				return col == 3 || col == 4;
			}
		};

		table = new JTable(model);
		ModernTableStyle.apply(table, true);

		CardPanel tableCard = new CardPanel(new BorderLayout());
		tableCard.setShadowSize(2);
		tableCard.setRadius(UIConstants.RADIUS_LG);
		tableCard.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

		JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
		scroll.getViewport().setBackground(Color.WHITE);
		tableCard.add(scroll, BorderLayout.CENTER);
		panel.add(tableCard, BorderLayout.CENTER);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		actions.setOpaque(false);
		ModernButton pickIngredient = new ModernButton("Chọn nguyên liệu", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.SMALL);
		ModernButton addRow = new ModernButton("Thêm dòng", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
		ModernButton removeRow = new ModernButton("Xóa dòng", ModernButton.ButtonType.DANGER, ModernButton.ButtonSize.SMALL);
		pickIngredient.setPreferredSize(new Dimension(130, 32));
		addRow.setPreferredSize(new Dimension(100, 32));
		removeRow.setPreferredSize(new Dimension(100, 32));
		actions.add(pickIngredient);
		actions.add(addRow);
		actions.add(removeRow);
		panel.add(actions, BorderLayout.NORTH);

		pickIngredient.addActionListener(e -> pickIngredientForSelectedRow());

		addRow.addActionListener(e -> {
			model.addRow(new Object[]{"", "", "", 0, 1, 0});
		});

		removeRow.addActionListener(e -> {
			int row = table.getSelectedRow();
			if (row >= 0) {
				model.removeRow(row);
				recalcTotal();
			}
		});

		model.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (e != null && e.getType() == TableModelEvent.UPDATE && e.getColumn() == 5) {
					return;
				}
				if (recalculating) return;
				recalcTotal();
			}
		});

		model.addRow(new Object[]{"", "", "", 0, 1, 0});

		return panel;
	}

	private JPanel buildActionsPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		panel.setOpaque(false);
		ModernButton cancelBtn = new ModernButton("Hủy", ModernButton.ButtonType.OUTLINE, ModernButton.ButtonSize.SMALL);
		ModernButton saveBtn = new ModernButton("Lưu phiếu nhập", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
		cancelBtn.setPreferredSize(new Dimension(80, 32));
		saveBtn.setPreferredSize(new Dimension(140, 32));
		panel.add(cancelBtn);
		panel.add(saveBtn);

		cancelBtn.addActionListener(e -> {
			dispose();
		});
		saveBtn.addActionListener(e -> doSave());
		return panel;
	}

	private void recalcTotal() {
		if (recalculating) return;
		recalculating = true;
		try {
		double total = 0;
		for (int r = 0; r < model.getRowCount(); r++) {
			double price = parseDouble(model.getValueAt(r, 3));
			double qty = parseDouble(model.getValueAt(r, 4));
			double line = price * qty;
			double currentLine = parseDouble(model.getValueAt(r, 5));
			if (Math.abs(currentLine - line) > 0.000001d) {
				model.setValueAt(line, r, 5);
			}
			total += line;
		}
		totalField.setText(CurrencyUtil.format(total));
		} finally {
			recalculating = false;
		}
	}

	private void pickSupplier() {
		String current = supplierField.getText() == null ? "" : supplierField.getText().trim();
		String v = JOptionPane.showInputDialog(this, "Nhập tên nhà cung cấp", current);
		if (v != null) {
			supplierField.setText(v.trim());
		}
	}

	private void pickEmployee() {
		List<Employee> employees = EmployeeDAO.findAllActive();
		if (employees.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Không có danh sách nhân viên (employees)");
			return;
		}
		Employee e = showEmployeePickerDialog(employees);
		if (e != null) {
			employeeField.setText(e.getUsername() != null ? e.getUsername() : e.getFullName());
			selectedEmployeeId = e.getId();
		}
	}

	private Employee showEmployeePickerDialog(List<Employee> employees) {
		JDialog dialog = new JDialog(this, "Nhân viên", Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		JPanel root = new JPanel(new BorderLayout(UIConstants.SPACING_SM, UIConstants.SPACING_SM));
		root.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

		JLabel title = new JLabel("Chọn nhân viên");
		title.setFont(UIConstants.FONT_BODY_BOLD);
		title.setForeground(UIConstants.NEUTRAL_700);
		root.add(title, BorderLayout.NORTH);

		ModernComboBox<Employee> combo = new ModernComboBox<>(employees.toArray(new Employee[0]));
		combo.setFont(UIConstants.FONT_BODY);
		combo.setPreferredSize(new Dimension(320, UIConstants.INPUT_HEIGHT_SM));
		root.add(combo, BorderLayout.CENTER);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SPACING_SM, 0));
		actions.setOpaque(false);
		ModernButton okBtn = new ModernButton("Chọn", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
		ModernButton cancelBtn = new ModernButton("Hủy", ModernButton.ButtonType.OUTLINE, ModernButton.ButtonSize.SMALL);
		okBtn.setPreferredSize(new Dimension(80, 32));
		cancelBtn.setPreferredSize(new Dimension(80, 32));
		actions.add(cancelBtn);
		actions.add(okBtn);
		root.add(actions, BorderLayout.SOUTH);

		final Employee[] picked = new Employee[1];
		okBtn.addActionListener(e -> {
			Object v = combo.getSelectedItem();
			picked[0] = v instanceof Employee ? (Employee) v : null;
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.setContentPane(root);
		dialog.pack();
		dialog.setResizable(false);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
		return picked[0];
	}

	private void pickIngredientForSelectedRow() {
		int row = table.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(this, "Chọn một dòng để gán nguyên liệu");
			return;
		}
		List<Ingredient> ingredients = IngredientDAO.findAll();
		if (ingredients.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Không có nguyên liệu (ingredients)");
			return;
		}
		Object selected = JOptionPane.showInputDialog(this, "Chọn nguyên liệu", "Nguyên liệu",
				JOptionPane.PLAIN_MESSAGE, null, ingredients.toArray(), ingredients.get(0));
		if (selected instanceof Ingredient) {
			Ingredient ing = (Ingredient) selected;
			model.setValueAt(String.valueOf(ing.getId()), row, 0);
			model.setValueAt(ing.getName(), row, 1);
			model.setValueAt(ing.getUnit(), row, 2);
			Double price = ing.getUnitPrice();
			if (price != null) {
				model.setValueAt(price, row, 3);
			}
			recalcTotal();
		}
	}

	private void doSave() {
		String supplier = supplierField.getText() == null ? "" : supplierField.getText().trim();
		if (supplier.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập Nhà cung cấp");
			return;
		}
		int employeeId = selectedEmployeeId != null ? selectedEmployeeId : 0;
		if (employeeId <= 0) {
			JOptionPane.showMessageDialog(this, "Không xác định được nhân viên");
			return;
		}

		boolean hasLine = false;
		for (int r = 0; r < model.getRowCount(); r++) {
			String idStr = model.getValueAt(r, 0) == null ? "" : String.valueOf(model.getValueAt(r, 0)).trim();
			double qty = parseDouble(model.getValueAt(r, 4));
			if (!idStr.isEmpty() && qty > 0) {
				hasLine = true;
				break;
			}
		}
		if (!hasLine) {
			JOptionPane.showMessageDialog(this, "Phiếu nhập chưa có dòng nguyên liệu hợp lệ");
			return;
		}

		int ok = JOptionPane.showConfirmDialog(this, "Lưu phiếu nhập kho?", "Xác nhận", JOptionPane.YES_NO_OPTION);
		if (ok != JOptionPane.YES_OPTION) return;

		String invoiceNo = invoiceNoField.getText() == null ? "" : invoiceNoField.getText().trim();
		String importDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
		importDateField.setText(importDate);
		String reason = (invoiceNo.isEmpty() ? "" : ("Invoice:" + invoiceNo + " ")) + "Supplier:" + supplier +
				(importDate.isEmpty() ? "" : (" Date:" + importDate));

		try (Connection c = DBConnection.getConnection()) {
			c.setAutoCommit(false);
			try {
				boolean canLog = hasTable(c, "inventory_transactions");
				boolean hasTxDateCol = canLog && hasColumn(c, "inventory_transactions", "transaction_date");
				LocalDate importLocalDate = LocalDate.now();
				for (int r = 0; r < model.getRowCount(); r++) {
					String idStr = model.getValueAt(r, 0) == null ? "" : String.valueOf(model.getValueAt(r, 0)).trim();
					if (idStr.isEmpty()) continue;
					int ingredientId;
					try {
						ingredientId = Integer.parseInt(idStr);
					} catch (NumberFormatException nfe) {
						throw new SQLException("Mã nguyên liệu không hợp lệ ở dòng " + (r + 1));
					}
					double unitPrice = parseDouble(model.getValueAt(r, 3));
					double qty = parseDouble(model.getValueAt(r, 4));
					if (qty <= 0) continue;
					double totalCost = unitPrice * qty;

					if (canLog) {
						insertInventoryTransaction(c, hasTxDateCol, importLocalDate, ingredientId, qty, unitPrice, totalCost, reason, employeeId);
					}
					boolean updated = IngredientDAO.addStock(c, ingredientId, qty);
					if (!updated) {
						throw new SQLException("Không cập nhật được tồn kho nguyên liệu id=" + ingredientId);
					}
				}

				c.commit();
				JOptionPane.showMessageDialog(this, "Lưu phiếu nhập thành công");
				dispose();
			} catch (Exception ex) {
				try { c.rollback(); } catch (SQLException ignored) {}
				JOptionPane.showMessageDialog(this, "Không thể lưu phiếu nhập: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
			} finally {
				try { c.setAutoCommit(true); } catch (SQLException ignored) {}
			}
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, "Lỗi kết nối CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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

	private boolean hasColumn(Connection c, String tableName, String columnName) {
		try {
			DatabaseMetaData md = c.getMetaData();
			try (ResultSet rs = md.getColumns(c.getCatalog(), null, tableName, columnName)) {
				if (rs.next()) return true;
			}
			try (ResultSet rs = md.getColumns(c.getCatalog(), null, tableName.toUpperCase(), columnName.toUpperCase())) {
				if (rs.next()) return true;
			}
		} catch (SQLException ignored) {
		}
		return false;
	}

	private LocalDate parseDateOrToday(String isoDate) {
		String s = isoDate == null ? "" : isoDate.trim();
		if (s.isEmpty()) return LocalDate.now();
		try {
			return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (Exception ignored) {
			return LocalDate.now();
		}
	}

	private void insertInventoryTransaction(Connection c, boolean hasTxDateCol, LocalDate importDate,
								  int ingredientId, double qty, double unitPrice, double totalCost,
								  String reason, int employeeId) throws SQLException {
		String sql = hasTxDateCol
				? "INSERT INTO inventory_transactions (transaction_date, ingredient_id, transaction_type, quantity, unit_price, total_cost, reason, employee_id) VALUES (?, ?, 'import', ?, ?, ?, ?, ?)"
				: "INSERT INTO inventory_transactions (ingredient_id, transaction_type, quantity, unit_price, total_cost, reason, employee_id) VALUES (?, 'import', ?, ?, ?, ?, ?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			int idx = 1;
			if (hasTxDateCol) {
				LocalDate d = importDate != null ? importDate : LocalDate.now();
				ps.setTimestamp(idx++, Timestamp.valueOf(d.atStartOfDay()));
			}
			ps.setInt(idx++, ingredientId);
			ps.setDouble(idx++, qty);
			ps.setDouble(idx++, unitPrice);
			ps.setDouble(idx++, totalCost);
			ps.setString(idx++, reason);
			ps.setInt(idx, employeeId);
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
			String cleaned = s.replaceAll("[^0-9\\.\\-]", "");
			if (cleaned.isEmpty()) return 0;
			try {
				return Double.parseDouble(cleaned);
			} catch (NumberFormatException ex2) {
				return 0;
			}
		}
	}

	private JLabel label(String text, Font font) {
		JLabel l = new JLabel(text);
		l.setFont(font);
		return l;
	}

	private JButton smallPickerButton() {
		JButton b = new JButton("...");
		b.setPreferredSize(new Dimension(34, 28));
		b.setFocusPainted(false);
		return b;
	}

	private JPanel wrapFieldWithPicker(JTextField field, JButton picker) {
		JPanel p = new JPanel(new BorderLayout(6, 0));
		p.setOpaque(false);
		p.add(field, BorderLayout.CENTER);
		p.add(picker, BorderLayout.EAST);
		return p;
	}
}
