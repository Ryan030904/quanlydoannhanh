package com.pos.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
	// Update these constants if your MySQL credentials are different
	private static final String HOST = "127.0.0.1";
	private static final int[] PORTS = new int[]{3306, 3307};
	private static final String DATABASE = "quanlybandoannhanh";
	private static final String PARAMS = "useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
	private static final String USER = "root";
	private static final String PASSWORD = "";

	/** Return a new Connection. Caller should close it. */
	public static Connection getConnection() throws SQLException {
		// Ensure driver class is available (helps give clearer error if missing)
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException cnf) {
			throw new SQLException(
					"MySQL Connector/J driver not found on classpath. Place the connector jar in project lib/ and include it in the classpath.",
					cnf);
		}

		SQLException last = null;
		for (int port : PORTS) {
			String url = "jdbc:mysql://" + HOST + ":" + port + "/" + DATABASE + "?" + PARAMS;
			try {
				return DriverManager.getConnection(url, USER, PASSWORD);
			} catch (SQLException ex) {
				last = ex;
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Không thể kết nối MySQL tới ").append(HOST).append(":");
		for (int i = 0; i < PORTS.length; i++) {
			if (i > 0) sb.append(",");
			sb.append(PORTS[i]);
		}
		sb.append(" (DB: ").append(DATABASE).append("). ");
		sb.append("Hãy đảm bảo MySQL đang chạy (XAMPP/MySQL Service) và database đã được tạo.");

		throw new SQLException(sb.toString(), last);
	}

	/** Quick test whether connection can be obtained */
	public static boolean testConnection() {
		try (Connection c = getConnection()) {
			return c != null && !c.isClosed();
		} catch (SQLException ex) {
			// Print helpful guidance for common failures
			System.err.println("Lỗi khi cố gắng kết nối tới CSDL: " + ex.getMessage());
			ex.printStackTrace();
			return false;
		}
	}
}


