package com.pos.ui;

import com.pos.dao.DashboardDAO;
import com.pos.util.CurrencyUtil;
import com.pos.ui.theme.UIConstants;
import com.pos.ui.components.*;

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
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel Thống kê tổng hợp với các tab:
 * - Thống kê tổng quát (Doanh thu, Đơn hàng, KPI)
 * - Thống kê Món ăn (Top bán chạy)
 * - Thống kê Nhân viên
 * - Thống kê Khách hàng
 * - Thống kê Nhà cung cấp (Nhập hàng)
 * 
 * HƯỚNG DẪN VỀ RESET/LỌC THỜI GIAN:
 * - Thống kê KHÔNG nên reset dữ liệu, chỉ nên lọc theo khoảng thời gian
 * - Các khoảng thời gian hợp lý:
 *   + Hôm nay: Xem doanh thu trong ngày
 *   + Tuần này: Xem tuần hiện tại (Thứ 2 - CN)
 *   + Tháng này: Mặc định, phổ biến nhất cho báo cáo
 *   + Quý này: Báo cáo quý
 *   + Năm nay: Báo cáo năm
 *   + Tùy chọn: Cho phép chọn khoảng bất kỳ
 * - Dữ liệu lịch sử KHÔNG BAO GIỜ bị xóa, chỉ được lọc hiển thị
 */
public class DashboardManagementPanel extends JPanel {
    private final Runnable onDataChanged;
    
    // Tab panels
    private JTabbedPane mainTabs;
    
    // Date filter
    private JComboBox<String> quickDateCombo;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    
    // Overview tab
    private KPICard revenueCard;
    private KPICard ordersCard;
    private KPICard avgCard;
    private KPICard importCostCard;
    private KPICard profitCard;
    private DefaultTableModel lowStockModel;
    private JTable lowStockTable;
    
    // Products tab
    private DefaultTableModel topProductsModel;
    private JTable topProductsTable;
    private KPICard totalProductsCard;
    private KPICard topProductCard;
    private KPICard totalSoldQtyCard;
    
    // Employees tab
    private DefaultTableModel employeeStatsModel;
    private JTable employeeStatsTable;
    private KPICard totalEmployeesCard;
    private KPICard topEmployeeCard;
    
    // Customers tab
    private DefaultTableModel customerStatsModel;
    private JTable customerStatsTable;
    private KPICard totalCustomersCard;
    private KPICard topCustomerCard;
    private KPICard newCustomersCard;
    
    // Suppliers tab
    private DefaultTableModel supplierStatsModel;
    private JTable supplierStatsTable;
    private KPICard totalSuppliersCard;
    private KPICard totalImportCostCard;
    private KPICard topSupplierCard;
    
    // Quick date options
    private static final String[] QUICK_DATE_OPTIONS = {
        "Hôm nay",
        "Hôm qua", 
        "7 ngày qua",
        "Tuần này",
        "Tháng này",
        "Tháng trước",
        "Quý này",
        "Năm nay",
        "Tất cả"
    };
    
