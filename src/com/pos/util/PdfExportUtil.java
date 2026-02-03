package com.pos.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Tiện ích xuất JPanel hoặc JTable thành file ảnh PNG hoặc in trực tiếp
 */
public class PdfExportUtil {

    /**
     * Xuất JPanel thành file ảnh PNG
     * @param panel Panel cần xuất
     * @param defaultFileName Tên file mặc định
     * @param parent Component cha để hiển thị dialog
     * @return true nếu xuất thành công
     */
    public static boolean exportPanelToImage(JPanel panel, String defaultFileName, Component parent) {
        int w = panel.getWidth();
        int h = panel.getHeight();
        
        if (w <= 0 || h <= 0) {
            w = 600;
            h = 800;
            panel.setSize(w, h);
            panel.doLayout();
        }
        
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        panel.paint(g2);
        g2.dispose();
        
        return saveImage(image, defaultFileName, parent);
    }

    public static boolean exportPanelToPdfDemo(JPanel panel, String defaultFileName, Component parent) {
        int w = panel.getWidth();
        int h = panel.getHeight();

        if (w <= 0 || h <= 0) {
            w = 600;
            h = 800;
            panel.setSize(w, h);
            panel.doLayout();
        }

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        panel.paint(g2);
        g2.dispose();

        return savePdfFromImage(image, defaultFileName, parent);
    }

    public static boolean exportTableToPdfDemo(JTable table, String title, String headerInfo,
                                               String footerInfo, String defaultFileName, Component parent) {
        JPanel invoicePanel = createInvoicePanel(table, title, headerInfo, footerInfo);

        invoicePanel.setSize(invoicePanel.getPreferredSize());
        invoicePanel.doLayout();
        layoutRecursive(invoicePanel);

        int w = invoicePanel.getWidth();
        int h = invoicePanel.getHeight();

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        invoicePanel.print(g2);
        g2.dispose();

        return savePdfFromImage(image, defaultFileName, parent);
    }

    /**
     * Xuất JTable thành file ảnh PNG
     * @param table Table cần xuất
     * @param title Tiêu đề hóa đơn
     * @param headerInfo Thông tin header (có thể null)
     * @param footerInfo Thông tin footer (có thể null)
     * @param defaultFileName Tên file mặc định
     * @param parent Component cha để hiển thị dialog
     * @return true nếu xuất thành công
     */
    public static boolean exportTableToImage(JTable table, String title, String headerInfo, 
                                             String footerInfo, String defaultFileName, Component parent) {
        // Tạo panel chứa table với format đẹp
        JPanel invoicePanel = createInvoicePanel(table, title, headerInfo, footerInfo);
        
        // Render panel
        invoicePanel.setSize(invoicePanel.getPreferredSize());
        invoicePanel.doLayout();
        layoutRecursive(invoicePanel);
        
        int w = invoicePanel.getWidth();
        int h = invoicePanel.getHeight();
        
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        invoicePanel.print(g2);
        g2.dispose();
        
        return saveImage(image, defaultFileName, parent);
    }

    /**
     * Tạo panel hóa đơn từ JTable
     */
    private static JPanel createInvoicePanel(JTable table, String title, String headerInfo, String footerInfo) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        // Header - Tên cửa hàng
        JLabel storeName = new JLabel("CỬA HÀNG ĐỒ ĂN NHANH");
        storeName.setFont(new Font("SansSerif", Font.BOLD, 18));
        storeName.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(storeName);
        
        JLabel storeAddress = new JLabel("Địa chỉ: 123 Đường ABC, Quận XYZ");
        storeAddress.setFont(new Font("SansSerif", Font.PLAIN, 11));
        storeAddress.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(storeAddress);
        
        JLabel storePhone = new JLabel("ĐT: 0123.456.789");
        storePhone.setFont(new Font("SansSerif", Font.PLAIN, 11));
        storePhone.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(storePhone);
        
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Header info
        if (headerInfo != null && !headerInfo.isEmpty()) {
            JLabel headerLabel = new JLabel("<html>" + headerInfo.replace("\n", "<br>") + "</html>");
            headerLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(headerLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        
        // Separator
        JSeparator sep1 = new JSeparator();
        sep1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(sep1);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Table - tạo bản sao để render
        JTable tableCopy = createTableCopy(table);
        tableCopy.setPreferredScrollableViewportSize(tableCopy.getPreferredSize());
        
        // Tạo panel chứa table header và table body
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.add(tableCopy.getTableHeader(), BorderLayout.NORTH);
        tablePanel.add(tableCopy, BorderLayout.CENTER);
        tablePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, tableCopy.getPreferredSize().height + tableCopy.getTableHeader().getPreferredSize().height));
        tablePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(tablePanel);
        
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Separator
        JSeparator sep2 = new JSeparator();
        sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(sep2);
        
        // Footer info
        if (footerInfo != null && !footerInfo.isEmpty()) {
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            JLabel footerLabel = new JLabel("<html>" + footerInfo.replace("\n", "<br>") + "</html>");
            footerLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            footerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(footerLabel);
        }
        
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Thank you
        JLabel thanks = new JLabel("Cảm ơn quý khách!");
        thanks.setFont(new Font("SansSerif", Font.BOLD, 14));
        thanks.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(thanks);
        
