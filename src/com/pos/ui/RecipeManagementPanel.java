package com.pos.ui;

import com.pos.Session;
import com.pos.dao.CategoryDAO;
import com.pos.dao.IngredientDAO;
import com.pos.dao.ItemDAO;
import com.pos.dao.RecipeDAO;
import com.pos.model.Category;
import com.pos.model.Ingredient;
import com.pos.model.Item;
import com.pos.service.PermissionService;
import com.pos.ui.components.CardPanel;
import com.pos.ui.components.ModernButton;
import com.pos.ui.components.ModernTableStyle;
import com.pos.ui.theme.UIConstants;
import com.pos.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
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
    private JComboBox<Object> ingredientFilterCombo;

    private JLabel selectedProductLabel;

    private final Map<Integer, Category> categoryMap = new HashMap<>();

    private Integer selectedProductId;
    private String selectedProductBaseLabel;

    private static class NumericDocumentFilter extends DocumentFilter {
        private boolean isAllowed(String s) {
            if (s == null || s.isEmpty()) return true;
            return s.matches("[0-9\\.,]*");
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (isAllowed(string)) super.insertString(fb, offset, string, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (isAllowed(text)) super.replace(fb, offset, length, text, attrs);
        }
    }

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
        reloadIngredientsFilter();
        refreshProducts();
    }

    private JPanel buildLeftProducts() {
        CardPanel panel = new CardPanel(new BorderLayout(UIConstants.SPACING_SM, UIConstants.SPACING_SM));
        panel.setShadowSize(2);
        panel.setRadius(UIConstants.RADIUS_LG);
        panel.setBorder(new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, UIConstants.SPACING_SM, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, UIConstants.SPACING_XS, UIConstants.SPACING_SM);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        JLabel catLabel = new JLabel("Danh mục:");
        catLabel.setFont(UIConstants.FONT_BODY);
        catLabel.setForeground(UIConstants.NEUTRAL_700);
        gbc.gridx = 0;
        gbc.gridy = 0;
        top.add(catLabel, gbc);

        categoryCombo = new JComboBox<>();
        categoryCombo.setFont(UIConstants.FONT_BODY);
        categoryCombo.setPreferredSize(new Dimension(160, UIConstants.INPUT_HEIGHT_SM));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        top.add(categoryCombo, gbc);

        JLabel searchLabel = new JLabel("Tìm món:");
        searchLabel.setFont(UIConstants.FONT_BODY);
        searchLabel.setForeground(UIConstants.NEUTRAL_700);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        top.add(searchLabel, gbc);

        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setPreferredSize(new Dimension(200, UIConstants.INPUT_HEIGHT_SM));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        top.add(searchField, gbc);

        JLabel ingFilterLabel = new JLabel("Lọc theo nguyên liệu:");
        ingFilterLabel.setFont(UIConstants.FONT_BODY);
        ingFilterLabel.setForeground(UIConstants.NEUTRAL_700);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, UIConstants.SPACING_SM);
        top.add(ingFilterLabel, gbc);

        ingredientFilterCombo = new JComboBox<>();
        ingredientFilterCombo.setFont(UIConstants.FONT_BODY);
        ingredientFilterCombo.setPreferredSize(new Dimension(200, UIConstants.INPUT_HEIGHT_SM));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        top.add(ingredientFilterCombo, gbc);
        gbc.gridwidth = 1;

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

        ingredientFilterCombo.addActionListener(e -> refreshProducts());

        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            onProductSelected();
        });

        return panel;
    }

    public void reloadIngredientsFilter() {
        if (ingredientFilterCombo == null) return;
        Integer selectedId = null;
        Object selected = ingredientFilterCombo == null ? null : ingredientFilterCombo.getSelectedItem();
        if (selected instanceof Ingredient) {
            selectedId = ((Ingredient) selected).getId();
        }

        DefaultComboBoxModel<Object> m = new DefaultComboBoxModel<>();
        m.addElement("-- Tất cả nguyên liệu --");
        List<Ingredient> ingredients = IngredientDAO.findByFilter(null, null, false, false);
        for (Ingredient ing : ingredients) {
            if (ing != null) m.addElement(ing);
        }
        ingredientFilterCombo.setModel(m);
        ingredientFilterCombo.setMaximumRowCount(20);

        if (selectedId != null) {
            for (int i = 0; i < ingredientFilterCombo.getItemCount(); i++) {
                Object o = ingredientFilterCombo.getItemAt(i);
                if (o instanceof Ingredient && ((Ingredient) o).getId() == selectedId) {
                    ingredientFilterCombo.setSelectedIndex(i);
                    return;
                }
            }
        }
        ingredientFilterCombo.setSelectedIndex(0);
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

        boolean canEdit = PermissionService.canEditTab(Session.getCurrentUser(), "Công thức");
        boolean canDelete = PermissionService.canDeleteTab(Session.getCurrentUser(), "Công thức");
        addIngBtn.setVisible(canEdit);
        saveBtn.setVisible(canEdit);
        removeBtn.setVisible(canDelete);

        header.add(buttons, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        recipeModel = new DefaultTableModel(new Object[]{"Mã NL", "Tên nguyên liệu", "Đơn vị", "Lượng dùng"}, 0) {
            public boolean isCellEditable(int row, int col) {
                return col == 3;
            }

            public void setValueAt(Object aValue, int row, int column) {
                if (column == 3) {
                    if (aValue == null) {
                        super.setValueAt("", row, column);
                        return;
                    }
                    String s = String.valueOf(aValue).trim();
                    if (s.isEmpty()) {
                        super.setValueAt("", row, column);
                        return;
                    }
                    try {
                        double v = Double.parseDouble(s.replace(',', '.'));
                        super.setValueAt(CurrencyUtil.formatQuantity(v), row, column);
                    } catch (Exception ex) {
                        super.setValueAt(s, row, column);
                    }
                    return;
                }
                super.setValueAt(aValue, row, column);
            }
        };
        recipeTable = new JTable(recipeModel);
        ModernTableStyle.apply(recipeTable, true);

        recipeTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        recipeTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        recipeTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        recipeTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        recipeTable.getColumnModel().getColumn(3).setPreferredWidth(80);

        JTextField qtyEditorField = new JTextField();
        qtyEditorField.setFont(UIConstants.FONT_BODY);
        if (qtyEditorField.getDocument() instanceof AbstractDocument) {
            ((AbstractDocument) qtyEditorField.getDocument()).setDocumentFilter(new NumericDocumentFilter());
        }
        recipeTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(qtyEditorField));

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

    public void refreshProductsList() {
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

        int ingredientId = 0;
        Object ingSel = ingredientFilterCombo == null ? null : ingredientFilterCombo.getSelectedItem();
        if (ingSel instanceof Ingredient) {
            ingredientId = ((Ingredient) ingSel).getId();
        }

        java.util.Set<Integer> allowedIds = null;
        if (ingredientId > 0) {
            allowedIds = RecipeDAO.findProductIdsUsingIngredient(ingredientId);
        }
        for (Item it : items) {
            if (allowedIds != null && (it == null || !allowedIds.contains(it.getId()))) {
                continue;
            }
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
                    CurrencyUtil.formatQuantity(l.getQuantityNeeded())
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

        List<Ingredient> ingredients = IngredientDAO.findByFilter(null, null, false, false);
        if (ingredients.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có nguyên liệu (ingredients)");
            return;
        }

        Ingredient ing = showIngredientPickerDialog(ingredients);
        if (ing == null) return;
        int ingId = ing.getId();
        JTextField qtyField = new JTextField("1");
        qtyField.setFont(UIConstants.FONT_BODY);
        if (qtyField.getDocument() instanceof AbstractDocument) {
            ((AbstractDocument) qtyField.getDocument()).setDocumentFilter(new NumericDocumentFilter());
        }
        int ok = JOptionPane.showConfirmDialog(this, qtyField, "Nhập lượng dùng cho món (vd: 1, 0.5)", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        String qtyStr = qtyField.getText();
        double qty;
        try {
            qty = Double.parseDouble(qtyStr.trim().replace(',', '.'));
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

        recipeModel.addRow(new Object[]{ingId, ing.getName(), ing.getUnit(), CurrencyUtil.formatQuantity(qty)});
    }

    private Ingredient showIngredientPickerDialog(List<Ingredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) return null;

        DefaultListModel<Ingredient> listModel = new DefaultListModel<>();
        for (Ingredient ing : ingredients) {
            if (ing != null) listModel.addElement(ing);
        }

        JList<Ingredient> list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(10);
        list.setFont(UIConstants.FONT_BODY);
        list.setSelectedIndex(0);

        JTextField search = new JTextField();
        search.setFont(UIConstants.FONT_BODY);
        search.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));

        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void apply() {
                String kw = search.getText() == null ? "" : search.getText().trim().toLowerCase();
                listModel.clear();
                for (Ingredient ing : ingredients) {
                    if (ing == null) continue;
                    if (kw.isEmpty()) {
                        listModel.addElement(ing);
                    } else {
                        String name = ing.getName() == null ? "" : ing.getName().toLowerCase();
                        String id = String.valueOf(ing.getId());
                        if (name.contains(kw) || id.contains(kw)) {
                            listModel.addElement(ing);
                        }
                    }
                }
                if (!listModel.isEmpty()) list.setSelectedIndex(0);
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) { apply(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { apply(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { apply(); }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel content = new JPanel(new BorderLayout(UIConstants.SPACING_SM, UIConstants.SPACING_SM));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));
        content.add(search, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);

        JOptionPane optionPane = new JOptionPane(content, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = optionPane.createDialog(this, "Nguyên liệu");
        dialog.setResizable(true);

        list.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    optionPane.setValue(JOptionPane.OK_OPTION);
                    dialog.setVisible(false);
                }
            }
        });

        dialog.setVisible(true);

        Object v = optionPane.getValue();
        if (!(v instanceof Integer) || ((Integer) v) != JOptionPane.OK_OPTION) return null;
        return list.getSelectedValue();
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
                qty = Double.parseDouble(String.valueOf(qObj).trim().replace(',', '.'));
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
