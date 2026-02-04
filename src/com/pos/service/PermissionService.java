package com.pos.service;

import com.pos.model.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class PermissionService {
    private static final String CONFIG_FILE = "permissions.properties";
    private static final String ADMIN_USERNAME = "admin";

    public static boolean canAccessTab(User user, String tabName) {
        if (tabName == null || tabName.trim().isEmpty()) return false;

        String t = tabName.trim();
        String code = permissionCodeForUser(user);
        if ("PQ0".equalsIgnoreCase(code)) {
            return true;
        }

        String username = user == null ? null : user.getUsername();
        Set<String> allowed = loadTabsForUsername(username, code);
        return allowed.contains(t);
    }

	public static boolean canMutateTab(User user, String tabName) {
		if (tabName == null || tabName.trim().isEmpty()) return false;
		String t = tabName.trim();
		String code = permissionCodeForUser(user);
		if ("PQ0".equalsIgnoreCase(code)) return true;
		if (!canAccessTab(user, t)) return false;
		return canAddTab(user, t) || canEditTab(user, t) || canDeleteTab(user, t);
	}

	public static boolean canAddTab(User user, String tabName) {
		if (tabName == null || tabName.trim().isEmpty()) return false;
		String t = tabName.trim();
		String code = permissionCodeForUser(user);
		if ("PQ0".equalsIgnoreCase(code)) return true;
		if (!canAccessTab(user, t)) return false;
		String username = user == null ? null : user.getUsername();
		Set<String> allowed = loadAddTabsForUsername(username, code);
		return allowed.contains(t);
	}

	public static boolean canEditTab(User user, String tabName) {
		if (tabName == null || tabName.trim().isEmpty()) return false;
		String t = tabName.trim();
		String code = permissionCodeForUser(user);
		if ("PQ0".equalsIgnoreCase(code)) return true;
		if (!canAccessTab(user, t)) return false;
		String username = user == null ? null : user.getUsername();
		Set<String> allowed = loadEditTabsForUsername(username, code);
		return allowed.contains(t);
	}

	public static boolean canDeleteTab(User user, String tabName) {
		if (tabName == null || tabName.trim().isEmpty()) return false;
		String t = tabName.trim();
		String code = permissionCodeForUser(user);
		if ("PQ0".equalsIgnoreCase(code)) return true;
		if (!canAccessTab(user, t)) return false;
		String username = user == null ? null : user.getUsername();
		Set<String> allowed = loadDeleteTabsForUsername(username, code);
		return allowed.contains(t);
	}

    public static Set<String> loadTabsForUsername(String username, String permissionCode) {
        String un = username == null ? "" : username.trim();
        if (!un.isEmpty() && un.equalsIgnoreCase(ADMIN_USERNAME)) {
            return parseCsvTabs(defaultTabsForPermissionCode("PQ0"));
        }

        Properties p = loadProperties();
        if (!un.isEmpty()) {
            String vUser = p.getProperty("user." + un + ".tabs");
            if (vUser != null && !vUser.trim().isEmpty()) {
                return parseCsvTabs(vUser);
            }
        }
        return loadTabsForPermissionCode(permissionCode);
    }

	public static Set<String> loadMutateTabsForUsername(String username, String permissionCode) {
		String un = username == null ? "" : username.trim();
		if (!un.isEmpty() && un.equalsIgnoreCase(ADMIN_USERNAME)) {
			return parseCsvTabs(defaultTabsForPermissionCode("PQ0"));
		}

		Properties p = loadProperties();
		if (!un.isEmpty()) {
			String key = "user." + un + ".mutateTabs";
			if (p.containsKey(key)) {
				return parseCsvTabs(p.getProperty(key));
			}
		}
		return loadMutateTabsForPermissionCode(permissionCode);
	}

	public static Set<String> loadEditTabsForUsername(String username, String permissionCode) {
		String un = username == null ? "" : username.trim();
		if (!un.isEmpty() && un.equalsIgnoreCase(ADMIN_USERNAME)) {
			return parseCsvTabs(defaultTabsForPermissionCode("PQ0"));
		}

		Properties p = loadProperties();
		if (!un.isEmpty()) {
			String key = "user." + un + ".editTabs";
			if (p.containsKey(key)) {
				return parseCsvTabs(p.getProperty(key));
			}
			String legacyKey = "user." + un + ".mutateTabs";
			if (p.containsKey(legacyKey)) {
				return parseCsvTabs(p.getProperty(legacyKey));
			}
		}
		return loadEditTabsForPermissionCode(permissionCode);
	}

	public static Set<String> loadAddTabsForUsername(String username, String permissionCode) {
		String un = username == null ? "" : username.trim();
		if (!un.isEmpty() && un.equalsIgnoreCase(ADMIN_USERNAME)) {
			return parseCsvTabs(defaultTabsForPermissionCode("PQ0"));
		}

		Properties p = loadProperties();
		if (!un.isEmpty()) {
			String key = "user." + un + ".addTabs";
			if (p.containsKey(key)) {
				return parseCsvTabs(p.getProperty(key));
			}
			String legacyKey = "user." + un + ".editTabs";
			if (p.containsKey(legacyKey)) {
				return parseCsvTabs(p.getProperty(legacyKey));
			}
			String legacyKey2 = "user." + un + ".mutateTabs";
			if (p.containsKey(legacyKey2)) {
				return parseCsvTabs(p.getProperty(legacyKey2));
			}
		}
		return loadAddTabsForPermissionCode(permissionCode);
	}

	public static Set<String> loadDeleteTabsForUsername(String username, String permissionCode) {
		String un = username == null ? "" : username.trim();
		if (!un.isEmpty() && un.equalsIgnoreCase(ADMIN_USERNAME)) {
			return parseCsvTabs(defaultTabsForPermissionCode("PQ0"));
		}

		Properties p = loadProperties();
		if (!un.isEmpty()) {
			String key = "user." + un + ".deleteTabs";
			if (p.containsKey(key)) {
				return parseCsvTabs(p.getProperty(key));
			}
			String legacyKey = "user." + un + ".mutateTabs";
			if (p.containsKey(legacyKey)) {
				return parseCsvTabs(p.getProperty(legacyKey));
			}
		}
		return loadDeleteTabsForPermissionCode(permissionCode);
	}

    public static Set<String> loadTabsForRole(String role) {
        String roleKey = canonicalRole(role);
        String code = roleToPermissionCode(roleKey);
        if ("PQ0".equalsIgnoreCase(code)) {
            return parseCsvTabs(defaultTabsForPermissionCode("PQ0"));
        }
        Set<String> byCode = loadTabsForPermissionCode(code);
        if (!byCode.isEmpty()) return byCode;

        // backward compatibility
        Properties p = loadProperties();
        String key = "role." + roleKey + ".tabs";
        String v = p.getProperty(key);
        if (v == null || v.trim().isEmpty()) {
            v = defaultTabsForRole(roleKey);
        }
        return parseCsvTabs(v);
    }

	public static Set<String> loadMutateTabsForPermissionCode(String code) {
		String c = code == null ? "" : code.trim();
		if (c.isEmpty()) c = "PQ2";
		if ("PQ0".equalsIgnoreCase(c)) {
			return parseCsvTabs(defaultTabsForPermissionCode("PQ0"));
		}
		if (!"PQ2".equalsIgnoreCase(c)) {
			c = "PQ2";
		}

		Properties p = loadProperties();
		String permKey = "perm." + c + ".mutateTabs";
		if (p.containsKey(permKey)) {
			return parseCsvTabs(p.getProperty(permKey));
		}
		String roleKey = "role.Staff.mutateTabs";
		if (p.containsKey(roleKey)) {
			return parseCsvTabs(p.getProperty(roleKey));
		}

		{
			// Backward compatible default: allow mutations on allowed tabs, but keep Promotions view-only.
			Set<String> base = loadTabsForPermissionCode(c);
			base.remove("Khuyến mãi");
			base.remove("Nhà cung cấp");
			return base;
		}
	}

	public static Set<String> loadEditTabsForPermissionCode(String code) {
		String c = code == null ? "" : code.trim();
		if (c.isEmpty()) c = "PQ2";
		if ("PQ0".equalsIgnoreCase(c)) {
			return parseCsvTabs(defaultTabsForPermissionCode("PQ0"));
		}
		if (!"PQ2".equalsIgnoreCase(c)) {
			c = "PQ2";
		}

		Properties p = loadProperties();
		String permKey = "perm." + c + ".editTabs";
		if (p.containsKey(permKey)) {
			return parseCsvTabs(p.getProperty(permKey));
		}
		String roleKey = "role.Staff.editTabs";
		if (p.containsKey(roleKey)) {
			return parseCsvTabs(p.getProperty(roleKey));
		}
		return loadMutateTabsForPermissionCode(c);
	}

	public static Set<String> loadAddTabsForPermissionCode(String code) {
		String c = code == null ? "" : code.trim();
		if (c.isEmpty()) c = "PQ2";
		if ("PQ0".equalsIgnoreCase(c)) {
			return parseCsvTabs(defaultTabsForPermissionCode("PQ0"));
		}
		if (!"PQ2".equalsIgnoreCase(c)) {
			c = "PQ2";
		}

		Properties p = loadProperties();
		String permKey = "perm." + c + ".addTabs";
		if (p.containsKey(permKey)) {
			return parseCsvTabs(p.getProperty(permKey));
		}
		String roleKey = "role.Staff.addTabs";
		if (p.containsKey(roleKey)) {
			return parseCsvTabs(p.getProperty(roleKey));
		}
		return loadEditTabsForPermissionCode(c);
	}

	public static Set<String> loadDeleteTabsForPermissionCode(String code) {
		String c = code == null ? "" : code.trim();
		if (c.isEmpty()) c = "PQ2";
		if ("PQ0".equalsIgnoreCase(c)) {
			return parseCsvTabs(defaultTabsForPermissionCode("PQ0"));
		}
		if (!"PQ2".equalsIgnoreCase(c)) {
			c = "PQ2";
		}

		Properties p = loadProperties();
		String permKey = "perm." + c + ".deleteTabs";
		if (p.containsKey(permKey)) {
			return parseCsvTabs(p.getProperty(permKey));
		}
		String roleKey = "role.Staff.deleteTabs";
		if (p.containsKey(roleKey)) {
			return parseCsvTabs(p.getProperty(roleKey));
		}
		return loadMutateTabsForPermissionCode(c);
	}

    public static Set<String> loadTabsForPermissionCode(String code) {
        String c = code == null ? "" : code.trim();
        if (c.isEmpty()) c = "PQ2";
        if ("PQ0".equalsIgnoreCase(c)) {
            return parseCsvTabs(defaultTabsForPermissionCode("PQ0"));
        }

        // Only PQ0 and PQ2 are supported.
        if (!"PQ2".equalsIgnoreCase(c)) {
            c = "PQ2";
        }

        Properties p = loadProperties();
        String v = p.getProperty("perm." + c + ".tabs");
        if (v == null || v.trim().isEmpty()) {
            // fallback to legacy role keys
            v = p.getProperty("role.Staff.tabs");
        }
        if (v == null || v.trim().isEmpty()) {
            v = defaultTabsForPermissionCode(c);
        }
        return parseCsvTabs(v);
    }

    public static String defaultTabsForRole(String role) {
        if (role != null && role.equalsIgnoreCase("Manager")) {
            return "Bán hàng,Nhập hàng,Món ăn,Nguyên liệu,Công thức,Hóa đơn,Hóa đơn nhập,Khuyến mãi,Khách hàng,Nhân viên,Nhà cung cấp,Tài khoản,Phân quyền,Thống kê";
        }
        return "Bán hàng,Nhập hàng,Hóa đơn,Khách hàng,Khuyến mãi";
    }

    public static String defaultTabsForPermissionCode(String code) {
        String c = code == null ? "" : code.trim();
        if (c.equalsIgnoreCase("PQ0")) {
            return defaultTabsForRole("Manager");
        }
        if (c.equalsIgnoreCase("PQ2")) {
            return "Bán hàng,Nhập hàng,Hóa đơn,Khách hàng,Khuyến mãi";
        }
        return "Bán hàng,Nhập hàng,Hóa đơn,Khách hàng,Khuyến mãi";
    }

    public static String permissionCodeForUser(User user) {
        if (user != null) {
            String un = user.getUsername();
            if (un != null && un.trim().equalsIgnoreCase(ADMIN_USERNAME)) {
                return "PQ0";
            }
			String byDb = user.getPermissionCode();
			if (byDb != null && !byDb.trim().isEmpty()) {
				String c = byDb.trim().toUpperCase();
				return c.equals("PQ0") ? "PQ0" : "PQ2";
			}
        }
        String role = canonicalRole(user == null ? null : user.getRole());
        return roleToPermissionCode(role);
    }

	public static String getPermissionCodeForAccount(String username, String roleOrPosition) {
		if (username != null && username.trim().equalsIgnoreCase(ADMIN_USERNAME)) {
			return "PQ0";
		}
		String canonical = canonicalRole(roleOrPosition);
		return roleToPermissionCode(canonical);
	}

	public static String getPermissionNameVi(String code) {
		if (code == null) return "";
		String c = code.trim().toUpperCase();
		if (c.equals("PQ0")) return "Admin";
		if (c.equals("PQ2")) return "Nhân viên";
		return c;
	}

	public static boolean saveUserTabs(String username, String csvTabs) {
		try {
			String un = username == null ? "" : username.trim();
			if (un.isEmpty()) return false;
			if (un.equalsIgnoreCase(ADMIN_USERNAME)) return false;
			String csv = csvTabs == null ? "" : csvTabs.trim();
			if (csv.isEmpty()) return false;

			File f = new File(CONFIG_FILE);
			Path path = f.toPath();
			List<String> lines = new ArrayList<>();
			if (f.exists() && f.isFile()) {
				lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			}

			String key = "user." + un + ".tabs=";
			boolean updated = false;
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				if (line != null && line.startsWith(key)) {
					lines.set(i, key + csv);
					updated = true;
					break;
				}
			}
			if (!updated) lines.add(key + csv);
			Files.write(path, lines, StandardCharsets.UTF_8);
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	public static boolean saveUserEditTabs(String username, String csvTabs) {
		return saveUserCsvKey(username, "editTabs", csvTabs);
	}

	public static boolean saveUserAddTabs(String username, String csvTabs) {
		return saveUserCsvKey(username, "addTabs", csvTabs);
	}

	public static boolean saveUserDeleteTabs(String username, String csvTabs) {
		return saveUserCsvKey(username, "deleteTabs", csvTabs);
	}

	private static boolean saveUserCsvKey(String username, String suffix, String csvTabs) {
		try {
			String un = username == null ? "" : username.trim();
			if (un.isEmpty()) return false;
			if (un.equalsIgnoreCase(ADMIN_USERNAME)) return false;
			String suf = suffix == null ? "" : suffix.trim();
			if (suf.isEmpty()) return false;
			String csv = csvTabs == null ? "" : csvTabs.trim();

			File f = new File(CONFIG_FILE);
			Path path = f.toPath();
			List<String> lines = new ArrayList<>();
			if (f.exists() && f.isFile()) {
				lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			}

			String key = "user." + un + "." + suf + "=";
			boolean updated = false;
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				if (line != null && line.startsWith(key)) {
					lines.set(i, key + csv);
					updated = true;
					break;
				}
			}
			if (!updated) lines.add(key + csv);
			Files.write(path, lines, StandardCharsets.UTF_8);
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}


    public static String roleToPermissionCode(String canonicalRole) {
        String r = canonicalRole == null ? "" : canonicalRole.trim();
        if (r.equalsIgnoreCase("Manager")) return "PQ0";
        if (r.equalsIgnoreCase("Staff")) return "PQ2";
        return "PQ2";
    }

    private static String canonicalRole(String role) {
        if (role == null || role.trim().isEmpty()) return "Staff";
        String r = role.trim();
        if (r.equalsIgnoreCase("Manager") || r.equalsIgnoreCase("manager") || r.equalsIgnoreCase("admin") || r.equalsIgnoreCase("administrator")) {
            return "Manager";
        }
        // Everything else collapses to Staff (legacy cashier/chef/waiter/...)
        return "Staff";
    }


    private static Properties loadProperties() {
        Properties p = new Properties();
        File f = new File(CONFIG_FILE);
        if (f.exists() && f.isFile()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                p.load(reader);
            } catch (IOException ignored) {
            }
        }
        return p;
    }

    private static Set<String> parseCsvTabs(String csv) {
        Set<String> set = new HashSet<>();
        if (csv == null) return set;
        Arrays.stream(csv.split(","))
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isEmpty())
                .forEach(set::add);
        return set;
    }
}
