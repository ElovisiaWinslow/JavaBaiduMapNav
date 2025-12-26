package view;

import controller.PublicTransportSystem;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

public class LoginPanel extends JPanel {
    private PublicTransportSystem controller;

    public LoginPanel(PublicTransportSystem controller) {
        this.controller = controller;
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        // 1. 使用自定义的绘图面板作为背景
        RichBackgroundPanel bgPanel = new RichBackgroundPanel();
        bgPanel.setLayout(new GridBagLayout()); // 使用 GridBagLayout 居中内容

        // 2. 创建悬浮卡片容器 (登录框)
        ShadowPanel loginCard = new ShadowPanel();
        loginCard.setLayout(new BoxLayout(loginCard, BoxLayout.Y_AXIS));
        loginCard.setBorder(new EmptyBorder(40, 50, 40, 50));
        loginCard.setPreferredSize(new Dimension(460, 520));

        // --- 卡片内部内容 ---

        // 标题部分
        JLabel logoIcon = new JLabel("🚇"); // 使用Emoji作为Logo
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        logoIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("南京公共交通导航");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 28));
        titleLabel.setForeground(new Color(44, 62, 80));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Nanjing Metro & Bus System");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(149, 165, 166));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 按钮容器
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new GridLayout(4, 1, 10, 15));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(new EmptyBorder(30, 0, 10, 0));
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(360, 300));

        // 创建带图标的按钮
        JButton btnUser = createStyledButton("👤  我是普通用户", ModernUI.PRIMARY_COLOR);
        btnUser.addActionListener(e -> controller.showUserPanel());

        JButton btnAdmin = createStyledButton("🛡️  我是管理员", new Color(52, 73, 94));
        btnAdmin.addActionListener(e -> controller.showAdminLogin());

        // 新增的 Web 地图按钮 (醒目设计)
        JButton btnWebMap = createStyledButton("🌏  打开3D网页地图", new Color(39, 174, 96));
        btnWebMap.setToolTipText("启动本地服务并在浏览器中打开 HTML 地图");
        btnWebMap.addActionListener(e -> MiniServer.startAndOpen());

        JButton btnExit = createStyledButton("🚪  退出系统", new Color(231, 76, 60));
        btnExit.addActionListener(e -> System.exit(0));

        btnPanel.add(btnUser);
        btnPanel.add(btnAdmin);
        btnPanel.add(btnWebMap);
        btnPanel.add(btnExit);

        // 组装卡片
        loginCard.add(logoIcon);
        loginCard.add(Box.createVerticalStrut(10));
        loginCard.add(titleLabel);
        loginCard.add(subtitleLabel);
        loginCard.add(btnPanel);

        // 3. 将卡片添加到背景
        bgPanel.add(loginCard);

        // 4. 版权信息 (放到底部)
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        JLabel copyLabel = new JLabel("© 2025 Nanjing University of Posts and Telecommunications | Computer Science");
        copyLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        copyLabel.setForeground(new Color(255, 255, 255, 150)); // 半透明白色
        footer.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        // 使用 BorderLayout 将 footer 放到底部
        bgPanel.setLayout(new BorderLayout());
        
        // 为了让卡片居中，我们需要再套一层 GridBag 的 Panel
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(loginCard);
        
        bgPanel.add(centerWrapper, BorderLayout.CENTER);
        bgPanel.add(footer, BorderLayout.SOUTH);

        add(bgPanel, BorderLayout.CENTER);
    }

    // 辅助方法：创建统一样式的按钮（修改版：正确显示Emoji图标）
    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text) {
            private boolean isHovered = false;
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // 绘制背景（带悬停效果）
                Color backgroundColor;
                if (getModel().isPressed()) {
                    backgroundColor = bgColor.darker().darker();
                } else if (getModel().isRollover()) {
                    backgroundColor = bgColor.brighter();
                    isHovered = true;
                } else {
                    backgroundColor = bgColor;
                    isHovered = false;
                }
                
                g2.setColor(backgroundColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                // 添加高光效果
                if (isHovered) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight()/2, 10, 10);
                }
                
                // 绘制边框
                g2.setColor(new Color(255, 255, 255, 50));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                
                // 分离图标和文字（以第一个空格为分隔符）
                String buttonText = getText();
                int spaceIndex = buttonText.indexOf(' ');
                String iconPart = "";
                String textPart = buttonText;
                
                if (spaceIndex > 0) {
                    iconPart = buttonText.substring(0, spaceIndex);
                    textPart = buttonText.substring(spaceIndex).trim();
                }
                
                // 准备绘制文本
                g2.setColor(Color.WHITE);
                
                // 计算总宽度
                Font emojiFont = new Font("Segoe UI Emoji", Font.BOLD, 16);
                Font textFont = new Font("微软雅黑", Font.BOLD, 15);
                
                FontMetrics emojiMetrics = g2.getFontMetrics(emojiFont);
                FontMetrics textMetrics = g2.getFontMetrics(textFont);
                
                int iconWidth = emojiMetrics.stringWidth(iconPart);
                int textWidth = textMetrics.stringWidth(textPart);
                int totalWidth = iconWidth + textWidth + 10; // 10像素间距
                
                // 计算绘制位置（居中）
                int startX = (getWidth() - totalWidth) / 2;
                int startY = getHeight()/2 + emojiMetrics.getHeight()/4;
                
                // 绘制图标（使用Emoji字体）
                if (!iconPart.isEmpty()) {
                    g2.setFont(emojiFont);
                    g2.drawString(iconPart, startX, startY);
                }
                
                // 绘制文字（使用中文字体）
                g2.setFont(textFont);
                g2.drawString(textPart, startX + iconWidth + 10, startY);
                
                g2.dispose();
            }
            
            @Override
            public void updateUI() {
                super.updateUI();
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setOpaque(false);
            }
        };
        
        // 设置默认字体（虽然我们自定义绘制，但设置字体可以保证按钮有合适的大小）
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 设置按钮大小
        Dimension btnSize = new Dimension(360, 50);
        btn.setPreferredSize(btnSize);
        btn.setMaximumSize(btnSize);
        
        return btn;
    }

    // 内部类：富背景面板 (绘制装饰性线条和渐变)
    class RichBackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. 深邃的渐变背景 (模拟夜空/科技蓝)
            GradientPaint gp = new GradientPaint(0, 0, new Color(41, 128, 185), w, h, new Color(142, 68, 173));
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            // 2. 绘制装饰性曲线 (模拟交通线路图)
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255, 255, 255, 30)); // 半透明白

            GeneralPath path1 = new GeneralPath();
            path1.moveTo(0, h * 0.7);
            path1.curveTo(w * 0.3, h * 0.5, w * 0.6, h * 0.9, w, h * 0.6);
            g2.draw(path1);

            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(255, 255, 255, 20));
            GeneralPath path2 = new GeneralPath();
            path2.moveTo(0, h * 0.3);
            path2.curveTo(w * 0.4, h * 0.1, w * 0.5, h * 0.8, w, h * 0.2);
            g2.draw(path2);

            // 绘制一些装饰性圆点 (模拟站点)
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillOval((int)(w*0.2), (int)(h*0.2), 10, 10);
            g2.fillOval((int)(w*0.8), (int)(h*0.8), 15, 15);
            g2.fillOval((int)(w*0.5), (int)(h*0.1), 8, 8);
        }
    }

    // 内部类：带阴影的卡片面板
    class ShadowPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int shadowSize = 10;
            int width = getWidth() - shadowSize * 2;
            int height = getHeight() - shadowSize * 2;
            int x = shadowSize;
            int y = shadowSize;

            // 1. 绘制阴影
            g2.setColor(new Color(0, 0, 0, 50));
            g2.fillRoundRect(x + 5, y + 5, width, height, 20, 20);

            // 2. 绘制卡片背景 (纯白)
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(x, y, width, height, 20, 20);

            g2.dispose();
            // 注意：这里不调用 super.paintComponent，因为我们完全自定义了绘制
            // 但需要确保子组件能画出来，Swing会自动处理容器内的子组件
        }
    }
    
    // 现代UI颜色定义（如果ModernUI类不存在，使用这个）
    static class ModernUI {
        public static final Color PRIMARY_COLOR = new Color(41, 128, 185);
    }
}