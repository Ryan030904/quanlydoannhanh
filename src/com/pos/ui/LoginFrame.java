package com.pos.ui;

import com.pos.service.AuthException;
import com.pos.service.AuthService;
import com.pos.ui.theme.UIConstants;
import com.pos.ui.components.LoadingSpinner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;

// Batik SVG imports
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Modern Login Frame with Glassmorphism Design 2026
 */
public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton showPasswordButton;
    private JCheckBox rememberCheckBox;
    private LoadingSpinner loadingSpinner;
    private JLabel statusLabel;
    private int dragX, dragY;
    private boolean passwordVisible = false;
    private Preferences prefs;
    private BufferedImage backgroundImage;

    public LoginFrame() {
        // Khởi tạo preferences để lưu thông tin đăng nhập
        prefs = Preferences.userNodeForPackage(LoginFrame.class);
        
        try {
			setIconImages(AppFrame.getAppIconImages());
		} catch (Exception ignored) {
		}
        
        setTitle("Đăng nhập - FoodPOS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1100, 700);
        setLocationRelativeTo(null);
		setResizable(false);
		setUndecorated(true);
        
        // Tải ảnh nền
        loadBackgroundImage();
        
        // === BACKGROUND PANEL với ảnh nền ===
        JPanel background = new BackgroundPanel();
        background.setLayout(new BorderLayout());
        setContentPane(background);
		
		// === WINDOW CONTROL BUTTONS ở góc trên bên phải của cửa sổ ===
		JPanel windowControlPanel = createWindowControlPanel();
		windowControlPanel.setBorder(new EmptyBorder(10, 0, 0, 15));
		background.add(windowControlPanel, BorderLayout.NORTH);
        
        // === CENTER PANEL chứa glass card ===
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        background.add(centerPanel, BorderLayout.CENTER);
		
		// Enable window dragging
		enableWindowDragging(windowControlPanel);
        
        // === GLASS CARD ===
        JPanel card = createGlassCard();
        centerPanel.add(card);
        
        // Tải thông tin đăng nhập đã lưu
        loadSavedCredentials();
        
        // Listeners
        loginButton.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(loginButton);
        
        // Enter key support
        usernameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    passwordField.requestFocus();
                }
            }
        });
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    doLogin();
                }
            }
        });
        
        setVisible(true);
        usernameField.requestFocus();
    }
    
    /**
     * Tải ảnh nền cho login
     */
    private void loadBackgroundImage() {
        try {
            // Thử tải ảnh từ file background.jpg
            File bgFile = new File("img/background.jpg");
            if (bgFile.exists()) {
                BufferedImage originalImage = ImageIO.read(bgFile);
                
                int frameWidth = 1100;
                int frameHeight = 700;
                int imgWidth = originalImage.getWidth();
                int imgHeight = originalImage.getHeight();
                
                // Tính tỉ lệ để hiển thị toàn bộ ảnh (fit contain)
                double scaleX = (double) frameWidth / imgWidth;
                double scaleY = (double) frameHeight / imgHeight;
                double scale = Math.min(scaleX, scaleY); // Chọn scale nhỏ hơn để ảnh vừa khung
                
                int newWidth = (int) (imgWidth * scale);
                int newHeight = (int) (imgHeight * scale);
                
                // Tạo background với màu nền phù hợp
                backgroundImage = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = backgroundImage.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Màu nền tối khi ảnh không lấp đầy
                g2d.setColor(new Color(30, 30, 30));
                g2d.fillRect(0, 0, frameWidth, frameHeight);
                
                // Vẽ ảnh ở giữa khung
                int x = (frameWidth - newWidth) / 2;
                int y = (frameHeight - newHeight) / 2;
                g2d.drawImage(originalImage, x, y, newWidth, newHeight, null);
                
                g2d.dispose();
                System.out.println("Đã tải ảnh nền thành công: " + bgFile.getAbsolutePath());
            } else {
                // Fallback: Tạo ảnh nền gradient đẹp
                createGradientBackground();
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi tải ảnh nền, sử dụng gradient: " + e.getMessage());
            createGradientBackground();
        }
    }
    
    /**
     * Tạo ảnh nền gradient khi không có file
     */
    private void createGradientBackground() {
        backgroundImage = new BufferedImage(1100, 700, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = backgroundImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Gradient từ tím đến xanh dương
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(79, 70, 229), // Indigo-600
            1100, 700, new Color(139, 69, 193) // Purple-600
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, 1100, 700);
        
        // Thêm pattern decorative
        g2d.setColor(new Color(255, 255, 255, 15));
        for (int i = 0; i < 20; i++) {
            int x = (int)(Math.random() * 1100);
            int y = (int)(Math.random() * 700);
            int size = (int)(Math.random() * 100 + 20);
            g2d.fillOval(x, y, size, size);
        }
        
        g2d.dispose();
    }
    
    /**
     * Tải thông tin đăng nhập đã lưu
     */
    private void loadSavedCredentials() {
        String savedUsername = prefs.get("username", "");
        boolean rememberPassword = prefs.getBoolean("rememberPassword", false);
        
        if (!savedUsername.isEmpty()) {
            usernameField.setText(savedUsername);
            rememberCheckBox.setSelected(rememberPassword);
            
            if (rememberPassword) {
                String savedPassword = prefs.get("password", "");
                passwordField.setText(savedPassword);
            }
        }
    }
    
    /**
     * Lưu thông tin đăng nhập
     */
    private void saveCredentials() {
        String username = usernameField.getText().trim();
        boolean remember = rememberCheckBox.isSelected();
        
        if (remember && !username.isEmpty()) {
            prefs.put("username", username);
            prefs.putBoolean("rememberPassword", true);
            prefs.put("password", new String(passwordField.getPassword()));
        } else if (!username.isEmpty()) {
            prefs.put("username", username);
            prefs.putBoolean("rememberPassword", false);
            prefs.remove("password");
        } else {
            prefs.remove("username");
            prefs.remove("password");
            prefs.putBoolean("rememberPassword", false);
        }
    }

    private class BackgroundPanel extends JPanel {
		private transient BufferedImage scaledBackground;
		private transient int scaledW = -1;
		private transient int scaledH = -1;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (backgroundImage != null) {
				int w = getWidth();
				int h = getHeight();
				if (w > 0 && h > 0) {
					if (scaledBackground == null || w != scaledW || h != scaledH) {
						scaledBackground = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
						Graphics2D bg = scaledBackground.createGraphics();
						bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
						bg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
						bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						bg.drawImage(backgroundImage, 0, 0, w, h, null);
						bg.dispose();
						scaledW = w;
						scaledH = h;
					}
					g2d.drawImage(scaledBackground, 0, 0, null);
				}
            } else {
                GradientPaint gradient = new GradientPaint(
                        0, 0, UIConstants.PRIMARY_700,
                        0, getHeight(), UIConstants.PRIMARY_500
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
            }

            g2d.dispose();
        }
    }
    
    /**
     * Create glass card with form
     */
    private JPanel createGlassCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Glass effect background với độ mờ cao hơn
                g2d.setColor(new Color(255, 255, 255, 180)); // Tăng độ mờ
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 
                          UIConstants.RADIUS_XL, UIConstants.RADIUS_XL));
                
                // Hiệu ứng blur border
                g2d.setColor(new Color(255, 255, 255, 120));
                g2d.setStroke(new BasicStroke(2f));
                g2d.draw(new RoundRectangle2D.Float(1, 1, getWidth()-2, getHeight()-2,
                          UIConstants.RADIUS_XL, UIConstants.RADIUS_XL));
                
                g2d.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(460, 560));
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(10, 45, 15, 45));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0; // Quan trọng: cho phép mở rộng theo chiều ngang
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 8, 0);

        // === LOGO SECTION (phóng to, thay cho tiêu đề chữ) ===
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Fill ngang để wrapper panel chiếm toàn bộ chiều rộng
        gbc.insets = new Insets(0, 0, 5, 0);
        
        // Wrapper panel để căn giữa logo tuyệt đối
        JPanel logoWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logoWrapper.setOpaque(false);
        JLabel logoIcon = createLogoLabel();
        logoWrapper.add(logoIcon);
        card.add(logoWrapper, gbc);
        
        // === USERNAME LABEL ===
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.anchor = GridBagConstraints.WEST;
        JLabel usernameLabel = new JLabel("Tên đăng nhập");
        usernameLabel.setFont(UIConstants.FONT_BODY_BOLD);
        usernameLabel.setForeground(UIConstants.NEUTRAL_700);
        card.add(usernameLabel, gbc);

        // === USERNAME FIELD ===
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 10, 0);
        JPanel usernamePanel = createUsernamePanel();
        usernamePanel.setPreferredSize(new Dimension(380, 48));
        usernamePanel.setMinimumSize(new Dimension(380, 48));
        usernamePanel.setMaximumSize(new Dimension(380, 48));
        card.add(usernamePanel, gbc);

        // === PASSWORD LABEL ===
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.anchor = GridBagConstraints.WEST;
        JLabel passwordLabel = new JLabel("Mật khẩu");
        passwordLabel.setFont(UIConstants.FONT_BODY_BOLD);
        passwordLabel.setForeground(UIConstants.NEUTRAL_700);
        card.add(passwordLabel, gbc);

        // === PASSWORD FIELD ===
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 8, 0);
        JPanel passwordPanel = createPasswordPanel();
        passwordPanel.setPreferredSize(new Dimension(380, 48));
        passwordPanel.setMinimumSize(new Dimension(380, 48));
        passwordPanel.setMaximumSize(new Dimension(380, 48));
        card.add(passwordPanel, gbc);
        
        // === REMEMBER CHECKBOX ===
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.anchor = GridBagConstraints.WEST;
        rememberCheckBox = new JCheckBox("Ghi nhớ mật khẩu");
        rememberCheckBox.setFont(UIConstants.FONT_BODY);
        rememberCheckBox.setForeground(UIConstants.NEUTRAL_600);
        rememberCheckBox.setOpaque(false);
        rememberCheckBox.setFocusPainted(false);
        card.add(rememberCheckBox, gbc);
        
        // === STATUS LABEL ===
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 8, 0);
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(UIConstants.FONT_BODY_SM);
        statusLabel.setForeground(UIConstants.DANGER);
        card.add(statusLabel, gbc);

        // === LOGIN BUTTON ===
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 15, 0);
        loginButton = createPrimaryButton("ĐĂNG NHẬP");
        card.add(loginButton, gbc);

        // === FOOTER ===
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 5, 0);
        JLabel footer = new JLabel("© 2026 FoodPOS v2.0", SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.setForeground(new Color(80, 80, 80));
        card.add(footer, gbc);

        return card;
    }
    
    /**
     * Tạo logo label với SVG hoặc fallback
     */
    private JLabel createLogoLabel() {
        int logoSize = 180; // logo vừa phải
        
        // Panel wrapper để căn giữa hoàn hảo
        JLabel logoLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                super.paintComponent(g);
            }
        };
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel.setVerticalAlignment(SwingConstants.CENTER);
        logoLabel.setPreferredSize(new Dimension(logoSize, logoSize));
        logoLabel.setMinimumSize(new Dimension(logoSize, logoSize));
        logoLabel.setMaximumSize(new Dimension(logoSize, logoSize));
        
        // Ưu tiên dùng file SVG nếu có
        try {
            File svgFile = new File("img/logo.svg");
            if (svgFile.exists()) {
                ImageIcon svgIcon = loadSvgIcon(svgFile, logoSize, logoSize);
                if (svgIcon != null) {
                    logoLabel.setIcon(svgIcon);
                    return logoLabel;
                }
            }
        } catch (Exception ex) {
            // Nếu lỗi, sẽ fallback sang logo vẽ bằng code ở dưới
        }

        try {
            // Tạo logo POS bằng code nếu không tìm thấy SVG
            BufferedImage logoImage = new BufferedImage(logoSize, logoSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = logoImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Vẽ hình tròn gradient
            GradientPaint logoGradient = new GradientPaint(
                0, 0, UIConstants.PRIMARY_500,
                logoSize, logoSize, UIConstants.PRIMARY_700
            );
            g2d.setPaint(logoGradient);
            int circleSize = logoSize - 10;
            g2d.fillOval(5, 5, circleSize, circleSize);
            
            // Viền trắng
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawOval(5, 5, circleSize, circleSize);
            
            // Chữ POS
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g2d.getFontMetrics();
            String text = "POS";
            int x = (logoSize - fm.stringWidth(text)) / 2;
            int y = logoSize / 2 - 5;
            g2d.drawString(text, x, y);
            
            // Chữ SYSTEM
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            fm = g2d.getFontMetrics();
            String subText = "SYSTEM";
            x = (logoSize - fm.stringWidth(subText)) / 2;
            int y2 = y + fm.getHeight();
            g2d.drawString(subText, x, y2);
            
            // Biểu tượng check
            g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(25, 55, 35, 65);
            g2d.drawLine(35, 65, 55, 45);
            
            g2d.dispose();
            logoLabel.setIcon(new ImageIcon(logoImage));
            
        } catch (Exception e) {
            // Fallback cuối cùng: chỉ hiển thị chữ POS đơn giản
            logoLabel.setText("POS");
            logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
            logoLabel.setForeground(Color.WHITE);
        }
        
        return logoLabel;
    }

    /**
     * Tải SVG và convert sang ImageIcon sử dụng Apache Batik
     */
    private ImageIcon loadSvgIcon(File svgFile, int width, int height) throws Exception {
        // Ưu tiên render SVG bằng Batik
        if (svgFile.exists() && svgFile.getName().toLowerCase().endsWith(".svg")) {
            try {
                // Sử dụng Batik PNGTranscoder để render SVG
                PNGTranscoder transcoder = new PNGTranscoder();
                transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) width);
                transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) height);
                
                // Input từ file SVG
                TranscoderInput input = new TranscoderInput(svgFile.toURI().toString());
                
                // Output sang byte array
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                TranscoderOutput output = new TranscoderOutput(outputStream);
                
                // Transcode SVG sang PNG
                transcoder.transcode(input, output);
                outputStream.flush();
                
                // Đọc ảnh từ byte array
                byte[] imgData = outputStream.toByteArray();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(imgData);
                BufferedImage img = ImageIO.read(inputStream);
                
                if (img != null) {
                    System.out.println("Đã load logo SVG thành công: " + svgFile.getAbsolutePath());
                    return new ImageIcon(img);
                }
            } catch (Exception e) {
                System.out.println("Lỗi khi render SVG bằng Batik: " + e.getMessage());
            }
        }
        
        // Fallback: Thử load PNG/JPG
        String[] extensions = {".png", ".jpg", ".jpeg"};
        String baseName = svgFile.getAbsolutePath();
        if (baseName.toLowerCase().endsWith(".svg")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        
        for (String ext : extensions) {
            File imgFile = new File(baseName + ext);
            if (imgFile.exists()) {
                BufferedImage img = ImageIO.read(imgFile);
                if (img != null) {
                    Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Tạo username panel với style tương tự password
     */
    private JPanel createUsernamePanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Vẽ nền trắng với bo góc
                g2d.setColor(new Color(255, 255, 255, 250));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                // Vẽ viền
                g2d.setColor(UIConstants.NEUTRAL_300);
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                
                g2d.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 16, 8, 16));
        
        // Username field
        usernameField = new JTextField() {
            private String hint = "Nhập tên đăng nhập...";
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(UIConstants.NEUTRAL_400);
                    g2.setFont(UIConstants.FONT_BODY);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(hint, 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            }
        };
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        usernameField.setBorder(null);
        usernameField.setOpaque(false);
        usernameField.setCaretColor(UIConstants.PRIMARY_600);
        
        // Focus effect cho panel
        usernameField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIConstants.PRIMARY_500, 2, true),
                    new EmptyBorder(6, 14, 6, 14)
                ));
                panel.repaint();
            }
            public void focusLost(FocusEvent e) {
                panel.setBorder(new EmptyBorder(8, 16, 8, 16));
                panel.repaint();
            }
        });
        
        panel.add(usernameField, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Tạo password panel với nút ẩn/hiện NẰM TRONG ô mật khẩu
     */
    private JPanel createPasswordPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Vẽ nền trắng với bo góc
                g2d.setColor(new Color(255, 255, 255, 250));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                // Vẽ viền
                g2d.setColor(UIConstants.NEUTRAL_300);
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                
                g2d.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 16, 8, 8));
        
        // Password field không có viền riêng
        passwordField = new JPasswordField() {
            private String hint = "Nhập mật khẩu...";
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getPassword().length == 0 && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(UIConstants.NEUTRAL_400);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(hint, 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            }
        };
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        passwordField.setEchoChar('•');
        passwordField.setBorder(null);
        passwordField.setOpaque(false);
        passwordField.setCaretColor(UIConstants.PRIMARY_600);
        
        // Nút ẩn/hiện mật khẩu nằm trong ô
        showPasswordButton = new JButton() {
            private boolean isHovered = false;
            
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        repaint();
                    }
                });
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (isHovered) {
                    g2d.setColor(UIConstants.NEUTRAL_200);
                    g2d.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 6, 6);
                }
                
                // Vẽ icon mắt
                g2d.setColor(UIConstants.NEUTRAL_500);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                
                if (passwordVisible) {
                    // Mắt bị gạch chéo (closed)
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.drawOval(cx - 10, cy - 6, 20, 12);
                    g2d.fillOval(cx - 3, cy - 3, 6, 6);
                    g2d.setStroke(new BasicStroke(2f));
                    g2d.drawLine(cx - 12, cy + 8, cx + 12, cy - 8);
                } else {
                    // Mắt mở (open)
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.drawOval(cx - 10, cy - 6, 20, 12);
                    g2d.fillOval(cx - 3, cy - 3, 6, 6);
                }
                
                g2d.dispose();
            }
        };
        showPasswordButton.setPreferredSize(new Dimension(36, 36));
        showPasswordButton.setContentAreaFilled(false);
        showPasswordButton.setBorderPainted(false);
        showPasswordButton.setFocusPainted(false);
        showPasswordButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showPasswordButton.setToolTipText("Ẩn/Hiện mật khẩu");
        
        // Xử lý sự kiện ẩn/hiện mật khẩu
        showPasswordButton.addActionListener(e -> togglePasswordVisibility());
        
        // Focus effect cho panel
        passwordField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIConstants.PRIMARY_500, 2, true),
                    new EmptyBorder(6, 14, 6, 6)
                ));
                panel.repaint();
            }
            public void focusLost(FocusEvent e) {
                panel.setBorder(new EmptyBorder(8, 16, 8, 8));
                panel.repaint();
            }
        });
        
        panel.add(passwordField, BorderLayout.CENTER);
        panel.add(showPasswordButton, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * Chuyển đổi hiển thị mật khẩu
     */
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        
        if (passwordVisible) {
            passwordField.setEchoChar('\u0000'); // Hiện mật khẩu
        } else {
            passwordField.setEchoChar('•'); // Ẩn mật khẩu
        }
        showPasswordButton.repaint();
    }

    /**
     * Create modern text field with placeholder
     */
    private JTextField createModernTextField(String placeholder) {
        JTextField field = new JTextField(20) {
            private String hint = placeholder;
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(UIConstants.NEUTRAL_400);
                    g2.setFont(UIConstants.FONT_BODY);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(hint, 16, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            }
        };
        field.setFont(UIConstants.FONT_BODY);
        field.setPreferredSize(new Dimension(360, UIConstants.INPUT_HEIGHT_LG));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 2, true),
            new EmptyBorder(12, 16, 12, 16)
        ));
        field.setBackground(new Color(255, 255, 255, 240));
        field.setCaretColor(UIConstants.PRIMARY_600);
        
        // Focus effect với animation
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIConstants.PRIMARY_500, 3, true),
                    new EmptyBorder(11, 15, 11, 15)
                ));
                field.setBackground(new Color(255, 255, 255, 255));
                field.repaint();
            }
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 2, true),
                    new EmptyBorder(12, 16, 12, 16)
                ));
                field.setBackground(new Color(255, 255, 255, 240));
                field.repaint();
            }
        });
        
        return field;
    }
    
    /**
     * Create modern password field with placeholder
     */
    private JPasswordField createModernPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField(20) {
            private String hint = placeholder;
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getPassword().length == 0 && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(UIConstants.NEUTRAL_400);
                    g2.setFont(UIConstants.FONT_BODY);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(hint, 16, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            }
        };
        field.setFont(UIConstants.FONT_BODY);
        field.setEchoChar('•');
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 2, true),
            new EmptyBorder(12, 16, 12, 16)
        ));
        field.setBackground(new Color(255, 255, 255, 240));
        field.setCaretColor(UIConstants.PRIMARY_600);
        
        // Focus effect
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIConstants.PRIMARY_500, 3, true),
                    new EmptyBorder(11, 15, 11, 15)
                ));
                field.setBackground(new Color(255, 255, 255, 255));
                field.repaint();
            }
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 2, true),
                    new EmptyBorder(12, 16, 12, 16)
                ));
                field.setBackground(new Color(255, 255, 255, 240));
                field.repaint();
            }
        });
        
        return field;
    }
    
    /**
     * Create primary button with gradient
     */
    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            private boolean isHovered = false;
            private boolean isPressed = false;
            
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        isPressed = false;
                        repaint();
                    }
                    @Override
                    public void mousePressed(MouseEvent e) {
                        isPressed = true;
                        repaint();
                    }
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        isPressed = false;
                        repaint();
                    }
                });
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Color topColor = isPressed ? UIConstants.PRIMARY_700 : 
                                 (isHovered ? UIConstants.PRIMARY_600 : UIConstants.PRIMARY_500);
                Color bottomColor = isPressed ? UIConstants.PRIMARY_800 : 
                                    (isHovered ? UIConstants.PRIMARY_700 : UIConstants.PRIMARY_600);
                
                // Shadow
                if (!isPressed) {
                    g2d.setColor(new Color(0, 0, 0, 30));
                    g2d.fill(new RoundRectangle2D.Float(2, 3, getWidth()-4, getHeight()-2, 
                              UIConstants.RADIUS_MD, UIConstants.RADIUS_MD));
                }
                
                // Gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, topColor,
                    0, getHeight(), bottomColor
                );
                g2d.setPaint(gradient);
                g2d.fill(new RoundRectangle2D.Float(0, isPressed ? 1 : 0, getWidth()-1, getHeight()-(isPressed ? 1 : 2), 
                          UIConstants.RADIUS_MD, UIConstants.RADIUS_MD));
                
                // Text
                g2d.setFont(getFont());
                g2d.setColor(Color.WHITE);
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
                
                g2d.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(380, 52));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        return btn;
    }
    
    /**
     * Create ghost button
     */
    private JButton createGhostButton(String text) {
        JButton btn = new JButton(text) {
            private boolean isHovered = false;
            
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        repaint();
                    }
                });
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (isHovered) {
                    g2d.setColor(UIConstants.NEUTRAL_200);
                    g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 
                              UIConstants.RADIUS_MD, UIConstants.RADIUS_MD));
                }
                
                g2d.setFont(getFont());
                g2d.setColor(UIConstants.NEUTRAL_600);
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
                
                g2d.dispose();
            }
        };
        btn.setFont(UIConstants.FONT_BODY);
        btn.setPreferredSize(new Dimension(320, UIConstants.BUTTON_HEIGHT));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        return btn;
    }
    
    /**
     * Create window control panel - 3 nút: thu nhỏ, phóng to (disabled), đóng
     */
    private JPanel createWindowControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);
        
        JButton minimizeBtn = createSimpleWindowButton("", 0); // 0 = minimize
        minimizeBtn.setToolTipText("Thu nhỏ");
        minimizeBtn.addActionListener(e -> setState(Frame.ICONIFIED));
        
        JButton maximizeBtn = createSimpleWindowButton("", 1); // 1 = maximize
        maximizeBtn.setToolTipText("Phóng to / Thu nhỏ");
        maximizeBtn.addActionListener(e -> {
            int st = getExtendedState();
            if ((st & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
                setExtendedState(JFrame.NORMAL);
                setSize(1100, 700);
                setLocationRelativeTo(null);
            } else {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });
        
        JButton closeBtn = createSimpleWindowButton("", 2); // 2 = close
        closeBtn.setToolTipText("Đóng");
        closeBtn.addActionListener(e -> System.exit(0));
        
        panel.add(minimizeBtn);
        panel.add(maximizeBtn);
        panel.add(closeBtn);
        
        return panel;
    }

    private JButton createSimpleWindowButton(String symbol, int type) {
        JButton btn = new JButton(symbol) {
            private boolean isHovered = false;
            private boolean isPressed = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        isPressed = false;
                        repaint();
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        isPressed = true;
                        repaint();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        isPressed = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color base;
                if (type == 2) base = new Color(239, 68, 68);
                else if (type == 1) base = new Color(34, 197, 94);
                else base = new Color(245, 158, 11);

                int alpha = isHovered ? 235 : 190;
                if (isPressed) alpha = 255;

                int w = getWidth();
                int h = getHeight();
                int d = Math.min(w, h);

                g2d.setColor(new Color(0, 0, 0, 45));
                g2d.fillOval(1, 2, d - 2, d - 2);

                g2d.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
                g2d.fillOval(0, 0, d - 1, d - 1);

                g2d.setColor(new Color(0, 0, 0, isHovered ? 70 : 45));
                g2d.setStroke(new BasicStroke(1.15f));
                g2d.drawOval(0, 0, d - 2, d - 2);

                int cx = d / 2;
                int cy = d / 2;
                g2d.setColor(new Color(20, 20, 20, isHovered ? 230 : 180));
                g2d.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (type == 0) {
                    g2d.drawLine(cx - 5, cy, cx + 5, cy);
                } else if (type == 1) {
                    g2d.drawRect(cx - 5, cy - 5, 10, 10);
                } else {
                    g2d.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
                    g2d.drawLine(cx + 4, cy - 4, cx - 4, cy + 4);
                }

                g2d.dispose();
            }
        };

        btn.setPreferredSize(new Dimension(18, 18));
        btn.setMinimumSize(new Dimension(18, 18));
        btn.setMaximumSize(new Dimension(18, 18));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        return btn;
    }
    
    /**
     * Tạo nút window control đơn giản
            
            g2d.dispose();
     */
    private JButton createCloseButton() {
        JButton btn = new JButton("✕") {
            private boolean isHovered = false;
            
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        repaint();
                    }
                });
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (isHovered) {
                    g2d.setColor(UIConstants.DANGER_LIGHT);
                    g2d.fillOval(0, 0, getWidth(), getHeight());
                }
                
                g2d.setFont(UIConstants.FONT_BODY_BOLD);
                g2d.setColor(isHovered ? UIConstants.DANGER : UIConstants.NEUTRAL_400);
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
                
                g2d.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(32, 32));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> System.exit(0));
        
        return btn;
    }
    
    /**
     * Enable window dragging - với threshold chống nhạy
     */
    private void enableWindowDragging(JPanel panel) {
        final int DRAG_THRESHOLD = 5; // Số pixel cần kéo trước khi bắt đầu di chuyển
        final int[] startX = {0};
        final int[] startY = {0};
        final boolean[] isDragging = {false};
        
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragX = e.getX();
                dragY = e.getY();
                startX[0] = e.getXOnScreen();
                startY[0] = e.getYOnScreen();
                isDragging[0] = false;
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging[0] = false;
            }
        });
        
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int currentX = e.getXOnScreen();
                int currentY = e.getYOnScreen();
                
                // Chỉ bắt đầu kéo khi vượt quá threshold
                if (!isDragging[0]) {
                    int deltaX = Math.abs(currentX - startX[0]);
                    int deltaY = Math.abs(currentY - startY[0]);
                    if (deltaX > DRAG_THRESHOLD || deltaY > DRAG_THRESHOLD) {
                        isDragging[0] = true;
                    }
                }
                
                if (isDragging[0]) {
                    setLocation(currentX - dragX, currentY - dragY);
					Toolkit.getDefaultToolkit().sync();
					repaint();
                }
            }
        });
    }
    
    /**
     * Perform login
     */
    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Vui lòng nhập tên đăng nhập và mật khẩu");
            statusLabel.setForeground(UIConstants.WARNING_DARK);
            return;
        }

        // Lưu thông tin đăng nhập nếu được chọn
        saveCredentials();

        // Disable inputs during login
        loginButton.setEnabled(false);
        loginButton.setText("Đang đăng nhập...");
        statusLabel.setText(" ");

        // Use SwingWorker for async login
        SwingWorker<com.pos.model.User, Void> worker = new SwingWorker<>() {
            @Override
            protected com.pos.model.User doInBackground() throws Exception {
                return new AuthService().login(username, password);
            }
            
            @Override
            protected void done() {
                try {
                    com.pos.model.User user = get();
                    com.pos.Session.setCurrentUser(user);
                    statusLabel.setText("Đăng nhập thành công!");
                    statusLabel.setForeground(UIConstants.SUCCESS_DARK);
                    
                    // Delay before opening main window
                    Timer timer = new Timer(500, e -> {
                        new AppFrame();
                        dispose();
                    });
                    timer.setRepeats(false);
                    timer.start();
                    
                } catch (Exception ex) {
                    String message = "Đăng nhập thất bại";
                    if (ex.getCause() instanceof AuthException) {
                        message = ex.getCause().getMessage();
                    }
                    statusLabel.setText(message);
                    statusLabel.setForeground(UIConstants.DANGER);
                    
                    loginButton.setEnabled(true);
                    loginButton.setText("ĐĂNG NHẬP");
                    passwordField.setText("");
                    passwordField.requestFocus();
                }
            }
        };
        worker.execute();
    }
}


