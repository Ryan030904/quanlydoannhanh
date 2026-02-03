package com.pos.ui;

import com.pos.service.PermissionService;
import com.pos.ui.components.CardPanel;
import com.pos.ui.components.ModernButton;
import com.pos.ui.components.ModernTableStyle;
import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

public class PermissionsManagementPanel extends JPanel {
    private final Runnable onDataChanged;

	private static final String[] ALL_TABS = {
			"Bán hàng", "Nhập hàng", "Món ăn", "Danh mục", "Nguyên liệu", "Công thức", "Hóa đơn",
			"Hóa đơn nhập", "Khuyến mãi", "Khách hàng", "Nhân viên", "Nhà cung cấp",
			"Tài khoản", "Phân quyền", "Thống kê"
	};

    // Danh sách mã quyền và tên quyền
    private static class Permission {
        String code;
        String name;
        String description;
        
        Permission(String code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }
    }

    private final List<Permission> permissions = new ArrayList<>();
    
    private JTextField searchField;
    private DefaultTableModel model;
    private JTable table;

    public PermissionsManagementPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;

        // Khởi tạo danh sách phân quyền (có thể mở rộng sau này)
        permissions.add(new Permission("PQ0", "Admin", "Toàn quyền - 14 tab"));
        permissions.add(new Permission("PQ2", "Nhân viên", "Bán hàng, Nhập hàng, Hóa đơn, Khách hàng"));

        setLayout(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        setOpaque(false);
        setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

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

        JLabel filterIcon = new JLabel("Tim kiem");
        filterIcon.setFont(UIConstants.FONT_BODY_BOLD);
        filterIcon.setForeground(UIConstants.PRIMARY_700);
        top.add(filterIcon);

        // TextField nhập từ khóa
        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setPreferredSize(new Dimension(200, UIConstants.INPUT_HEIGHT_SM));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        top.add(searchField);

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
                "Mã quyền", "Tên quyền"
        }, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(model);
        ModernTableStyle.apply(table, true);

