package com.pos.ui;

import com.pos.dao.CategoryDAO;
import com.pos.dao.IngredientDAO;
import com.pos.dao.ItemDAO;
import com.pos.dao.RecipeDAO;
import com.pos.model.Category;
import com.pos.model.Ingredient;
import com.pos.model.Item;
import com.pos.ui.components.CardPanel;
import com.pos.ui.components.ModernButton;
import com.pos.ui.components.ModernTableStyle;
import com.pos.ui.theme.UIConstants;
import com.pos.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeManagementPanel extends JPanel {
    private final Runnable onDataChanged;

    private DefaultTableModel productModel;
    private JTable productTable;

    private DefaultTableModel recipeModel;
    private JTable recipeTable;

    private JComboBox<Category> categoryCombo;
    private JTextField searchField;

    private JLabel selectedProductLabel;

    private final Map<Integer, Category> categoryMap = new HashMap<>();

    private Integer selectedProductId;
    private String selectedProductBaseLabel;

    public RecipeManagementPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(UIConstants.SPACING_MD, UIConstants.SPACING_MD));
        setOpaque(false);
        setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM));

        if (!RecipeDAO.supportsRecipe()) {
            JPanel p = new JPanel(new GridBagLayout());
            p.setOpaque(false);
            JLabel msg = new JLabel("Không tìm thấy bảng công thức (product_ingredients / ingredients)");
            msg.setFont(UIConstants.FONT_BODY);
            msg.setForeground(UIConstants.WARNING_DARK);
            p.add(msg);
            add(p, BorderLayout.CENTER);
            return;
        }

        // Fixed layout - không cho phép kéo co dãn
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        centerPanel.setOpaque(false);

        JPanel left = buildLeftProducts();
        JPanel right = buildRightRecipe();

        centerPanel.add(left);
        centerPanel.add(right);

        add(centerPanel, BorderLayout.CENTER);

        reloadCategories();
        refreshProducts();
    }

    private JPanel buildLeftProducts() {
        CardPanel panel = new CardPanel(new BorderLayout(UIConstants.SPACING_SM, UIConstants.SPACING_SM));
        panel.setShadowSize(2);
        panel.setRadius(UIConstants.RADIUS_LG);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

        JPanel top = new JPanel(new WrapLayout(FlowLayout.LEFT, UIConstants.SPACING_SM, UIConstants.SPACING_XS));
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, UIConstants.SPACING_SM, 0));

        JLabel catLabel = new JLabel("Danh mục:");
        catLabel.setFont(UIConstants.FONT_BODY);
        catLabel.setForeground(UIConstants.NEUTRAL_700);
        top.add(catLabel);

        categoryCombo = new JComboBox<>();
        categoryCombo.setFont(UIConstants.FONT_BODY);
        categoryCombo.setPreferredSize(new Dimension(180, UIConstants.INPUT_HEIGHT_SM));
        top.add(categoryCombo);

        JLabel searchLabel = new JLabel("Tìm món:");
        searchLabel.setFont(UIConstants.FONT_BODY);
        searchLabel.setForeground(UIConstants.NEUTRAL_700);
        top.add(searchLabel);

        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setPreferredSize(new Dimension(180, UIConstants.INPUT_HEIGHT_SM));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        top.add(searchField);

        panel.add(top, BorderLayout.NORTH);

        productModel = new DefaultTableModel(new Object[]{"ID", "Mã", "Tên", "Danh mục"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        productTable = new JTable(productModel);
        ModernTableStyle.apply(productTable, true);

        productTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        productTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        productTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        productTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        productTable.getColumnModel().getColumn(3).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(productTable);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);

        categoryCombo.addActionListener(e -> refreshProducts());
        searchField.addActionListener(e -> refreshProducts());
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { refreshProducts(); }
        });

        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            onProductSelected();
        });

        return panel;
    }

    private JPanel buildRightRecipe() {
        CardPanel panel = new CardPanel(new BorderLayout(UIConstants.SPACING_SM, UIConstants.SPACING_SM));
        panel.setShadowSize(2);
        panel.setRadius(UIConstants.RADIUS_LG);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

        JPanel header = new JPanel(new BorderLayout(UIConstants.SPACING_SM, 0));
        header.setOpaque(false);
        selectedProductLabel = new JLabel("Chọn món ở danh sách bên trái");
        selectedProductLabel.setFont(UIConstants.FONT_HEADING_4);
        selectedProductLabel.setForeground(UIConstants.PRIMARY_700);
        header.add(selectedProductLabel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SPACING_SM, 0));
        buttons.setOpaque(false);
        ModernButton addIngBtn = new ModernButton("Thêm NL", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
        ModernButton removeBtn = new ModernButton("Xóa khỏi CT", ModernButton.ButtonType.DANGER, ModernButton.ButtonSize.SMALL);
        ModernButton saveBtn = new ModernButton("Lưu công thức", ModernButton.ButtonType.SUCCESS, ModernButton.ButtonSize.SMALL);
        addIngBtn.setPreferredSize(new Dimension(100, 32));
        removeBtn.setPreferredSize(new Dimension(100, 32));
        saveBtn.setPreferredSize(new Dimension(110, 32));
        buttons.add(addIngBtn);
        buttons.add(removeBtn);
        buttons.add(saveBtn);

        header.add(buttons, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        recipeModel = new DefaultTableModel(new Object[]{"Mã NL", "Tên nguyên liệu", "Đơn vị", "Lượng dùng"}, 0) {
            public boolean isCellEditable(int row, int col) {
                return col == 3;
            }
        };
        recipeTable = new JTable(recipeModel);
        ModernTableStyle.apply(recipeTable, true);

        recipeTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        recipeTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        recipeTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        recipeTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        recipeTable.getColumnModel().getColumn(3).setPreferredWidth(80);

        JScrollPane scroll = new JScrollPane(recipeTable);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);

        addIngBtn.addActionListener(e -> addIngredientToRecipe());
        removeBtn.addActionListener(e -> removeSelectedLines());
        saveBtn.addActionListener(e -> saveRecipe());

        return panel;
    }

    public void reloadCategories() {
        DefaultComboBoxModel<Category> m = new DefaultComboBoxModel<>();
        Category all = new Category(0, "Tất cả");
        m.addElement(all);
        categoryMap.clear();
        List<Category> cats = CategoryDAO.findAll(true);
        for (Category c : cats) {
            categoryMap.put(c.getId(), c);
            m.addElement(c);
        }
        categoryCombo.setModel(m);
        refreshProducts();
    }

    public void refreshSelectedRecipe() {
        if (selectedProductId == null || selectedProductId <= 0) return;
        loadRecipe(selectedProductId);
    }

    private void refreshProducts() {
        productModel.setRowCount(0);

        String keyword = searchField.getText() == null ? null : searchField.getText().trim();
        Integer categoryId = null;
        Object sel = categoryCombo.getSelectedItem();
        if (sel instanceof Category) {
            Category c = (Category) sel;
            if (c.getId() > 0) categoryId = c.getId();
        }

        List<Item> items = ItemDAO.findByFilterAdmin(keyword, categoryId, false);
        for (Item it : items) {
            Category cat = categoryMap.get(it.getCategoryId());
            if (cat == null) cat = new Category(it.getCategoryId(), String.valueOf(it.getCategoryId()));
            productModel.addRow(new Object[]{
                    it.getId(),
                    it.getCode(),
                    it.getName(),
                    cat
            });
        }

        if (productModel.getRowCount() == 0) {
            selectedProductId = null;
            recipeModel.setRowCount(0);
            selectedProductLabel.setText("Không có món phù hợp");
        }
    }

    private void onProductSelected() {
        int row = productTable.getSelectedRow();
        if (row < 0) return;

        Object idObj = productModel.getValueAt(row, 0);
        if (!(idObj instanceof Integer)) return;

        selectedProductId = (Integer) idObj;
        String code = productModel.getValueAt(row, 1) == null ? "" : String.valueOf(productModel.getValueAt(row, 1));
        String name = productModel.getValueAt(row, 2) == null ? "" : String.valueOf(productModel.getValueAt(row, 2));

        this.selectedProductBaseLabel = "Công thức: " + code + " - " + name;
        selectedProductLabel.setText(this.selectedProductBaseLabel);
        loadRecipe(selectedProductId);
    }

    private void loadRecipe(int productId) {
        recipeModel.setRowCount(0);
        List<RecipeDAO.RecipeLine> lines = RecipeDAO.findByProduct(productId);
        for (RecipeDAO.RecipeLine l : lines) {
            recipeModel.addRow(new Object[]{
                    l.getIngredientId(),
                    l.getIngredientName(),
                    l.getUnit(),
                    l.getQuantityNeeded()
            });
        }
        String base = this.selectedProductBaseLabel;
        if (base == null || base.trim().isEmpty()) {
            base = selectedProductLabel.getText();
            if (base == null) base = "";
            int idx = base.indexOf(" (Chưa có công thức)");
            if (idx >= 0) base = base.substring(0, idx);
            this.selectedProductBaseLabel = base;
        }
        if (lines == null || lines.isEmpty()) {
            selectedProductLabel.setText(base + " (Chưa có công thức)");
        } else {
            selectedProductLabel.setText(base);
        }
    }

    private void addIngredientToRecipe() {
        if (selectedProductId == null || selectedProductId <= 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn món trước");
            return;
        }

        List<Ingredient> ingredients = IngredientDAO.findAll();
        if (ingredients.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có nguyên liệu (ingredients)");
            return;
        }

        Object selected = JOptionPane.showInputDialog(this, "Chọn nguyên liệu", "Nguyên liệu",
                JOptionPane.PLAIN_MESSAGE, null, ingredients.toArray(), ingredients.get(0));
        if (!(selected instanceof Ingredient)) return;

        Ingredient ing = (Ingredient) selected;
        int ingId = ing.getId();
        String qtyStr = JOptionPane.showInputDialog(this, "Nhập lượng dùng cho món (vd: 1, 0.5)", "1");
        if (qtyStr == null) return;
        double qty;
        try {
            qty = Double.parseDouble(qtyStr.trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lượng dùng không hợp lệ");
            return;
        }
        if (qty <= 0) {
            JOptionPane.showMessageDialog(this, "Lượng dùng phải > 0");
            return;
        }

        for (int r = 0; r < recipeModel.getRowCount(); r++) {
            Object existingId = recipeModel.getValueAt(r, 0);
            if (existingId != null && String.valueOf(existingId).trim().equals(String.valueOf(ingId))) {
                JOptionPane.showMessageDialog(this, "Nguyên liệu đã có trong công thức");
                return;
            }
        }

        recipeModel.addRow(new Object[]{ingId, ing.getName(), ing.getUnit(), qty});
    }

    private void removeSelectedLines() {
        if (selectedProductId == null || selectedProductId <= 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn món trước");
            return;
        }

        int[] rows = recipeTable.getSelectedRows();
        if (rows == null || rows.length == 0) {
            JOptionPane.showMessageDialog(this, "Chọn dòng để xóa");
            return;
        }

        for (int i = rows.length - 1; i >= 0; i--) {
            recipeModel.removeRow(rows[i]);
        }
    }

    private void saveRecipe() {
        if (selectedProductId == null || selectedProductId <= 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn món trước");
            return;
        }

        List<RecipeDAO.RecipeLine> lines = new ArrayList<>();
        for (int r = 0; r < recipeModel.getRowCount(); r++) {
            int ingredientId;
            try {
                ingredientId = Integer.parseInt(String.valueOf(recipeModel.getValueAt(r, 0)).trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Mã nguyên liệu không hợp lệ ở dòng " + (r + 1));
                return;
            }

            String ingName = recipeModel.getValueAt(r, 1) == null ? null : String.valueOf(recipeModel.getValueAt(r, 1));
            String unit = recipeModel.getValueAt(r, 2) == null ? null : String.valueOf(recipeModel.getValueAt(r, 2));

            double qty;
            try {
                Object qObj = recipeModel.getValueAt(r, 3);
                qty = Double.parseDouble(String.valueOf(qObj).trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lượng dùng không hợp lệ ở dòng " + (r + 1));
                return;
            }
            if (qty <= 0) {
                JOptionPane.showMessageDialog(this, "Lượng dùng phải > 0 ở dòng " + (r + 1));
                return;
            }

            lines.add(new RecipeDAO.RecipeLine(ingredientId, ingName, unit, qty));
        }

        int ok = JOptionPane.showConfirmDialog(this, "Lưu công thức cho món này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            RecipeDAO.replaceRecipe(selectedProductId, lines);
            JOptionPane.showMessageDialog(this, "Lưu công thức thành công");
            loadRecipe(selectedProductId);
            if (onDataChanged != null) onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không thể lưu công thức: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
