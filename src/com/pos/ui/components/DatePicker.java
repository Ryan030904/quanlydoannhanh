package com.pos.ui.components;

import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Date Picker component
 */
public class DatePicker extends JPanel {
    private JTextField dateField;
    private JButton calendarButton;
    private JPopupMenu calendarPopup;
    private LocalDate selectedDate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final List<ActionListener> listeners = new ArrayList<>();
    
    public DatePicker() {
        this(null);
    }
    
    public DatePicker(LocalDate initialDate) {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        
        this.selectedDate = initialDate;
        
        // Date text field
        dateField = new JTextField();
        dateField.setFont(UIConstants.FONT_CAPTION);
        dateField.setEditable(false);
        dateField.setBackground(Color.WHITE);
        dateField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.NEUTRAL_300),
            new EmptyBorder(2, 6, 2, 6)
        ));
        dateField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        if (selectedDate != null) {
            dateField.setText(selectedDate.format(formatter));
        }
        
        // Calendar button
        calendarButton = new JButton("▼");
        calendarButton.setFont(UIConstants.FONT_CAPTION);
        calendarButton.setPreferredSize(new Dimension(24, 24));
        calendarButton.setFocusPainted(false);
        calendarButton.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_300));
        calendarButton.setBackground(UIConstants.NEUTRAL_100);
        calendarButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        add(dateField, BorderLayout.CENTER);
        add(calendarButton, BorderLayout.EAST);
        
        // Create popup
        calendarPopup = new JPopupMenu();
        calendarPopup.setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_300));
        
        // Click handlers
        MouseAdapter clickHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showCalendar();
            }
        };
        
        dateField.addMouseListener(clickHandler);
        calendarButton.addActionListener(e -> showCalendar());
    }
    
    private void showCalendar() {
        if (!isEnabled()) return;
        calendarPopup.removeAll();
        
        LocalDate displayMonth = selectedDate != null ? selectedDate : LocalDate.now();
        JPanel calendarPanel = createCalendarPanel(displayMonth);
        calendarPopup.add(calendarPanel);
        
        calendarPopup.show(this, 0, getHeight());
    }
    
    private JPanel createCalendarPanel(LocalDate monthDate) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        YearMonth yearMonth = YearMonth.from(monthDate);
        
        // Header with month/year navigation
        JPanel header = new JPanel(new BorderLayout(5, 0));
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(250, 30));
        
        JButton prevMonth = new JButton();
        prevMonth.setText("<");
        prevMonth.setFont(new Font("Arial", Font.BOLD, 14));
        prevMonth.setPreferredSize(new Dimension(45, 28));
        prevMonth.setMinimumSize(new Dimension(45, 28));
        prevMonth.setFocusPainted(false);
        prevMonth.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        prevMonth.setMargin(new Insets(0, 0, 0, 0));
        
        JButton nextMonth = new JButton();
        nextMonth.setText(">");
        nextMonth.setFont(new Font("Arial", Font.BOLD, 14));
        nextMonth.setPreferredSize(new Dimension(45, 28));
        nextMonth.setMinimumSize(new Dimension(45, 28));
        nextMonth.setFocusPainted(false);
        nextMonth.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nextMonth.setMargin(new Insets(0, 0, 0, 0));
        
        JLabel monthLabel = new JLabel(
            String.format("Tháng %d / %d", yearMonth.getMonthValue(), yearMonth.getYear()),
            SwingConstants.CENTER
        );
        monthLabel.setFont(UIConstants.FONT_BODY_BOLD);
        monthLabel.setForeground(UIConstants.PRIMARY_700);
        
        header.add(prevMonth, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextMonth, BorderLayout.EAST);
        
        prevMonth.addActionListener(e -> {
            calendarPopup.removeAll();
            calendarPopup.add(createCalendarPanel(monthDate.minusMonths(1)));
            calendarPopup.revalidate();
            calendarPopup.repaint();
        });
        
        nextMonth.addActionListener(e -> {
            calendarPopup.removeAll();
            calendarPopup.add(createCalendarPanel(monthDate.plusMonths(1)));
            calendarPopup.revalidate();
            calendarPopup.repaint();
        });
        
        panel.add(header, BorderLayout.NORTH);
        
        // Days grid - fixed size for 7 columns
        JPanel daysPanel = new JPanel(new GridLayout(0, 7, 2, 2));
        daysPanel.setOpaque(false);
        daysPanel.setPreferredSize(new Dimension(245, 170));
        
        // Day headers
        String[] dayNames = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        for (String day : dayNames) {
            JLabel lbl = new JLabel(day, SwingConstants.CENTER);
            lbl.setFont(UIConstants.FONT_CAPTION);
            lbl.setForeground(UIConstants.NEUTRAL_500);
            lbl.setPreferredSize(new Dimension(32, 22));
            daysPanel.add(lbl);
        }
        
        // Calculate first day of month
        LocalDate firstOfMonth = yearMonth.atDay(1);
        int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7; // Sunday = 0
        int daysInMonth = yearMonth.lengthOfMonth();
        
        LocalDate today = LocalDate.now();
        
        // Empty cells before first day
        for (int i = 0; i < startDayOfWeek; i++) {
            JLabel empty = new JLabel("");
            empty.setPreferredSize(new Dimension(32, 26));
            daysPanel.add(empty);
        }
        
        // Day buttons
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            JButton dayBtn = new JButton(String.valueOf(day));
            dayBtn.setFont(UIConstants.FONT_CAPTION);
            dayBtn.setMargin(new Insets(2, 2, 2, 2));
            dayBtn.setPreferredSize(new Dimension(32, 26));
            dayBtn.setFocusPainted(false);
            dayBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            dayBtn.setBorderPainted(false);
            
            // Highlight today
            if (date.equals(today)) {
                dayBtn.setForeground(UIConstants.PRIMARY_700);
                dayBtn.setFont(UIConstants.FONT_CAPTION.deriveFont(Font.BOLD));
            }
            
            // Highlight selected
            if (date.equals(selectedDate)) {
                dayBtn.setBackground(UIConstants.PRIMARY_500);
                dayBtn.setForeground(Color.WHITE);
                dayBtn.setOpaque(true);
            } else {
                dayBtn.setContentAreaFilled(false);
            }
            
            final LocalDate selectedDay = date;
            dayBtn.addActionListener(e -> {
                setDate(selectedDay);
                calendarPopup.setVisible(false);
                fireActionPerformed();
            });
            
            // Hover effect
            dayBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!selectedDay.equals(selectedDate)) {
                        dayBtn.setBackground(UIConstants.PRIMARY_50);
                        dayBtn.setOpaque(true);
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!selectedDay.equals(selectedDate)) {
                        dayBtn.setOpaque(false);
                    }
                }
            });
            
            daysPanel.add(dayBtn);
        }
        
        // Fill remaining cells
        int totalCells = startDayOfWeek + daysInMonth;
        int remaining = (7 - (totalCells % 7)) % 7;
        for (int i = 0; i < remaining; i++) {
            JLabel empty = new JLabel("");
            empty.setPreferredSize(new Dimension(32, 26));
            daysPanel.add(empty);
        }
        
        panel.add(daysPanel, BorderLayout.CENTER);
        
        // Quick buttons
        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        quickPanel.setOpaque(false);
        
        JButton todayBtn = new JButton("Hôm nay");
        todayBtn.setFont(UIConstants.FONT_CAPTION);
        todayBtn.setFocusPainted(false);
        todayBtn.addActionListener(e -> {
            setDate(LocalDate.now());
            calendarPopup.setVisible(false);
            fireActionPerformed();
        });
        
        JButton clearBtn = new JButton("Xóa");
        clearBtn.setFont(UIConstants.FONT_CAPTION);
        clearBtn.setFocusPainted(false);
        clearBtn.addActionListener(e -> {
            setDate(null);
            calendarPopup.setVisible(false);
            fireActionPerformed();
        });
        
        quickPanel.add(todayBtn);
        quickPanel.add(clearBtn);
        
        panel.add(quickPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    public void setDate(LocalDate date) {
		LocalDate old = this.selectedDate;
		this.selectedDate = date;
		if (date != null) {
			dateField.setText(date.format(formatter));
		} else {
			dateField.setText("");
		}

		boolean changed;
		if (old == null) changed = this.selectedDate != null;
		else changed = !old.equals(this.selectedDate);
		if (changed) {
			firePropertyChange("date", old, this.selectedDate);
		}
    }
    
    public LocalDate getDate() {
        return selectedDate;
    }
    
    public String getText() {
        return dateField.getText();
    }

    public void setTextFont(Font font) {
        if (dateField != null && font != null) {
            dateField.setFont(font);
        }
    }

    public void setCalendarButtonVisible(boolean visible) {
        if (calendarButton != null) {
            calendarButton.setVisible(visible);
        }
        revalidate();
        repaint();
    }
    
    public void setText(String text) {
        if (text == null || text.trim().isEmpty()) {
            setDate(null);
        } else {
            try {
                setDate(LocalDate.parse(text.trim(), formatter));
            } catch (Exception e) {
                setDate(null);
            }
        }
    }
    
    @Override
    public void setPreferredSize(Dimension preferredSize) {
        super.setPreferredSize(preferredSize);
        if (dateField != null) {
            int btnW = (calendarButton != null && calendarButton.isVisible()) ? 24 : 0;
            dateField.setPreferredSize(new Dimension(preferredSize.width - btnW, preferredSize.height));
        }
    }
    
    public void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (dateField != null) {
            dateField.setEnabled(enabled);
            dateField.setDisabledTextColor(Color.BLACK);
            if (!enabled) {
                dateField.setBackground(Color.WHITE);
                dateField.setFocusable(false);
                dateField.setCursor(Cursor.getDefaultCursor());
            } else {
                dateField.setFocusable(true);
                dateField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }
        if (calendarButton != null) {
            calendarButton.setEnabled(enabled);
        }
    }
    
    public void removeActionListener(ActionListener listener) {
        listeners.remove(listener);
    }
    
    private void fireActionPerformed() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "dateChanged");
        for (ActionListener listener : listeners) {
            listener.actionPerformed(event);
        }
    }
}
