package com.pos;

import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import com.pos.ui.LoginFrame;
import com.pos.db.DBConnection;

public class Main {
	public static void main(String[] args) {
		// Bỏ kiểm tra kết nối CSDL khi khởi động để tránh chặn mở màn hình đăng nhập
		// Việc kết nối CSDL sẽ được xử lý khi người dùng đăng nhập/thao tác
		SwingUtilities.invokeLater(LoginFrame::new);
	}
}


