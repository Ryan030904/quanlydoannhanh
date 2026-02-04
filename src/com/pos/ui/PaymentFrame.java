package com.pos.ui;

import com.pos.Session;
import com.pos.dao.CustomerDAO;
import com.pos.dao.IngredientDAO;
import com.pos.dao.OrderDAO;
import com.pos.dao.PromotionDAO;
import com.pos.model.CartItem;
import com.pos.model.Customer;
import com.pos.model.Ingredient;
import com.pos.model.Item;
import com.pos.model.Promotion;
import com.pos.service.CheckoutException;
import com.pos.service.CheckoutService;
import com.pos.util.CurrencyUtil;
import com.pos.util.PaymentConfig;
import com.pos.util.VietQRBanks;
import com.pos.ui.theme.UIConstants;
import com.pos.ui.components.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Màn hình thanh toán đơn giản với 2 phương thức: Tiền mặt & QR VietQR
 */
public class PaymentFrame extends JFrame {
    
    private final AppFrame parent;
    private final Map<Integer, CartItem> cart = new LinkedHashMap<>();
    private Promotion appliedPromotion;
    private String generatedOrderNumber;
    
    // Left panel - Order info
    private JPanel itemsPanel;
    private JLabel subtotalLabel;
    private JLabel discountAmountLabel;
    private JLabel totalPayLabel;
    private JTextField discountField;
    
    // Payment method selection
    private JToggleButton cashBtn;
    private JToggleButton qrBtn;
    private JPanel paymentDetailsPanel;
    private CardLayout paymentCardLayout;
    
    // Cash panel
    private JTextField cashReceivedField;
    private JLabel changeLabel;
    
    // QR panel
    private JComboBox<String> bankCombo;
    private JTextField accountNoField;
    private JTextField accountNameField;
    private JLabel qrPreview;
    
    // Customer info (dùng chung cho cả 2 phương thức)
    private JTextField customerNameField;
    private JTextField customerPhoneField;
	private JTextField qrCustomerNameField;
	private JTextField qrCustomerPhoneField;
	private boolean syncingCustomerFields = false;
    
    // Quick cash buttons values
    private static final long[] QUICK_CASH = {50000, 100000, 200000, 500000, 1000000, 2000000};
    
    public PaymentFrame(AppFrame parent, List<CartItem> cartItems) {
        this.parent = parent;
        this.generatedOrderNumber = generateOrderNumber();
		try {
			setIconImages(AppFrame.getAppIconImages());
		} catch (Exception ignored) {
		}
        
        setTitle("Thanh toán đơn hàng");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1050, 800);
        setLocationRelativeTo(parent);
        setResizable(false);
        
        // Copy cart items
        if (cartItems != null) {
            for (CartItem ci : cartItems) {
                if (ci != null && ci.getItem() != null) {
                    cart.put(ci.getItem().getId(), new CartItem(ci.getItem(), ci.getQuantity()));
                }
            }
        }
        
        // Root panel
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.setBackground(UIConstants.BG_SECONDARY);
        setContentPane(root);
        
        // Header
        JPanel header = createHeader();
        root.add(header, BorderLayout.NORTH);
        
        // Main content: Left (order) + Right (payment)
        JPanel mainContent = new JPanel(new BorderLayout(16, 0));
        mainContent.setOpaque(false);
        
        JPanel leftPanel = createOrderPanel();
        JPanel rightPanel = createPaymentPanel();
        
        mainContent.add(leftPanel, BorderLayout.CENTER);
        mainContent.add(rightPanel, BorderLayout.EAST);
        
        root.add(mainContent, BorderLayout.CENTER);
        
        // Footer buttons
        JPanel footer = createFooter();
        root.add(footer, BorderLayout.SOUTH);
        
        // Initialize
        renderItems();
        recalcTotals();
        showCashPanel();
        
        // Window listener - không cần ẩn/hiện parent nữa
        
