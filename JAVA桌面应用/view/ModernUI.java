package view;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class ModernUI {
    // 2025年流行色盘 (莫兰迪/科技风)
    public static final Color PRIMARY_COLOR = new Color(59, 130, 246);    // 科技蓝
    public static final Color SUCCESS_COLOR = new Color(16, 185, 129);    // 翡翠绿
    public static final Color DANGER_COLOR = new Color(239, 68, 68);      // 警示红
    public static final Color TEXT_COLOR = new Color(31, 41, 55);         // 深灰黑
    public static final Color BG_COLOR = new Color(243, 244, 246);        // 极淡灰背景
    public static final Color PANEL_BG = Color.WHITE;                     // 纯白卡片
    public static final Color LINE_GRAY = new Color(229, 231, 235);       // 分割线灰

    public static final Font TITLE_FONT = new Font("微软雅黑", Font.BOLD, 20);
    public static final Font SUBTITLE_FONT = new Font("微软雅黑", Font.BOLD, 16);
    public static final Font NORMAL_FONT = new Font("微软雅黑", Font.PLAIN, 14);
    public static final Font SMALL_FONT = new Font("微软雅黑", Font.PLAIN, 12);

    // 设置全局UI外观
    public static void setupGlobalUI() {
        try {
            // 尝试启用抗锯齿
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // 全局字体
            UIManager.put("Label.font", NORMAL_FONT);
            UIManager.put("Button.font", NORMAL_FONT);
            UIManager.put("TextField.font", NORMAL_FONT);
            UIManager.put("TextArea.font", new Font("Monospaced", Font.PLAIN, 13));
        } catch (Exception ignored) {}
    }

    // 创建带圆角和悬停效果的按钮
    public static JButton createButton(String text, Color baseColor, Color textColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2.setColor(baseColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(baseColor.brighter());
                } else {
                    g2.setColor(baseColor);
                }
                
                // 圆角矩形 (20px圆角)
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                
                super.paintComponent(g);
            }
        };
        
        btn.setFont(new Font("微软雅黑", Font.BOLD, 14));
        btn.setForeground(textColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        return btn;
    }
    
    public static JButton createPrimaryButton(String text) { return createButton(text, PRIMARY_COLOR, Color.WHITE); }
    public static JButton createSuccessButton(String text) { return createButton(text, SUCCESS_COLOR, Color.WHITE); }
    public static JButton createDangerButton(String text) { return createButton(text, DANGER_COLOR, Color.WHITE); }
    
    // 幽灵按钮（只有边框，用于次要操作）
    public static JButton createButton(String text, Color textColor) {
        JButton btn = createButton(text, new Color(0,0,0,0), textColor);
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(textColor, 1, true),
            new EmptyBorder(7, 19, 7, 19)
        ));
        return btn;
    }

    // 创建现代化输入框 (底部线条风格 或 柔和边框风格)
    public static JTextField createTextField(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(NORMAL_FONT);
        tf.setForeground(TEXT_COLOR);
        tf.setBackground(Color.WHITE);
        tf.setCaretColor(PRIMARY_COLOR);
        
        // 增加内边距
        Border line = new LineBorder(LINE_GRAY, 1, true);
        Border empty = new EmptyBorder(8, 12, 8, 12);
        tf.setBorder(new CompoundBorder(line, empty));
        return tf;
    }

    public static JTextField createTextField() { return createTextField(""); }

    // 装饰面板背景
    public static void decoratePanel(JPanel panel) {
        panel.setBackground(BG_COLOR);
    }
    
    // 创建带阴影感的卡片面板
    public static JPanel createCardPanel() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            }
        };
        p.setBackground(PANEL_BG);
        p.setOpaque(false); // 必须透明以便绘制圆角
        p.setBorder(new EmptyBorder(20, 20, 20, 20));
        return p;
    }

    // 美化滚动条 (非常重要，默认的滚动条太丑了)
    public static JScrollPane createModernScrollPane(JComponent content) {
        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null);
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(200, 200, 200);
                this.trackColor = Color.WHITE;
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
        });
        return sp;
    }
    
    private static JButton createZeroButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        return b;
    }
}