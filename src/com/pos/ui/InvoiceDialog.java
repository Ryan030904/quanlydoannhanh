package com.pos.ui;

import com.pos.model.CartItem;
import com.pos.util.CurrencyUtil;
import com.pos.util.PdfExportUtil;
import com.pos.ui.theme.UIConstants;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dialog hiển thị hóa đơn sau khi thanh toán thành công
 */
public class InvoiceDialog extends JDialog implements Printable {
    
    private final String orderNumber;
    private final List<CartItem> items;
    private final double subtotal;
    private final double discount;
    private final double total;
    private final String paymentMethod;
    private final double cashReceived;
    private final double change;
    private final LocalDateTime orderTime;
    
    private JPanel invoicePanel;
    
    public InvoiceDialog(Window parent, String orderNumber, List<CartItem> items,
                         double subtotal, double discount, double total,
                         String paymentMethod, double cashReceived, double change) {
        super(parent, "Hóa đơn thanh toán", ModalityType.APPLICATION_MODAL);
        
        this.orderNumber = orderNumber;
        this.items = items;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.cashReceived = cashReceived;
        this.change = change;
        this.orderTime = LocalDateTime.now();
        
        setSize(500, 650);
        setLocationRelativeTo(parent);
        setResizable(false);
        
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(Color.WHITE);
        setContentPane(root);
        
        // Invoice content
        invoicePanel = createInvoicePanel();
        JScrollPane scroll = new JScrollPane(invoicePanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        root.add(scroll, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        buttons.setOpaque(false);
        
        JButton printBtn = new JButton("In hóa đơn");
        printBtn.setFont(UIConstants.FONT_BODY_BOLD);
        printBtn.setBackground(UIConstants.PRIMARY_500);
        printBtn.setForeground(Color.WHITE);
        printBtn.setPreferredSize(new Dimension(130, 38));
        printBtn.addActionListener(e -> exportPdfDemo());
        
        JButton closeBtn = new JButton("Đóng");
        closeBtn.setFont(UIConstants.FONT_BODY_BOLD);
        closeBtn.setPreferredSize(new Dimension(100, 38));
        closeBtn.addActionListener(e -> dispose());
        
        buttons.add(printBtn);
        buttons.add(closeBtn);
        
        root.add(buttons, BorderLayout.SOUTH);
    }
    
    private JPanel createInvoicePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        // Header
        JLabel storeName = new JLabel("CỬA HÀNG ĐỒ ĂN NHANH", SwingConstants.CENTER);
        storeName.setFont(new Font("SansSerif", Font.BOLD, 18));
        storeName.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(storeName);
        
        JLabel storeAddress = new JLabel("Địa chỉ: 123 Đường ABC, Quận XYZ", SwingConstants.CENTER);
        storeAddress.setFont(new Font("SansSerif", Font.PLAIN, 11));
        storeAddress.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(storeAddress);
        
        JLabel storePhone = new JLabel("ĐT: 0123.456.789", SwingConstants.CENTER);
        storePhone.setFont(new Font("SansSerif", Font.PLAIN, 11));
        storePhone.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(storePhone);
        
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(createSeparator());
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        
        // Title
        JLabel title = new JLabel("HÓA ĐƠN BÁN HÀNG", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        
        // Order info
        JPanel infoPanel = new JPanel(new GridLayout(2, 2, 10, 4));
        infoPanel.setOpaque(false);
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        infoPanel.add(createLabel("Số HĐ:", Font.BOLD, 12));
        infoPanel.add(createLabel(orderNumber, Font.PLAIN, 12));
        infoPanel.add(createLabel("Ngày:", Font.BOLD, 12));
        infoPanel.add(createLabel(orderTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), Font.PLAIN, 12));
        
        panel.add(infoPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(createSeparator());
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        
        // Items header
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        
        JLabel hName = createLabel("Mon", Font.BOLD, 12);
        JPanel hRight = new JPanel(new GridLayout(1, 3, 10, 0));
        hRight.setOpaque(false);
        hRight.setPreferredSize(new Dimension(150, 20));
        hRight.add(createLabelRight("SL", Font.BOLD, 12));
        hRight.add(createLabelRight("Don gia", Font.BOLD, 12));
        hRight.add(createLabelRight("T.Tien", Font.BOLD, 12));
        
        headerRow.add(hName, BorderLayout.WEST);
        headerRow.add(hRight, BorderLayout.EAST);
        panel.add(headerRow);
        
        panel.add(createDashedSeparator());
        
        // Items
        for (CartItem item : items) {
            JPanel itemRow = new JPanel(new BorderLayout(5, 0));
            itemRow.setOpaque(false);
            
            String name = item.getItem().getName();
            
            JLabel itemName = createLabel(name, Font.PLAIN, 12);
            JPanel itemRight = new JPanel(new GridLayout(1, 3, 10, 0));
            itemRight.setOpaque(false);
            itemRight.setPreferredSize(new Dimension(150, 20));
            itemRight.add(createLabelRight(String.valueOf(item.getQuantity()), Font.PLAIN, 12));
            itemRight.add(createLabelRight(formatShort(item.getItem().getPrice()), Font.PLAIN, 12));
            itemRight.add(createLabelRight(formatShort(item.getLineTotal()), Font.PLAIN, 12));
            
            itemRow.add(itemName, BorderLayout.WEST);
            itemRow.add(itemRight, BorderLayout.EAST);
            panel.add(itemRow);
            panel.add(Box.createRigidArea(new Dimension(0, 2)));
        }
        
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(createSeparator());
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        
        // Totals
        panel.add(createTotalRow("Tổng tiền hàng:", CurrencyUtil.format(subtotal), false));
        if (discount > 0) {
            panel.add(createTotalRow("Giảm giá:", "-" + CurrencyUtil.format(discount), false));
        }
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(createTotalRow("TỔNG THANH TOÁN:", CurrencyUtil.format(total), true));
        
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(createDashedSeparator());
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        // Payment info
        String pmText = "Tiền mặt".equals(paymentMethod) || "Cash".equals(paymentMethod) ? "Tiền mặt" : "Chuyển khoản";
        panel.add(createTotalRow("Phương thức:", pmText, false));
        
        if ("Tiền mặt".equals(pmText) || "Cash".equals(paymentMethod)) {
            panel.add(createTotalRow("Tiền khách đưa:", CurrencyUtil.format(cashReceived), false));
            panel.add(createTotalRow("Tiền thừa:", CurrencyUtil.format(change), false));
        }
        
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(createSeparator());
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Footer
        JLabel thanks = new JLabel("Cảm ơn quý khách!", SwingConstants.CENTER);
        thanks.setFont(new Font("SansSerif", Font.BOLD, 14));
        thanks.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(thanks);
        
        JLabel welcome = new JLabel("Hẹn gặp lại!", SwingConstants.CENTER);
        welcome.setFont(new Font("SansSerif", Font.ITALIC, 12));
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(welcome);
        
        return panel;
    }
    
    private JLabel createLabel(String text, int style, int size) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", style, size));
        return lbl;
    }
    
    private JLabel createLabelRight(String text, int style, int size) {
        JLabel lbl = new JLabel(text, SwingConstants.RIGHT);
        lbl.setFont(new Font("SansSerif", style, size));
        return lbl;
    }
    
    private JPanel createTotalRow(String label, String value, boolean bold) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        
        int style = bold ? Font.BOLD : Font.PLAIN;
        int size = bold ? 14 : 12;
        
        row.add(createLabel(label, style, size), BorderLayout.WEST);
        row.add(createLabelRight(value, style, size), BorderLayout.EAST);
        
        return row;
    }
    
    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }
    
    private JPanel createDashedSeparator() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.GRAY);
                float[] dash = {4f, 4f};
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                g2.drawLine(0, getHeight()/2, getWidth(), getHeight()/2);
            }
        };
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        p.setPreferredSize(new Dimension(100, 5));
        return p;
    }
    
    private String formatShort(double value) {
        if (value >= 1000) {
            return String.format("%.0fk", value / 1000);
        }
        return String.format("%.0f", value);
    }
    
    private void printInvoice() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        
        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this, "Đã gửi lệnh in!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi in: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportPdfDemo() {
        PdfExportUtil.exportPanelToPdfDemo(invoicePanel, "HoaDon_" + orderNumber, this);
    }
    
    private void exportImage() {
        // Create image from invoice panel
        int w = invoicePanel.getWidth();
        int h = invoicePanel.getHeight();
        
        if (w <= 0 || h <= 0) {
            w = 400;
            h = 600;
            invoicePanel.setSize(w, h);
            invoicePanel.doLayout();
        }
        
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        invoicePanel.paint(g2);
        g2.dispose();
        
        // Save dialog
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("HoaDon_" + orderNumber + ".png"));
        
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
            
            try {
                ImageIO.write(image, "PNG", file);
                JOptionPane.showMessageDialog(this, 
                    "Đã lưu hóa đơn tại:\n" + file.getAbsolutePath(), 
                    "Thành công", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Lỗi lưu file: " + ex.getMessage(), 
                    "Lỗi", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }
        
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        
        // Scale to fit page
        double pageWidth = pageFormat.getImageableWidth();
        double pageHeight = pageFormat.getImageableHeight();
        double panelWidth = invoicePanel.getWidth();
        double panelHeight = invoicePanel.getHeight();
        
        double scaleX = pageWidth / panelWidth;
        double scaleY = pageHeight / panelHeight;
        double scale = Math.min(scaleX, scaleY);
        
        g2d.scale(scale, scale);
        invoicePanel.print(g2d);
        
        return PAGE_EXISTS;
    }
    
    /**
     * Hiển thị dialog hóa đơn
     */
    public static void show(Window parent, String orderNumber, List<CartItem> items,
                           double subtotal, double discount, double total,
                           String paymentMethod, double cashReceived, double change) {
        InvoiceDialog dialog = new InvoiceDialog(parent, orderNumber, items, 
            subtotal, discount, total, paymentMethod, cashReceived, change);
        dialog.setVisible(true);
    }
}
