package com.pos;

import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import com.pos.ui.LoginFrame;
import com.pos.db.DBConnection;

public class Main {
	public static void main(String[] args) {
		// quick DB connectivity check
		boolean dbOk = DBConnection.testConnection();
		if (!dbOk) {
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(null,
						"Không thể kết nối tới cơ sở dữ liệu (pos_db).\\nVui lòng kiểm tra MySQL và cấu hình JDBC trong DBConnection.java",
						"Lỗi kết nối CSDL", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			});
			return;
		}

		SwingUtilities.invokeLater(() -> {
			new LoginFrame();
		});
	}
}


