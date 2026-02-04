package com.pos.ui;

import com.pos.Session;
import com.pos.dao.ItemDAO;
import com.pos.dao.PromotionDAO;
import com.pos.model.Item;
import com.pos.model.Promotion;
import com.pos.model.User;
import com.pos.service.PermissionService;
import com.pos.util.CurrencyUtil;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PromotionsManagementPanel extends JPanel {
    private final Runnable onDataChanged;

    private JTextField searchField;

    private DefaultTableModel model;
    private JTable table;

    private List<Promotion> current = new ArrayList<>();

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public PromotionsManagementPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        setOpaque(false);
        setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM));

        if (!PromotionDAO.supportsPromotions()) {
            JPanel p = new JPanel(new GridBagLayout());
            p.setOpaque(false);
            JLabel msg = new JLabel("Không tìm thấy bảng promotions");
            msg.setFont(UIConstants.FONT_BODY);
            msg.setForeground(UIConstants.WARNING_DARK);
            p.add(msg);
            add(p, BorderLayout.CENTER);
            return;
        }

        add(buildTop(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);

        PromotionDAO.ensureSequentialIdsIfNeeded();
        refreshTable();
    }

    private JPanel buildTop() {
        CardPanel top = new CardPanel(new WrapLayout(FlowLayout.LEFT, UIConstants.SPACING_MD, UIConstants.SPACING_SM));
        top.setShadowSize(2);
        top.setRadius(UIConstants.RADIUS_MD);
        top.setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_MD, UIConstants.SPACING_SM, UIConstants.SPACING_MD));

        JLabel filterIcon = new JLabel("Bộ lọc");
        filterIcon.setFont(UIConstants.FONT_BODY_BOLD);
        filterIcon.setForeground(UIConstants.PRIMARY_700);
        top.add(filterIcon);

        JLabel searchLabel = new JLabel("Tìm kiếm:");
        searchLabel.setFont(UIConstants.FONT_BODY);
        searchLabel.setForeground(UIConstants.NEUTRAL_700);
        top.add(searchLabel);

        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setPreferredSize(new Dimension(220, UIConstants.INPUT_HEIGHT_SM));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        top.add(searchField);

        ModernButton filterBtn = new ModernButton("Lọc", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        ModernButton resetBtn = new ModernButton("Reset", ModernButton.ButtonType.GHOST, ModernButton.ButtonSize.SMALL);
        filterBtn.setPreferredSize(new Dimension(80, 32));
        resetBtn.setPreferredSize(new Dimension(80, 32));

        top.add(filterBtn);
        top.add(resetBtn);

        filterBtn.addActionListener(e -> refreshTable());
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });

        searchField.addActionListener(e -> refreshTable());
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { refreshTable(); }
        });

        return top;
    }

    private JPanel buildTable() {
        CardPanel panel = new CardPanel(new BorderLayout());
        panel.setShadowSize(2);
        panel.setRadius(UIConstants.RADIUS_LG);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

        model = new DefaultTableModel(new Object[]{
                "Ma", "Ten", "Giam (%)", "DK toi thieu", "Tu ngay", "Den ngay"
        }, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(model);
        ModernTableStyle.apply(table, true);

        // Tối ưu column widths
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);   // Mã
        table.getColumnModel().getColumn(1).setPreferredWidth(200);  // Tên
        table.getColumnModel().getColumn(2).setPreferredWidth(80);   // Giảm (%)
        table.getColumnModel().getColumn(3).setPreferredWidth(130);  // ĐK tối thiểu
        table.getColumnModel().getColumn(4).setPreferredWidth(110);  // Từ ngày
        table.getColumnModel().getColumn(5).setPreferredWidth(110);  // Đến ngày

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

        boolean canAdd = PermissionService.canAddTab(Session.getCurrentUser(), "Khuyến mãi");
        boolean canEdit = PermissionService.canEditTab(Session.getCurrentUser(), "Khuyến mãi");
        boolean canDelete = PermissionService.canDeleteTab(Session.getCurrentUser(), "Khuyến mãi");
        if (!canAdd && !canEdit && !canDelete) {
            JLabel viewOnlyLabel = new JLabel("Chế độ xem - Không có quyền chỉnh sửa/xóa khuyến mãi");
            viewOnlyLabel.setFont(UIConstants.FONT_BODY);
            viewOnlyLabel.setForeground(UIConstants.NEUTRAL_500);
            panel.add(viewOnlyLabel);
            return panel;
        }

        ModernButton addBtn = new ModernButton("Thêm", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        ModernButton editBtn = new ModernButton("Sửa", ModernButton.ButtonType.SECONDARY, ModernButton.ButtonSize.SMALL);
        ModernButton deleteBtn = new ModernButton("Xóa", ModernButton.ButtonType.DANGER, ModernButton.ButtonSize.SMALL);
        addBtn.setPreferredSize(new Dimension(84, 32));
        editBtn.setPreferredSize(new Dimension(74, 32));
        deleteBtn.setPreferredSize(new Dimension(74, 32));

        panel.add(addBtn);
        panel.add(editBtn);
        panel.add(deleteBtn);

		addBtn.setVisible(canAdd);
		editBtn.setVisible(canEdit);
		deleteBtn.setVisible(canDelete);

        addBtn.addActionListener(e -> {
            Promotion p = showPromotionDialog(null);
            if (p != null) {
                if (PromotionDAO.create(p)) {
                    refreshTable();
                    if (onDataChanged != null) onDataChanged.run();
                }
            }
        });

        editBtn.addActionListener(e -> {
            Promotion existing = getSelectedPromotion();
            if (existing == null) {
                JOptionPane.showMessageDialog(this, "Chọn một khuyến mãi để sửa");
                return;
            }
            Promotion edited = showPromotionDialog(existing);
            if (edited != null) {
                if (PromotionDAO.update(edited)) {
                    refreshTable();
                    if (onDataChanged != null) onDataChanged.run();
                }
            }
        });

        deleteBtn.addActionListener(e -> {
            Promotion existing = getSelectedPromotion();
            if (existing == null) {
                JOptionPane.showMessageDialog(this, "Chọn một khuyến mãi để xóa");
                return;
            }
            
            int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa khuyến mãi \"" + existing.getName() + "\"?\nLưu ý: Xóa khuyến mãi có thể ảnh hưởng đến dữ liệu hóa đơn.",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (confirm != JOptionPane.YES_OPTION) return;
            
            if (PromotionDAO.delete(existing.getId())) {
                JOptionPane.showMessageDialog(this, "Đã xóa khuyến mãi");
                refreshTable();
                if (onDataChanged != null) onDataChanged.run();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể xóa khuyến mãi này", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private void refreshTable() {
        String keyword = searchField.getText() == null ? null : searchField.getText().trim();

        current = PromotionDAO.findByFilter(keyword, null);

        // Sort ascending by ID to keep order 1..n
        current.sort((a, b) -> Integer.compare(a.getId(), b.getId()));

        model.setRowCount(0);
        for (Promotion p : current) {
            String code = p.getCode() == null || p.getCode().isEmpty() ? "" : p.getCode();
            String valueLabel = String.valueOf((long) p.getDiscountValue()) + "%";
            String minOrder = CurrencyUtil.format(p.getMinOrderAmount());
            String start = formatDate(p.getStartDate());
            String end = formatDate(p.getEndDate());

            model.addRow(new Object[]{
                    code,
                    p.getName(),
                    valueLabel,
                    minOrder,
                    start,
                    end
            });
        }

        if (model.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private Promotion getSelectedPromotion() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= current.size()) return null;
        return clonePromotion(current.get(row));
    }

    private Promotion clonePromotion(Promotion src) {
        if (src == null) return null;
        return new Promotion(src.getId(), src.getName(), src.getDescription(), src.getDiscountType(), src.getDiscountValue(),
                src.getMinOrderAmount(), src.getStartDate(), src.getEndDate(), src.isActive(),
                src.getApplicableProductIds() == null ? null : new ArrayList<>(src.getApplicableProductIds()));
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY_DATE);
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private Promotion showPromotionDialog(Promotion existing) {
        JTextField name = new JTextField();
        JTextArea desc = new JTextArea(3, 20);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);

        JTextField value = new JTextField();
        JTextField minOrder = new JTextField("0");
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();

        JCheckBox allProducts = new JCheckBox("Tất cả món");
        allProducts.setSelected(true);
        JButton pickProductsBtn = new JButton("Chọn món...");

        List<Integer> selectedProductIds = new ArrayList<>();

        if (existing != null) {
            name.setText(existing.getName());
            desc.setText(existing.getDescription());
            value.setText(String.valueOf((long) existing.getDiscountValue()));
            minOrder.setText(String.valueOf((long) existing.getMinOrderAmount()));
            if (existing.getStartDate() != null) startDatePicker.setDate(existing.getStartDate());
            if (existing.getEndDate() != null) endDatePicker.setDate(existing.getEndDate());

            if (existing.getApplicableProductIds() != null && !existing.getApplicableProductIds().isEmpty()) {
                allProducts.setSelected(false);
                selectedProductIds.addAll(existing.getApplicableProductIds());
            }
        } else {
            LocalDate today = LocalDate.now();
            startDatePicker.setDate(today);
            endDatePicker.setDate(today.plusDays(30));
        }

        pickProductsBtn.setEnabled(!allProducts.isSelected());
        allProducts.addActionListener(e -> {
            boolean all = allProducts.isSelected();
            pickProductsBtn.setEnabled(!all);
            if (all) selectedProductIds.clear();
        });

        pickProductsBtn.addActionListener(e -> {
            List<Integer> ids = pickApplicableProducts(selectedProductIds);
            if (ids != null) {
                selectedProductIds.clear();
                selectedProductIds.addAll(ids);
            }
        });

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(new JLabel("Tên:"));
        p.add(name);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(new JLabel("Mô tả:"));
        p.add(new JScrollPane(desc));
        p.add(Box.createRigidArea(new Dimension(0, 6)));

        JPanel row1 = new JPanel(new GridLayout(1, 2, 8, 6));
        row1.add(new JLabel("Giam (%):"));
        row1.add(value);
        p.add(row1);
        p.add(Box.createRigidArea(new Dimension(0, 6)));

        JPanel row2 = new JPanel(new GridLayout(3, 2, 8, 6));
        row2.add(new JLabel("Dieu kien toi thieu:"));
        row2.add(minOrder);
        row2.add(new JLabel("Tu ngay:"));
        row2.add(startDatePicker);
        row2.add(new JLabel("Den ngay:"));
        row2.add(endDatePicker);
        p.add(row2);
        p.add(Box.createRigidArea(new Dimension(0, 6)));

        JPanel scope = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        scope.add(allProducts);
        scope.add(pickProductsBtn);
        p.add(scope);

        int res = JOptionPane.showConfirmDialog(this, p, existing == null ? "Thêm khuyến mãi" : "Sửa khuyến mãi",
                JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return null;

        String n = name.getText() == null ? "" : name.getText().trim();
        if (n.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên khuyến mãi không được để trống");
            return null;
        }

        double dv;
        try {
            dv = Double.parseDouble(value.getText() == null ? "0" : value.getText().trim().replaceAll("[^0-9\\.\\-]", ""));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Gia tri giam khong hop le");
            return null;
        }

        if (dv < 0 || dv > 100) {
            JOptionPane.showMessageDialog(this, "Gia tri % phai trong khoang 0..100");
            return null;
        }

        double min;
        try {
            min = Double.parseDouble(minOrder.getText() == null ? "0" : minOrder.getText().trim().replaceAll("[^0-9\\.\\-]", ""));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Điều kiện tối thiểu không hợp lệ");
            return null;
        }
        if (min < 0) min = 0;

        LocalDate sd = startDatePicker.getDate();
        LocalDate ed = endDatePicker.getDate();
        
        if (sd == null || ed == null) {
            JOptionPane.showMessageDialog(this, "Vui long chon ngay bat dau va ket thuc");
            return null;
        }

        if (sd.isAfter(ed)) {
            JOptionPane.showMessageDialog(this, "Ngay bat dau phai <= ngay ket thuc");
            return null;
        }

        Promotion out = existing == null ? new Promotion() : existing;
        out.setName(n);
        out.setDescription(desc.getText() == null ? null : desc.getText().trim());
        out.setDiscountType("percentage");
        out.setDiscountValue(dv);
        if (out.getCode() == null || out.getCode().isEmpty()) {
            out.setCode(generateCode());
        }
        out.setMinOrderAmount(min);
        out.setStartDate(sd);
        out.setEndDate(ed);

        if (allProducts.isSelected()) {
            out.setApplicableProductIds(new ArrayList<>());
        } else {
            out.setApplicableProductIds(new ArrayList<>(selectedProductIds));
        }

        return out;
    }

    private List<Integer> pickApplicableProducts(List<Integer> preselected) {
        List<Item> items = ItemDAO.findByFilterAdmin(null, null, true);
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có danh sách món");
            return null;
        }

        DefaultListModel<Item> lm = new DefaultListModel<>();
        for (Item it : items) lm.addElement(it);

        JList<Item> list = new JList<>(lm);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        Map<Integer, Integer> indexById = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            indexById.put(items.get(i).getId(), i);
        }
        if (preselected != null && !preselected.isEmpty()) {
            List<Integer> idxs = new ArrayList<>();
            for (Integer id : preselected) {
                Integer idx = indexById.get(id);
                if (idx != null) idxs.add(idx);
            }
            int[] arr = new int[idxs.size()];
            for (int i = 0; i < idxs.size(); i++) arr[i] = idxs.get(i);
            list.setSelectedIndices(arr);
        }

        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(520, 320));

        int res = JOptionPane.showConfirmDialog(this, sp, "Chọn món áp dụng", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return null;

        List<Item> selected = list.getSelectedValuesList();
        List<Integer> ids = new ArrayList<>();
        for (Item it : selected) ids.add(it.getId());
        return ids;
    }
}