    public DashboardManagementPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        
        setLayout(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        setOpaque(false);
        setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM));
        
        // Filter bar at top
        add(buildFilterBar(), BorderLayout.NORTH);
        
        // Main tabbed pane
        mainTabs = new JTabbedPane();
        mainTabs.setFont(UIConstants.FONT_BODY_BOLD);
        mainTabs.addTab("Tổng quát", createOverviewTab());
        mainTabs.addTab("Món ăn", createProductsTab());
        mainTabs.addTab("Nhân viên", createEmployeesTab());
        mainTabs.addTab("Khách hàng", createCustomersTab());
        mainTabs.addTab("Nhà cung cấp", createSuppliersTab());
        
        add(mainTabs, BorderLayout.CENTER);
        
        // Footer with actions
        add(buildActionsPanel(), BorderLayout.SOUTH);
        
        // Default: This month
        quickDateCombo.setSelectedItem("Tháng này");
        applyQuickDate();
    }
    
    private JPanel buildFilterBar() {
        CardPanel top = new CardPanel(new FlowLayout(FlowLayout.LEFT, UIConstants.SPACING_MD, UIConstants.SPACING_SM));
        top.setShadowSize(2);
        top.setRadius(UIConstants.RADIUS_MD);
        top.setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_MD, UIConstants.SPACING_SM, UIConstants.SPACING_MD));
        
        // Quick date selection
        JLabel quickLabel = new JLabel("Chọn nhanh:");
        quickLabel.setFont(UIConstants.FONT_BODY_BOLD);
        quickLabel.setForeground(UIConstants.PRIMARY_700);
        top.add(quickLabel);
        
        quickDateCombo = new JComboBox<>(QUICK_DATE_OPTIONS);
        quickDateCombo.setFont(UIConstants.FONT_BODY);
        quickDateCombo.setPreferredSize(new Dimension(130, UIConstants.INPUT_HEIGHT_SM));
        quickDateCombo.addActionListener(e -> applyQuickDate());
        top.add(quickDateCombo);
        
        top.add(Box.createHorizontalStrut(UIConstants.SPACING_LG));
        
        // Custom date range
        JLabel fromLabel = new JLabel("Từ:");
        fromLabel.setFont(UIConstants.FONT_BODY);
        fromLabel.setForeground(UIConstants.NEUTRAL_700);
        top.add(fromLabel);
        
        fromDatePicker = new DatePicker();
        fromDatePicker.setPreferredSize(new Dimension(130, UIConstants.INPUT_HEIGHT_SM));
        top.add(fromDatePicker);
        
        JLabel toLabel = new JLabel("Đến:");
        toLabel.setFont(UIConstants.FONT_BODY);
        toLabel.setForeground(UIConstants.NEUTRAL_700);
        top.add(toLabel);
        
        toDatePicker = new DatePicker();
        toDatePicker.setPreferredSize(new Dimension(130, UIConstants.INPUT_HEIGHT_SM));
        top.add(toDatePicker);
        
        ModernButton applyBtn = new ModernButton("Lọc", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        applyBtn.setPreferredSize(new Dimension(80, 32));
        applyBtn.addActionListener(e -> refreshAll());
        top.add(applyBtn);
        
        ModernButton resetBtn = new ModernButton("Reset", ModernButton.ButtonType.GHOST, ModernButton.ButtonSize.SMALL);
        resetBtn.setPreferredSize(new Dimension(80, 32));
        resetBtn.addActionListener(e -> {
            quickDateCombo.setSelectedItem("Tháng này");
            applyQuickDate();
        });
        top.add(resetBtn);
        
        return top;
    }
    
    // ==================== OVERVIEW TAB ====================
    private JPanel createOverviewTab() {
        JPanel panel = new JPanel(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, 0, 0, 0));
        
        // KPI cards row 1 - Revenue & Orders
        JPanel kpiRow1 = new JPanel(new GridLayout(1, 3, UIConstants.SPACING_MD, 0));
        kpiRow1.setOpaque(false);
        
        revenueCard = new KPICard("Tong doanh thu", "0", "", UIConstants.SUCCESS);
        ordersCard = new KPICard("So don hang", "0", "", UIConstants.PRIMARY_500);
        avgCard = new KPICard("Gia tri TB/don", "0", "", UIConstants.WARNING);
        
        kpiRow1.add(revenueCard);
        kpiRow1.add(ordersCard);
        kpiRow1.add(avgCard);
        
        // KPI cards row 2 - Import & Profit
        JPanel kpiRow2 = new JPanel(new GridLayout(1, 2, UIConstants.SPACING_MD, 0));
        kpiRow2.setOpaque(false);
        
        importCostCard = new KPICard("Tong chi phi nhap", "0", "", UIConstants.DANGER);
        profitCard = new KPICard("Loi nhuan (uoc tinh)", "0", "", UIConstants.ACCENT_500);
        
        kpiRow2.add(importCostCard);
        kpiRow2.add(profitCard);
        
        // Combine KPI rows
        JPanel kpiPanel = new JPanel(new GridLayout(2, 1, 0, UIConstants.SPACING_SM));
        kpiPanel.setOpaque(false);
        kpiPanel.add(kpiRow1);
        kpiPanel.add(kpiRow2);
        
        panel.add(kpiPanel, BorderLayout.NORTH);
        
        // Low stock warning table
        CardPanel lowStockCard = new CardPanel(new BorderLayout(0, UIConstants.SPACING_SM));
        lowStockCard.setShadowSize(2);
        lowStockCard.setRadius(UIConstants.RADIUS_LG);
        lowStockCard.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Nguyên liệu sắp hết");
        title.setFont(UIConstants.FONT_HEADING_4);
        title.setForeground(UIConstants.WARNING_DARK);
        header.add(title, BorderLayout.WEST);
        StatusBadge badge = new StatusBadge("Cảnh báo", StatusBadge.BadgeType.WARNING);
        header.add(badge, BorderLayout.EAST);
        lowStockCard.add(header, BorderLayout.NORTH);
        
        lowStockModel = new DefaultTableModel(new Object[]{
            "Mã", "Tên nguyên liệu", "Đơn vị", "Tồn hiện tại", "Tồn tối thiểu"
        }, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        lowStockTable = new JTable(lowStockModel);
        ModernTableStyle.apply(lowStockTable, true);
        
        JScrollPane scroll = new JScrollPane(lowStockTable);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        lowStockCard.add(scroll, BorderLayout.CENTER);
        
        panel.add(lowStockCard, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== PRODUCTS TAB ====================
    private JPanel createProductsTab() {
        JPanel panel = new JPanel(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, 0, 0, 0));
        
        // KPI cards
        JPanel kpiPanel = new JPanel(new GridLayout(1, 3, UIConstants.SPACING_MD, 0));
        kpiPanel.setOpaque(false);
        
        totalProductsCard = new KPICard("Tong so mon", "0", "", UIConstants.PRIMARY_500);
        topProductCard = new KPICard("Mon ban chay nhat", "-", "", UIConstants.SUCCESS);
        totalSoldQtyCard = new KPICard("Tong SL ban", "0", "", UIConstants.WARNING);
        
        kpiPanel.add(totalProductsCard);
        kpiPanel.add(topProductCard);
        kpiPanel.add(totalSoldQtyCard);
        
        panel.add(kpiPanel, BorderLayout.NORTH);
        
        // Top products table
        CardPanel tableCard = new CardPanel(new BorderLayout(0, UIConstants.SPACING_SM));
        tableCard.setShadowSize(2);
        tableCard.setRadius(UIConstants.RADIUS_LG);
        tableCard.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        
        JLabel title = new JLabel("Top 10 món bán chạy");
        title.setFont(UIConstants.FONT_HEADING_4);
        title.setForeground(UIConstants.PRIMARY_700);
        tableCard.add(title, BorderLayout.NORTH);
        
        topProductsModel = new DefaultTableModel(new Object[]{
            "STT", "Tên món", "Số lượng bán", "Doanh thu"
        }, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        topProductsTable = new JTable(topProductsModel);
        ModernTableStyle.apply(topProductsTable, true);
        
        JScrollPane scroll = new JScrollPane(topProductsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        tableCard.add(scroll, BorderLayout.CENTER);
        
        panel.add(tableCard, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== EMPLOYEES TAB ====================
    private JPanel createEmployeesTab() {
        JPanel panel = new JPanel(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, 0, 0, 0));
        
        // KPI cards
        JPanel kpiPanel = new JPanel(new GridLayout(1, 2, UIConstants.SPACING_MD, 0));
        kpiPanel.setOpaque(false);
        
        totalEmployeesCard = new KPICard("Tong nhan vien", "0", "", UIConstants.PRIMARY_500);
        topEmployeeCard = new KPICard("NV doanh thu cao nhat", "-", "", UIConstants.SUCCESS);
        
        kpiPanel.add(totalEmployeesCard);
        kpiPanel.add(topEmployeeCard);
        
        panel.add(kpiPanel, BorderLayout.NORTH);
        
        // Employee stats table
        CardPanel tableCard = new CardPanel(new BorderLayout(0, UIConstants.SPACING_SM));
        tableCard.setShadowSize(2);
        tableCard.setRadius(UIConstants.RADIUS_LG);
        tableCard.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        
        JLabel title = new JLabel("Thống kê theo nhân viên");
        title.setFont(UIConstants.FONT_HEADING_4);
        title.setForeground(UIConstants.PRIMARY_700);
        tableCard.add(title, BorderLayout.NORTH);
        
        employeeStatsModel = new DefaultTableModel(new Object[]{
            "Mã NV", "Tên nhân viên", "Số đơn xử lý", "Doanh thu"
        }, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        employeeStatsTable = new JTable(employeeStatsModel);
        ModernTableStyle.apply(employeeStatsTable, true);
        
        JScrollPane scroll = new JScrollPane(employeeStatsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        tableCard.add(scroll, BorderLayout.CENTER);
        
        panel.add(tableCard, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== CUSTOMERS TAB ====================
    private JPanel createCustomersTab() {
        JPanel panel = new JPanel(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, 0, 0, 0));
        
        // KPI cards
        JPanel kpiPanel = new JPanel(new GridLayout(1, 3, UIConstants.SPACING_MD, 0));
        kpiPanel.setOpaque(false);
        
        totalCustomersCard = new KPICard("Tong khach hang", "0", "", UIConstants.PRIMARY_500);
        topCustomerCard = new KPICard("KH mua nhieu nhat", "-", "", UIConstants.SUCCESS);
        newCustomersCard = new KPICard("KH moi (trong ky)", "0", "", UIConstants.ACCENT_500);
        
        kpiPanel.add(totalCustomersCard);
        kpiPanel.add(topCustomerCard);
        kpiPanel.add(newCustomersCard);
        
        panel.add(kpiPanel, BorderLayout.NORTH);
        
        // Customer stats table
        CardPanel tableCard = new CardPanel(new BorderLayout(0, UIConstants.SPACING_SM));
        tableCard.setShadowSize(2);
        tableCard.setRadius(UIConstants.RADIUS_LG);
        tableCard.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        
        JLabel title = new JLabel("Thống kê theo khách hàng");
        title.setFont(UIConstants.FONT_HEADING_4);
        title.setForeground(UIConstants.PRIMARY_700);
        tableCard.add(title, BorderLayout.NORTH);
        
        customerStatsModel = new DefaultTableModel(new Object[]{
            "Mã KH", "Tên khách hàng", "Số đơn", "Tổng chi tiêu"
        }, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        customerStatsTable = new JTable(customerStatsModel);
        ModernTableStyle.apply(customerStatsTable, true);
        
        JScrollPane scroll = new JScrollPane(customerStatsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        tableCard.add(scroll, BorderLayout.CENTER);
        
        panel.add(tableCard, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== SUPPLIERS TAB ====================
    private JPanel createSuppliersTab() {
        JPanel panel = new JPanel(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, 0, 0, 0));
        
        // KPI cards
        JPanel kpiPanel = new JPanel(new GridLayout(1, 3, UIConstants.SPACING_MD, 0));
        kpiPanel.setOpaque(false);
        
        totalSuppliersCard = new KPICard("Tong NCC", "0", "", UIConstants.PRIMARY_500);
        totalImportCostCard = new KPICard("Tong chi phi nhap", "0", "", UIConstants.DANGER);
        topSupplierCard = new KPICard("NCC nhập nhiều nhất", "-", "", UIConstants.WARNING);
        
        kpiPanel.add(totalSuppliersCard);
        kpiPanel.add(totalImportCostCard);
        kpiPanel.add(topSupplierCard);
        
        panel.add(kpiPanel, BorderLayout.NORTH);
        
        // Supplier stats table
        CardPanel tableCard = new CardPanel(new BorderLayout(0, UIConstants.SPACING_SM));
        tableCard.setShadowSize(2);
        tableCard.setRadius(UIConstants.RADIUS_LG);
        tableCard.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        
        JLabel title = new JLabel("Thống kê nhập hàng theo NCC");
        title.setFont(UIConstants.FONT_HEADING_4);
        title.setForeground(UIConstants.PRIMARY_700);
        tableCard.add(title, BorderLayout.NORTH);
        
        supplierStatsModel = new DefaultTableModel(new Object[]{
            "Mã NCC", "Tên nhà cung cấp", "Số lần nhập", "Tổng giá trị nhập"
        }, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        supplierStatsTable = new JTable(supplierStatsModel);
        ModernTableStyle.apply(supplierStatsTable, true);
        
        JScrollPane scroll = new JScrollPane(supplierStatsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        tableCard.add(scroll, BorderLayout.CENTER);
        
        panel.add(tableCard, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== ACTIONS PANEL ====================
    private JPanel buildActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SPACING_SM, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_SM, 0, 0, 0));
        
        ModernButton refreshBtn = new ModernButton("Tải lại", ModernButton.ButtonType.SECONDARY);
        ModernButton exportBtn = new ModernButton("Xuất Excel", ModernButton.ButtonType.PRIMARY);
        
        panel.add(refreshBtn);
        panel.add(exportBtn);
        
        refreshBtn.addActionListener(e -> refreshAll());
        exportBtn.addActionListener(e -> exportCsv());
        
        return panel;
    }
    
    // ==================== QUICK DATE LOGIC ====================
    private void applyQuickDate() {
        String selected = (String) quickDateCombo.getSelectedItem();
        if (selected == null) return;
        
        LocalDate today = LocalDate.now();
        LocalDate from = null;
        LocalDate to = null;
        
        switch (selected) {
            case "Hôm nay":
                from = today;
                to = today;
                break;
            case "Hôm qua":
                from = today.minusDays(1);
                to = today.minusDays(1);
                break;
            case "7 ngày qua":
                from = today.minusDays(6);
                to = today;
                break;
            case "Tuần này":
                // Monday of current week
                from = today.minusDays(today.getDayOfWeek().getValue() - 1);
                to = today;
                break;
            case "Tháng này":
                from = today.withDayOfMonth(1);
                to = today;
                break;
            case "Tháng trước":
                from = today.minusMonths(1).withDayOfMonth(1);
                to = today.withDayOfMonth(1).minusDays(1);
                break;
            case "Quý này":
                int quarter = (today.getMonthValue() - 1) / 3;
                from = today.withMonth(quarter * 3 + 1).withDayOfMonth(1);
                to = today;
                break;
            case "Năm nay":
                from = today.withDayOfYear(1);
                to = today;
                break;
            case "Tất cả":
                from = null;
                to = null;
                break;
        }
        
        fromDatePicker.setDate(from);
        toDatePicker.setDate(to);
        refreshAll();
    }
    
    // ==================== REFRESH DATA ====================
    private void refreshAll() {
        LocalDate from = fromDatePicker.getDate();
        LocalDate to = toDatePicker.getDate();
        
        refreshOverview(from, to);
        refreshProducts(from, to);
        refreshEmployees(from, to);
        refreshCustomers(from, to);
        refreshSuppliers(from, to);
        
        if (onDataChanged != null) onDataChanged.run();
    }
    
    private void refreshOverview(LocalDate from, LocalDate to) {
        // Load KPIs
        DashboardDAO.Kpi kpi = DashboardDAO.loadKpi(from, to);
        revenueCard.setValue(CurrencyUtil.format(kpi.revenue));
        ordersCard.setValue(String.valueOf(kpi.orderCount));
        avgCard.setValue(CurrencyUtil.format(kpi.avgOrder));
        
        // Load import cost
        double importCost = 0;
        List<DashboardDAO.SupplierImportStatRow> imports = DashboardDAO.findSupplierImportStats(from, to);
        for (DashboardDAO.SupplierImportStatRow r : imports) {
            importCost += r.totalCost;
        }
        importCostCard.setValue(CurrencyUtil.format(importCost));
        
        // Profit estimate
        double profit = kpi.revenue - importCost;
        profitCard.setValue(CurrencyUtil.format(profit));
        
        // Low stock
        List<DashboardDAO.LowStockRow> lowStock = DashboardDAO.findLowStock();
        lowStockModel.setRowCount(0);
        for (DashboardDAO.LowStockRow r : lowStock) {
            lowStockModel.addRow(new Object[]{
                r.ingredientId,
                r.name,
                r.unit,
                CurrencyUtil.formatQuantity(r.currentStock),
                CurrencyUtil.formatQuantity(r.minStock)
            });
        }
    }
    
    private void refreshProducts(LocalDate from, LocalDate to) {
        // Load counts
        DashboardDAO.Counts counts = DashboardDAO.loadCounts();
        totalProductsCard.setValue(String.valueOf(counts.products));
        
        // Load top products
        List<DashboardDAO.TopProductRow> topProducts = DashboardDAO.findTopProducts(from, to, 10);
        
        int totalSoldQty = 0;
        topProductsModel.setRowCount(0);
        int stt = 1;
        for (DashboardDAO.TopProductRow r : topProducts) {
            topProductsModel.addRow(new Object[]{
                stt++,
                r.productName,
                r.quantity,
                CurrencyUtil.format(r.revenue)
            });
            totalSoldQty += r.quantity;
        }
        
        if (!topProducts.isEmpty()) {
            topProductCard.setValue(topProducts.get(0).productName);
        } else {
            topProductCard.setValue("-");
        }
        totalSoldQtyCard.setValue(String.valueOf(totalSoldQty));
    }
    
    private void refreshEmployees(LocalDate from, LocalDate to) {
        // Load counts
        DashboardDAO.Counts counts = DashboardDAO.loadCounts();
        totalEmployeesCard.setValue(String.valueOf(counts.employees));
        
        // Load employee stats
        List<DashboardDAO.EmployeeStatRow> empStats = DashboardDAO.findEmployeeStats(from, to);
        
        employeeStatsModel.setRowCount(0);
        for (DashboardDAO.EmployeeStatRow r : empStats) {
            employeeStatsModel.addRow(new Object[]{
                r.employeeId,
                r.employeeName,
                r.orderCount,
                CurrencyUtil.format(r.revenue)
            });
        }
        
        if (!empStats.isEmpty()) {
            topEmployeeCard.setValue(empStats.get(0).employeeName);
        } else {
            topEmployeeCard.setValue("-");
        }
    }
    
    private void refreshCustomers(LocalDate from, LocalDate to) {
        // Load counts
        DashboardDAO.Counts counts = DashboardDAO.loadCounts();
        totalCustomersCard.setValue(String.valueOf(counts.customers));
        
        // Load customer stats
        List<DashboardDAO.CustomerStatRow> custStats = DashboardDAO.findCustomerStats(from, to);
        
        customerStatsModel.setRowCount(0);
        for (DashboardDAO.CustomerStatRow r : custStats) {
            customerStatsModel.addRow(new Object[]{
                r.customerId > 0 ? r.customerId : "-",
                r.customerName.isEmpty() ? "Khách lẻ" : r.customerName,
                r.orderCount,
                CurrencyUtil.format(r.revenue)
            });
        }
        
        if (!custStats.isEmpty()) {
            String topName = custStats.get(0).customerName;
            topCustomerCard.setValue(topName.isEmpty() ? "Khách lẻ" : topName);
        } else {
            topCustomerCard.setValue("-");
        }
        
        // New customers count (simple estimate - just count in period)
        newCustomersCard.setValue(String.valueOf(custStats.size()));
    }
    
    private void refreshSuppliers(LocalDate from, LocalDate to) {
        // Load counts
        DashboardDAO.Counts counts = DashboardDAO.loadCounts();
        totalSuppliersCard.setValue(String.valueOf(counts.suppliers));
        
        // Load supplier import stats
        List<DashboardDAO.SupplierImportStatRow> supStats = DashboardDAO.findSupplierImportStats(from, to);
        
        double totalCost = 0;
        supplierStatsModel.setRowCount(0);
        for (DashboardDAO.SupplierImportStatRow r : supStats) {
            supplierStatsModel.addRow(new Object[]{
                r.supplierId,
                r.supplierName,
                r.importCount,
                CurrencyUtil.format(r.totalCost)
            });
            totalCost += r.totalCost;
        }
        
        totalImportCostCard.setValue(CurrencyUtil.format(totalCost));
        
        if (!supStats.isEmpty()) {
            topSupplierCard.setValue(supStats.get(0).supplierName);
        } else {
            topSupplierCard.setValue("-");
        }
    }
    
    // ==================== EXPORT ====================
    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("bao_cao_thong_ke.xls"));
        fc.setDialogTitle("Xuất báo cáo Excel");
        int res = fc.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".xls") && !f.getName().toLowerCase().endsWith(".csv")) {
            f = new File(f.getAbsolutePath() + ".xls");
        }
        
        LocalDate from = fromDatePicker.getDate();
        LocalDate to = toDatePicker.getDate();
        DashboardDAO.Kpi kpi = DashboardDAO.loadKpi(from, to);
        
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            // BOM for Excel UTF-8
            pw.print('\ufeff');
            
            // Date range
            pw.println("BÁO CÁO THỐNG KÊ");
            pw.println("Từ ngày," + (from == null ? "Tất cả" : from.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
            pw.println("Đến ngày," + (to == null ? "Tất cả" : to.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
            pw.println();
            
            // Overview KPIs
            pw.println("TỔNG QUAN");
            pw.println("Doanh thu," + kpi.revenue);
            pw.println("Số đơn hàng," + kpi.orderCount);
            pw.println("Giá trị TB/đơn," + kpi.avgOrder);
            pw.println();
            
            // Top products
            pw.println("TOP MÓN ĂN BÁN CHẠY");
            pw.println("STT,Tên món,Số lượng,Doanh thu");
            for (int r = 0; r < topProductsModel.getRowCount(); r++) {
                pw.println(
                    topProductsModel.getValueAt(r, 0) + "," +
                    escapeCsv(String.valueOf(topProductsModel.getValueAt(r, 1))) + "," +
                    topProductsModel.getValueAt(r, 2) + "," +
                    topProductsModel.getValueAt(r, 3)
                );
            }
            pw.println();
            
            // Employee stats
            pw.println("THỐNG KÊ NHÂN VIÊN");
            pw.println("Mã NV,Tên,Số đơn,Doanh thu");
            for (int r = 0; r < employeeStatsModel.getRowCount(); r++) {
                pw.println(
                    employeeStatsModel.getValueAt(r, 0) + "," +
                    escapeCsv(String.valueOf(employeeStatsModel.getValueAt(r, 1))) + "," +
                    employeeStatsModel.getValueAt(r, 2) + "," +
                    employeeStatsModel.getValueAt(r, 3)
                );
            }
            pw.println();
            
            // Customer stats
            pw.println("THỐNG KÊ KHÁCH HÀNG");
            pw.println("Mã KH,Tên,Số đơn,Tổng chi");
            for (int r = 0; r < customerStatsModel.getRowCount(); r++) {
                pw.println(
                    customerStatsModel.getValueAt(r, 0) + "," +
                    escapeCsv(String.valueOf(customerStatsModel.getValueAt(r, 1))) + "," +
                    customerStatsModel.getValueAt(r, 2) + "," +
                    customerStatsModel.getValueAt(r, 3)
                );
            }
            pw.println();
            
            // Supplier stats
            pw.println("THỐNG KÊ NHÀ CUNG CẤP");
            pw.println("Mã NCC,Tên,Số lần nhập,Tổng giá trị");
            for (int r = 0; r < supplierStatsModel.getRowCount(); r++) {
                pw.println(
                    supplierStatsModel.getValueAt(r, 0) + "," +
                    escapeCsv(String.valueOf(supplierStatsModel.getValueAt(r, 1))) + "," +
                    supplierStatsModel.getValueAt(r, 2) + "," +
                    supplierStatsModel.getValueAt(r, 3)
                );
            }
            
            JOptionPane.showMessageDialog(this, "Xuất báo cáo thành công:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không thể xuất báo cáo: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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
}
