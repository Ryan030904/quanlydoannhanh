package com.pos.ui;

import com.pos.Session;
import com.pos.model.CartItem;
import com.pos.service.CheckoutException;
import com.pos.service.CheckoutService;
import com.pos.util.CurrencyUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PaymentDialog extends JDialog {
    private final JComboBox<String> method;
    private final JTextField customerName;
    private final JTextField reference;
    private final JTextField bankCode;
    private final JTextField accountNo;
    private final JTextField accountName;
    private final JLabel qrPreview;
    private final JPanel qrPanel;
    private final long amount;

    public PaymentDialog(AppFrame parent, List<CartItem> cartItems) {
        super(parent, "Thanh toán", true);
        setSize(520, 360);
        setLocationRelativeTo(parent);

        double total = 0;
        for (CartItem ci : cartItems) total += ci.getLineTotal();
        amount = Math.max(0, Math.round(total));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        JLabel title = new JLabel("Xác nhận thanh toán");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel totalLabel = new JLabel("Tổng tiền: " + CurrencyUtil.formatUSDAsVND(total));
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        form.add(totalLabel);
        form.add(Box.createRigidArea(new Dimension(0, 10)));

        customerName = new JTextField();
        method = new JComboBox<>(new String[]{"Cash", "BankTransfer", "Other"});
        reference = new JTextField();
        bankCode = new JTextField("VCB");
        accountNo = new JTextField();
        accountName = new JTextField();
        qrPreview = new JLabel("QR sẽ hiển thị ở đây", SwingConstants.CENTER);
        qrPreview.setOpaque(true);
        qrPreview.setBackground(new Color(245, 247, 249));
        qrPreview.setPreferredSize(new Dimension(220, 220));
        qrPanel = new JPanel(new BorderLayout(8, 8));

        form.add(new JLabel("Tên khách hàng (tùy chọn):"));
        form.add(customerName);
        form.add(Box.createRigidArea(new Dimension(0, 8)));

        form.add(new JLabel("Phương thức:"));
        form.add(method);
        form.add(Box.createRigidArea(new Dimension(0, 8)));

        form.add(new JLabel("Mã tham chiếu / Ghi chú thanh toán:"));
        form.add(reference);

        JPanel bankForm = new JPanel(new GridLayout(3, 2, 8, 8));
        bankForm.add(new JLabel("Bank code (VD: VCB, ACB, MB...):"));
        bankForm.add(bankCode);
        bankForm.add(new JLabel("Số tài khoản:"));
        bankForm.add(accountNo);
        bankForm.add(new JLabel("Tên chủ tài khoản:"));
        bankForm.add(accountName);

        qrPanel.add(bankForm, BorderLayout.NORTH);
        qrPanel.add(qrPreview, BorderLayout.CENTER);
        qrPanel.setVisible(false);
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        form.add(qrPanel);

        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton paidBtn = new JButton("Thanh toán thành công");
        JButton cancelBtn = new JButton("Hủy");
        actions.add(cancelBtn);
        actions.add(paidBtn);
        root.add(actions, BorderLayout.SOUTH);

        cancelBtn.addActionListener(e -> dispose());

        method.addActionListener(e -> {
            boolean show = "BankTransfer".equals(method.getSelectedItem());
            qrPanel.setVisible(show);
            packToFit();
            if (show) {
                updateQr();
            }
        });
        reference.addActionListener(e -> updateQr());
        bankCode.addActionListener(e -> updateQr());
        accountNo.addActionListener(e -> updateQr());
        accountName.addActionListener(e -> updateQr());

        paidBtn.addActionListener(e -> {
            try {
                int userId = Session.getCurrentUser() != null ? Session.getCurrentUser().getId() : 0;
                String cn = customerName.getText() == null ? null : customerName.getText().trim();
                String pm = (String) method.getSelectedItem();
                String ref = reference.getText() == null ? null : reference.getText().trim();

                String orderNo = new CheckoutService().checkout(userId, cn, pm, ref, cartItems);
                JOptionPane.showMessageDialog(this, "Thanh toán thành công. Mã hóa đơn: " + orderNo);
                parent.clearCartAfterCheckout();
                dispose();
            } catch (CheckoutException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        setVisible(true);
    }

    private void updateQr() {
        if (!qrPanel.isVisible()) return;
        String b = bankCode.getText() == null ? "" : bankCode.getText().trim();
        String acc = accountNo.getText() == null ? "" : accountNo.getText().trim();
        String name = accountName.getText() == null ? "" : accountName.getText().trim();
        if (b.isEmpty() || acc.isEmpty()) {
            qrPreview.setIcon(null);
            qrPreview.setText("Nhập bank code và số tài khoản để tạo QR");
            return;
        }

        String addInfo = reference.getText() == null ? "" : reference.getText().trim();
        if (addInfo.isEmpty()) addInfo = "Thanh toan POS";

        String url = buildVietQrUrl(b, acc, name, amount, addInfo);
        qrPreview.setIcon(null);
        qrPreview.setText("Đang tải QR...");

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                BufferedImage bi = ImageIO.read(URI.create(url).toURL());
                if (bi == null) return null;
                Image scaled = bi.getScaledInstance(220, 220, Image.SCALE_SMOOTH);
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

    private void packToFit() {
        int h = qrPanel.isVisible() ? 560 : 360;
        setSize(520, h);
        setLocationRelativeTo(getOwner());
    }
}
