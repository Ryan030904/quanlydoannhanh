package com.pos.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import com.pos.Session;
import com.pos.util.CurrencyUtil;
import com.pos.dao.CategoryDAO;
import com.pos.dao.InventoryDAO;
import com.pos.dao.ItemDAO;
import com.pos.db.DBConnection;
import com.pos.model.CartItem;
import com.pos.model.Category;
import com.pos.model.Item;
import com.pos.model.User;
import com.pos.service.PermissionService;
import com.pos.ui.components.*;
import com.pos.ui.theme.UIConstants;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Batik SVG imports
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

public class AppFrame extends JFrame {
	private final Color PRIMARY = UIConstants.PRIMARY_700;
	private final Color ACCENT = UIConstants.PRIMARY_500;
	private final Font HEADER_FONT = UIConstants.FONT_HEADING_3;
	private final Font NORMAL_FONT = UIConstants.FONT_BODY;
	// singleton instance to allow other frames to request menu refresh
	private static AppFrame instance;
	private JPanel menuGrid;
	private JScrollPane menuScroll;
	private JComboBox<Category> categoryCombo;
	private JTextField searchField;
	private JPanel cartListPanel;
	private JLabel totalLabel;
	private JTable itemTable;
	private DefaultTableModel itemTableModel;
	private JTable cartTable;
	private DefaultTableModel cartTableModel;
	private JTextField tfMaMon;
	private JTextField tfTenMon;
	private JTextField tfDonGia;
	private JTextField tfSoLuong;
	private JLabel itemImagePreview;
	private Item selectedItem;
	private final Map<Integer, CartItem> cart = new LinkedHashMap<>();
	private List<Item> currentItems = new ArrayList<>();
	private final Map<Integer, String> categoryNameById = new HashMap<>();
	private JPanel tabCards;
	private CardLayout tabCardLayout;
	private OrdersManagementPanel ordersPanel;
	private ImportHistoryManagementPanel importHistoryPanel;
	private ImportGoodsPanel importGoodsPanel;
	private CategoryManagementPanel categoryManagementPanel;
	private ItemManagementPanel itemManagementPanel;
	private RecipeManagementPanel recipeManagementPanel;
	private IngredientManagementPanel ingredientManagementPanel;
	private SuppliersManagementPanel suppliersManagementPanel;