        return panel;
    }

    /**
     * Tạo bản sao của table để render
     */
    private static JTable createTableCopy(JTable source) {
        int rowCount = source.getRowCount();
        int colCount = source.getColumnCount();
        
        String[] columns = new String[colCount];
        for (int c = 0; c < colCount; c++) {
            columns[c] = source.getColumnName(c);
        }
        
        Object[][] data = new Object[rowCount][colCount];
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < colCount; c++) {
                data[r][c] = source.getValueAt(r, c);
            }
        }
        
        JTable copy = new JTable(data, columns);
        copy.setRowHeight(25);
        copy.setFont(new Font("SansSerif", Font.PLAIN, 11));
        copy.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        copy.setShowGrid(true);
        copy.setGridColor(Color.LIGHT_GRAY);
        
        // Set column widths from source
        for (int c = 0; c < colCount; c++) {
            int width = source.getColumnModel().getColumn(c).getPreferredWidth();
            copy.getColumnModel().getColumn(c).setPreferredWidth(width);
        }
        
        return copy;
    }

    /**
     * Lưu ảnh vào file
     */
    private static boolean saveImage(BufferedImage image, String defaultFileName, Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultFileName + ".png"));
        
        int result = chooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
            
            try {
                ImageIO.write(image, "PNG", file);
                JOptionPane.showMessageDialog(parent, 
                    "Đã lưu hóa đơn tại:\n" + file.getAbsolutePath(), 
                    "Thành công", 
                    JOptionPane.INFORMATION_MESSAGE);
                return true;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, 
                    "Lỗi lưu file: " + ex.getMessage(), 
                    "Lỗi", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    private static boolean savePdfFromImage(BufferedImage image, String defaultFileName, Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultFileName + ".pdf"));

        int result = chooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) return false;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getAbsolutePath() + ".pdf");
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            writeSinglePageImagePdf(fos, image);
            JOptionPane.showMessageDialog(parent,
                    "Đã lưu hóa đơn tại:\n" + file.getAbsolutePath(),
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    "Lỗi lưu file: " + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private static void writeSinglePageImagePdf(OutputStream out, BufferedImage image) throws Exception {
        int imgW = Math.max(1, image.getWidth());
        int imgH = Math.max(1, image.getHeight());

        ByteArrayOutputStream jpg = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", jpg);
        byte[] imgBytes = jpg.toByteArray();

        int pageW = 595;
        int pageH = 842;
        double s = Math.min(pageW / (double) imgW, pageH / (double) imgH);
        if (s <= 0) s = 1.0;
        double drawW = imgW * s;
        double drawH = imgH * s;
        double x = (pageW - drawW) / 2.0;
        double y = (pageH - drawH) / 2.0;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);

        writeAscii(baos, "%PDF-1.4\n");

        offsets.add(baos.size());
        writeAscii(baos, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        offsets.add(baos.size());
        writeAscii(baos, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        offsets.add(baos.size());
        writeAscii(baos,
                "3 0 obj\n" +
                        "<< /Type /Page /Parent 2 0 R\n" +
                        "/Resources << /XObject << /Im0 4 0 R >> /ProcSet [/PDF /ImageC] >>\n" +
                        "/MediaBox [0 0 " + pageW + " " + pageH + "]\n" +
                        "/Contents 5 0 R >>\n" +
                        "endobj\n");

        offsets.add(baos.size());
        writeAscii(baos,
                "4 0 obj\n" +
                        "<< /Type /XObject /Subtype /Image /Width " + imgW + " /Height " + imgH +
                        " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length " + imgBytes.length + " >>\n" +
                        "stream\n");
        baos.write(imgBytes);
        writeAscii(baos, "\nendstream\nendobj\n");

        String content = "q " + fmt(drawW) + " 0 0 " + fmt(drawH) + " " + fmt(x) + " " + fmt(y) + " cm /Im0 Do Q";
        byte[] contentBytes = content.getBytes(StandardCharsets.US_ASCII);
        offsets.add(baos.size());
        writeAscii(baos,
                "5 0 obj\n" +
                        "<< /Length " + contentBytes.length + " >>\n" +
                        "stream\n");
        baos.write(contentBytes);
        writeAscii(baos, "\nendstream\nendobj\n");

        int xrefPos = baos.size();
        writeAscii(baos, "xref\n");
        writeAscii(baos, "0 " + offsets.size() + "\n");
        writeAscii(baos, String.format("%010d 65535 f \n", 0));
        for (int i = 1; i < offsets.size(); i++) {
            writeAscii(baos, String.format("%010d 00000 n \n", offsets.get(i)));
        }
        writeAscii(baos,
                "trailer\n" +
                        "<< /Size " + offsets.size() + " /Root 1 0 R >>\n" +
                        "startxref\n" +
                        xrefPos + "\n" +
                        "%%EOF\n");

        out.write(baos.toByteArray());
    }

    private static void writeAscii(ByteArrayOutputStream baos, String s) {
        try {
            baos.write(s.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception ignored) {
        }
    }

    private static String fmt(double v) {
        String s = String.format(java.util.Locale.US, "%.2f", v);
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
            if (s.endsWith(".")) return s.substring(0, s.length() - 1);
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * Layout đệ quy tất cả component con
     */
    private static void layoutRecursive(Container c) {
        c.doLayout();
        for (Component child : c.getComponents()) {
            if (child instanceof Container) {
                layoutRecursive((Container) child);
            }
        }
    }

    /**
     * In trực tiếp một panel
     */
    public static void printPanel(JPanel panel, Component parent) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            
            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            
            double pageWidth = pageFormat.getImageableWidth();
            double pageHeight = pageFormat.getImageableHeight();
            double panelWidth = panel.getWidth();
            double panelHeight = panel.getHeight();
            
            double scaleX = pageWidth / panelWidth;
            double scaleY = pageHeight / panelHeight;
            double scale = Math.min(scaleX, scaleY);
            
            g2d.scale(scale, scale);
            panel.print(g2d);
            
            return Printable.PAGE_EXISTS;
        });
        
        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(parent, "Đã gửi lệnh in!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(parent, "Lỗi in: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