        setVisible(true);
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 12, 0));
        
        JLabel title = new JLabel("THANH TOÁN ĐƠN HÀNG");
        title.setFont(UIConstants.FONT_HEADING_2);
        title.setForeground(UIConstants.PRIMARY_700);
        
        JLabel orderNoLabel = new JLabel("Mã đơn: " + generatedOrderNumber);
        orderNoLabel.setFont(UIConstants.FONT_BODY_BOLD);
        orderNoLabel.setForeground(UIConstants.NEUTRAL_600);
        
        header.add(title, BorderLayout.WEST);
        header.add(orderNoLabel, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel createOrderPanel() {
        CardPanel panel = new CardPanel(new BorderLayout(8, 8));
        panel.setShadowSize(2);
        panel.setRadius(UIConstants.RADIUS_LG);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        
        JLabel title = new JLabel("Chi tiết đơn hàng");
        title.setFont(UIConstants.FONT_HEADING_3);
        title.setForeground(UIConstants.PRIMARY_700);
        panel.add(title, BorderLayout.NORTH);
        
        // Items list
        itemsPanel = new JPanel();
        itemsPanel.setOpaque(false);
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        
        JScrollPane scroll = new JScrollPane(itemsPanel);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(0, 300));
        panel.add(scroll, BorderLayout.CENTER);
        
        // Summary panel
        JPanel summary = new JPanel();
        summary.setOpaque(false);
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        summary.setBorder(new EmptyBorder(12, 0, 0, 0));
        
        subtotalLabel = new JLabel();
        subtotalLabel.setFont(UIConstants.FONT_BODY);
        
        discountAmountLabel = new JLabel();
        discountAmountLabel.setFont(UIConstants.FONT_BODY);
        discountAmountLabel.setForeground(UIConstants.SUCCESS_DARK);
        
        totalPayLabel = new JLabel();
        totalPayLabel.setFont(UIConstants.FONT_HEADING_2);
        totalPayLabel.setForeground(UIConstants.PRIMARY_700);
        
        // Discount input row
        JPanel discountRow = new JPanel(new BorderLayout(8, 0));
        discountRow.setOpaque(false);
        discountRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        discountField = new JTextField();
        discountField.setFont(UIConstants.FONT_BODY);
        discountField.setPreferredSize(new Dimension(100, 28));
        
        ModernButton applyDiscountBtn = new ModernButton("Ap dung", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.SMALL);
        applyDiscountBtn.setPreferredSize(new Dimension(70, 28));
        
        JPanel discountInputPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        discountInputPanel.setOpaque(false);
        discountInputPanel.add(discountField);
        discountInputPanel.add(applyDiscountBtn);
        
        discountRow.add(new JLabel("Ma giam gia:"), BorderLayout.WEST);
        discountRow.add(discountInputPanel, BorderLayout.EAST);
        
        applyDiscountBtn.addActionListener(e -> applyDiscountCode());
        
        summary.add(createSummaryRow("Tổng tiền hàng:", subtotalLabel));
        summary.add(Box.createRigidArea(new Dimension(0, 6)));
        summary.add(discountRow);
        summary.add(Box.createRigidArea(new Dimension(0, 6)));
        summary.add(createSummaryRow("Giảm giá:", discountAmountLabel));
        summary.add(Box.createRigidArea(new Dimension(0, 10)));
        summary.add(new JSeparator());
        summary.add(Box.createRigidArea(new Dimension(0, 10)));
        summary.add(createSummaryRow("TỔNG THANH TOÁN:", totalPayLabel));
        
        panel.add(summary, BorderLayout.SOUTH);
        
        return panel;
    }

	private void syncCustomerText(JTextField src, JTextField dst) {
		if (syncingCustomerFields) return;
		if (src == null || dst == null) return;
		String s = src.getText();
		String d = dst.getText();
		if (s == null) s = "";
		if (d == null) d = "";
		if (d.equals(s)) return;
		final String text = s;
		syncingCustomerFields = true;
		try {
			// DocumentListener đã chạy trên EDT, set trực tiếp để tránh trễ dữ liệu khi bấm thanh toán nhanh
			dst.setText(text);
		} finally {
			syncingCustomerFields = false;
		}
	}
    
    private JPanel createSummaryRow(String label, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JLabel left = new JLabel(label);
        left.setFont(UIConstants.FONT_BODY);
        
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        row.add(left, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        
        return row;
    }
    
    private JPanel createPaymentPanel() {
        CardPanel panel = new CardPanel(new BorderLayout(8, 12));
        panel.setShadowSize(2);
        panel.setRadius(UIConstants.RADIUS_LG);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setPreferredSize(new Dimension(460, 0));
        
        JLabel title = new JLabel("Phương thức thanh toán");
        title.setFont(UIConstants.FONT_HEADING_3);
        title.setForeground(UIConstants.PRIMARY_700);
        panel.add(title, BorderLayout.NORTH);
        
        // Center content
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        
        // Payment method buttons
        JPanel methodPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        methodPanel.setOpaque(false);
        methodPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        
        cashBtn = createMethodButton("TIỀN MẶT", true);
        qrBtn = createMethodButton("QR VIETQR", false);
        
        ButtonGroup group = new ButtonGroup();
        group.add(cashBtn);
        group.add(qrBtn);
        
        methodPanel.add(cashBtn);
        methodPanel.add(qrBtn);
        
        content.add(methodPanel, BorderLayout.NORTH);
        
        // Payment details card layout
        paymentCardLayout = new CardLayout();
        paymentDetailsPanel = new JPanel(paymentCardLayout);
        paymentDetailsPanel.setOpaque(false);
        
        paymentDetailsPanel.add(createCashPanel(), "cash");
        paymentDetailsPanel.add(createQRPanel(), "qr");
        
        content.add(paymentDetailsPanel, BorderLayout.CENTER);
        
        panel.add(content, BorderLayout.CENTER);
        
        // Listeners
        cashBtn.addActionListener(e -> showCashPanel());
        qrBtn.addActionListener(e -> showQRPanel());
        
        return panel;
    }
    
    private JToggleButton createMethodButton(String text, boolean selected) {
        JToggleButton btn = new JToggleButton(text);
        btn.setFont(UIConstants.FONT_BODY_BOLD);
        btn.setPreferredSize(new Dimension(130, 35));
        btn.setFocusPainted(false);
        btn.setSelected(selected);
        
        if (selected) {
            btn.setBackground(UIConstants.PRIMARY_500);
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(UIConstants.NEUTRAL_100);
            btn.setForeground(UIConstants.NEUTRAL_700);
        }
        
        btn.addChangeListener(e -> {
            if (btn.isSelected()) {
                btn.setBackground(UIConstants.PRIMARY_500);
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(UIConstants.NEUTRAL_100);
                btn.setForeground(UIConstants.NEUTRAL_700);
            }
        });
        
        return btn;
    }
    
    private JPanel createCashPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));
        
        // Customer info section
        JPanel customerPanel = new JPanel(new GridLayout(2, 2, 8, 6));
        customerPanel.setOpaque(false);
        customerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        JLabel nameLabel = new JLabel("Tên khách hàng:");
        nameLabel.setFont(UIConstants.FONT_BODY);
        customerNameField = new JTextField();
        customerNameField.setFont(UIConstants.FONT_BODY);
        
        JLabel phoneLabel = new JLabel("Số điện thoại:");
        phoneLabel.setFont(UIConstants.FONT_BODY);
        customerPhoneField = new JTextField();
        customerPhoneField.setFont(UIConstants.FONT_BODY);
        
        customerPanel.add(nameLabel);
        customerPanel.add(customerNameField);
        customerPanel.add(phoneLabel);
        customerPanel.add(customerPhoneField);
        
        panel.add(customerPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        
        // Cash received input
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JLabel lbl = new JLabel("Tiền khách đưa:");
        lbl.setFont(UIConstants.FONT_BODY_BOLD);
        
        cashReceivedField = new JTextField();
        cashReceivedField.setFont(new Font("SansSerif", Font.BOLD, 18));
        cashReceivedField.setHorizontalAlignment(JTextField.RIGHT);
        cashReceivedField.setPreferredSize(new Dimension(160, 36));
        
        inputRow.add(lbl, BorderLayout.WEST);
        inputRow.add(cashReceivedField, BorderLayout.EAST);
        
        panel.add(inputRow);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        
        // Quick cash buttons
        JLabel quickLabel = new JLabel("Chọn nhanh:");
        quickLabel.setFont(UIConstants.FONT_BODY);
        quickLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(quickLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        
        JPanel quickPanel = new JPanel(new GridLayout(2, 4, 8, 8));
        quickPanel.setOpaque(false);
        quickPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        for (long val : QUICK_CASH) {
            JButton qBtn = new JButton(formatQuickCash(val));
            qBtn.setFont(UIConstants.FONT_BODY_BOLD);
            qBtn.setBackground(UIConstants.NEUTRAL_100);
            qBtn.setFocusPainted(false);
            qBtn.addActionListener(e -> {
                cashReceivedField.setText(String.valueOf(val));
                recalcTotals();
            });
            quickPanel.add(qBtn);
        }
        
        // "Đủ tiền" button
        JButton exactBtn = new JButton("Đủ tiền");
        exactBtn.setFont(UIConstants.FONT_BODY_BOLD);
        exactBtn.setBackground(UIConstants.SUCCESS);
        exactBtn.setForeground(Color.WHITE);
        exactBtn.setFocusPainted(false);
        exactBtn.addActionListener(e -> {
            double total = getTotalPay();
            cashReceivedField.setText(String.valueOf(Math.round(total)));
            recalcTotals();
        });
        quickPanel.add(exactBtn);
        
        // Clear button
        JButton clearBtn = new JButton("Xóa");
        clearBtn.setFont(UIConstants.FONT_BODY_BOLD);
        clearBtn.setBackground(UIConstants.NEUTRAL_300);
        clearBtn.setFocusPainted(false);
        clearBtn.addActionListener(e -> {
            cashReceivedField.setText("");
            recalcTotals();
        });
        quickPanel.add(clearBtn);
        
        panel.add(quickPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 16)));
        
        // Change display
        JPanel changeRow = new JPanel(new BorderLayout());
        changeRow.setOpaque(false);
        changeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        changeRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.SUCCESS, 2),
            new EmptyBorder(10, 12, 10, 12)
        ));
        changeRow.setBackground(new Color(232, 245, 233));
        
        JLabel changeLbl = new JLabel("Tiền thừa:");
        changeLbl.setFont(UIConstants.FONT_HEADING_3);
        
        changeLabel = new JLabel("0 đ");
        changeLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        changeLabel.setForeground(UIConstants.SUCCESS_DARK);
        changeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        changeRow.add(changeLbl, BorderLayout.WEST);
        changeRow.add(changeLabel, BorderLayout.EAST);
        
        panel.add(changeRow);
        panel.add(Box.createVerticalGlue());
        
        // Listeners
        cashReceivedField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                recalcTotals();
            }
        });
        
        return panel;
    }
    
    private JPanel createQRPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));
        
        // Load saved config (fixed VCB info)
        PaymentConfig config = PaymentConfig.getInstance();
        
        // Hidden fields to store config values
        bankCombo = new JComboBox<>(new String[]{config.getBankCode()});
        bankCombo.setVisible(false);
        accountNoField = new JTextField(config.getAccountNo());
        accountNoField.setVisible(false);
        accountNameField = new JTextField(config.getAccountName());
        accountNameField.setVisible(false);
        
        // Bank info display (read-only)
        JPanel infoPanel = new JPanel(new GridLayout(3, 2, 8, 4));
        infoPanel.setOpaque(false);
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        JLabel bankLabel = new JLabel("Ngân hàng:");
        bankLabel.setFont(UIConstants.FONT_BODY);
        JLabel bankValue = new JLabel(VietQRBanks.getName(config.getBankCode()));
        bankValue.setFont(UIConstants.FONT_BODY_BOLD);
        bankValue.setForeground(UIConstants.PRIMARY_700);
        
        JLabel accLabel = new JLabel("Số TK:");
        accLabel.setFont(UIConstants.FONT_BODY);
        JLabel accValue = new JLabel(config.getAccountNo());
        accValue.setFont(UIConstants.FONT_BODY_BOLD);
        accValue.setForeground(UIConstants.PRIMARY_700);
        
        JLabel nameLabel = new JLabel("Chủ TK:");
        nameLabel.setFont(UIConstants.FONT_BODY);
        JLabel nameValue = new JLabel(config.getAccountName());
        nameValue.setFont(UIConstants.FONT_BODY_BOLD);
        nameValue.setForeground(UIConstants.PRIMARY_700);
        
        infoPanel.add(bankLabel);
        infoPanel.add(bankValue);
        infoPanel.add(accLabel);
        infoPanel.add(accValue);
        infoPanel.add(nameLabel);
        infoPanel.add(nameValue);
        
        panel.add(infoPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        
        // QR Preview - larger size
        JPanel qrContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        qrContainer.setOpaque(false);
        
        qrPreview = new JLabel("Đang tải mã QR...", SwingConstants.CENTER);
        qrPreview.setPreferredSize(new Dimension(280, 310));
        qrPreview.setOpaque(true);
        qrPreview.setBackground(new Color(248, 249, 250));
        qrPreview.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_300));
        
        qrContainer.add(qrPreview);
        panel.add(qrContainer);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        
        // Customer info section in QR panel
        JPanel customerQRPanel = new JPanel(new GridLayout(2, 2, 8, 6));
        customerQRPanel.setOpaque(false);
        customerQRPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        JLabel custNameLbl = new JLabel("Tên khách hàng:");
        custNameLbl.setFont(UIConstants.FONT_BODY);
        qrCustomerNameField = new JTextField();
        qrCustomerNameField.setFont(UIConstants.FONT_BODY);
        
        JLabel custPhoneLbl = new JLabel("Số điện thoại:");
        custPhoneLbl.setFont(UIConstants.FONT_BODY);
        qrCustomerPhoneField = new JTextField();
        qrCustomerPhoneField.setFont(UIConstants.FONT_BODY);
        
        customerQRPanel.add(custNameLbl);
        customerQRPanel.add(qrCustomerNameField);
        customerQRPanel.add(custPhoneLbl);
        customerQRPanel.add(qrCustomerPhoneField);
        
        panel.add(customerQRPanel);
        
        // Sync customer fields between Cash and QR panels
        qrCustomerNameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { syncCustomerText(qrCustomerNameField, customerNameField); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { syncCustomerText(qrCustomerNameField, customerNameField); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { syncCustomerText(qrCustomerNameField, customerNameField); }
        });
        qrCustomerPhoneField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { syncCustomerText(qrCustomerPhoneField, customerPhoneField); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { syncCustomerText(qrCustomerPhoneField, customerPhoneField); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { syncCustomerText(qrCustomerPhoneField, customerPhoneField); }
        });
        customerNameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { syncCustomerText(customerNameField, qrCustomerNameField); }
		            public void removeUpdate(javax.swing.event.DocumentEvent e) { syncCustomerText(customerNameField, qrCustomerNameField); }
	            public void changedUpdate(javax.swing.event.DocumentEvent e) { syncCustomerText(customerNameField, qrCustomerNameField); }
        });
        customerPhoneField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) { syncCustomerText(customerPhoneField, qrCustomerPhoneField); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { syncCustomerText(customerPhoneField, qrCustomerPhoneField); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { syncCustomerText(customerPhoneField, qrCustomerPhoneField); }
        });
        
        return panel;
    }
    
    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(12, 0, 0, 0));
        
        JButton backBtn = new JButton("← Quay lại");
        backBtn.setFont(UIConstants.FONT_BODY_BOLD);
        backBtn.setPreferredSize(new Dimension(130, 40));
        backBtn.addActionListener(e -> {
            syncBackToParent();
            dispose();
        });
        
        JButton cancelBtn = new JButton("Hủy đơn");
        cancelBtn.setFont(UIConstants.FONT_BODY_BOLD);
        cancelBtn.setPreferredSize(new Dimension(130, 40));
        cancelBtn.setBackground(UIConstants.DANGER);
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this, 
                "Bạn có chắc muốn hủy đơn hàng này?", 
                "Xác nhận hủy đơn", 
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) {
                if (parent != null) parent.clearCartAfterCheckout();
                dispose();
            }
        });
        
        JButton confirmBtn = new JButton("XÁC NHẬN THANH TOÁN");
        confirmBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        confirmBtn.setPreferredSize(new Dimension(220, 45));
        confirmBtn.setBackground(UIConstants.SUCCESS);
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.addActionListener(e -> doCheckout());
        
        footer.add(backBtn);
        footer.add(cancelBtn);
        footer.add(confirmBtn);
        
        return footer;
    }
    
    // ==================== Helper Methods ====================
    
    private void renderItems() {
        itemsPanel.removeAll();
        
        if (cart.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setOpaque(false);
            JLabel msg = new JLabel("Chưa có món trong đơn hàng");
            msg.setFont(UIConstants.FONT_BODY);
            msg.setForeground(UIConstants.NEUTRAL_500);
            empty.add(msg);
            itemsPanel.add(empty);
        } else {
            for (CartItem ci : cart.values()) {
                itemsPanel.add(buildItemRow(ci));
                itemsPanel.add(Box.createRigidArea(new Dimension(0, 4)));
            }
        }
        
        itemsPanel.revalidate();
        itemsPanel.repaint();
    }
    
    private JPanel buildItemRow(CartItem ci) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 8, 8, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        row.setBackground(Color.WHITE);
        
        Item item = ci.getItem();
        
        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setFont(UIConstants.FONT_BODY);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);
        
        JLabel qtyLabel = new JLabel("x" + ci.getQuantity());
        qtyLabel.setFont(UIConstants.FONT_BODY_BOLD);
        
        JLabel priceLabel = new JLabel(CurrencyUtil.format(ci.getLineTotal()));
        priceLabel.setFont(UIConstants.FONT_BODY_BOLD);
        priceLabel.setForeground(UIConstants.PRIMARY_700);
        
        rightPanel.add(qtyLabel);
        rightPanel.add(priceLabel);
        
        row.add(nameLabel, BorderLayout.WEST);
        row.add(rightPanel, BorderLayout.EAST);
        
        return row;
    }
    
    private void recalcTotals() {
        double subtotal = 0;
        for (CartItem ci : cart.values()) {
            subtotal += ci.getLineTotal();
        }
        
        double discount = computeDiscount(subtotal);
        double totalPay = subtotal - discount;
        if (totalPay < 0) totalPay = 0;
        
        subtotalLabel.setText(CurrencyUtil.format(subtotal));
        discountAmountLabel.setText("-" + CurrencyUtil.format(discount));
        totalPayLabel.setText(CurrencyUtil.format(totalPay));
        
        // Update change
        if (cashBtn.isSelected()) {
            double received = parseDoubleSafe(cashReceivedField.getText());
            double change = received - totalPay;
            if (change < 0) change = 0;
            changeLabel.setText(CurrencyUtil.format(change));
        }
        
        // Update QR
        if (qrBtn.isSelected()) {
            updateQR();
        }
    }
    
    private double computeDiscount(double subtotal) {
        if (appliedPromotion == null) {
            return 0;
        }
        
        // Kiểm tra điều kiện tối thiểu
        if (subtotal < appliedPromotion.getMinOrderAmount()) {
            return 0;
        }
        
        if ("percentage".equalsIgnoreCase(appliedPromotion.getDiscountType())) {
            return subtotal * appliedPromotion.getDiscountValue() / 100.0;
        } else {
            return Math.min(appliedPromotion.getDiscountValue(), subtotal);
        }
    }
    
    private void applyDiscountCode() {
        String code = discountField.getText();
        if (code == null || code.trim().isEmpty()) {
            appliedPromotion = null;
            recalcTotals();
            return;
        }
        
        String s = code.trim();
        double subtotal = 0;
        for (CartItem ci : cart.values()) {
            subtotal += ci.getLineTotal();
        }
        
        // Tìm khuyến mãi theo mã
        try {
            Promotion found = PromotionDAO.findByCode(s);
            
            if (found == null) {
                JOptionPane.showMessageDialog(this, "Ma giam gia khong ton tai hoac da het han!", "Loi", JOptionPane.ERROR_MESSAGE);
                appliedPromotion = null;
                recalcTotals();
                return;
            }
            
            // Kiểm tra điều kiện tối thiểu
            if (subtotal < found.getMinOrderAmount()) {
                JOptionPane.showMessageDialog(this, 
                    "Don hang chua dat dieu kien toi thieu!\nYeu cau: " + CurrencyUtil.format(found.getMinOrderAmount()) + "\nDon hang hien tai: " + CurrencyUtil.format(subtotal), 
                    "Khong du dieu kien", JOptionPane.WARNING_MESSAGE);
                appliedPromotion = null;
                recalcTotals();
                return;
            }
            
            // Áp dụng thành công
            appliedPromotion = found;
            recalcTotals();
            
            double discount = computeDiscount(subtotal);
            JOptionPane.showMessageDialog(this, 
                "Ap dung ma thanh cong!\nGiam: " + CurrencyUtil.format(discount), 
                "Thanh cong", JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Loi khi kiem tra ma giam gia!", "Loi", JOptionPane.ERROR_MESSAGE);
            appliedPromotion = null;
            recalcTotals();
        }
    }
    
    private double getTotalPay() {
        double subtotal = 0;
        for (CartItem ci : cart.values()) {
            subtotal += ci.getLineTotal();
        }
        double discount = computeDiscount(subtotal);
        double total = subtotal - discount;
        return total < 0 ? 0 : total;
    }
    
    private void showCashPanel() {
        cashBtn.setSelected(true);
        qrBtn.setSelected(false);
        paymentCardLayout.show(paymentDetailsPanel, "cash");
        recalcTotals();
    }
    
    private void showQRPanel() {
        qrBtn.setSelected(true);
        cashBtn.setSelected(false);
        paymentCardLayout.show(paymentDetailsPanel, "qr");
        updateQR();
    }
    
    private void updateQR() {
        // Lấy thông tin từ config cố định
        PaymentConfig config = PaymentConfig.getInstance();
        String bankCode = config.getBankCode();
        String accountNo = config.getAccountNo();
        String accountName = config.getAccountName();
        
        if (bankCode.isEmpty() || accountNo.isEmpty()) {
            qrPreview.setIcon(null);
            qrPreview.setText("Chưa cấu hình tài khoản");
            return;
        }
        
        double totalPay = getTotalPay();
        long amount = Math.max(0, Math.round(totalPay));
        String addInfo = generatedOrderNumber;
        
        String url = buildVietQrUrl(bankCode, accountNo, accountName, amount, addInfo);
        qrPreview.setIcon(null);
        qrPreview.setText("Đang tải QR...");
        
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                BufferedImage bi = ImageIO.read(URI.create(url).toURL());
                if (bi == null) return null;

				int maxW = qrPreview.getWidth();
				int maxH = qrPreview.getHeight();
				if (maxW <= 0 || maxH <= 0) {
					Dimension pref = qrPreview.getPreferredSize();
					if (pref != null) {
						maxW = pref.width;
						maxH = pref.height;
					}
				}
				if (maxW <= 0) maxW = bi.getWidth();
				if (maxH <= 0) maxH = bi.getHeight();

				double scale = Math.min(maxW / (double) bi.getWidth(), maxH / (double) bi.getHeight());
				if (scale <= 0) scale = 1.0;
				int newW = Math.max(1, (int) Math.round(bi.getWidth() * scale));
				int newH = Math.max(1, (int) Math.round(bi.getHeight() * scale));
				Image scaled = bi.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
				return new ImageIcon(scaled);
            }
            
            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon == null) {
                        qrPreview.setIcon(null);
                        qrPreview.setText("Không tải được QR");
                        return;
                    }
                    qrPreview.setText("");
                    qrPreview.setIcon(icon);
                } catch (Exception ex) {
                    qrPreview.setIcon(null);
                    qrPreview.setText("Không tải được QR");
                }
            }
        }.execute();
    }
    
    private String buildVietQrUrl(String bank, String account, String accountName, long amount, String addInfo) {
        String base = "https://img.vietqr.io/image/" + bank + "-" + account + "-compact2.png";
        StringBuilder qs = new StringBuilder();
        qs.append("?amount=").append(amount);
        if (addInfo != null && !addInfo.trim().isEmpty()) {
            qs.append("&addInfo=").append(URLEncoder.encode(addInfo.trim(), StandardCharsets.UTF_8));
        }
        if (accountName != null && !accountName.trim().isEmpty()) {
            qs.append("&accountName=").append(URLEncoder.encode(accountName.trim(), StandardCharsets.UTF_8));
        }
        return base + qs;
    }
    
    private void savePaymentConfig() {
        String bankDisplay = (String) bankCombo.getSelectedItem();
        String bankCode = VietQRBanks.extractCode(bankDisplay);
        String accountNo = accountNoField.getText().trim();
        String accountName = accountNameField.getText().trim();
        
        PaymentConfig.getInstance().update(bankCode, accountNo, accountName);
        JOptionPane.showMessageDialog(this, 
            "Đã lưu thông tin ngân hàng!", 
            "Thành công", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void selectBankByCode(String code) {
        if (code == null || code.isEmpty()) return;
        for (int i = 0; i < bankCombo.getItemCount(); i++) {
            String item = bankCombo.getItemAt(i);
            if (item.startsWith(code + " - ")) {
                bankCombo.setSelectedIndex(i);
                return;
            }
        }
    }
    
    private void doCheckout() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Đơn hàng đang trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        double subtotal = 0;
        for (CartItem ci : cart.values()) {
            subtotal += ci.getLineTotal();
        }
        double discount = computeDiscount(subtotal);
        double totalPay = subtotal - discount;
        if (totalPay < 0) totalPay = 0;
        
        String paymentMethod;
        
        if (cashBtn.isSelected()) {
            // Cash payment validation
            double received = parseDoubleSafe(cashReceivedField.getText());
            if (received < totalPay) {
                JOptionPane.showMessageDialog(this, 
                    "Tiền khách đưa chưa đủ!\n\nCần: " + CurrencyUtil.format(totalPay) + "\nĐã nhận: " + CurrencyUtil.format(received), 
                    "Thiếu tiền", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            paymentMethod = "Cash";
        } else {
            // QR payment - không cần validation thêm
            paymentMethod = "BankTransfer";
        }
        
		String custName;
		String custPhone;
		if (qrBtn != null && qrBtn.isSelected() && qrCustomerNameField != null && qrCustomerPhoneField != null) {
			custName = qrCustomerNameField.getText() != null ? qrCustomerNameField.getText().trim() : "";
			custPhone = qrCustomerPhoneField.getText() != null ? qrCustomerPhoneField.getText().trim() : "";
		} else {
			custName = customerNameField.getText() != null ? customerNameField.getText().trim() : "";
			custPhone = customerPhoneField.getText() != null ? customerPhoneField.getText().trim() : "";
		}
		// Fallback nếu vì lý do nào đó field đang chọn rỗng nhưng field còn lại có dữ liệu
		if ((custName == null || custName.trim().isEmpty()) && customerNameField != null && customerNameField.getText() != null) {
			String other = customerNameField.getText().trim();
			if (!other.isEmpty()) custName = other;
		}
		if ((custPhone == null || custPhone.trim().isEmpty()) && customerPhoneField != null && customerPhoneField.getText() != null) {
			String other = customerPhoneField.getText().trim();
			if (!other.isEmpty()) custPhone = other;
		}
		Customer existingByPhone = null;
		if (custPhone != null && !custPhone.trim().isEmpty()) {
			existingByPhone = CustomerDAO.findByPhone(custPhone);
			if (existingByPhone != null) {
				String existingName = existingByPhone.getFullName() == null ? "" : existingByPhone.getFullName().trim();
				String inputName = custName == null ? "" : custName.trim();
				if (!inputName.isEmpty() && !existingName.equalsIgnoreCase(inputName)) {
					JOptionPane.showMessageDialog(this,
						"Số điện thoại đã tồn tại nhưng tên khách hàng không khớp.\nVui lòng kiểm tra và nhập lại thông tin.",
						"Trùng SĐT",
						JOptionPane.WARNING_MESSAGE);
					return;
				}
				if (inputName.isEmpty()) {
					custName = existingName;
				}
			}
		}
		String customerNameForOrder = (custName == null || custName.trim().isEmpty()) ? "Khách lẻ" : custName.trim();
		String referenceForOrder = (custPhone == null || custPhone.trim().isEmpty()) ? null : ("SĐT: " + custPhone.trim());
        
        int userId = Session.getCurrentUser() != null ? Session.getCurrentUser().getId() : 0;
        
        try {
            List<CheckoutService.AppliedPromotion> promos = new ArrayList<>();
            if (appliedPromotion != null) {
                promos.add(new CheckoutService.AppliedPromotion(appliedPromotion.getId(), discount));
            }
            
            String orderNo = new CheckoutService().checkoutWithOrderNumber(
                userId, 
                generatedOrderNumber, 
                customerNameForOrder, 
                paymentMethod, 
                referenceForOrder,
                new ArrayList<>(cart.values()), 
                subtotal, 
                0, // no tax
                totalPay,
                promos
            );

			Integer linkedCustomerId = null;
			if (custPhone != null && !custPhone.trim().isEmpty()) {
				Customer byPhone = CustomerDAO.findByPhone(custPhone);
				if (byPhone == null) {
					saveCustomerIfNew(custName, custPhone);
					byPhone = CustomerDAO.findByPhone(custPhone);
					if (byPhone != null && parent != null) {
						SwingUtilities.invokeLater(() -> parent.refreshCustomers());
					}
				}
				if (byPhone != null && byPhone.getId() > 0) linkedCustomerId = byPhone.getId();
			}
			OrderDAO.updateCustomerForOrderNumber(orderNo, linkedCustomerId, customerNameForOrder);
            
            // Tính tiền thừa nếu thanh toán tiền mặt
            double cashReceived = 0;
            double changeAmount = 0;
            if (cashBtn.isSelected()) {
                cashReceived = parseDoubleSafe(cashReceivedField.getText());
                changeAmount = cashReceived - totalPay;
                if (changeAmount < 0) changeAmount = 0;
            }
            
            // Hiển thị hóa đơn
            InvoiceDialog.show(this, orderNo, new ArrayList<>(cart.values()),
                subtotal, discount, totalPay,
                cashBtn.isSelected() ? "Tiền mặt" : "Chuyển khoản",
                cashReceived, changeAmount);

			showLowStockWarningIfNeeded();
            
			if (parent != null) {
				parent.clearCartAfterCheckout();
			}
            dispose();
            
        } catch (CheckoutException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi thanh toán", JOptionPane.ERROR_MESSAGE);
        }
    }

	private void showLowStockWarningIfNeeded() {
		if (!IngredientDAO.supportsMinStockLevel()) return;
		List<Ingredient> low = IngredientDAO.findByFilter("", null, true, false);
		if (low == null || low.isEmpty()) return;
		StringBuilder sb = new StringBuilder();
		for (Ingredient ing : low) {
			if (ing == null) continue;
			double min = ing.getMinStockLevel();
			if (min <= 0) continue;
			double cur = ing.getCurrentStock();
			if (cur > min) continue;
			String name = ing.getName() == null ? "" : ing.getName();
			String unit = ing.getUnit() == null ? "" : ing.getUnit();
			if (sb.length() == 0) {
				sb.append("Cảnh báo tồn kho thấp (<= tồn tối thiểu):\n");
			}
			sb.append("- ").append("#").append(ing.getId()).append(" ").append(name);
			if (!unit.trim().isEmpty()) sb.append(" (").append(unit).append(")");
			sb.append(": còn ").append(CurrencyUtil.formatQuantity(cur))
				.append(", tối thiểu ").append(CurrencyUtil.formatQuantity(min))
				.append("\n");
		}
		if (sb.length() == 0) return;
		JOptionPane.showMessageDialog(this, sb.toString(), "Cảnh báo tồn kho", JOptionPane.WARNING_MESSAGE);
	}
    
    private void syncBackToParent() {
        if (parent == null) return;
        parent.setCartFromSnapshot(new ArrayList<>(cart.values()));
    }
    
    private String generateOrderNumber() {
        // Tạo mã ngẫu nhiên gồm 8 chữ số và đảm bảo không trùng
        String orderNo;
        int maxAttempts = 100;
        int attempt = 0;
        
        do {
            // Tạo mã dạng: HD + 8 số ngẫu nhiên
            long rnd = ThreadLocalRandom.current().nextLong(10000000L, 100000000L);
            orderNo = "HD" + rnd;
            attempt++;
        } while (OrderDAO.existsOrderNumber(orderNo) && attempt < maxAttempts);
        
        // Nếu sau 100 lần vẫn trùng, thêm timestamp để đảm bảo duy nhất
        if (attempt >= maxAttempts) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            orderNo = "HD" + ts + ThreadLocalRandom.current().nextInt(1000, 10000);
        }
        
        return orderNo;
    }
    
    private String formatQuickCash(long value) {
        if (value >= 1000000) {
            return (value / 1000000) + "M";
        } else {
            return (value / 1000) + "K";
        }
    }
    
    private double parseDoubleSafe(String s) {
        if (s == null) return 0;
        String t = s.trim().replace(",", "").replace(".", "");
        if (t.isEmpty()) return 0;
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
    
    private boolean saveCustomerIfNew(String name, String phone) {
        if (phone == null || phone.trim().isEmpty()) return false;

        try {
			if (CustomerDAO.phoneExists(phone, 0)) return false;

            Customer customer = new Customer();
            customer.setFullName(name == null || name.trim().isEmpty() ? "Khách lẻ" : name.trim());
            customer.setPhone(phone.trim());
            customer.setAddress("");
            customer.setMembershipLevel("bronze");
            customer.setLoyaltyPoints(0);

            return CustomerDAO.create(customer);
        } catch (Exception ex) {
            ex.printStackTrace();
			return false;
        }
    }
}
