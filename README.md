# quanlybandoannhanh

Ứng dụng quản lý bán đồ ăn nhanh (POS System) viết bằng Java Swing + MySQL.

## Tính năng

- Đăng nhập/Đăng xuất với phân quyền (Admin/Nhân viên)
- Quản lý danh mục và món ăn
- Giỏ hàng và thanh toán
- Tồn kho và quản lý kho
- Thanh toán tiền mặt và chuyển khoản
- Tạo mã QR thanh toán (VietQR)
- Định dạng tiền tệ VND

## Công nghệ

- **Ngôn ngữ:** Java
- **Giao diện:** Java Swing
- **Cơ sở dữ liệu:** MySQL
- **Driver:** MySQL Connector/J 9.5.0

## Cài đặt

1. Tạo database `pos_db` trong MySQL
2. Chạy file `db/schema.sql` để tạo bảng
3. Import dữ liệu mẫu từ `db/drop_pos.sql` (nếu cần)
4. Cấu hình kết nối MySQL trong `src/com/pos/db/DBConnection.java`
5. Chạy ứng dụng bằng `run.bat`

## Cấu trúc thư mục

```
quanlybandoannhanh/
├── src/
│   └── com/pos/
│       ├── dao/          # Data Access Objects
│       ├── db/           # Database connection
│       ├── model/        # Model classes
│       ├── service/      # Business logic
│       ├── ui/           # User interface
│       └── util/         # Utilities (CurrencyUtil, etc.)
├── db/
│   ├── schema.sql        # Database schema
│   └── drop_pos.sql      # Sample data
├── lib/                  # Libraries
├── img/                  # Product images
└── run.bat              # Run script
```

## Tài khoản mẫu

- Admin: `admin` / `admin123`
- Nhân viên: `staff` / `staff123`

## Tác giả

- GitHub: [@Ryan030904](https://github.com/Ryan030904)

