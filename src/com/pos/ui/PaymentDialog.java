package com.pos.ui;

import com.pos.Session;
import com.pos.dao.IngredientDAO;
import com.pos.model.Ingredient;
import com.pos.model.CartItem;
import com.pos.service.CheckoutException;
import com.pos.service.CheckoutService;
import com.pos.util.CurrencyUtil;
import com.pos.ui.components.ModernButton;
import com.pos.ui.theme.UIConstants;

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
		try {
			setIconImages(AppFrame.getAppIconImages());
		} catch (Exception ignored) {
		}
        setSize(540, 380);
        setLocationRelativeTo(parent);

        double total = 0;
        for (CartItem ci : cartItems) total += ci.getLineTotal();
        amount = Math.max(0, Math.round(total));

        JPanel root = new JPanel(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        root.setBorder(new EmptyBorder(UIConstants.SPACING_LG, UIConstants.SPACING_LG, UIConstants.SPACING_LG, UIConstants.SPACING_LG));
        root.setBackground(Color.WHITE);
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Xác nhận thanh toán");
        title.setFont(UIConstants.FONT_HEADING_3);
        title.setForeground(UIConstants.PRIMARY_700);
        header.add(title, BorderLayout.WEST);
        
        JLabel totalLabel = new JLabel(CurrencyUtil.format(total));
        totalLabel.setFont(UIConstants.FONT_HEADING_2);
        totalLabel.setForeground(UIConstants.SUCCESS);
        header.add(totalLabel, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        customerName = new JTextField();
        method = new JComboBox<>(new String[]{"Cash", "BankTransfer", "Other"});
        reference = new JTextField();
        bankCode = new JTextField("VCB");
        accountNo = new JTextField();
        accountName = new JTextField();
        qrPreview = new JLabel("QR sẽ hiển thị ở đây", SwingConstants.CENTER);
        qrPreview.setOpaque(true);
        qrPreview.setBackground(UIConstants.NEUTRAL_100);
        qrPreview.setPreferredSize(new Dimension(220, 220));
        qrPanel = new JPanel(new BorderLayout(UIConstants.SPACING_SM, UIConstants.SPACING_SM));
        qrPanel.setOpaque(false);

        // Style fields
        styleTextField(customerName);
        styleTextField(reference);
        styleTextField(bankCode);
        styleTextField(accountNo);
        styleTextField(accountName);
        method.setFont(UIConstants.FONT_BODY);

        form.add(createFormLabel("Tên khách hàng (tùy chọn):"));
        form.add(customerName);
        form.add(Box.createRigidArea(new Dimension(0, UIConstants.SPACING_SM)));

        form.add(createFormLabel("Phương thức:"));
        form.add(method);
        form.add(Box.createRigidArea(new Dimension(0, UIConstants.SPACING_SM)));

        form.add(createFormLabel("Mã tham chiếu / Ghi chú thanh toán:"));
        form.add(reference);

        JPanel bankForm = new JPanel(new GridLayout(3, 2, UIConstants.SPACING_SM, UIConstants.SPACING_SM));
        bankForm.setOpaque(false);
        bankForm.add(createFormLabel("Bank code (VCB, ACB, MB...):"));
        bankForm.add(bankCode);
        bankForm.add(createFormLabel("Số tài khoản:"));
        bankForm.add(accountNo);
        bankForm.add(createFormLabel("Tên chủ TK:"));
        bankForm.add(accountName);

        qrPanel.add(bankForm, BorderLayout.NORTH);
        qrPanel.add(qrPreview, BorderLayout.CENTER);
        qrPanel.setVisible(false);
        form.add(Box.createRigidArea(new Dimension(0, UIConstants.SPACING_MD)));
        form.add(qrPanel);

        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SPACING_SM, 0));
        actions.setOpaque(false);
        ModernButton cancelBtn = new ModernButton("Hủy", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.MEDIUM);
        ModernButton paidBtn = new ModernButton("Thanh toán thành công", ModernButton.ButtonType.SUCCESS, ModernButton.ButtonSize.MEDIUM);
        cancelBtn.setPreferredSize(new Dimension(100, UIConstants.BUTTON_HEIGHT));
        paidBtn.setPreferredSize(new Dimension(180, UIConstants.BUTTON_HEIGHT));
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
				showLowStockWarningIfNeeded();
                parent.clearCartAfterCheckout();
                dispose();
            } catch (CheckoutException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        setVisible(true);
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
        int h = qrPanel.isVisible() ? 580 : 380;
        setSize(540, h);
        setLocationRelativeTo(getOwner());
    }

    private JLabel createFormLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_BODY);
        l.setForeground(UIConstants.NEUTRAL_700);
        return l;
    }

    private void styleTextField(JTextField field) {
        field.setFont(UIConstants.FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
    }
}