        // Tối ưu column widths theo hình 2
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);  // Mã quyền
        table.getColumnModel().getColumn(1).setPreferredWidth(400);  // Tên quyền

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

	private void editPermissionTabs() {
		int idx = table.getSelectedRow();
		if (idx < 0) {
			JOptionPane.showMessageDialog(this, "Chọn một mã quyền để cấp quyền");
			return;
		}

		String code = (String) model.getValueAt(idx, 0);
		if (code == null) return;
		String c = code.trim().toUpperCase();
		if (c.equals("PQ0")) {
			JOptionPane.showMessageDialog(this, "PQ0 (Admin) luôn toàn quyền và không cần cấp quyền tab");
			return;
		}

		Set<String> allowed = PermissionService.loadTabsForPermissionCode(c);
		JPanel grid = new JPanel(new GridLayout(0, 2, 10, 8));
		grid.setBorder(new EmptyBorder(10, 10, 10, 10));
		List<JCheckBox> boxes = new ArrayList<>();
		for (String tab : ALL_TABS) {
			JCheckBox cb = new JCheckBox(tab);
			cb.setSelected(allowed.contains(tab));
			boxes.add(cb);
			grid.add(cb);
		}

		JScrollPane scroll = new JScrollPane(grid);
		scroll.setPreferredSize(new Dimension(520, 360));
		int res = JOptionPane.showConfirmDialog(this, scroll, "Cấp quyền tab cho " + c,
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (res != JOptionPane.OK_OPTION) return;

		StringJoiner sj = new StringJoiner(",");
		for (int i = 0; i < boxes.size(); i++) {
			if (boxes.get(i).isSelected()) sj.add(ALL_TABS[i]);
		}
		String csv = sj.toString();
		if (csv.trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Phải chọn ít nhất 1 tab");
			return;
		}

		if (saveTabsToProperties(c, csv)) {
			JOptionPane.showMessageDialog(this, "Đã lưu phân quyền. Vui lòng đăng xuất/đăng nhập lại để áp dụng.");
			if (onDataChanged != null) onDataChanged.run();
		}
	}

    private void refreshTable() {
        String keyword = searchField.getText() == null ? null : searchField.getText().trim().toLowerCase();

        model.setRowCount(0);

        for (Permission p : permissions) {
            boolean ok = true;
            if (keyword != null && !keyword.isEmpty()) {
                // Tìm theo cả mã quyền và tên quyền
                ok = p.code.toLowerCase().contains(keyword) || p.name.toLowerCase().contains(keyword);
            }

            if (ok) {
                model.addRow(new Object[]{
                    p.code,
                    p.name
                });
            }
        }

        if (model.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private JPanel buildActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SPACING_SM, 0));
        panel.setOpaque(false);

        ModernButton detailBtn = new ModernButton("Xem chi tiết", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        ModernButton editBtn = new ModernButton("Cấp quyền tab", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.SMALL);
        detailBtn.setPreferredSize(new Dimension(120, 32));
        editBtn.setPreferredSize(new Dimension(130, 32));
        panel.add(detailBtn);
        panel.add(editBtn);

        detailBtn.addActionListener(e -> showPermissionDetail());
		editBtn.addActionListener(e -> editPermissionTabs());

        return panel;
    }

	private boolean saveTabsToProperties(String code, String csv) {
		try {
			File f = new File("permissions.properties");
			Path path = f.toPath();
			List<String> lines = new ArrayList<>();
			if (f.exists() && f.isFile()) {
				lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			}

			boolean updatedPerm = false;
			boolean updatedRoleStaff = false;
			boolean updatedRoleManager = false;
			String permKey = "perm." + code + ".tabs=";
			String roleStaffKey = "role.Staff.tabs=";
			String roleManagerKey = "role.Manager.tabs=";
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				if (line != null && line.startsWith(permKey)) {
					lines.set(i, permKey + csv);
					updatedPerm = true;
					continue;
				}
				if (code.equalsIgnoreCase("PQ2") && line != null && line.startsWith(roleStaffKey)) {
					lines.set(i, roleStaffKey + csv);
					updatedRoleStaff = true;
					continue;
				}
				if (code.equalsIgnoreCase("PQ0") && line != null && line.startsWith(roleManagerKey)) {
					lines.set(i, roleManagerKey + csv);
					updatedRoleManager = true;
				}
			}
			if (!updatedPerm) lines.add(permKey + csv);
			if (code.equalsIgnoreCase("PQ2") && !updatedRoleStaff) lines.add(roleStaffKey + csv);
			if (code.equalsIgnoreCase("PQ0") && !updatedRoleManager) lines.add(roleManagerKey + csv);

			Files.write(path, lines, StandardCharsets.UTF_8);
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "Không thể lưu permissions.properties", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

    private void showPermissionDetail() {
        int idx = table.getSelectedRow();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một mã quyền để xem chi tiết");
            return;
        }

        String code = (String) model.getValueAt(idx, 0);
        Permission selected = null;
        for (Permission p : permissions) {
            if (p.code.equals(code)) {
                selected = p;
                break;
            }
        }

        if (selected == null) return;

        // Lấy danh sách tab được phép truy cập
        Set<String> allowedTabs = PermissionService.loadTabsForPermissionCode(code);
        
        StringBuilder detail = new StringBuilder();
        detail.append("Mã quyền: ").append(selected.code).append("\n");
        detail.append("Tên quyền: ").append(selected.name).append("\n");
        detail.append("Mô tả: ").append(selected.description).append("\n\n");
        detail.append("Các tab được truy cập:\n");
        
        if (allowedTabs.isEmpty()) {
            detail.append("  (Không có quyền truy cập)\n");
        } else {
            int count = 1;
            for (String tab : allowedTabs) {
                detail.append("  ").append(count++).append(". ").append(tab).append("\n");
            }
        }

        JTextArea textArea = new JTextArea(detail.toString());
        textArea.setEditable(false);
        textArea.setFont(UIConstants.FONT_BODY);
        textArea.setRows(15);
        textArea.setColumns(40);

        JScrollPane scrollPane = new JScrollPane(textArea);
        JOptionPane.showMessageDialog(this, scrollPane, "Chi tiết phân quyền", JOptionPane.INFORMATION_MESSAGE);
    }
}
