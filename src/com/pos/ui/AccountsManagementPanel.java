package com.pos.ui;

import com.pos.dao.AccountsDAO;
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
import java.util.Set;
import java.util.StringJoiner;

public class AccountsManagementPanel extends JPanel {
    private final Runnable onDataChanged;

	private static final String[] ALL_TABS = {
			"Bán hàng", "Nhập hàng", "Món ăn", "Danh mục", "Nguyên liệu", "Công thức", "Hóa đơn",
			"Hóa đơn nhập", "Khuyến mãi", "Khách hàng", "Nhân viên", "Nhà cung cấp",
			"Tài khoản", "Phân quyền", "Thống kê"
	};

    private JTextField searchField;

    private DefaultTableModel model;
    private JTable table;

    private ModernButton createBtn;
    private ModernButton changePwdBtn;
    private ModernButton deleteBtn;

    private List<AccountsDAO.AccountRow> current = new ArrayList<>();

    public AccountsManagementPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        setOpaque(false);
        setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM));

        if (!AccountsDAO.supportsAccounts()) {
            JPanel p = new JPanel(new GridBagLayout());
            p.setOpaque(false);
            JLabel msg = new JLabel("Không hỗ trợ quản lý tài khoản (employees.username/password_hash)");
            msg.setFont(UIConstants.FONT_BODY);
            msg.setForeground(UIConstants.WARNING_DARK);
            p.add(msg);
            add(p, BorderLayout.CENTER);
            return;
        }

        add(buildTop(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);

        if (table != null) {
            table.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                updateActionButtons();
            });
        }

        refreshTable();
    }

    private JPanel buildTop() {
        CardPanel top = new CardPanel(new WrapLayout(FlowLayout.LEFT, UIConstants.SPACING_SM, UIConstants.SPACING_XS));
        top.setShadowSize(2);
        top.setRadius(UIConstants.RADIUS_MD);
        top.setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_MD, UIConstants.SPACING_SM, UIConstants.SPACING_MD));

        JLabel filterIcon = new JLabel("Tìm");
        filterIcon.setFont(UIConstants.FONT_BODY_BOLD);
        filterIcon.setForeground(UIConstants.PRIMARY_700);
        top.add(filterIcon);

        // TextField nhập từ khóa
        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setPreferredSize(new Dimension(260, UIConstants.INPUT_HEIGHT_SM));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        top.add(searchField);

        ModernButton filterBtn = new ModernButton("Lọc", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        filterBtn.setPreferredSize(new Dimension(80, 32));
        top.add(filterBtn);

        filterBtn.addActionListener(e -> refreshTable());

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
                "Tài khoản", "Mã nhân viên", "Mã quyền"
        }, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(model);
        ModernTableStyle.apply(table, true);

        // Tối ưu column widths theo hình 1
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);  // Tài khoản
        table.getColumnModel().getColumn(1).setPreferredWidth(150);  // Mã nhân viên
        table.getColumnModel().getColumn(2).setPreferredWidth(120);  // Mã quyền

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

        this.createBtn = new ModernButton("Tạo tài khoản", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        this.changePwdBtn = new ModernButton("Đổi mật khẩu", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.SMALL);
        this.deleteBtn = new ModernButton("Xóa", ModernButton.ButtonType.DANGER, ModernButton.ButtonSize.SMALL);
        this.createBtn.setPreferredSize(new Dimension(130, 32));
        this.changePwdBtn.setPreferredSize(new Dimension(120, 32));
        this.deleteBtn.setPreferredSize(new Dimension(80, 32));

        panel.add(this.createBtn);
        panel.add(this.changePwdBtn);
        panel.add(this.deleteBtn);

        this.createBtn.addActionListener(e -> createAccount());
        this.changePwdBtn.addActionListener(e -> changePassword());
        this.deleteBtn.addActionListener(e -> deleteAccount());

        return panel;
    }

    private void refreshTable() {
        String keyword = searchField.getText() == null ? null : searchField.getText().trim().toLowerCase();

        List<AccountsDAO.AccountRow> all = AccountsDAO.findAll();
        current = new ArrayList<>();

        for (AccountsDAO.AccountRow r : all) {
            if (!r.isActive()) continue;

            boolean ok = true;
            if (keyword != null && !keyword.isEmpty()) {
                String un = r.getUsername() == null ? "" : r.getUsername().toLowerCase();
                String empId = "NV" + String.format("%02d", r.getEmployeeId());
                String permCode = "";
                if (r.getUsername() != null && !r.getUsername().trim().isEmpty()) {
                    permCode = PermissionService.getPermissionCodeForAccount(r.getUsername(), r.getRole());
                }
                ok = un.contains(keyword) || empId.toLowerCase().contains(keyword) || permCode.toLowerCase().contains(keyword);
            }

            if (ok) current.add(r);
        }

        model.setRowCount(0);
        for (AccountsDAO.AccountRow r : current) {
            String username = safe(r.getUsername());
            String empCode = "NV" + String.format("%02d", r.getEmployeeId());
            String permCode = "";
            if (r.getUsername() != null && !r.getUsername().trim().isEmpty()) {
                permCode = PermissionService.getPermissionCodeForAccount(r.getUsername(), r.getRole());
            }

            model.addRow(new Object[]{
                    username.isEmpty() ? "(chưa có)" : username,
                    empCode,
                    permCode
            });
        }

        if (model.getRowCount() > 0) {
			int selectIdx = 0;
			for (int i = 0; i < current.size(); i++) {
				AccountsDAO.AccountRow rr = current.get(i);
				String un = rr.getUsername() == null ? "" : rr.getUsername().trim();
				if (!"admin".equalsIgnoreCase(un)) {
					selectIdx = i;
					break;
				}
			}
			table.setRowSelectionInterval(selectIdx, selectIdx);
        }

        updateActionButtons();

        if (onDataChanged != null) onDataChanged.run();
    }

    private void updateActionButtons() {
        if (createBtn == null || changePwdBtn == null || deleteBtn == null) return;
        AccountsDAO.AccountRow r = getSelectedRow();
        if (r == null) {
            createBtn.setEnabled(false);
            changePwdBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
            return;
        }

        String un = r.getUsername() == null ? "" : r.getUsername().trim();
        boolean isAdmin = "admin".equalsIgnoreCase(un);
        boolean hasUsername = !un.isEmpty();

        createBtn.setEnabled(!isAdmin && !hasUsername);
        changePwdBtn.setEnabled(!isAdmin && hasUsername);
        deleteBtn.setEnabled(!isAdmin && hasUsername);
    }

    private void createAccount() {
        AccountsDAO.AccountRow r = getSelectedRow();
        if (r == null) {
            JOptionPane.showMessageDialog(this, "Chọn một nhân viên");
            return;
        }

        String selUn = r.getUsername() == null ? "" : r.getUsername().trim();
        if ("admin".equalsIgnoreCase(selUn)) {
            JOptionPane.showMessageDialog(this,
                "Khong the tao/cap nhat tai khoan admin tu day.",
                "Bảo vệ Admin",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
		if (!selUn.isEmpty()) {
			JOptionPane.showMessageDialog(this,
				"Nhân viên này đã có tài khoản. Vui lòng dùng chức năng Đổi mật khẩu.",
				"Đã có tài khoản",
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}

        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        char defaultEcho = password.getEchoChar();
        JCheckBox showPw = new JCheckBox("Hiện");
        showPw.addActionListener(e -> password.setEchoChar(showPw.isSelected() ? (char) 0 : defaultEcho));

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(new JLabel("Username:"));
        p.add(username);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(new JLabel("Mật khẩu:"));
        JPanel pwRow = new JPanel(new BorderLayout(8, 0));
        pwRow.add(password, BorderLayout.CENTER);
        pwRow.add(showPw, BorderLayout.EAST);
        p.add(pwRow);

        int res = JOptionPane.showConfirmDialog(this, p, "Tạo tài khoản", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;

        String un = username.getText() == null ? "" : username.getText().trim();
        if (un.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username không được để trống");
            return;
        }
        
        // Kiểm tra username trùng
        if (isUsernameTakenByOther(un, r.getEmployeeId())) {
            JOptionPane.showMessageDialog(this, 
                "Username '" + un + "' da ton tai!\nVui long chon username khac.", 
                "Trùng tài khoản", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        char[] pwChars = password.getPassword();
        String pw = pwChars == null ? "" : new String(pwChars);
        if (pw.length() < 6) {
            JOptionPane.showMessageDialog(this, "Mật khẩu tối thiểu 6 ký tự");
            return;
        }

        boolean ok1 = AccountsDAO.setUsername(r.getEmployeeId(), un);
        boolean ok2 = AccountsDAO.setPasswordPlain(r.getEmployeeId(), pw);

        if (ok1 && ok2) {
            grantTabsForNewAccount(un, r.getRole());
            JOptionPane.showMessageDialog(this, "Tạo tài khoản thành công");
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(this, "Không thể lưu tài khoản", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

	private void grantTabsForNewAccount(String username, String roleOrPosition) {
		String un = username == null ? "" : username.trim();
		if (un.isEmpty()) return;
		if ("admin".equalsIgnoreCase(un)) return;

		int ask = JOptionPane.showConfirmDialog(this,
				"Bạn có muốn cấp quyền truy cập tab cho tài khoản mới tạo không?\n" +
				"(Nếu bỏ qua, hệ thống sẽ dùng quyền mặc định theo chức vụ.)",
				"Cấp quyền tab",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		if (ask != JOptionPane.YES_OPTION) return;

		String code = PermissionService.getPermissionCodeForAccount(un, roleOrPosition);
		Set<String> defaultAllowed = PermissionService.loadTabsForPermissionCode(code);

		JPanel grid = new JPanel(new GridLayout(0, 2, 10, 8));
		grid.setBorder(new EmptyBorder(10, 10, 10, 10));
		List<JCheckBox> boxes = new ArrayList<>();
		for (String tab : ALL_TABS) {
			JCheckBox cb = new JCheckBox(tab);
			cb.setSelected(defaultAllowed.contains(tab));
			boxes.add(cb);
			grid.add(cb);
		}

		JScrollPane scroll = new JScrollPane(grid);
		scroll.setPreferredSize(new Dimension(560, 380));
		int res = JOptionPane.showConfirmDialog(this, scroll,
				"Chọn tab được phép truy cập cho: " + un,
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
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

		if (!PermissionService.saveUserTabs(un, csv)) {
			JOptionPane.showMessageDialog(this,
					"Không thể lưu phân quyền vào permissions.properties",
					"Lỗi",
					JOptionPane.ERROR_MESSAGE);
		}
	}

    private void changePassword() {
        AccountsDAO.AccountRow r = getSelectedRow();
        if (r == null) {
            JOptionPane.showMessageDialog(this, "Chọn một tài khoản");
            return;
        }

        if (r.getUsername() == null || r.getUsername().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhân viên này chưa có username");
            return;
        }
        
        // Bảo vệ tài khoản admin
        String username = r.getUsername().trim();
        if ("admin".equalsIgnoreCase(username)) {
            JOptionPane.showMessageDialog(this, 
                "Khong the doi mat khau tai khoan admin tu day.\n" +
                "Admin có toàn quyền và không thể sửa đổi.", 
                "Bảo vệ Admin", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPasswordField pf = new JPasswordField();
        char defaultEcho = pf.getEchoChar();
        JCheckBox showPw = new JCheckBox("Hiện");
        showPw.addActionListener(e -> pf.setEchoChar(showPw.isSelected() ? (char) 0 : defaultEcho));
        JPanel pwRow = new JPanel(new BorderLayout(8, 0));
        pwRow.add(pf, BorderLayout.CENTER);
        pwRow.add(showPw, BorderLayout.EAST);
        int res = JOptionPane.showConfirmDialog(this, pwRow, "Nhập mật khẩu mới", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;
        char[] chars = pf.getPassword();
        String pw = chars == null ? "" : new String(chars);
        if (pw.length() < 6) {
            JOptionPane.showMessageDialog(this, "Mật khẩu tối thiểu 6 ký tự");
            return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
                "Đổi mật khẩu?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        if (AccountsDAO.setPasswordPlain(r.getEmployeeId(), pw)) {
            JOptionPane.showMessageDialog(this, "Đổi mật khẩu thành công");
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(this, "Không thể cập nhật mật khẩu", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteAccount() {
        AccountsDAO.AccountRow r = getSelectedRow();
        if (r == null) {
            JOptionPane.showMessageDialog(this, "Chọn một tài khoản để xóa");
            return;
        }

        String un = r.getUsername() == null ? "" : r.getUsername().trim();
        
        // Bảo vệ tuyệt đối tài khoản admin - KHÔNG THỂ XÓA
        if ("admin".equalsIgnoreCase(un)) {
            JOptionPane.showMessageDialog(this, 
                "KHONG THE XOA TAI KHOAN ADMIN!\n\n" +
                "Tài khoản admin có toàn quyền hệ thống và được bảo vệ tuyệt đối.\n" +
                "Không thể xóa hoặc vô hiệu hóa tài khoản này.", 
                "Bảo vệ Admin", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Xác nhận xóa tài khoản
        String fullName = r.getFullName() == null ? "" : r.getFullName();
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Ban co chac muon XOA tai khoan nay?\n\n" +
            "Tài khoản: " + un + "\n" +
            "Nhân viên: " + fullName + "\n" +
            "Mã nhân viên: NV" + String.format("%02d", r.getEmployeeId()) + "\n\n" +
            "Hành động này sẽ XÓA USERNAME và MẬT KHẨU.\n" +
            "Nhân viên vẫn tồn tại nhưng không thể đăng nhập.", 
            "Xác nhận xóa tài khoản", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Xóa tài khoản bằng cách set username = NULL và password_hash = NULL
        if (AccountsDAO.deleteAccount(r.getEmployeeId())) {
            JOptionPane.showMessageDialog(this, 
                "Da xoa tai khoan thanh cong!\n\n" +
                "Nhân viên NV" + String.format("%02d", r.getEmployeeId()) + " không còn quyền đăng nhập.",
                "Thành công",
                JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Khong the xoa tai khoan!\n" +
                "Vui lòng thử lại.", 
                "Lỗi", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private AccountsDAO.AccountRow getSelectedRow() {
        int idx = table.getSelectedRow();
		if (idx < 0) return null;
		int modelIdx = table.convertRowIndexToModel(idx);
        if (modelIdx < 0 || modelIdx >= current.size()) return null;
        return current.get(modelIdx);
    }

    private boolean isUsernameTakenByOther(String username, int employeeId) {
        if (username == null) return false;
        String target = username.trim().toLowerCase();
        for (AccountsDAO.AccountRow r : AccountsDAO.findAll()) {
            if (r == null) continue;
            if (r.getEmployeeId() == employeeId) continue;
            String un = r.getUsername() == null ? "" : r.getUsername().trim().toLowerCase();
            if (!un.isEmpty() && un.equals(target)) return true;
        }
        return false;
    }

    private String buildEmployeeLabel(String fullName, int id) {
        String fn = fullName == null ? "" : fullName.trim();
        if (!fn.isEmpty()) return fn + " (#" + id + ")";
        return "Employee#" + id;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
