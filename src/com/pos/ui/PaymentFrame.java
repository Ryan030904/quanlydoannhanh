package com.pos.ui;

import com.pos.Session;
import com.pos.dao.InventoryDAO;
import com.pos.model.CartItem;
import com.pos.model.Item;
import com.pos.service.CheckoutException;
import com.pos.service.CheckoutService;
import com.pos.util.CurrencyUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PaymentFrame extends JFrame {
    private final AppFrame parent;
    private final Map<Integer, CartItem> cart = new LinkedHashMap<>();
    private Map<Integer, Integer> stockByItemId = new LinkedHashMap<>();

    private JPanel itemsPanel;
    private JLabel subtotalLabel;
    private JLabel taxAmountLabel;
    private JLabel discountAmountLabel;
    private JLabel totalPayLabel;

    private JTextField taxPercentField;
    private JTextField discountField;

    private JRadioButton cashRadio;
    private JRadioButton transferRadio;
    private JRadioButton ewalletRadio;

    private JPanel cashPanel;
    private JTextField cashReceivedField;
    private JLabel changeLabel;

    private JPanel qrPanel;
    private JTextField bankCodeField;
    private JTextField accountNoField;
    private JTextField accountNameField;
    private JLabel qrPreview;

    private JTextField referenceField;

    public PaymentFrame(AppFrame parent, List<CartItem> cartItems) {
        this.parent = parent;
        setTitle("Thanh toán");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(parent);

        if (this.parent != null) {
            this.parent.setVisible(false);
        }

        if (cartItems != null) {
            for (CartItem ci : cartItems) {
                if (ci != null && ci.getItem() != null) {
                    cart.put(ci.getItem().getId(), new CartItem(ci.getItem(), ci.getQuantity()));
                }
            }
        }

        stockByItemId = InventoryDAO.getAllQuantities();

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(new Color(245, 247, 249));
        setContentPane(root);

        JPanel left = buildLeftOrderPanel();
        JPanel right = buildRightPaymentPanel();

        root.add(left, BorderLayout.CENTER);
        root.add(right, BorderLayout.EAST);

        renderItems();
        recalcTotals();
        updatePaymentMode();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (PaymentFrame.this.parent != null) PaymentFrame.this.parent.setVisible(true);
            }
        });

        setVisible(true);
    }

    private JPanel buildLeftOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Thông tin đơn hàng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panel.add(title, BorderLayout.NORTH);

        itemsPanel = new JPanel();
        itemsPanel.setOpaque(false);
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(itemsPanel);
        scroll.setBorder(new LineBorder(new Color(230, 235, 236)));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildRightPaymentPanel() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(360, 0));
        panel.setLayout(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Thông tin thanh toán");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        subtotalLabel = new JLabel();
        taxAmountLabel = new JLabel();
        discountAmountLabel = new JLabel();
        totalPayLabel = new JLabel();
        totalPayLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JPanel taxRow = new JPanel(new BorderLayout(8, 0));
        taxRow.setOpaque(false);
        taxPercentField = new JTextField("0");
        setCompactFieldWidth(taxPercentField);
        taxRow.add(new JLabel("Thuế/Phí (%):"), BorderLayout.WEST);
        taxRow.add(taxPercentField, BorderLayout.EAST);

        JPanel discountRow = new JPanel(new BorderLayout(8, 0));
        discountRow.setOpaque(false);
        discountField = new JTextField();
        discountRow.add(new JLabel("Giảm giá (mã hoặc %):"), BorderLayout.WEST);
        discountRow.add(discountField, BorderLayout.EAST);

        form.add(rowLabel("Tổng tiền hàng:", subtotalLabel));
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        form.add(taxRow);
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        form.add(rowLabel("Thuế/Phí:", taxAmountLabel));
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        form.add(discountRow);
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        form.add(rowLabel("Giảm giá:", discountAmountLabel));
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        form.add(rowLabel("Tổng cần thanh toán:", totalPayLabel));
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel pmTitle = new JLabel("Phương thức thanh toán:");
        pmTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        form.add(pmTitle);
        form.add(Box.createRigidArea(new Dimension(0, 6)));

        cashRadio = new JRadioButton("Tiền mặt");
        transferRadio = new JRadioButton("Chuyển khoản");
        ewalletRadio = new JRadioButton("Ví điện tử");
        ButtonGroup g = new ButtonGroup();
        g.add(cashRadio);
        g.add(transferRadio);
        g.add(ewalletRadio);
        cashRadio.setSelected(true);

        JPanel pmRow = new JPanel(new GridLayout(3, 1, 0, 6));
        pmRow.setOpaque(false);
        pmRow.add(cashRadio);
        pmRow.add(transferRadio);
        pmRow.add(ewalletRadio);
        form.add(pmRow);
        form.add(Box.createRigidArea(new Dimension(0, 10)));

        cashPanel = new JPanel();
        cashPanel.setOpaque(false);
        cashPanel.setLayout(new BoxLayout(cashPanel, BoxLayout.Y_AXIS));
        cashReceivedField = new JTextField();
        changeLabel = new JLabel();
        cashPanel.add(new JLabel("Tiền khách đưa:"));
        cashPanel.add(cashReceivedField);
        cashPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        cashPanel.add(rowLabel("Tiền thừa:", changeLabel));
        form.add(cashPanel);

        qrPanel = new JPanel();
        qrPanel.setOpaque(false);
        qrPanel.setLayout(new BoxLayout(qrPanel, BoxLayout.Y_AXIS));
        bankCodeField = new JTextField("VCB");
        accountNoField = new JTextField();
        accountNameField = new JTextField();
        qrPreview = new JLabel("QR sẽ hiển thị ở đây", SwingConstants.CENTER);
        qrPreview.setOpaque(true);
        qrPreview.setBackground(new Color(245, 247, 249));
        qrPreview.setPreferredSize(new Dimension(240, 240));
        JPanel bankForm = new JPanel(new GridLayout(3, 2, 8, 6));
        bankForm.setOpaque(false);
        bankForm.add(new JLabel("Bank code:"));
        bankForm.add(bankCodeField);
        bankForm.add(new JLabel("Số tài khoản:"));
        bankForm.add(accountNoField);
        bankForm.add(new JLabel("Tên chủ TK:"));
        bankForm.add(accountNameField);
        qrPanel.add(bankForm);
        qrPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        qrPanel.add(qrPreview);
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        form.add(qrPanel);

        form.add(Box.createRigidArea(new Dimension(0, 10)));
        form.add(new JLabel("Mã tham chiếu / ghi chú:"));
        referenceField = new JTextField();
        form.add(referenceField);

        panel.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        JButton backBtn = new JButton("Quay lại đơn hàng");
        JButton cancelBtn = new JButton("Hủy đơn");
        JButton confirmBtn = new JButton("Xác nhận thanh toán");
        confirmBtn.setBackground(new Color(10, 140, 160));
        confirmBtn.setForeground(Color.WHITE);
        actions.add(backBtn);
        actions.add(cancelBtn);
        actions.add(confirmBtn);
        panel.add(actions, BorderLayout.SOUTH);

        backBtn.addActionListener(e -> {
            syncBackToParent();
            dispose();
        });

        cancelBtn.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn hủy đơn?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                if (parent != null) parent.clearCartAfterCheckout();
                dispose();
            }
        });

        cashRadio.addActionListener(e -> updatePaymentMode());
        transferRadio.addActionListener(e -> updatePaymentMode());
        ewalletRadio.addActionListener(e -> updatePaymentMode());

        cashReceivedField.addActionListener(e -> recalcTotals());
        taxPercentField.addActionListener(e -> recalcTotals());
        discountField.addActionListener(e -> recalcTotals());
        referenceField.addActionListener(e -> updateQr());
        bankCodeField.addActionListener(e -> updateQr());
        accountNoField.addActionListener(e -> updateQr());
        accountNameField.addActionListener(e -> updateQr());

        taxPercentField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                recalcTotals();
            }
        });
        discountField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                recalcTotals();
            }
        });
        cashReceivedField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                recalcTotals();
            }
        });
        referenceField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateQr();
            }
        });

        bankCodeField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateQr();
            }
        });
        accountNoField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateQr();
            }
        });
        accountNameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateQr();
            }
        });

        confirmBtn.addActionListener(e -> doCheckout());

        return panel;
    }

    private JPanel rowLabel(String left, JLabel right) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.add(new JLabel(left), BorderLayout.WEST);
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private void renderItems() {
        itemsPanel.removeAll();
        if (cart.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setOpaque(false);
            JLabel msg = new JLabel("Chưa có món trong đơn.");
            msg.setForeground(new Color(120, 120, 120));
            empty.add(msg);
            itemsPanel.add(empty);
        } else {
            for (CartItem ci : cart.values()) {
                itemsPanel.add(buildItemRow(ci));
                itemsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }
        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    private JPanel buildItemRow(CartItem ci) {
        Item it = ci.getItem();
        JPanel row = new JPanel(new BorderLayout(10, 10));
        row.setBorder(new LineBorder(new Color(230, 235, 236)));
        row.setBackground(Color.WHITE);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(it.getName());
        name.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel unit = new JLabel("Đơn giá: " + CurrencyUtil.formatUSDAsVND(it.getPrice()));
        unit.setForeground(new Color(90, 90, 90));
        left.add(name);
        left.add(Box.createRigidArea(new Dimension(0, 2)));
        left.add(unit);
        row.add(left, BorderLayout.WEST);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        center.setOpaque(false);
        JButton minus = new JButton("-");
        JButton plus = new JButton("+");
        JLabel qty = new JLabel(String.valueOf(ci.getQuantity()));
        qty.setFont(new Font("Segoe UI", Font.BOLD, 13));
        minus.setPreferredSize(new Dimension(44, 32));
        plus.setPreferredSize(new Dimension(44, 32));
        center.add(minus);
        center.add(qty);
        center.add(plus);
        row.add(center, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        right.setOpaque(false);
        JLabel lineTotal = new JLabel(CurrencyUtil.formatUSDAsVND(ci.getLineTotal()));
        lineTotal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JButton remove = new JButton("X");
        remove.setForeground(new Color(180, 0, 0));
        right.add(lineTotal);
        right.add(remove);
        row.add(right, BorderLayout.EAST);

        int itemId = it.getId();
        minus.addActionListener(e -> {
            changeQty(itemId, -1);
        });
        plus.addActionListener(e -> {
            changeQty(itemId, +1);
        });
        remove.addActionListener(e -> {
            cart.remove(itemId);
            renderItems();
            recalcTotals();
        });

        return row;
    }

    private void changeQty(int itemId, int delta) {
        CartItem existing = cart.get(itemId);
        if (existing == null) return;
        int next = existing.getQuantity() + delta;
        if (next <= 0) {
            cart.remove(itemId);
        } else {
            int stock = stockByItemId.getOrDefault(itemId, 0);
            if (delta > 0) {
                if (stock <= 0) {
                    JOptionPane.showMessageDialog(this, "Món này đã hết hàng");
                    return;
                }
                if (next > stock) {
                    JOptionPane.showMessageDialog(this, "Không đủ tồn kho. Tồn hiện tại: " + stock);
                    return;
                }
            }
            existing.setQuantity(next);
        }
        renderItems();
        recalcTotals();
    }

    private void recalcTotals() {
        double subtotal = 0;
        for (CartItem ci : cart.values()) subtotal += ci.getLineTotal();

        double taxPercent = parseDoubleSafe(taxPercentField.getText());
        if (taxPercent < 0) taxPercent = 0;
        double tax = subtotal * taxPercent / 100.0;

        double discount = computeDiscount(subtotal);
        if (discount < 0) discount = 0;
        if (discount > subtotal + tax) discount = subtotal + tax;

        double totalPay = subtotal + tax - discount;

        subtotalLabel.setText(CurrencyUtil.formatUSDAsVND(subtotal));
        taxAmountLabel.setText(CurrencyUtil.formatUSDAsVND(tax));
        discountAmountLabel.setText("-" + CurrencyUtil.formatUSDAsVND(discount));
        totalPayLabel.setText(CurrencyUtil.formatUSDAsVND(totalPay));

        if (cashRadio.isSelected()) {
            double received = parseDoubleSafe(cashReceivedField.getText());
            double change = received - totalPay;
            if (change < 0) change = 0;
            changeLabel.setText(CurrencyUtil.formatUSDAsVND(change));
        } else {
            changeLabel.setText(CurrencyUtil.formatUSDAsVND(0));
        }

        updateQr();
    }

    private double computeDiscount(double subtotal) {
        String s = discountField.getText() == null ? "" : discountField.getText().trim();
        if (s.isEmpty()) return 0;
        Double percent = tryParseDouble(s);
        if (percent != null) {
            if (percent < 0) percent = 0.0;
            if (percent > 100) percent = 100.0;
            return subtotal * percent / 100.0;
        }
        if (s.equalsIgnoreCase("SALE10")) return subtotal * 0.10;
        if (s.equalsIgnoreCase("SALE5")) return subtotal * 0.05;
        return 0;
    }

    private void updatePaymentMode() {
        cashPanel.setVisible(cashRadio.isSelected());
        qrPanel.setVisible(transferRadio.isSelected());
        recalcTotals();
        revalidate();
        repaint();
    }

    private void updateQr() {
        if (!transferRadio.isSelected()) {
            qrPreview.setIcon(null);
            qrPreview.setText("QR sẽ hiển thị ở đây");
            return;
        }
        String bank = bankCodeField.getText() == null ? "" : bankCodeField.getText().trim();
        String acc = accountNoField.getText() == null ? "" : accountNoField.getText().trim();
        String name = accountNameField.getText() == null ? "" : accountNameField.getText().trim();
        if (bank.isEmpty() || acc.isEmpty()) {
            qrPreview.setIcon(null);
            qrPreview.setText("Nhập bank code và số tài khoản để tạo QR");
            return;
        }

        double subtotal = 0;
        for (CartItem ci : cart.values()) subtotal += ci.getLineTotal();
        double taxPercent = parseDoubleSafe(taxPercentField.getText());
        if (taxPercent < 0) taxPercent = 0;
        double tax = subtotal * taxPercent / 100.0;
        double discount = computeDiscount(subtotal);
        double totalPay = subtotal + tax - discount;
        long amount = Math.max(0, Math.round(totalPay));

        String addInfo = referenceField.getText() == null ? "" : referenceField.getText().trim();
        if (addInfo.isEmpty()) addInfo = "Thanh toan POS";

        String url = buildVietQrUrl(bank, acc, name, amount, addInfo);
        qrPreview.setIcon(null);
        qrPreview.setText("Đang tải QR...");

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                BufferedImage bi = ImageIO.read(URI.create(url).toURL());
                if (bi == null) return null;
                Image scaled = bi.getScaledInstance(240, 240, Image.SCALE_SMOOTH);
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
        String b = bank.trim();
        String acc = account.trim();
        String base = "https://img.vietqr.io/image/" + b + "-" + acc + "-compact2.png";
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

    private void doCheckout() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Đơn hàng đang trống");
            return;
        }

        double subtotal = 0;
        for (CartItem ci : cart.values()) subtotal += ci.getLineTotal();
        double taxPercent = parseDoubleSafe(taxPercentField.getText());
        if (taxPercent < 0) taxPercent = 0;
        double tax = subtotal * taxPercent / 100.0;
        double discount = computeDiscount(subtotal);
        double totalPay = subtotal + tax - discount;

        if (cashRadio.isSelected()) {
            double received = parseDoubleSafe(cashReceivedField.getText());
            if (received < totalPay) {
                JOptionPane.showMessageDialog(this, "Tiền khách đưa chưa đủ");
                return;
            }
        }

        String pm;
        if (cashRadio.isSelected()) pm = "Cash";
        else if (transferRadio.isSelected()) pm = "BankTransfer";
        else pm = "Other";

        int userId = Session.getCurrentUser() != null ? Session.getCurrentUser().getId() : 0;
        String ref = referenceField.getText() == null ? null : referenceField.getText().trim();

        try {
            String orderNo = new CheckoutService().checkout(userId, "Khách lẻ", pm, ref, new ArrayList<>(cart.values()), subtotal, tax, totalPay);
            JOptionPane.showMessageDialog(this, "Thanh toán thành công. Mã hóa đơn: " + orderNo);
            if (parent != null) parent.clearCartAfterCheckout();
            dispose();
        } catch (CheckoutException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void syncBackToParent() {
        if (parent == null) return;
        parent.setCartFromSnapshot(new ArrayList<>(cart.values()));
    }

    private double parseDoubleSafe(String s) {
        Double d = tryParseDouble(s);
        return d == null ? 0 : d;
    }

    private Double tryParseDouble(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void setCompactFieldWidth(JTextField f) {
        f.setColumns(6);
        Dimension d = new Dimension(80, 28);
        f.setPreferredSize(d);
    }
}
