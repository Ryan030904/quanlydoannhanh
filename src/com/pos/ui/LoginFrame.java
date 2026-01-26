package com.pos.ui;

import com.pos.service.AuthException;
import com.pos.service.AuthService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class LoginFrame extends JFrame {
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JButton loginButton;
	private JButton exitButton;

	public LoginFrame() {
		setTitle("POS - Đăng nhập");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(520, 420);
		setLocationRelativeTo(null);
		setResizable(false);

		// Background panel
		JPanel background = new JPanel();
		background.setBackground(new Color(235, 245, 248));
		background.setLayout(new GridBagLayout());
		add(background);

		// Logo panel (centered)
		JPanel logoPanel = new JPanel(new GridBagLayout());
		logoPanel.setBackground(new Color(235, 245, 248));
		JLabel logoLabel = new JLabel();
		try {
			File f = new File("img\\Gemini_Generated_Image_ffiz7yffiz7yffiz-removebg-preview.png");
			if (f.exists()) {
				ImageIcon icon = new ImageIcon(f.getPath());
				Image img = icon.getImage().getScaledInstance(180, 90, Image.SCALE_SMOOTH);
				logoLabel.setIcon(new ImageIcon(img));
			} else {
				logoLabel.setText("POS - Quán ăn nhanh");
				logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
			}
		} catch (Exception ex) {
			logoLabel.setText("POS - Quán ăn nhanh");
			logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
		}
		logoPanel.add(logoLabel);

		// Card panel (centered)
		JPanel card = new JPanel();
		card.setPreferredSize(new Dimension(420, 300));
		card.setBackground(Color.WHITE);
		card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 230, 235), 1),
				BorderFactory.createEmptyBorder(16, 16, 16, 16)));
		card.setLayout(new BorderLayout());

		// Header within card: title only (no logo)
		JPanel cardHeader = new JPanel(new BorderLayout());
		cardHeader.setBackground(Color.WHITE);
		JLabel cardTitle = new JLabel("Đăng nhập vào hệ thống", SwingConstants.CENTER);
		cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
		cardTitle.setForeground(new Color(60, 80, 80));
		cardHeader.add(cardTitle, BorderLayout.CENTER);

		card.add(cardHeader, BorderLayout.NORTH);

		// Form
		JPanel formPanel = new JPanel();
		formPanel.setBackground(Color.WHITE);
		formPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(8, 8, 8, 8);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		formPanel.add(new JLabel("Tên đăng nhập:"), gbc);

		gbc.gridx = 1;
		usernameField = new JTextField(18);
		usernameField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200, 210, 215)),
				BorderFactory.createEmptyBorder(6, 6, 6, 6)));
		formPanel.add(usernameField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		formPanel.add(new JLabel("Mật khẩu:"), gbc);

		gbc.gridx = 1;
		passwordField = new JPasswordField(18);
		passwordField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200, 210, 215)),
				BorderFactory.createEmptyBorder(6, 6, 6, 6)));
		formPanel.add(passwordField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		JPanel buttons = new JPanel();
		buttons.setBackground(Color.WHITE);
		loginButton = new JButton("Đăng nhập");
		loginButton.setBackground(new Color(10, 120, 140));
		loginButton.setForeground(Color.WHITE);
		loginButton.setFocusPainted(false);
		exitButton = new JButton("Thoát");
		exitButton.setFocusPainted(false);
		buttons.add(loginButton);
		buttons.add(exitButton);
		formPanel.add(buttons, gbc);

		card.add(formPanel, BorderLayout.CENTER);

		// add logo and card to background (logo above, card below)
		GridBagConstraints bgc = new GridBagConstraints();
		bgc.gridx = 0;
		bgc.gridy = 0;
		bgc.weightx = 1.0;
		bgc.weighty = 0.0;
		bgc.anchor = GridBagConstraints.NORTH;
		background.add(logoPanel, bgc);

		bgc = new GridBagConstraints();
		bgc.gridx = 0;
		bgc.gridy = 1;
		bgc.weightx = 1.0;
		bgc.weighty = 1.0;
		bgc.fill = GridBagConstraints.BOTH;
		bgc.insets = new Insets(20, 0, 0, 0); // push card down a bit
		background.add(card, bgc);

		// Listeners
		loginButton.addActionListener(e -> doLogin());
		exitButton.addActionListener(e -> System.exit(0));
		getRootPane().setDefaultButton(loginButton);

		setVisible(true);
	}

	private void doLogin() {
		String username = usernameField.getText().trim();
		String password = new String(passwordField.getPassword()).trim();

		if (username.isEmpty() || password.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập và mật khẩu", "Xác thực",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		try {
			com.pos.model.User user = new AuthService().login(username, password);
			com.pos.Session.setCurrentUser(user);
			JOptionPane.showMessageDialog(this, "Đăng nhập thành công (" + user.getRole() + ")");
			new AppFrame();
			dispose();
		} catch (AuthException ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}
}