	public AppFrame() {
		// register instance for refresh callbacks
		AppFrame.instance = this;
		setTitle("POS - Quản lý đồ ăn nhanh");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Kích thước tối thiểu khi thu nhỏ
		final int MIN_WIDTH = 1200;
		final int MIN_HEIGHT = 700;
		setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
		
		// Mặc định phóng to full màn hình
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		
		// Khi từ maximize về normal, đặt kích thước hợp lý
		addWindowStateListener(e -> {
			int newState = e.getNewState();
			if ((newState & JFrame.MAXIMIZED_BOTH) == 0) {
				SwingUtilities.invokeLater(() -> {
					setSize(1500, 900);
					setLocationRelativeTo(null);
				});
			}
		});

		// Root panel
		JPanel root = new JPanel(new BorderLayout(0, 0));
		root.setBackground(UIConstants.BG_SECONDARY);
		setContentPane(root);

		// Left sidebar menu (vertical tabs) with modern styling
		JPanel sidebar = createModernSidebar();
		root.add(sidebar, BorderLayout.WEST);

		// Center area: sales screen (table-based like the reference image)
		JPanel center = new JPanel(new BorderLayout(10, 10));
		center.setOpaque(false);
		center.setBorder(new EmptyBorder(UIConstants.SPACING_SM, 0, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

		// Main content: fixed left (items) + fixed right (invoice + cart)
		JPanel mainContent = new JPanel(new BorderLayout(12, 10));
		mainContent.setOpaque(false);

		// LEFT: item list + item form
		CardPanel leftPane = new CardPanel(new BorderLayout(8, 8));
		leftPane.setShadowSize(3);
		leftPane.setRadius(UIConstants.RADIUS_LG);

		JPanel leftTop = new JPanel(new BorderLayout(8, 8));
		leftTop.setOpaque(false);
		leftTop.setBorder(new EmptyBorder(0, 0, UIConstants.SPACING_MD, 0));

		JPanel searchRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 8));
		searchRow.setOpaque(false);
		
		JLabel searchLabel = new JLabel("Tìm kiếm:");
		searchLabel.setFont(UIConstants.FONT_BODY_BOLD);
		searchLabel.setForeground(UIConstants.NEUTRAL_700);
		searchRow.add(searchLabel);
		
		this.searchField = new JTextField();
		this.searchField.setFont(NORMAL_FONT);
		this.searchField.setPreferredSize(new Dimension(200, UIConstants.INPUT_HEIGHT_SM));
		this.searchField.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(UIConstants.NEUTRAL_300),
			new EmptyBorder(6, 10, 6, 10)
		));
		searchRow.add(this.searchField);

		JLabel categoryLabel = new JLabel("Danh mục:");
		categoryLabel.setFont(UIConstants.FONT_BODY_BOLD);
		categoryLabel.setForeground(UIConstants.NEUTRAL_700);
		searchRow.add(categoryLabel);
		
		this.categoryCombo = new JComboBox<>();
		this.categoryCombo.setFont(NORMAL_FONT);
		this.categoryCombo.setPreferredSize(new Dimension(160, UIConstants.INPUT_HEIGHT_SM));
		searchRow.add(this.categoryCombo);

		leftTop.add(searchRow, BorderLayout.CENTER);
		leftPane.add(leftTop, BorderLayout.NORTH);

		String[] itemCols = { "Mã", "Tên món", "ĐVT", "Giá", "Loại" };
		this.itemTableModel = new DefaultTableModel(itemCols, 0) {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		this.itemTable = new JTable(this.itemTableModel);
		ModernTableStyle.apply(this.itemTable, true);
		
		// Set column widths - auto resize mode
		this.itemTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		this.itemTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // Mã
		this.itemTable.getColumnModel().getColumn(1).setPreferredWidth(140); // Tên món
		this.itemTable.getColumnModel().getColumn(2).setPreferredWidth(50);  // ĐVT
		this.itemTable.getColumnModel().getColumn(3).setPreferredWidth(70);  // Giá
		this.itemTable.getColumnModel().getColumn(4).setPreferredWidth(70);  // Loại
		
		JScrollPane itemScroll = new JScrollPane(this.itemTable);
		itemScroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
		itemScroll.getViewport().setBackground(Color.WHITE);

		JPanel itemForm = new JPanel();
		itemForm.setOpaque(false);
		itemForm.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(UIConstants.NEUTRAL_200, 1, true),
			new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_SM)
		));
		itemForm.setLayout(new BorderLayout(0, 0));
		itemForm.setPreferredSize(new Dimension(0, 190));
		itemForm.setMinimumSize(new Dimension(0, 190));
		JPanel itemFormGrid = new JPanel(new GridBagLayout());
		itemFormGrid.setOpaque(false);
		GridBagConstraints ig = new GridBagConstraints();
		ig.insets = new Insets(4, 4, 4, 4);
		ig.fill = GridBagConstraints.NONE;
		ig.anchor = GridBagConstraints.NORTHWEST;

		this.tfMaMon = new JTextField();
		this.tfTenMon = new JTextField();
		this.tfDonGia = new JTextField();
		this.tfSoLuong = new JTextField();
		Dimension fieldSize = new Dimension(220, 26);
		this.tfMaMon.setPreferredSize(fieldSize);
		this.tfTenMon.setPreferredSize(fieldSize);
		this.tfDonGia.setPreferredSize(fieldSize);
		this.tfSoLuong.setPreferredSize(fieldSize);
		this.tfMaMon.setMinimumSize(fieldSize);
		this.tfTenMon.setMinimumSize(fieldSize);
		this.tfDonGia.setMinimumSize(fieldSize);
		this.tfSoLuong.setMinimumSize(fieldSize);
		this.tfMaMon.setEditable(false);
		this.tfTenMon.setEditable(false);
		this.tfDonGia.setEditable(false);
		this.tfMaMon.setFocusable(false);
		this.tfTenMon.setFocusable(false);
		this.tfDonGia.setFocusable(false);
		this.tfMaMon.setRequestFocusEnabled(false);
		this.tfTenMon.setRequestFocusEnabled(false);
		this.tfDonGia.setRequestFocusEnabled(false);
		this.tfSoLuong.setFocusable(true);
		this.tfSoLuong.setRequestFocusEnabled(true);

		ig.gridx = 0;
		ig.gridy = 0;
		ig.weightx = 0;
		itemFormGrid.add(new JLabel("Mã món"), ig);
		ig.gridx = 1;
		ig.gridy = 0;
		ig.fill = GridBagConstraints.HORIZONTAL;
		ig.weightx = 0;
		itemFormGrid.add(this.tfMaMon, ig);

		ig.gridx = 0;
		ig.gridy = 1;
		ig.fill = GridBagConstraints.NONE;
		ig.weightx = 0;
		itemFormGrid.add(new JLabel("Tên món"), ig);
		ig.gridx = 1;
		ig.gridy = 1;
		ig.fill = GridBagConstraints.HORIZONTAL;
		itemFormGrid.add(this.tfTenMon, ig);

		ig.gridx = 0;
		ig.gridy = 2;
		ig.fill = GridBagConstraints.NONE;
		itemFormGrid.add(new JLabel("Đơn giá"), ig);
		ig.gridx = 1;
		ig.fill = GridBagConstraints.HORIZONTAL;
		itemFormGrid.add(this.tfDonGia, ig);

		ig.gridx = 0;
		ig.gridy = 3;
		ig.fill = GridBagConstraints.NONE;
		itemFormGrid.add(new JLabel("Số lượng"), ig);
		ig.gridx = 1;
		ig.fill = GridBagConstraints.HORIZONTAL;
		itemFormGrid.add(this.tfSoLuong, ig);

		this.itemImagePreview = new JLabel("Hình món", SwingConstants.CENTER);
		this.itemImagePreview.setOpaque(true);
		this.itemImagePreview.setBackground(new Color(245, 247, 249));
		this.itemImagePreview.setBorder(new LineBorder(new Color(170, 175, 178), 2));
		this.itemImagePreview.setPreferredSize(new Dimension(140, 130));

		JPanel imageWrap = new JPanel(new BorderLayout());
		imageWrap.setOpaque(false);
		imageWrap.add(this.itemImagePreview, BorderLayout.NORTH);

		JPanel itemFormWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		itemFormWrap.setOpaque(false);
		itemFormWrap.setBorder(new EmptyBorder(0, 12, 0, 0));
		itemFormWrap.add(itemFormGrid);

		JPanel itemDetailRow = new JPanel(new BorderLayout(12, 0));
		itemDetailRow.setOpaque(false);
		itemDetailRow.add(imageWrap, BorderLayout.WEST);
		itemDetailRow.add(itemFormWrap, BorderLayout.CENTER);

		JPanel itemDetailCenter = new JPanel(new GridBagLayout());
		itemDetailCenter.setOpaque(false);
		GridBagConstraints dc = new GridBagConstraints();
		dc.gridx = 0;
		dc.gridy = 0;
		dc.anchor = GridBagConstraints.CENTER;
		dc.weightx = 1;
		dc.weighty = 1;
		dc.fill = GridBagConstraints.NONE;
		itemDetailCenter.add(itemDetailRow, dc);

		itemForm.add(itemDetailCenter, BorderLayout.CENTER);

		// Fixed layout - không cho phép kéo co dãn
		JPanel leftBody = new JPanel(new BorderLayout(0, 8));
		leftBody.setOpaque(false);
		
		// Table có chiều cao cố định
		itemScroll.setPreferredSize(new Dimension(0, 300));
		leftBody.add(itemScroll, BorderLayout.CENTER);
		
		// Form chi tiết ở dưới
		itemForm.setPreferredSize(new Dimension(0, 200));
		leftBody.add(itemForm, BorderLayout.SOUTH);
		
		leftPane.add(leftBody, BorderLayout.CENTER);

		// RIGHT: cart only
		JPanel rightPane = new JPanel(new BorderLayout(6, 6));
		rightPane.setBackground(new Color(245, 247, 249));
		rightPane.setPreferredSize(new Dimension(480, 0));

		// Cart table with header
		JPanel cartSection = new JPanel(new BorderLayout(0, 8));
		cartSection.setOpaque(false);
		
		JLabel cartTitle = new JLabel("Giỏ hàng");
		cartTitle.setFont(UIConstants.FONT_HEADING_3);
		cartTitle.setForeground(UIConstants.PRIMARY_700);
		cartTitle.setBorder(new EmptyBorder(0, 0, UIConstants.SPACING_SM, 0));
		cartSection.add(cartTitle, BorderLayout.NORTH);

		String[] cartCols = { "Mã", "Tên món", "Giá", "Loại", "SL" };
		this.cartTableModel = new DefaultTableModel(cartCols, 0) {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		this.cartTable = new JTable(this.cartTableModel);
		ModernTableStyle.apply(this.cartTable, true);
		
		// Set cart column widths
		this.cartTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		this.cartTable.getColumnModel().getColumn(0).setPreferredWidth(60);   // Mã
		this.cartTable.getColumnModel().getColumn(1).setPreferredWidth(180);  // Tên món
		this.cartTable.getColumnModel().getColumn(2).setPreferredWidth(90);   // Giá
		this.cartTable.getColumnModel().getColumn(3).setPreferredWidth(120);  // Loại
		this.cartTable.getColumnModel().getColumn(4).setPreferredWidth(50);   // SL
		
		JScrollPane cartScroll = new JScrollPane(this.cartTable);
		cartScroll.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
		cartScroll.getViewport().setBackground(Color.WHITE);
		cartSection.add(cartScroll, BorderLayout.CENTER);
		
		rightPane.add(cartSection, BorderLayout.CENTER);

		mainContent.add(leftPane, BorderLayout.CENTER);
		mainContent.add(rightPane, BorderLayout.EAST);
		center.add(mainContent, BorderLayout.CENTER);

		// Bottom action bar with modern buttons
		JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, UIConstants.SPACING_LG, 0));
		bottomBar.setOpaque(false);
		bottomBar.setBorder(new EmptyBorder(UIConstants.SPACING_LG, 0, UIConstants.SPACING_SM, 0));
		
		ModernButton btnThem = new ModernButton("Thêm vào giỏ", ModernButton.ButtonType.PRIMARY);
		
		btnThem.setPreferredSize(new Dimension(160, UIConstants.BUTTON_HEIGHT));
		
		ModernButton btnXoa = new ModernButton("Xóa khỏi giỏ", ModernButton.ButtonType.DANGER);
		
		btnXoa.setPreferredSize(new Dimension(160, UIConstants.BUTTON_HEIGHT));
		
		ModernButton btnDatLai = new ModernButton("Đặt lại", ModernButton.ButtonType.WARNING);
		
		btnDatLai.setPreferredSize(new Dimension(160, UIConstants.BUTTON_HEIGHT));
		
		ModernButton btnThanhToan = new ModernButton("Thanh toán", ModernButton.ButtonType.SUCCESS);
		
		btnThanhToan.setPreferredSize(new Dimension(180, UIConstants.BUTTON_HEIGHT_LG));
		
		bottomBar.add(btnThem);
		bottomBar.add(btnXoa);
		bottomBar.add(btnDatLai);
		bottomBar.add(btnThanhToan);
		center.add(bottomBar, BorderLayout.SOUTH);

		this.tabCardLayout = new CardLayout();
		this.tabCards = new JPanel(this.tabCardLayout);
		this.tabCards.setBackground(new Color(245, 247, 249));

		JPanel salesPage = new JPanel(new BorderLayout(12, 12));
		salesPage.setBackground(new Color(245, 247, 249));
		salesPage.add(center, BorderLayout.CENTER);
		this.tabCards.add(salesPage, "Bán hàng");

		this.importGoodsPanel = new ImportGoodsPanel(this);
		this.tabCards.add(wrapTabPage("Nhập hàng", this.importGoodsPanel), "Nhập hàng");
		this.itemManagementPanel = new ItemManagementPanel(() -> {
			reloadCategories();
			refreshMenu();
			if (recipeManagementPanel != null) recipeManagementPanel.reloadCategories();
		});
		this.tabCards.add(wrapTabPage("Món ăn", this.itemManagementPanel), "Món ăn");
		this.categoryManagementPanel = new CategoryManagementPanel(() -> {
			reloadCategories();
			refreshMenu();
			if (itemManagementPanel != null) itemManagementPanel.reloadCategories();
			if (recipeManagementPanel != null) recipeManagementPanel.reloadCategories();
		});
		this.tabCards.add(wrapTabPage("Danh mục", this.categoryManagementPanel), "Danh mục");
		this.ingredientManagementPanel = new IngredientManagementPanel(() -> {
			if (recipeManagementPanel != null) recipeManagementPanel.refreshSelectedRecipe();
			if (recipeManagementPanel != null) {
				recipeManagementPanel.reloadIngredientsFilter();
				recipeManagementPanel.refreshProductsList();
			}
			if (importGoodsPanel != null) importGoodsPanel.refreshIngredients();
		});
		this.tabCards.add(wrapTabPage("Nguyên liệu", this.ingredientManagementPanel), "Nguyên liệu");
		this.recipeManagementPanel = new RecipeManagementPanel(null);
		this.tabCards.add(wrapTabPage("Công thức", this.recipeManagementPanel), "Công thức");
		this.ordersPanel = new OrdersManagementPanel(null);
		this.tabCards.add(wrapTabPage("Hóa đơn", this.ordersPanel), "Hóa đơn");
		this.importHistoryPanel = new ImportHistoryManagementPanel(null);
		this.tabCards.add(wrapTabPage("Hóa đơn nhập", this.importHistoryPanel), "Hóa đơn nhập");
		this.tabCards.add(wrapTabPage("Khuyến mãi", new PromotionsManagementPanel(null)), "Khuyến mãi");
		this.tabCards.add(wrapTabPage("Khách hàng", new CustomersManagementPanel(this, null)), "Khách hàng");
		this.tabCards.add(wrapTabPage("Nhân viên", new EmployeesManagementPanel(null)), "Nhân viên");
		this.suppliersManagementPanel = new SuppliersManagementPanel(() -> {
			if (ingredientManagementPanel != null) ingredientManagementPanel.onSuppliersChanged();
		});
		this.tabCards.add(wrapTabPage("Nhà cung cấp", this.suppliersManagementPanel), "Nhà cung cấp");
		this.tabCards.add(wrapTabPage("Tài khoản", new AccountsManagementPanel(null)), "Tài khoản");
		this.tabCards.add(wrapTabPage("Phân quyền", new PermissionsManagementPanel(null)), "Phân quyền");
		this.tabCards.add(wrapTabPage("Thống kê", new DashboardManagementPanel(null)), "Thống kê");

		root.add(this.tabCards, BorderLayout.CENTER);

		loadCategories();
		this.categoryCombo.addActionListener(e -> refreshMenu());
		this.searchField.addActionListener(e -> refreshMenu());
		this.searchField.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyReleased(java.awt.event.KeyEvent e) {
				refreshMenu();
			}
		});

		this.itemTable.getSelectionModel().addListSelectionListener(e -> {
			if (e.getValueIsAdjusting()) return;
			int row = itemTable.getSelectedRow();
			if (row < 0 || row >= currentItems.size()) return;
			selectedItem = currentItems.get(row);
			if (selectedItem == null) return;
			this.tfMaMon.setText(selectedItem.getCode());
			this.tfTenMon.setText(selectedItem.getName());
			this.tfDonGia.setText(CurrencyUtil.format(selectedItem.getPrice()));
			this.tfSoLuong.setText("1");
			updateSelectedItemImage(selectedItem);
		});

		btnThem.addActionListener(e -> {
			if (selectedItem == null) {
				JOptionPane.showMessageDialog(this, "Vui lòng chọn món trong danh sách");
				return;
			}
			int qty = parseIntSafe(this.tfSoLuong.getText(), 1);
			if (qty <= 0) {
				JOptionPane.showMessageDialog(this, "Số lượng phải > 0");
				return;
			}
			addToCart(selectedItem, qty);
		});
		btnXoa.addActionListener(e -> {
			int row = cartTable.getSelectedRow();
			if (row < 0) {
				JOptionPane.showMessageDialog(this, "Vui lòng chọn món cần xóa trong bảng hóa đơn");
				return;
			}
			Object code = cartTableModel.getValueAt(row, 0);
			if (code == null) return;
			int removeId = -1;
			for (CartItem ci : cart.values()) {
				if (ci != null && ci.getItem() != null && String.valueOf(code).equals(ci.getItem().getCode())) {
					removeId = ci.getItem().getId();
					break;
				}
			}
			if (removeId > 0) {
				cart.remove(removeId);
				renderCart();
			}
		});
		btnDatLai.addActionListener(e -> {
			if (cart.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Giỏ hàng đã trống");
				return;
			}
			int confirm = JOptionPane.showConfirmDialog(this, 
				"Bạn có chắc muốn xóa toàn bộ giỏ hàng?", 
				"Xác nhận đặt lại", 
				JOptionPane.YES_NO_OPTION, 
				JOptionPane.QUESTION_MESSAGE);
			if (confirm == JOptionPane.YES_OPTION) {
				cart.clear();
				renderCart();
				JOptionPane.showMessageDialog(this, "Đã đặt lại giỏ hàng");
			}
		});
		btnThanhToan.addActionListener(e -> {
			if (cart.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Giỏ hàng đang trống");
				return;
			}
			new PaymentFrame(this, getCartSnapshot());
		});

		refreshMenu();
		renderCart();
		navigateToTab("Bán hàng");

		setVisible(true);
	}
	
	/**
	 * Create modern sidebar with icons and hover effects
	 */
	private JPanel createModernSidebar() {
		JPanel sidebar = new JPanel();
		sidebar.setLayout(new BorderLayout());
		sidebar.setBackground(Color.WHITE);
		sidebar.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 0));
		sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIConstants.NEUTRAL_200));
		
		// Header with logo
		JPanel sidebarHeader = new JPanel(new GridBagLayout());
		sidebarHeader.setBackground(Color.WHITE);
		sidebarHeader.setPreferredSize(new Dimension(0, 85));
		sidebarHeader.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		// Load và hiển thị logo
		JLabel logoLabel = createSidebarLogo();
		sidebarHeader.add(logoLabel);
		
		sidebar.add(sidebarHeader, BorderLayout.NORTH);
		
		// Menu items
		JPanel menuPanel = new JPanel();
		menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
		menuPanel.setBackground(Color.WHITE);
		menuPanel.setBorder(new EmptyBorder(UIConstants.SPACING_XS, 4, UIConstants.SPACING_XS, 4));
		
		// Danh sách menu với các section dividers
		String[][] menuSections = {
			{ "Bán hàng", "Nhập hàng", "Món ăn", "Danh mục", "Nguyên liệu", "Công thức", "Hóa đơn" },
			{ "Hóa đơn nhập", "Khuyến mãi" },
			{ "Khách hàng", "Nhân viên", "Nhà cung cấp" },
			{ "Tài khoản", "Phân quyền", "Thống kê" }
		};
		
		User currentUser = Session.getCurrentUser();
		ButtonGroup menuGroup = new ButtonGroup();
		boolean firstSelected = false;
		
		for (int s = 0; s < menuSections.length; s++) {
			String[] section = menuSections[s];
			boolean sectionHasItems = false;
			
			// Kiểm tra section này có ít nhất 1 item user được phép truy cập không
			for (String itemName : section) {
				if (PermissionService.canAccessTab(currentUser, itemName)) {
					sectionHasItems = true;
					break;
				}
			}
			
			// Nếu section không có item nào thì bỏ qua
			if (!sectionHasItems) continue;
			
			// Thêm divider giữa các sections (trừ section đầu tiên)
			if (menuPanel.getComponentCount() > 0) {
				JSeparator sep = new JSeparator();
				sep.setForeground(UIConstants.NEUTRAL_200);
				sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
				menuPanel.add(Box.createRigidArea(new Dimension(0, UIConstants.SPACING_SM)));
				menuPanel.add(sep);
				menuPanel.add(Box.createRigidArea(new Dimension(0, UIConstants.SPACING_SM)));
			}
			
			// Thêm các menu items trong section
			for (String itemName : section) {
				// Chỉ hiển thị tab mà user có quyền truy cập
				if (!PermissionService.canAccessTab(currentUser, itemName)) {
					continue;
				}
				
				String icon = UIConstants.getMenuIcon(itemName);
				SidebarButton button = new SidebarButton(itemName, icon);
				menuGroup.add(button);
				
				button.addActionListener(e -> navigateToTab(itemName));
				
				menuPanel.add(button);
				menuPanel.add(Box.createRigidArea(new Dimension(0, 2)));
				
				// Select first visible item by default
				if (!firstSelected) {
					button.setSelected(true);
					firstSelected = true;
				}
			}
		}
		
		JScrollPane menuScroll = new JScrollPane(menuPanel);
		menuScroll.setBorder(null);
		menuScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		menuScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		menuScroll.getVerticalScrollBar().setUnitIncrement(16);
		sidebar.add(menuScroll, BorderLayout.CENTER);
		
		// Footer with user info
		JPanel userPanel = new JPanel();
		userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
		userPanel.setBackground(UIConstants.NEUTRAL_50);
		userPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.NEUTRAL_200),
			new EmptyBorder(UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD, UIConstants.SPACING_MD)
		));

		JPanel userTopRow = new JPanel(new BorderLayout(UIConstants.SPACING_MD, 0));
		userTopRow.setOpaque(false);

		JLabel userIcon = new JLabel(UIConstants.ICON_USER);
		userIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
		userTopRow.add(userIcon, BorderLayout.WEST);

		JPanel userInfo = new JPanel();
		userInfo.setLayout(new BoxLayout(userInfo, BoxLayout.Y_AXIS));
		userInfo.setOpaque(false);
		userInfo.setBorder(new EmptyBorder(0, UIConstants.SPACING_SM, 0, UIConstants.SPACING_SM));
		userInfo.setPreferredSize(new Dimension(160, 56));

		// Sử dụng lại currentUser đã khai báo ở trên
		String username = currentUser != null ? currentUser.getUsername() : "Guest";
		String fullName = currentUser != null ? currentUser.getFullName() : "";
		String role = currentUser != null ? currentUser.getRole() : "";
		String displayName = (fullName != null && !fullName.trim().isEmpty()) ? fullName.trim() : username;
		String roleDisplay = (role != null && !role.trim().isEmpty()) ? role.trim() : (Session.isManager() ? "Manager" : "Staff");
		String roleVi;
		if (roleDisplay.equalsIgnoreCase("Manager")) {
			roleVi = "Quản lý";
		} else {
			roleVi = "Nhân viên";
		}

		JLabel nameLabel = new JLabel(displayName);
		nameLabel.setFont(UIConstants.FONT_BODY_BOLD);
		nameLabel.setForeground(UIConstants.NEUTRAL_900);
		nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel metaLabel = new JLabel(username);
		metaLabel.setFont(UIConstants.FONT_CAPTION);
		metaLabel.setForeground(UIConstants.NEUTRAL_500);
		metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel roleLabel = new JLabel("Chức vụ: " + roleVi);
		roleLabel.setFont(UIConstants.FONT_CAPTION);
		roleLabel.setForeground(UIConstants.NEUTRAL_600);
		roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		userInfo.add(nameLabel);
		userInfo.add(metaLabel);
		userInfo.add(roleLabel);
		userTopRow.add(userInfo, BorderLayout.CENTER);

		ModernButton logoutBtn = new ModernButton("Đăng xuất", ModernButton.ButtonType.PRIMARY, ModernButton.ButtonSize.SMALL);
		logoutBtn.setPreferredSize(new Dimension(110, 36));
		logoutBtn.setToolTipText("Đăng xuất");
		logoutBtn.addActionListener(e -> {
			int confirm = JOptionPane.showConfirmDialog(this, 
				"Bạn có chắc muốn đăng xuất?", 
				"Xác nhận đăng xuất", 
				JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION) {
				Session.setCurrentUser(null);
				dispose();
				new LoginFrame();
			}
		});

		JPanel userBottomRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		userBottomRow.setOpaque(false);
		userBottomRow.add(logoutBtn);

		userPanel.add(userTopRow);
		userPanel.add(Box.createRigidArea(new Dimension(0, UIConstants.SPACING_SM)));
		userPanel.add(userBottomRow);
		
		sidebar.add(userPanel, BorderLayout.SOUTH);
		
		return sidebar;
	}
	
	/**
	 * Tạo logo cho sidebar từ file SVG
	 */
	private JLabel createSidebarLogo() {
		int logoSize = 75;
		JLabel logoLabel = new JLabel();
		logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
		logoLabel.setVerticalAlignment(SwingConstants.CENTER);
		logoLabel.setPreferredSize(new Dimension(logoSize, logoSize));
		
		try {
			File svgFile = new File("img/logo.svg");
			if (svgFile.exists()) {
				// Sử dụng Batik để render SVG
				PNGTranscoder transcoder = new PNGTranscoder();
				transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) logoSize);
				transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) logoSize);
				
				TranscoderInput input = new TranscoderInput(svgFile.toURI().toString());
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				TranscoderOutput output = new TranscoderOutput(outputStream);
				
				transcoder.transcode(input, output);
				outputStream.flush();
				
				byte[] imgData = outputStream.toByteArray();
				ByteArrayInputStream inputStream = new ByteArrayInputStream(imgData);
				BufferedImage img = ImageIO.read(inputStream);
				
				if (img != null) {
					logoLabel.setIcon(new ImageIcon(img));
					return logoLabel;
				}
			}
		} catch (Exception e) {
			System.out.println("Lỗi khi load logo SVG cho sidebar: " + e.getMessage());
		}
		
		// Fallback: hiển thị chữ POS
		logoLabel.setText("POS");
		logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
		logoLabel.setForeground(UIConstants.PRIMARY_700);
		return logoLabel;
	}

	public static AppFrame getInstance() {
		return instance;
	}

	public void navigateToTab(String tabName) {
		if (tabName == null || tabName.trim().isEmpty()) return;
		if (this.tabCards == null || this.tabCardLayout == null) return;
		this.tabCardLayout.show(this.tabCards, tabName.trim());
		// Auto refresh orders panel when switching to it
		if ("Hóa đơn".equals(tabName.trim()) && this.ordersPanel != null) {
			this.ordersPanel.refreshData();
		}
		// Auto refresh import history panel when switching to it
		if ("Hóa đơn nhập".equals(tabName.trim()) && this.importHistoryPanel != null) {
			this.importHistoryPanel.refreshData();
		}
	}

	private JPanel wrapTabPage(String title, JPanel content) {
		JPanel page = new JPanel(new BorderLayout(10, 10));
		page.setBackground(UIConstants.BG_SECONDARY);
		page.setBorder(new EmptyBorder(UIConstants.SPACING_SM, UIConstants.SPACING_SM, UIConstants.SPACING_MD, UIConstants.SPACING_MD));

		// Content fills the entire space - no header
		page.add(content == null ? new JPanel() : content, BorderLayout.CENTER);
		return page;
	}

	/** Refresh menu cards from database */
	public void refreshMenu() {
		if (instance == null) instance = this;
		if (itemTableModel == null) return;
		String keyword = searchField != null ? searchField.getText().trim() : null;
		Integer categoryId = null;
		if (categoryCombo != null && categoryCombo.getSelectedItem() instanceof Category) {
			Category c = (Category) categoryCombo.getSelectedItem();
			if (c != null && c.getId() > 0) categoryId = c.getId();
		}
		List<Item> items = ItemDAO.findByFilter(keyword, categoryId);
		if (items == null) items = new ArrayList<>();
		currentItems = items;
		itemTableModel.setRowCount(0);
		for (Item it : items) {
			String categoryName = categoryNameById.getOrDefault(it.getCategoryId(), "");
			itemTableModel.addRow(new Object[] {
					it.getCode(),
					it.getName(),
					"Phần",
					CurrencyUtil.format(it.getPrice()),
					categoryName
			});
		}
	}

	private void loadCategories() {
		if (this.categoryCombo == null) return;
		categoryNameById.clear();
		DefaultComboBoxModel<Category> model = new DefaultComboBoxModel<>();
		model.addElement(new Category(0, "Tất cả"));
		for (Category c : CategoryDAO.findAllActive()) {
			model.addElement(c);
			if (c != null) categoryNameById.put(c.getId(), c.getName());
		}
		this.categoryCombo.setModel(model);
	}

	public void reloadCategories() {
		loadCategories();
	}

	private void addToCart(Item item) {
		addToCart(item, 1);
	}

	private void addToCart(Item item, int qty) {
		if (item == null) return;
		if (qty <= 0) return;
		int nextQty = qty;
		CartItem existing = cart.get(item.getId());
		if (existing != null) nextQty = existing.getQuantity() + qty;
		try (java.sql.Connection c = DBConnection.getConnection()) {
			List<CartItem> check = new ArrayList<>();
			for (CartItem ci : cart.values()) {
				if (ci == null || ci.getItem() == null) continue;
				if (ci.getItem().getId() == item.getId()) continue;
				check.add(new CartItem(ci.getItem(), ci.getQuantity()));
			}
			check.add(new CartItem(item, nextQty));
			List<InventoryDAO.IngredientShortage> shortages = InventoryDAO.findIngredientShortagesForCart(c, check);
			if (shortages != null && !shortages.isEmpty()) {
				JOptionPane.showMessageDialog(this, buildIngredientShortageMessage(shortages));
				return;
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Không thể kiểm tra tồn nguyên liệu: " + ex.getMessage());
			return;
		}
		if (existing == null) {
			cart.put(item.getId(), new CartItem(item, qty));
		} else {
			existing.setQuantity(existing.getQuantity() + qty);
		}
		renderCart();
	}

	private String buildIngredientShortageMessage(List<InventoryDAO.IngredientShortage> shortages) {
		StringBuilder sb = new StringBuilder();
		List<String> missingRecipes = new ArrayList<>();
		boolean hasStockShortage = false;
		for (InventoryDAO.IngredientShortage s : shortages) {
			if (s == null) continue;
			if (s.ingredientId == 0) {
				String name = s.ingredientName == null ? "" : s.ingredientName.trim();
				if (!name.isEmpty()) missingRecipes.add(name);
				continue;
			}
			hasStockShortage = true;
		}
		if (!missingRecipes.isEmpty()) {
			sb.append("Món chưa có công thức. Vui lòng thiết lập ở tab Công thức:\n");
			for (String n : missingRecipes) sb.append("- ").append(n).append("\n");
			sb.append("\n");
		}
		if (hasStockShortage) {
			sb.append("Không đủ nguyên liệu. Vui lòng nhập thêm:\n");
			for (InventoryDAO.IngredientShortage s : shortages) {
				if (s == null) continue;
				if (s.ingredientId == 0) continue;
				double missing = s.required - s.available;
				if (missing < 0) missing = 0;
				String name = s.ingredientName == null ? "" : s.ingredientName;
				String unit = s.unit == null ? "" : s.unit;
				sb.append("- ").append(name);
				if (!unit.trim().isEmpty()) sb.append(" (").append(unit).append(")");
				sb.append(": cần ").append(trimFloat(s.required))
					.append(", còn ").append(trimFloat(s.available))
					.append(", thiếu ").append(trimFloat(missing))
					.append("\n");
			}
		}
		return sb.toString();
	}

	private String trimFloat(double v) {
		long asLong = (long) v;
		if (v == asLong) return String.valueOf(asLong);
		return String.format("%.2f", v);
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
		if (cartTableModel == null) return;
		double total = 0;
		cartTableModel.setRowCount(0);
		for (CartItem ci : cart.values()) {
			if (ci == null || ci.getItem() == null) continue;
			Item it = ci.getItem();
			total += ci.getLineTotal();
			String categoryName = categoryNameById.getOrDefault(it.getCategoryId(), "");
			cartTableModel.addRow(new Object[] {
					it.getCode(),
					it.getName(),
					CurrencyUtil.format(it.getPrice()),
					categoryName,
					ci.getQuantity()
			});
		}
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
		selectedItem = null;
		updateSelectedItemImage(null);
		if (tfMaMon != null) tfMaMon.setText("");
		if (tfTenMon != null) tfTenMon.setText("");
		if (tfDonGia != null) tfDonGia.setText("");
		if (tfSoLuong != null) tfSoLuong.setText("");
		reloadCategories();
		refreshMenu();
		if (ordersPanel != null) ordersPanel.refreshData();
		if (importHistoryPanel != null) importHistoryPanel.refreshData();
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

	private String generateInvoiceNo() {
		long t = System.currentTimeMillis();
		return "HD" + (t % 1000000000L);
	}

	private void applyUniformTableStyle(JTable table) {
		if (table == null) return;
		Color grid = new Color(220, 225, 226);
		table.setShowGrid(true);
		table.setGridColor(grid);
		table.setIntercellSpacing(new Dimension(1, 1));
		table.setRowMargin(0);
		table.setSelectionBackground(new Color(210, 230, 245));
		table.setSelectionForeground(Color.BLACK);

		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(t, value, isSelected, false, row, column);
				setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
				return c;
			}
		};
		table.setDefaultRenderer(Object.class, renderer);
	}

	private void updateSelectedItemImage(Item item) {
		if (this.itemImagePreview == null) return;
		this.itemImagePreview.setIcon(null);
		this.itemImagePreview.setText("Hình món");
		if (item == null) return;
		String path = item.getImagePath();
		if (path == null) return;
		String p = path.trim();
		if (p.isEmpty()) return;
		try {
			BufferedImage img = null;
			if (p.startsWith("http://") || p.startsWith("https://")) {
				img = ImageIO.read(new java.net.URL(p));
			} else {
				File f = new File(p);
				if (!f.isAbsolute()) {
					f = new File(System.getProperty("user.dir"), p);
				}
				if (f.exists()) {
					img = ImageIO.read(f);
				}
			}
			if (img == null) return;
			int w = this.itemImagePreview.getWidth();
			int h = this.itemImagePreview.getHeight();
			if (w <= 0 || h <= 0) {
				Dimension d = this.itemImagePreview.getPreferredSize();
				w = d.width;
				h = d.height;
			}
			int pad = 8;
			int tw = Math.max(1, w - pad);
			int th = Math.max(1, h - pad);
			Image scaled = img.getScaledInstance(tw, th, Image.SCALE_SMOOTH);
			this.itemImagePreview.setText("");
			this.itemImagePreview.setIcon(new ImageIcon(scaled));
		} catch (Exception ignored) {
		}
	}

	private String formatEmployeeCode(User u) {
		if (u == null) return "";
		if (u.getUsername() != null && !u.getUsername().trim().isEmpty()) return u.getUsername().trim();
		return "NV" + u.getId();
	}

	private int parseIntSafe(String s, int defaultValue) {
		if (s == null) return defaultValue;
		String t = s.trim();
		if (t.isEmpty()) return defaultValue;
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}
}


