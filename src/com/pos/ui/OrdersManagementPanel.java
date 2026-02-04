package com.pos.ui;

import com.pos.dao.OrderDAO;
import com.pos.dao.CustomerDAO;
import com.pos.model.Customer;
import com.pos.util.CurrencyUtil;
import com.pos.util.PdfExportUtil;
import com.pos.ui.components.CardPanel;
import com.pos.ui.components.DatePicker;
import com.pos.ui.components.ModernButton;
import com.pos.ui.components.ModernTableStyle;
import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class OrdersManagementPanel extends JPanel {
    private final Runnable onDataChanged;

    private JTextField searchField;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private JTextField minTotalField;
    private JTextField maxTotalField;

    private DefaultTableModel model;
    private JTable table;

    private List<OrderDAO.OrderSummary> currentOrders = new ArrayList<>();

    public OrdersManagementPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        setOpaque(false);
        setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM));

        add(buildFilterBar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);

        refreshTable();
    }

    private JPanel buildFilterBar() {
        CardPanel top = new CardPanel(new FlowLayout(FlowLayout.LEFT, UIConstants.SPACING_LG, UIConstants.SPACING_SM));
        top.setShadowSize(2);
        top.setRadius(UIConstants.RADIUS_MD);
        top.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

        // === Tim kiem ===
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        searchPanel.setOpaque(false);
        JLabel searchLabel = new JLabel("Tim kiem");
        searchLabel.setFont(UIConstants.FONT_BODY_BOLD);
        searchLabel.setForeground(UIConstants.NEUTRAL_700);
        searchPanel.add(searchLabel);

        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setPreferredSize(new Dimension(180, UIConstants.INPUT_HEIGHT_SM));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        searchPanel.add(searchField);
        top.add(searchPanel);

        // === Ngay lap ===
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        datePanel.setOpaque(false);
        JLabel dateLabel = new JLabel("Ngay lap");
        dateLabel.setFont(UIConstants.FONT_BODY_BOLD);
        dateLabel.setForeground(UIConstants.NEUTRAL_700);
        datePanel.add(dateLabel);

        JLabel fromLabel = new JLabel("Tu");
        fromLabel.setFont(UIConstants.FONT_BODY);
        fromLabel.setForeground(UIConstants.NEUTRAL_600);
        datePanel.add(fromLabel);

        fromDatePicker = new DatePicker();
        fromDatePicker.setPreferredSize(new Dimension(120, UIConstants.INPUT_HEIGHT_SM));
        datePanel.add(fromDatePicker);

        JLabel toLabel = new JLabel("Den");
        toLabel.setFont(UIConstants.FONT_BODY);
        toLabel.setForeground(UIConstants.NEUTRAL_600);
        datePanel.add(toLabel);

        toDatePicker = new DatePicker();
        toDatePicker.setPreferredSize(new Dimension(120, UIConstants.INPUT_HEIGHT_SM));
        datePanel.add(toDatePicker);
        top.add(datePanel);

        // === Tong tien ===
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        totalPanel.setOpaque(false);
        JLabel totalLabel = new JLabel("Tong tien");
        totalLabel.setFont(UIConstants.FONT_BODY_BOLD);
        totalLabel.setForeground(UIConstants.NEUTRAL_700);
        totalPanel.add(totalLabel);

        JLabel fromLabel2 = new JLabel("Tu");
        fromLabel2.setFont(UIConstants.FONT_BODY);
        fromLabel2.setForeground(UIConstants.NEUTRAL_600);
        totalPanel.add(fromLabel2);

        minTotalField = new JTextField();
        minTotalField.setFont(UIConstants.FONT_BODY);
        minTotalField.setPreferredSize(new Dimension(100, UIConstants.INPUT_HEIGHT_SM));
        minTotalField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        totalPanel.add(minTotalField);

        JLabel toLabel2 = new JLabel("Den");
        toLabel2.setFont(UIConstants.FONT_BODY);
        toLabel2.setForeground(UIConstants.NEUTRAL_600);
        totalPanel.add(toLabel2);

        maxTotalField = new JTextField();
        maxTotalField.setFont(UIConstants.FONT_BODY);
        maxTotalField.setPreferredSize(new Dimension(100, UIConstants.INPUT_HEIGHT_SM));
        maxTotalField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        totalPanel.add(maxTotalField);
        top.add(totalPanel);

        // Auto filter
        searchField.addActionListener(e -> refreshTable());
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { refreshTable(); }
        });
        minTotalField.addActionListener(e -> refreshTable());
        maxTotalField.addActionListener(e -> refreshTable());
        fromDatePicker.addPropertyChangeListener("date", e -> refreshTable());
        toDatePicker.addPropertyChangeListener("date", e -> refreshTable());
        fromDatePicker.addActionListener(e -> refreshTable());
        toDatePicker.addActionListener(e -> refreshTable());

        return top;
    }

    private JPanel buildTable() {
        CardPanel panel = new CardPanel(new BorderLayout());
        panel.setShadowSize(2);
        panel.setRadius(UIConstants.RADIUS_LG);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

        // Title
        JLabel titleLabel = new JLabel("Danh sach hoa don");
        titleLabel.setFont(UIConstants.FONT_HEADING_3);
        titleLabel.setForeground(UIConstants.PRIMARY_700);
        titleLabel.setBorder(new EmptyBorder(0, 0, UIConstants.SPACING_SM, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{
                "Ma hoa don", "Ma nhan vien", "Ma khach hang", "Ma khuyen mai", "Ngay lap", "Tien giam gia", "Tong tien"
        }, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(model);
        ModernTableStyle.apply(table, true);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(110);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SPACING_SM, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_SM, 0, 0, 0));

        ModernButton viewBtn = new ModernButton("Xem chi tiet", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        ModernButton printBtn = new ModernButton("In PDF", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.SMALL);
        ModernButton exportBtn = new ModernButton("Xuat Excel", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);

        viewBtn.setPreferredSize(new Dimension(110, 32));
        printBtn.setPreferredSize(new Dimension(80, 32));
        exportBtn.setPreferredSize(new Dimension(90, 32));

        panel.add(viewBtn);
        panel.add(printBtn);
        panel.add(exportBtn);

        viewBtn.addActionListener(e -> viewOrderDetail());
        printBtn.addActionListener(e -> printOrder());
        exportBtn.addActionListener(e -> exportExcel());

        return panel;
    }

    private void exportExcel() {
        if (model.getRowCount() <= 0) {
            JOptionPane.showMessageDialog(this, "Khong co hoa don nao de xuat");
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("danh_sach_hoa_don.xls"));
        fc.setDialogTitle("Xuat Excel");
        int res = fc.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".xls") && !f.getName().toLowerCase().endsWith(".csv")) {
            f = new File(f.getAbsolutePath() + ".xls");
        }

        boolean isXls = f.getName().toLowerCase().endsWith(".xls");

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            pw.print('\ufeff');

            if (isXls) {
                pw.println("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/></head><body>");
                pw.println("<table border='1' style='border-collapse:collapse;font-family:Segoe UI;font-size:12pt'>");
                pw.println("<tr><td colspan='" + model.getColumnCount() + "' style='font-weight:bold;background:#E6FFFA'>" + escapeHtml("DANH SÁCH HÓA ĐƠN") + "</td></tr>");

                pw.print("<tr style='font-weight:bold'>");
                for (int c = 0; c < model.getColumnCount(); c++) {
                    pw.print("<td>" + escapeHtml(String.valueOf(model.getColumnName(c))) + "</td>");
                }
                pw.println("</tr>");

                for (int r = 0; r < model.getRowCount(); r++) {
                    pw.print("<tr>");
                    for (int c = 0; c < model.getColumnCount(); c++) {
                        Object v = model.getValueAt(r, c);
                        String text = v == null ? "" : String.valueOf(v);
                        boolean right = (c == 5 || c == 6);
                        pw.print("<td" + (right ? " style='text-align:right'" : "") + ">" + escapeHtml(text) + "</td>");
                    }
                    pw.println("</tr>");
                }
                pw.println("</table></body></html>");

                JOptionPane.showMessageDialog(this, "Da xuat Excel thanh cong:\n" + f.getAbsolutePath());
                return;
            }

            int colCount = model.getColumnCount();
            for (int c = 0; c < colCount; c++) {
                if (c > 0) pw.print(",");
                pw.print(escapeCsv(String.valueOf(model.getColumnName(c))));
            }
            pw.println();

            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < colCount; c++) {
                    if (c > 0) pw.print(",");
                    Object v = model.getValueAt(r, c);
                    pw.print(escapeCsv(v == null ? "" : String.valueOf(v)));
                }
                pw.println();
            }

            JOptionPane.showMessageDialog(this, "Da xuat Excel thanh cong:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Khong the xuat Excel: " + ex.getMessage(), "Loi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        String t = s;
        boolean needs = t.contains(",") || t.contains("\"") || t.contains("\n") || t.contains("\r");
        if (t.contains("\"")) t = t.replace("\"", "\"\"");
        if (needs) t = "\"" + t + "\"";
        return t;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        String t = s;
        t = t.replace("&", "&amp;");
        t = t.replace("<", "&lt;");
        t = t.replace(">", "&gt;");
        t = t.replace("\"", "&quot;");
        return t;
    }

    /** Public method to refresh data from outside */
    public void refreshData() {
        refreshTable();
    }

    private void refreshTable() {
        String keyword = searchField.getText() == null ? null : searchField.getText().trim();
        LocalDate fromDate = fromDatePicker.getDate();
        LocalDate toDate = toDatePicker.getDate();
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }
        
        Double minTotal = null;
        Double maxTotal = null;
        try {
            String minStr = minTotalField.getText().trim();
            if (!minStr.isEmpty()) {
                minTotal = Double.parseDouble(minStr);
            }
        } catch (NumberFormatException ignored) {}
        
        try {
            String maxStr = maxTotalField.getText().trim();
            if (!maxStr.isEmpty()) {
                maxTotal = Double.parseDouble(maxStr);
            }
        } catch (NumberFormatException ignored) {}

        OrderDAO.OrderFilter filter = new OrderDAO.OrderFilter();
        filter.fromDate = fromDate;
        filter.toDate = toDate;
        filter.minTotal = minTotal;
        filter.maxTotal = maxTotal;

        if (keyword != null && !keyword.isEmpty()) {
            filter.orderNumber = keyword;
            if (keyword.toUpperCase().startsWith("NV")) {
                try {
                    int empId = Integer.parseInt(keyword.substring(2));
                    filter.employeeId = empId;
                } catch (Exception ignored) {}
            }
            if (keyword.toUpperCase().startsWith("KH")) {
                try {
                    int custId = Integer.parseInt(keyword.substring(2));
                    filter.customerId = custId;
                } catch (Exception ignored) {}
            }
        }

        currentOrders = OrderDAO.findOrders(filter);

        if (fromDate != null || toDate != null) {
            List<OrderDAO.OrderSummary> filteredByDate = new ArrayList<>();
            for (OrderDAO.OrderSummary o : currentOrders) {
                if (o == null || o.getOrderTime() == null) continue;
                LocalDate d = o.getOrderTime().toLocalDate();
                if (fromDate != null && d.isBefore(fromDate)) continue;
                if (toDate != null && d.isAfter(toDate)) continue;
                filteredByDate.add(o);
            }
            currentOrders = filteredByDate;
        }

        model.setRowCount(0);
        for (OrderDAO.OrderSummary order : currentOrders) {
            String orderId = order.getOrderNumber();
            String empCode = order.getEmployeeId() != null ? "NV" + String.format("%02d", order.getEmployeeId()) : "";
            String custCode = order.getCustomerId() != null ? "KH" + String.format("%02d", order.getCustomerId()) : "";
            String promoCode = order.getPromotionCode() != null ? order.getPromotionCode() : "";
            String orderDate = order.getOrderTime() != null ? order.getOrderTime().toLocalDate().toString() : "";
            String discount = CurrencyUtil.format(order.getDiscount());
            String total = CurrencyUtil.format(order.getTotal());

            model.addRow(new Object[]{
                orderId, empCode, custCode, promoCode, orderDate, discount, total
            });
        }

        if (model.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private void viewOrderDetail() {
        int idx = table.getSelectedRow();
        if (idx < 0 || idx >= currentOrders.size()) {
            JOptionPane.showMessageDialog(this, "Chon mot hoa don de xem chi tiet");
            return;
        }

        OrderDAO.OrderSummary order = currentOrders.get(idx);
        List<OrderDAO.OrderLine> lines = OrderDAO.findOrderLines(order.getOrderId());

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chi tiet hoa don: " + order.getOrderNumber(), true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(650, 450);
        dialog.setLocationRelativeTo(this);

        // Header info
        JPanel headerPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        headerPanel.setBorder(new EmptyBorder(15, 15, 10, 15));
        headerPanel.setBackground(Color.WHITE);
        
        headerPanel.add(createLabel("Ma hoa don:", true));
        headerPanel.add(createLabel(order.getOrderNumber(), false));
        headerPanel.add(createLabel("Ngay lap:", true));
        headerPanel.add(createLabel(order.getOrderTime() != null ? order.getOrderTime().toLocalDate().toString() : "", false));
		headerPanel.add(createLabel("Nhan vien:", true));
		headerPanel.add(createLabel(order.getEmployeeName() != null ? order.getEmployeeName() : (order.getEmployeeId() != null ? "NV" + String.format("%02d", order.getEmployeeId()) : ""), false));
		headerPanel.add(createLabel("Khach hang:", true));
		headerPanel.add(createLabel(order.getCustomerName() != null ? order.getCustomerName() : (order.getCustomerId() != null ? "KH" + String.format("%02d", order.getCustomerId()) : ""), false));

		String customerPhone = parseCustomerPhone(order.getNotes());
		if (customerPhone == null || customerPhone.trim().isEmpty()) {
			customerPhone = resolveCustomerPhoneByName(order.getCustomerName());
		}
		if (customerPhone == null || customerPhone.trim().isEmpty()) customerPhone = "-";
		final String customerPhoneForPrint = customerPhone;

		headerPanel.add(createLabel("SDT:", true));
		headerPanel.add(createLabel(customerPhone, false));

		dialog.add(headerPanel, BorderLayout.NORTH);

        DefaultTableModel detailModel = new DefaultTableModel(new Object[]{"STT", "Ten mon", "SL", "Don gia", "Thanh tien"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        
        int stt = 1;
        for (OrderDAO.OrderLine line : lines) {
            detailModel.addRow(new Object[]{
                stt++,
                line.getProductName(),
                line.getQuantity(),
                CurrencyUtil.format(line.getUnitPrice()),
                CurrencyUtil.format(line.getLineTotal())
            });
        }

        JTable detailTable = new JTable(detailModel);
        detailTable.setRowHeight(28);
        detailTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        detailTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        detailTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        detailTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        detailTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        
        JScrollPane scroll = new JScrollPane(detailTable);
        scroll.setBorder(new EmptyBorder(0, 15, 0, 15));
        dialog.add(scroll, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        footerPanel.setBackground(Color.WHITE);
        
        footerPanel.add(createLabel("Giam gia: " + CurrencyUtil.format(order.getDiscount()), false));
        footerPanel.add(new JLabel("   |   "));
        JLabel totalLabel = new JLabel("Tong cong: " + CurrencyUtil.format(order.getTotal()));
        totalLabel.setFont(UIConstants.FONT_BODY_BOLD);
        footerPanel.add(totalLabel);
        
        ModernButton printBtn = new ModernButton("In", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.SMALL);
        printBtn.addActionListener(e -> {
			String phoneLine = customerPhoneForPrint == null || customerPhoneForPrint.trim().isEmpty() || "-".equals(customerPhoneForPrint.trim())
					? ""
					: ("\nSĐT: " + customerPhoneForPrint.trim());
            String headerInfo = "Mã HĐ: " + order.getOrderNumber() + "\n" +
                "Ngày: " + (order.getOrderTime() != null ? order.getOrderTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "") + "\n" +
                "Nhân viên: " + (order.getEmployeeName() != null ? order.getEmployeeName() : "") + "\n" +
                "Khách hàng: " + (order.getCustomerName() != null ? order.getCustomerName() : "") +
				phoneLine;
            String footerInfo = "Giảm giá: " + CurrencyUtil.format(order.getDiscount()) + "\n" +
                "TỔNG CỘNG: " + CurrencyUtil.format(order.getTotal());
            PdfExportUtil.exportTableToPdfDemo(detailTable, "HÓA ĐƠN BÁN HÀNG", headerInfo, footerInfo, 
                "HoaDon_" + order.getOrderNumber(), dialog);
        });
        footerPanel.add(printBtn);
        
        ModernButton closeBtn = new ModernButton("Dong", ModernButton.ButtonType.GHOST, ModernButton.ButtonSize.SMALL);
        closeBtn.addActionListener(e -> dialog.dispose());
        footerPanel.add(closeBtn);
        
        dialog.add(footerPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void printOrder() {
        int idx = table.getSelectedRow();
        if (idx < 0 || idx >= currentOrders.size()) {
            JOptionPane.showMessageDialog(this, "Chon mot hoa don de in");
            return;
        }
        
        OrderDAO.OrderSummary order = currentOrders.get(idx);
        List<OrderDAO.OrderLine> lines = OrderDAO.findOrderLines(order.getOrderId());
        
        // Tạo table tạm để xuất
        DefaultTableModel tempModel = new DefaultTableModel(new Object[]{"STT", "Tên món", "SL", "Đơn giá", "Thành tiền"}, 0);
        int stt = 1;
        for (OrderDAO.OrderLine line : lines) {
            tempModel.addRow(new Object[]{
                stt++,
                line.getProductName(),
                line.getQuantity(),
                CurrencyUtil.format(line.getUnitPrice()),
                CurrencyUtil.format(line.getLineTotal())
            });
        }
        JTable tempTable = new JTable(tempModel);
        tempTable.setRowHeight(28);
        tempTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        tempTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        tempTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        tempTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        tempTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        
        String headerInfo = "Mã HĐ: " + order.getOrderNumber() + "\n" +
            "Ngày: " + (order.getOrderTime() != null ? order.getOrderTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "") + "\n" +
            "Nhân viên: " + (order.getEmployeeName() != null ? order.getEmployeeName() : "") + "\n" +
            "Khách hàng: " + (order.getCustomerName() != null ? order.getCustomerName() : "");
        String footerInfo = "Giảm giá: " + CurrencyUtil.format(order.getDiscount()) + "\n" +
            "TỔNG CỘNG: " + CurrencyUtil.format(order.getTotal());
        
        PdfExportUtil.exportTableToPdfDemo(tempTable, "HÓA ĐƠN BÁN HÀNG", headerInfo, footerInfo, 
            "HoaDon_" + order.getOrderNumber(), this);
    }

    private JLabel createLabel(String text, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(bold ? UIConstants.FONT_BODY_BOLD : UIConstants.FONT_BODY);
        return label;
    }

	private String resolveCustomerPhoneByName(String customerName) {
		if (customerName == null || customerName.trim().isEmpty()) return null;
		String name = customerName.trim();
		try {
			List<Customer> list = CustomerDAO.findByFilter(name, null, false);
			if (list == null || list.isEmpty()) return null;
			String onlyPhone = null;
			for (Customer c : list) {
				if (c == null) continue;
				String fn = c.getFullName() == null ? "" : c.getFullName().trim();
				if (!fn.equalsIgnoreCase(name)) continue;
				String p = c.getPhone() == null ? "" : c.getPhone().trim();
				if (p.isEmpty()) continue;
				if (onlyPhone == null) onlyPhone = p;
				else if (!onlyPhone.equals(p)) return null;
			}
			return onlyPhone;
		} catch (Exception ignored) {
			return null;
		}
	}

	private String parseCustomerPhone(String notes) {
		if (notes == null) return null;
		String s = notes.trim();
		String[] keys = {"SĐT:", "SDT:", "Sdt:", "Sđt:"};
		int idx = -1;
		String key = null;
		for (String k : keys) {
			int p = s.indexOf(k);
			if (p >= 0) {
				idx = p;
				key = k;
				break;
			}
		}
		if (idx < 0 || key == null) return null;
		String sub = s.substring(idx + key.length()).trim();
		int sep = sub.indexOf('|');
		if (sep >= 0) sub = sub.substring(0, sep).trim();
		if (sub.isEmpty()) return null;
		return sub;
	}
}
