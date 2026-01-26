package com.pos.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
	// Update these constants if your MySQL credentials are different
	private static final String URL = "jdbc:mysql://localhost:3306/pos_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
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
		return DriverManager.getConnection(URL, USER, PASSWORD);
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


