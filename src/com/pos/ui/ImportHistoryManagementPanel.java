package com.pos.ui;

import com.pos.dao.ImportInvoiceDAO;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ImportHistoryManagementPanel extends JPanel {
    private final Runnable onDataChanged;
    private JTextField searchField;
    private DatePicker fromDatePicker, toDatePicker;
    private JTextField minTotalField, maxTotalField;
    private DefaultTableModel model;
    private JTable table;
    private List<ImportInvoiceDAO.ImportInvoiceSummary> currentInvoices = new ArrayList<>();

    public ImportHistoryManagementPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        setLayout(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        setOpaque(false);
        setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM));
        
        if (!ImportInvoiceDAO.supportsImportHistory()) {
            JPanel p = new JPanel(new GridBagLayout());
            p.setOpaque(false);
            JLabel msg = new JLabel("Khong tim thay bang inventory_transactions");
            msg.setFont(UIConstants.FONT_BODY);
            msg.setForeground(UIConstants.WARNING_DARK);
            p.add(msg);
            add(p, BorderLayout.CENTER);
            return;
        }
        
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

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        searchPanel.setOpaque(false);
        JLabel searchLabel = new JLabel("Tim kiem");
        searchLabel.setFont(UIConstants.FONT_BODY_BOLD);
        searchLabel.setForeground(UIConstants.NEUTRAL_700);
        searchPanel.add(searchLabel);
        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setPreferredSize(new Dimension(180, UIConstants.INPUT_HEIGHT_SM));
        searchField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true), new EmptyBorder(6, 10, 6, 10)));
        searchPanel.add(searchField);
        top.add(searchPanel);

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        datePanel.setOpaque(false);
        JLabel dateLabel = new JLabel("Ngay nhap");
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
        minTotalField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true), new EmptyBorder(6, 10, 6, 10)));
        totalPanel.add(minTotalField);
        JLabel toLabel2 = new JLabel("Den");
        toLabel2.setFont(UIConstants.FONT_BODY);
        toLabel2.setForeground(UIConstants.NEUTRAL_600);
        totalPanel.add(toLabel2);
        maxTotalField = new JTextField();
        maxTotalField.setFont(UIConstants.FONT_BODY);
        maxTotalField.setPreferredSize(new Dimension(100, UIConstants.INPUT_HEIGHT_SM));
        maxTotalField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true), new EmptyBorder(6, 10, 6, 10)));
        totalPanel.add(maxTotalField);
        top.add(totalPanel);

        searchField.addActionListener(e -> refreshTable());
        searchField.addKeyListener(new java.awt.event.KeyAdapter() { public void keyReleased(java.awt.event.KeyEvent e) { refreshTable(); } });
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
        JLabel titleLabel = new JLabel("Danh sach hoa don nhap");
        titleLabel.setFont(UIConstants.FONT_HEADING_3);
        titleLabel.setForeground(UIConstants.PRIMARY_700);
        titleLabel.setBorder(new EmptyBorder(0, 0, UIConstants.SPACING_SM, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        model = new DefaultTableModel(new Object[]{"Ma hoa don", "Ma nhan vien", "Nha cung cap", "Ngay nhap", "Tong tien"}, 0) { public boolean isCellEditable(int row, int col) { return false; } };
        table = new JTable(model);
        ModernTableStyle.apply(table, true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);
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
        ModernButton exportBtn = new ModernButton("Xuat Excel", ModernButton.ButtonType.GHOST, ModernButton.ButtonSize.SMALL);
        viewBtn.setPreferredSize(new Dimension(110, 32));
        printBtn.setPreferredSize(new Dimension(80, 32));
        exportBtn.setPreferredSize(new Dimension(90, 32));
        panel.add(viewBtn);
        panel.add(printBtn);
        panel.add(exportBtn);
        viewBtn.addActionListener(e -> viewInvoiceDetail());
        printBtn.addActionListener(e -> printInvoice());
        exportBtn.addActionListener(e -> exportExcel());
        return panel;
    }

    /**
     * Public method để refresh dữ liệu từ bên ngoài (khi chuyển tab)
     */
    public void refreshData() {
        refreshTable();
    }

    private void refreshTable() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        LocalDate fromDate = fromDatePicker.getDate();
        LocalDate toDate = toDatePicker.getDate();
		if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
			LocalDate tmp = fromDate;
			fromDate = toDate;
			toDate = tmp;
		}
        Double minTotal = null, maxTotal = null;
        try { if (!minTotalField.getText().trim().isEmpty()) minTotal = Double.parseDouble(minTotalField.getText().trim()); } catch (NumberFormatException ignored) {}
        try { if (!maxTotalField.getText().trim().isEmpty()) maxTotal = Double.parseDouble(maxTotalField.getText().trim()); } catch (NumberFormatException ignored) {}

        ImportInvoiceDAO.ImportFilter filter = new ImportInvoiceDAO.ImportFilter();
        filter.fromDate = fromDate;
        filter.toDate = toDate;
        filter.minTotal = minTotal;
        filter.maxTotal = maxTotal;

        currentInvoices = ImportInvoiceDAO.findInvoices(filter);
		if (fromDate != null || toDate != null) {
			List<ImportInvoiceDAO.ImportInvoiceSummary> filteredByDate = new ArrayList<>();
			for (ImportInvoiceDAO.ImportInvoiceSummary inv : currentInvoices) {
				LocalDate d = null;
				if (inv != null) {
					if (inv.getImportDate() != null) d = inv.getImportDate().toLocalDate();
					else if (inv.getKey() != null) d = inv.getKey().getDate();
				}
				if (d == null) continue;
				if (fromDate != null && d.isBefore(fromDate)) continue;
				if (toDate != null && d.isAfter(toDate)) continue;
				filteredByDate.add(inv);
			}
			currentInvoices = filteredByDate;
		}
        
        if (!keyword.isEmpty()) {
            List<ImportInvoiceDAO.ImportInvoiceSummary> filtered = new ArrayList<>();
            String kw = keyword.toLowerCase();
            for (int i = 0; i < currentInvoices.size(); i++) {
                ImportInvoiceDAO.ImportInvoiceSummary inv = currentInvoices.get(i);
                String code = generateCode(inv, i + 1);
                String empCode = inv.getEmployeeId() != null ? "NV" + String.format("%02d", inv.getEmployeeId()) : "";
                String suppCode = inv.getSupplierId() != null ? "NCC" + inv.getSupplierId() : "";
                String suppName = inv.getSupplier() != null ? inv.getSupplier() : "";
                if (code.toLowerCase().contains(kw) || empCode.toLowerCase().contains(kw) || suppCode.toLowerCase().contains(kw) || suppName.toLowerCase().contains(kw)) {
                    filtered.add(inv);
                }
            }
            currentInvoices = filtered;
        }

        model.setRowCount(0);
        for (int i = 0; i < currentInvoices.size(); i++) {
            ImportInvoiceDAO.ImportInvoiceSummary inv = currentInvoices.get(i);
            String code = generateCode(inv, i + 1);
            String empCode = inv.getEmployeeId() != null ? "NV" + String.format("%02d", inv.getEmployeeId()) : "";
            String suppCode = inv.getSupplierId() != null ? "NCC" + inv.getSupplierId() : "";
            String dateStr = "";
            if (inv.getImportDate() != null) { dateStr = inv.getImportDate().toLocalDate().toString(); }
            else if (inv.getKey() != null && inv.getKey().getDate() != null) { dateStr = inv.getKey().getDate().toString(); }
            String total = CurrencyUtil.format(inv.getTotalCost());
            model.addRow(new Object[]{code, empCode, suppCode, dateStr, total});
        }
        if (model.getRowCount() > 0) { table.setRowSelectionInterval(0, 0); }
    }

    private String generateCode(ImportInvoiceDAO.ImportInvoiceSummary inv, int seq) {
        String dbCode = inv.getInvoiceNumber();
        if (dbCode != null && !dbCode.trim().isEmpty()) { return dbCode; }
        String dateStr = "";
        if (inv.getImportDate() != null) { dateStr = inv.getImportDate().toLocalDate().toString().replace("-", ""); }
        else if (inv.getKey() != null && inv.getKey().getDate() != null) { dateStr = inv.getKey().getDate().toString().replace("-", ""); }
        else { dateStr = LocalDate.now().toString().replace("-", ""); }
        return "HDN-" + dateStr + "-" + String.format("%03d", seq);
    }

    private void viewInvoiceDetail() {
        int idx = table.getSelectedRow();
        if (idx < 0 || idx >= currentInvoices.size()) { JOptionPane.showMessageDialog(this, "Chon mot hoa don de xem chi tiet"); return; }
        ImportInvoiceDAO.ImportInvoiceSummary inv = currentInvoices.get(idx);
        List<ImportInvoiceDAO.ImportInvoiceLine> lines = ImportInvoiceDAO.findLines(inv.getKey());
        String code = generateCode(inv, idx + 1);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chi tiet hoa don nhap: " + code, true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(650, 450);
        dialog.setLocationRelativeTo(this);

        JPanel headerPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        headerPanel.setBorder(new EmptyBorder(15, 15, 10, 15));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.add(createLabel("Ma hoa don:", true));
        headerPanel.add(createLabel(code, false));
        final String dateStr = inv.getImportDate() != null
                ? inv.getImportDate().toLocalDate().toString()
                : (inv.getKey() != null && inv.getKey().getDate() != null ? inv.getKey().getDate().toString() : "");
        headerPanel.add(createLabel("Ngay nhap:", true));
        headerPanel.add(createLabel(dateStr, false));
        final String empStr = inv.getEmployeeName() != null ? inv.getEmployeeName() : (inv.getEmployeeId() != null ? "NV" + String.format("%02d", inv.getEmployeeId()) : "");
        headerPanel.add(createLabel("Nhan vien:", true));
        headerPanel.add(createLabel(empStr, false));
        final String suppStr = inv.getSupplier() != null ? inv.getSupplier() : (inv.getSupplierId() != null ? "NCC" + inv.getSupplierId() : "");
        headerPanel.add(createLabel("Nha cung cap:", true));
        headerPanel.add(createLabel(suppStr, false));
        dialog.add(headerPanel, BorderLayout.NORTH);

        DefaultTableModel detailModel = new DefaultTableModel(new Object[]{"STT", "Nguyen lieu", "So luong", "Don gia", "Thanh tien"}, 0) { public boolean isCellEditable(int row, int col) { return false; } };
        int stt = 1;
        for (ImportInvoiceDAO.ImportInvoiceLine line : lines) {
            detailModel.addRow(new Object[]{stt++, line.getIngredientName(), formatQty(line.getQuantity()), CurrencyUtil.format(line.getUnitPrice()), CurrencyUtil.format(line.getLineTotal())});
        }
        JTable detailTable = new JTable(detailModel);
        detailTable.setRowHeight(28);
        detailTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        detailTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        detailTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        detailTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        detailTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        JScrollPane scroll = new JScrollPane(detailTable);
        scroll.setBorder(new EmptyBorder(0, 15, 0, 15));
        dialog.add(scroll, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        footerPanel.setBackground(Color.WHITE);
        JLabel totalLabel = new JLabel("Tong cong: " + CurrencyUtil.format(inv.getTotalCost()));
        totalLabel.setFont(UIConstants.FONT_BODY_BOLD);
        footerPanel.add(totalLabel);
        ModernButton printBtn = new ModernButton("In", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.SMALL);
        printBtn.addActionListener(e -> {
            String headerInfo = "Mã HĐN: " + code + "\n" +
                "Ngày: " + dateStr + "\n" +
                "Nhân viên: " + empStr + "\n" +
                "Nhà cung cấp: " + suppStr;
            String footerInfo = "TỔNG CỘNG: " + CurrencyUtil.format(inv.getTotalCost());
            PdfExportUtil.exportTableToPdfDemo(detailTable, "HÓA ĐƠN NHẬP HÀNG", headerInfo, footerInfo,
                "HoaDonNhap_" + code, dialog);
        });
        footerPanel.add(printBtn);
        ModernButton closeBtn = new ModernButton("Dong", ModernButton.ButtonType.GHOST, ModernButton.ButtonSize.SMALL);
        closeBtn.addActionListener(e -> dialog.dispose());
        footerPanel.add(closeBtn);
        dialog.add(footerPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void printInvoice() {
        int idx = table.getSelectedRow();
        if (idx < 0 || idx >= currentInvoices.size()) { JOptionPane.showMessageDialog(this, "Chon mot hoa don de in"); return; }

        ImportInvoiceDAO.ImportInvoiceSummary inv = currentInvoices.get(idx);
        List<ImportInvoiceDAO.ImportInvoiceLine> lines = ImportInvoiceDAO.findLines(inv.getKey());
        String code = generateCode(inv, idx + 1);

        DefaultTableModel tempModel = new DefaultTableModel(new Object[]{"STT", "Nguyên liệu", "Số lượng", "Đơn giá", "Thành tiền"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        int stt = 1;
        for (ImportInvoiceDAO.ImportInvoiceLine line : lines) {
            tempModel.addRow(new Object[]{
                stt++,
                line.getIngredientName(),
                formatQty(line.getQuantity()),
                CurrencyUtil.format(line.getUnitPrice()),
                CurrencyUtil.format(line.getLineTotal())
            });
        }
        JTable tempTable = new JTable(tempModel);
        tempTable.setRowHeight(28);

        String dateStr = "";
        if (inv.getImportDate() != null) { dateStr = inv.getImportDate().toLocalDate().toString(); }
        else if (inv.getKey() != null && inv.getKey().getDate() != null) { dateStr = inv.getKey().getDate().toString(); }
        String empStr = inv.getEmployeeName() != null ? inv.getEmployeeName() : (inv.getEmployeeId() != null ? "NV" + String.format("%02d", inv.getEmployeeId()) : "");
        String suppStr = inv.getSupplier() != null ? inv.getSupplier() : (inv.getSupplierId() != null ? "NCC" + inv.getSupplierId() : "");

        String headerInfo = "Mã HĐN: " + code + "\n" +
            "Ngày: " + dateStr + "\n" +
            "Nhân viên: " + empStr + "\n" +
            "Nhà cung cấp: " + suppStr;
        String footerInfo = "TỔNG CỘNG: " + CurrencyUtil.format(inv.getTotalCost());

        PdfExportUtil.exportTableToPdfDemo(tempTable, "HÓA ĐƠN NHẬP HÀNG", headerInfo, footerInfo,
            "HoaDonNhap_" + code, this);
    }

    private void exportExcel() {
        if (model.getRowCount() <= 0) {
            JOptionPane.showMessageDialog(this, "Khong co hoa don nao de xuat");
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("danh_sach_hoa_don_nhap.xls"));
        fc.setDialogTitle("Xuat Excel");
        int res = fc.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".xls") && !f.getName().toLowerCase().endsWith(".csv")) {
            f = new File(f.getAbsolutePath() + ".xls");
        }

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            pw.print('\ufeff');

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

    private JLabel createLabel(String text, boolean bold) {
        JLabel label = new JLabel(text != null ? text : "");
        label.setFont(bold ? UIConstants.FONT_BODY_BOLD : UIConstants.FONT_BODY);
        return label;
    }

    private String formatQty(double qty) {
        if (qty == (long) qty) { return String.valueOf((long) qty); }
        return String.format("%.2f", qty);
    }
}