package com.pos.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import com.pos.util.CurrencyUtil;
import com.pos.dao.CategoryDAO;
import com.pos.dao.InventoryDAO;
import com.pos.dao.ItemDAO;
import com.pos.model.CartItem;
import com.pos.model.Category;
import com.pos.model.Item;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppFrame extends JFrame {
	private final Color PRIMARY = new Color(11, 92, 101);
	private final Color ACCENT = new Color(10, 140, 160);
	private final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);
	private final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
	// singleton instance to allow other frames to request menu refresh
	private static AppFrame instance;
	private JPanel menuGrid;
	private JScrollPane menuScroll;
	private JComboBox<Category> categoryCombo;
	private JTextField searchField;
	private JPanel cartListPanel;
	private JLabel totalLabel;
	private final Map<Integer, CartItem> cart = new LinkedHashMap<>();
	private Map<Integer, Integer> stockByItemId = new LinkedHashMap<>();

	public AppFrame() {
		// register instance for refresh callbacks
		AppFrame.instance = this;
		setTitle("POS - Quản lý đồ ăn nhanh");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1200, 800);
		setLocationRelativeTo(null);

		// Root panel
		JPanel root = new JPanel(new BorderLayout(12, 12));
		root.setBackground(new Color(245, 247, 249));
		root.setBorder(new EmptyBorder(8, 8, 8, 8));
		setContentPane(root);

		// Right side: center (menu) + bill panel
		JPanel rightArea = new JPanel(new BorderLayout(12, 12));
		rightArea.setBackground(new Color(245, 247, 249));

		JPanel navBar = new JPanel(new BorderLayout());
		navBar.setOpaque(false);
		JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
		navButtons.setOpaque(false);
		String[] sidebarItems = { "Trang chủ", "Thanh toán", "Kho", "Báo cáo", "Cài đặt" };
		for (String item : sidebarItems) {
			JButton btn = new JButton(item);
			btn.setBackground(Color.WHITE);
			btn.setForeground(PRIMARY);
			btn.setFont(NORMAL_FONT);
			btn.setFocusPainted(false);
			btn.setBorder(new EmptyBorder(8, 14, 8, 14));
			btn.addActionListener(e -> {
				if ("Thanh toán".equals(item)) {
					if (cart.isEmpty()) {
						JOptionPane.showMessageDialog(this, "Giỏ hàng đang trống");
						return;
					}
					new PaymentFrame(this, getCartSnapshot());
					return;
				}
				if (!"Trang chủ".equals(item)) {
					JOptionPane.showMessageDialog(this, "Chức năng '" + item + "' sẽ được triển khai ở bước tiếp theo");
				}
			});
			navButtons.add(btn);
		}
		navBar.add(navButtons, BorderLayout.WEST);

		JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
		rightTop.setOpaque(false);
		String userName = com.pos.Session.getCurrentUser() != null ? com.pos.Session.getCurrentUser().getFullName() : "Người dùng";
		JLabel userLabel = new JLabel("Người dùng: " + userName);
		userLabel.setFont(NORMAL_FONT);
		rightTop.add(userLabel);
		if (com.pos.Session.isManager()) {
			JButton adminBtn = new JButton("Quản lý món (Admin)");
			adminBtn.setBackground(ACCENT);
			adminBtn.setForeground(Color.WHITE);
			adminBtn.setFocusPainted(false);
			adminBtn.setBorder(new EmptyBorder(8, 14, 8, 14));
			adminBtn.addActionListener(e -> new AdminFrame());
			rightTop.add(adminBtn);
		}
		navBar.add(rightTop, BorderLayout.EAST);
		rightArea.add(navBar, BorderLayout.NORTH);

		// Center area: categories + menu grid
		JPanel center = new JPanel(new BorderLayout(8, 8));
		center.setOpaque(false);

		JPanel categoryBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
		categoryBar.setOpaque(false);
		this.categoryCombo = new JComboBox<>();
		this.categoryCombo.setFont(NORMAL_FONT);
		this.categoryCombo.setPreferredSize(new Dimension(220, 34));
		this.searchField = new JTextField();
		this.searchField.setFont(NORMAL_FONT);
		this.searchField.setPreferredSize(new Dimension(260, 34));
		JButton clearBtn = new JButton("Xóa");
		clearBtn.setFont(NORMAL_FONT);
		clearBtn.setFocusPainted(false);
		clearBtn.setPreferredSize(new Dimension(90, 34));
		categoryBar.add(new JLabel("Danh mục:"));
		categoryBar.add(this.categoryCombo);
		categoryBar.add(new JLabel("Tìm:"));
		categoryBar.add(this.searchField);
		categoryBar.add(clearBtn);
		center.add(categoryBar, BorderLayout.NORTH);

		// Menu grid with balanced cards (populated from DB)
		this.menuGrid = new JPanel();
		this.menuGrid.setLayout(new GridLayout(0, 5, 12, 12));
		this.menuGrid.setBackground(new Color(245, 247, 249));

		this.menuScroll = new JScrollPane(this.menuGrid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		this.menuScroll.setBorder(null);
		center.add(this.menuScroll, BorderLayout.CENTER);

		rightArea.add(center, BorderLayout.CENTER);

		// Bill panel (right, fixed width)
		JPanel billPanel = new JPanel();
		billPanel.setPreferredSize(new Dimension(320, 0));
		billPanel.setLayout(new BorderLayout(8, 8));
		billPanel.setBackground(Color.WHITE);
		billPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

		JLabel billTitle = new JLabel("Hóa đơn");
		billTitle.setFont(HEADER_FONT);
		billPanel.add(billTitle, BorderLayout.NORTH);

		this.cartListPanel = new JPanel();
		this.cartListPanel.setLayout(new BoxLayout(this.cartListPanel, BoxLayout.Y_AXIS));
		this.cartListPanel.setOpaque(false);
		JScrollPane billScroll = new JScrollPane(this.cartListPanel);
		billScroll.setBorder(null);
		billPanel.add(billScroll, BorderLayout.CENTER);

		JPanel billBottom = new JPanel();
		billBottom.setLayout(new BoxLayout(billBottom, BoxLayout.Y_AXIS));
		billBottom.setOpaque(false);
		this.totalLabel = new JLabel("Tổng: " + CurrencyUtil.format(0));
		this.totalLabel.setFont(HEADER_FONT);
		this.totalLabel.setBorder(new EmptyBorder(8, 0, 8, 0));
		JButton placeOrder = new JButton("Đặt hàng");
		placeOrder.setBackground(ACCENT);
		placeOrder.setForeground(Color.WHITE);
		placeOrder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
		placeOrder.setAlignmentX(Component.CENTER_ALIGNMENT);
		billBottom.add(this.totalLabel);
		billBottom.add(placeOrder);
		billPanel.add(billBottom, BorderLayout.SOUTH);

		rightArea.add(billPanel, BorderLayout.EAST);

		root.add(rightArea, BorderLayout.CENTER);

		loadCategories();
		this.categoryCombo.addActionListener(e -> refreshMenu());
		this.searchField.addActionListener(e -> refreshMenu());
		this.searchField.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyReleased(java.awt.event.KeyEvent e) {
				refreshMenu();
			}
		});
		clearBtn.addActionListener(e -> {
			searchField.setText("");
			categoryCombo.setSelectedIndex(0);
			refreshMenu();
		});
		placeOrder.addActionListener(e -> {
			if (cart.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Giỏ hàng đang trống");
				return;
			}
			new PaymentFrame(this, getCartSnapshot());
		});

		refreshMenu();
		renderCart();

		setVisible(true);
	}

	public static AppFrame getInstance() {
		return instance;
	}

	/** Refresh menu cards from database */
	public void refreshMenu() {
		// lazy set instance
		if (instance == null) instance = this;
		if (menuGrid == null) return;
		menuGrid.removeAll();
		stockByItemId = InventoryDAO.getAllQuantities();
		String keyword = searchField != null ? searchField.getText().trim() : null;
		Integer categoryId = null;
		if (categoryCombo != null && categoryCombo.getSelectedItem() instanceof Category) {
			Category c = (Category) categoryCombo.getSelectedItem();
			if (c != null && c.getId() > 0) categoryId = c.getId();
		}
		List<Item> items = ItemDAO.findByFilter(keyword, categoryId);
		if (items == null) items = new ArrayList<>();
		if (items.isEmpty()) {
			JPanel placeholder = new JPanel(new GridBagLayout());
			placeholder.setBackground(new Color(245, 247, 249));
			JLabel ph = new JLabel("Chưa có sản phẩm. Vui lòng thêm sản phẩm bằng tài khoản Admin.");
			ph.setFont(NORMAL_FONT);
			ph.setForeground(new Color(120, 120, 120));
			placeholder.add(ph);
			menuGrid.add(placeholder);
		}
		for (Item it : items) {
			int stock = stockByItemId.getOrDefault(it.getId(), 0);
			boolean inStock = stock > 0;
			LineBorder normalBorder = new LineBorder(new Color(220, 225, 226));
			JPanel card = new JPanel(new BorderLayout(8, 8));
			card.setBackground(inStock ? Color.WHITE : new Color(245, 247, 249));
			card.setBorder(BorderFactory.createCompoundBorder(normalBorder, new EmptyBorder(10, 10, 10, 10)));
			card.setPreferredSize(new Dimension(200, 200));
			if (inStock) {
				card.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseEntered(MouseEvent e) {
						card.setBorder(BorderFactory.createCompoundBorder(new LineBorder(ACCENT, 2), new EmptyBorder(10, 10, 10, 10)));
					}
					@Override
					public void mouseExited(MouseEvent e) {
						card.setBorder(BorderFactory.createCompoundBorder(normalBorder, new EmptyBorder(10, 10, 10, 10)));
					}
				});
			}

			JLabel imgLabel = new JLabel("Không có ảnh", SwingConstants.CENTER);
			imgLabel.setPreferredSize(new Dimension(160, 110));
			imgLabel.setOpaque(true);
			imgLabel.setBackground(inStock ? new Color(245, 247, 249) : new Color(235, 238, 240));
			imgLabel.setBorder(new LineBorder(new Color(230, 235, 236)));
			if (it.getImagePath() != null && !it.getImagePath().isEmpty()) {
				try {
					BufferedImage bi = ImageIO.read(new File(it.getImagePath()));
					Image scaled = bi.getScaledInstance(160, 110, Image.SCALE_SMOOTH);
					imgLabel.setIcon(new ImageIcon(scaled));
					imgLabel.setText("");
				} catch (Exception ex) {
					// ignore if image can't be loaded
				}
			}
			card.add(imgLabel, BorderLayout.NORTH);

			JPanel info = new JPanel();
			info.setOpaque(false);
			info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
			JLabel name = new JLabel(it.getName());
			name.setFont(NORMAL_FONT);
			name.setForeground(inStock ? new Color(20, 20, 20) : new Color(140, 140, 140));
			JLabel price = new JLabel("Giá: " + CurrencyUtil.formatUSDAsVND(it.getPrice()));
			price.setFont(NORMAL_FONT);
			price.setForeground(inStock ? new Color(40, 40, 40) : new Color(150, 150, 150));
			JLabel status = new JLabel(inStock ? ("Còn hàng: " + stock) : "Hết hàng");
			status.setFont(NORMAL_FONT);
			status.setForeground(inStock ? new Color(0, 120, 0) : new Color(200, 0, 0));
			info.add(name);
			info.add(Box.createRigidArea(new Dimension(0, 6)));
			info.add(price);
			info.add(Box.createRigidArea(new Dimension(0, 6)));
			info.add(status);
			card.add(info, BorderLayout.CENTER);

			JPanel footer = new JPanel(new BorderLayout(8, 8));
			footer.setOpaque(false);
			JButton addBtn = new JButton("Chọn");
			addBtn.setBackground(ACCENT);
			addBtn.setForeground(Color.WHITE);
			addBtn.setFocusPainted(false);
			addBtn.setPreferredSize(new Dimension(84, 34));
			addBtn.setEnabled(inStock);
			addBtn.addActionListener(e -> addToCart(it));
			footer.add(addBtn, BorderLayout.EAST);
			card.add(footer, BorderLayout.SOUTH);

			menuGrid.add(card);
		}
		menuGrid.revalidate();
		menuGrid.repaint();
	}

	private void loadCategories() {
		if (this.categoryCombo == null) return;
		DefaultComboBoxModel<Category> model = new DefaultComboBoxModel<>();
		model.addElement(new Category(0, "Tất cả"));
		for (Category c : CategoryDAO.findAllActive()) {
			model.addElement(c);
		}
		this.categoryCombo.setModel(model);
	}

	public void reloadCategories() {
		loadCategories();
	}

	private void addToCart(Item item) {
		if (item == null) return;
		int stock = stockByItemId != null ? stockByItemId.getOrDefault(item.getId(), 0) : 0;
		CartItem existing = cart.get(item.getId());
		int currentQty = existing != null ? existing.getQuantity() : 0;
		if (stock <= 0) {
			JOptionPane.showMessageDialog(this, "Món này đã hết hàng");
			return;
		}
		if (currentQty + 1 > stock) {
			JOptionPane.showMessageDialog(this, "Không đủ tồn kho. Tồn hiện tại: " + stock);
			return;
		}
		if (existing == null) {
			cart.put(item.getId(), new CartItem(item, 1));
		} else {
			existing.setQuantity(existing.getQuantity() + 1);
		}
		renderCart();
	}

	private void changeQuantity(int itemId, int delta) {
		CartItem existing = cart.get(itemId);
		if (existing == null) return;
		int next = existing.getQuantity() + delta;
		if (next <= 0) {
			cart.remove(itemId);
		} else {
			existing.setQuantity(next);
		}
		renderCart();
	}

	private void renderCart() {
		if (cartListPanel == null || totalLabel == null) return;
		cartListPanel.removeAll();
		double total = 0;
		if (cart.isEmpty()) {
			JPanel empty = new JPanel(new GridBagLayout());
			empty.setOpaque(false);
			JLabel msg = new JLabel("Chưa có món trong giỏ. Hãy bấm 'Chọn' ở món ăn.");
			msg.setFont(NORMAL_FONT);
			msg.setForeground(new Color(120, 120, 120));
			empty.add(msg);
			cartListPanel.add(empty);
			totalLabel.setText("Tổng: " + CurrencyUtil.format(0));
			cartListPanel.revalidate();
			cartListPanel.repaint();
			return;
		}
		for (CartItem ci : cart.values()) {
			Item it = ci.getItem();
			double lineTotal = ci.getLineTotal();
			total += lineTotal;

			JPanel row = new JPanel(new BorderLayout(8, 8));
			row.setOpaque(false);
			row.setBorder(new EmptyBorder(6, 0, 6, 0));

			JLabel name = new JLabel(it.getName());
			name.setFont(NORMAL_FONT);
			row.add(name, BorderLayout.WEST);

			JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
			qtyPanel.setOpaque(false);
			JButton minus = new JButton("-");
			minus.setPreferredSize(new Dimension(44, 32));
			minus.setFocusPainted(false);
			JLabel qty = new JLabel(String.valueOf(ci.getQuantity()));
			qty.setFont(NORMAL_FONT);
			JButton plus = new JButton("+");
			plus.setPreferredSize(new Dimension(44, 32));
			plus.setFocusPainted(false);
			int itemId = it.getId();
			minus.addActionListener(e -> changeQuantity(itemId, -1));
			plus.addActionListener(e -> changeQuantity(itemId, 1));
			qtyPanel.add(minus);
			qtyPanel.add(qty);
			qtyPanel.add(plus);
			row.add(qtyPanel, BorderLayout.CENTER);

			JLabel price = new JLabel(CurrencyUtil.formatUSDAsVND(lineTotal));
			price.setFont(NORMAL_FONT);
			row.add(price, BorderLayout.EAST);

			cartListPanel.add(row);
		}
		totalLabel.setText("Tổng: " + CurrencyUtil.formatUSDAsVND(total));
		cartListPanel.revalidate();
		cartListPanel.repaint();
	}

	private List<CartItem> getCartSnapshot() {
		List<CartItem> list = new ArrayList<>();
		for (CartItem ci : cart.values()) {
			list.add(new CartItem(ci.getItem(), ci.getQuantity()));
		}
		return list;
	}

	public void clearCartAfterCheckout() {
		cart.clear();
		renderCart();
		reloadCategories();
		refreshMenu();
	}

	public void setCartFromSnapshot(List<CartItem> items) {
		cart.clear();
		if (items != null) {
			for (CartItem ci : items) {
				if (ci != null && ci.getItem() != null) {
					cart.put(ci.getItem().getId(), new CartItem(ci.getItem(), ci.getQuantity()));
				}
			}
		}
		renderCart();
	}
}


