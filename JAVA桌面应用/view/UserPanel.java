package view;

import controller.PublicTransportSystem;
import model.TransportGraph;
import service.NavigationService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class UserPanel extends JPanel {
    private PublicTransportSystem controller;
    private TransportGraph graph;
    private NavigationService service;
    private VisualMapPanel mapPanel;
    private List<String> allStationNames;
    
    public UserPanel(PublicTransportSystem controller, TransportGraph graph, NavigationService service) {
        this.controller = controller;
        this.graph = graph;
        this.service = service;
        this.mapPanel = new VisualMapPanel(graph);
        this.allStationNames = graph.getAllStations();
        ModernUI.decoratePanel(this);
        initUI();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());

        // 左侧 Tabs
        JTabbedPane leftTabbedPane = new JTabbedPane();
        leftTabbedPane.setFont(ModernUI.NORMAL_FONT);
        leftTabbedPane.setBackground(Color.WHITE);
        leftTabbedPane.addTab("智能导航", wrapInCard(createNavPanel()));
        leftTabbedPane.addTab("线路详情", wrapInCard(createLineQueryPanel()));
        leftTabbedPane.addTab("直连查询", wrapInCard(createDirectQueryPanel()));
        
        // 分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftTabbedPane, mapPanel);
        splitPane.setDividerLocation(380);
        splitPane.setBorder(null);

        // 顶部导航栏
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topBar.setBackground(Color.WHITE);
        
        JButton btnBack = ModernUI.createButton("← 退出", ModernUI.TEXT_COLOR);
        btnBack.addActionListener(e -> { mapPanel.clearHighlight(); controller.showLoginPanel(); });
        
        JButton btnReset = ModernUI.createButton("重置视角", ModernUI.PRIMARY_COLOR);
        btnReset.addActionListener(e -> mapPanel.resetView());

        // 视图切换按钮组
        JPanel viewToggle = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JToggleButton tAll = createToggleBtn("全部", true);
        JToggleButton tMetro = createToggleBtn("地铁", false);
        JToggleButton tBus = createToggleBtn("公交", false);
        
        ButtonGroup bg = new ButtonGroup();
        bg.add(tAll); bg.add(tMetro); bg.add(tBus);
        viewToggle.add(tAll); viewToggle.add(tMetro); viewToggle.add(tBus);
        
        tAll.addActionListener(e -> mapPanel.setViewMode(VisualMapPanel.VIEW_ALL));
        tMetro.addActionListener(e -> mapPanel.setViewMode(VisualMapPanel.VIEW_METRO));
        tBus.addActionListener(e -> mapPanel.setViewMode(VisualMapPanel.VIEW_BUS));
        
        topBar.add(btnBack);
        topBar.add(Box.createHorizontalStrut(20));
        topBar.add(new JLabel("视图:"));
        topBar.add(viewToggle);
        topBar.add(Box.createHorizontalStrut(20));
        topBar.add(btnReset);

        add(topBar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private JToggleButton createToggleBtn(String text, boolean selected) {
        JToggleButton btn = new JToggleButton(text, selected);
        btn.setFont(ModernUI.NORMAL_FONT);
        btn.setFocusPainted(false);
        btn.setBackground(Color.WHITE);
        btn.setForeground(ModernUI.TEXT_COLOR);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (b.isSelected()) {
                    g2.setColor(ModernUI.PRIMARY_COLOR);
                    g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10);
                    b.setForeground(Color.WHITE);
                } else {
                    g2.setColor(new Color(240, 240, 240));
                    g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10);
                    b.setForeground(ModernUI.TEXT_COLOR);
                }
                super.paint(g, c);
            }
        });
        btn.setPreferredSize(new Dimension(60, 32));
        return btn;
    }
    
    private JPanel wrapInCard(JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ModernUI.BG_COLOR);
        wrapper.setBorder(new EmptyBorder(15, 15, 15, 15));
        wrapper.add(content);
        return wrapper;
    }

    // ========== 1. 智能导航 (修改版：支持单策略选择) ==========
    private JPanel createNavPanel() {
        JPanel panel = ModernUI.createCardPanel();
        panel.setLayout(new BorderLayout(0, 10));

        JPanel form = new JPanel(new GridLayout(4, 1, 0, 10)); // 增加了一行放下拉框
        form.setOpaque(false);
        
        JTextField sField = ModernUI.createTextField("南京站");
        JTextField eField = ModernUI.createTextField("黄里");
        new StationAutoCompleter(sField, allStationNames);
        new StationAutoCompleter(eField, allStationNames);
        
        // ★★★ 新增：策略选择下拉框
        String[] strategies = {"时间最短", "换乘最少", "只坐公交", "只坐地铁"};
        JComboBox<String> strategyBox = new JComboBox<>(strategies);
        strategyBox.setFont(ModernUI.NORMAL_FONT);
        strategyBox.setBackground(Color.WHITE);
        
        form.add(createFieldGroup("起点", sField));
        form.add(createFieldGroup("终点", eField));
        form.add(createFieldGroup("偏好", strategyBox));
        
        JPanel btnP = new JPanel(new GridLayout(1, 2, 10, 0)); btnP.setOpaque(false);
        JButton btnSearch = ModernUI.createPrimaryButton("开始导航");
        JButton btnClear = ModernUI.createDangerButton("清空");
        btnP.add(btnSearch); btnP.add(btnClear);
        form.add(btnP);

        JTextArea area = new JTextArea();
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        
        // ★★★ 修改：查询逻辑
        btnSearch.addActionListener(e -> {
            String s = sField.getText().trim(), t = eField.getText().trim();
            if (!graph.adjList.containsKey(s) || !graph.adjList.containsKey(t)) {
                JOptionPane.showMessageDialog(this, "站点不存在，请检查输入", "错误", JOptionPane.ERROR_MESSAGE); return;
            }
            
            // 清除旧的高亮
            mapPanel.clearHighlight();
            
            // 获取用户选择的策略索引 (0, 1, 2, 3)
            int selectedStrategy = strategyBox.getSelectedIndex();
            
            // 执行搜索
            model.RouteResult result = service.search(s, t, selectedStrategy);
            
            if (result != null) {
                // 显示路书
                StringBuilder sb = new StringBuilder();
                sb.append("★ 导航方案 [").append(result.strategyName).append("]\n");
                sb.append("============================\n");
                sb.append(formatVerticalPath(result));
                area.setText(sb.toString());
                area.setCaretPosition(0);
                
                // 地图高亮
                mapPanel.highlightPath(result.stations);
            } else {
                String errorMsg = "未找到路径";
                if (selectedStrategy == 2) errorMsg += " (两点间可能无纯公交连接)";
                if (selectedStrategy == 3) errorMsg += " (两点间可能无纯地铁连接)";
                area.setText(errorMsg);
            }
        });
        
        btnClear.addActionListener(e -> { 
            sField.setText(""); eField.setText(""); area.setText(""); mapPanel.clearHighlight(); 
        });
        
        panel.add(form, BorderLayout.NORTH); 
        panel.add(ModernUI.createModernScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private String formatVerticalPath(model.RouteResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("总计: %d分钟 | 换乘: %d次\n\n", r.totalTime, r.transferCount));
        
        for (int i = 0; i < r.stations.size(); i++) {
            String station = r.stations.get(i);
            
            if (i == 0) sb.append("●  [起] ").append(station).append("\n");
            else if (i == r.stations.size() - 1) sb.append("●  [终] ").append(station).append("\n");
            else if (r.transferPoints.contains(station)) sb.append("◎  [换] ").append(station).append("\n");
            else sb.append("○  ").append(station).append("\n");
            
            if (i < r.stations.size() - 1) {
                String lineName = r.lineSegments.get(i);
                String nextStation = r.stations.get(i + 1);
                int time = graph.getConnectionTime(station, nextStation, lineName);
                
                sb.append("│\n");
                sb.append(String.format("│  %s (%d分)\n", lineName, time));
                sb.append("↓\n");
            }
        }
        return sb.toString();
    }

    // ========== 2. 线路详情 ==========
    private JPanel createLineQueryPanel() {
        JPanel panel = ModernUI.createCardPanel();
        panel.setLayout(new BorderLayout(0, 10));
        JTextField input = ModernUI.createTextField("1号线");
        new StationAutoCompleter(input, graph.getAllLines());
        JButton btn = ModernUI.createPrimaryButton("查询详情");
        JPanel top = new JPanel(new BorderLayout(0, 5)); top.setOpaque(false);
        top.add(createFieldGroup("线路名称", input), BorderLayout.CENTER); top.add(btn, BorderLayout.SOUTH);
        
        JTextArea area = new JTextArea(); 
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        btn.addActionListener(e -> {
           String line = input.getText().trim();
           if(graph.lineStationsMap.containsKey(line)) {
               mapPanel.clearHighlight();
               model.LineInfo m = graph.lineMetaMap.get(line);
               List<String> sts = graph.lineStationsMap.get(line);
               
               StringBuilder sb = new StringBuilder();
               sb.append("=== ").append(line).append(" ===\n");
               sb.append("运营: ").append(m.firstTime).append(" - ").append(m.lastTime).append("\n");
               sb.append("站点: ").append(sts.size()).append("个\n\n");
               
               for (int i = 0; i < sts.size(); i++) {
                   String curr = sts.get(i);
                   sb.append(String.format("%02d. %s\n", i+1, curr));
                   
                   if (i < sts.size() - 1) {
                       String next = sts.get(i+1);
                       int t = graph.getConnectionTime(curr, next, line);
                       sb.append(String.format("     ↓  (%d分钟)\n", t));
                   }
               }
               
               area.setText(sb.toString());
               area.setCaretPosition(0);
               mapPanel.highlightPath(sts); // 线路查询也高亮整条线
           } else area.setText("未找到线路");
        });
        panel.add(top, BorderLayout.NORTH); panel.add(ModernUI.createModernScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    // ========== 3. 直连查询 ==========
    private JPanel createDirectQueryPanel() {
        JPanel panel = ModernUI.createCardPanel();
        panel.setLayout(new BorderLayout(0, 20));
        JPanel form = new JPanel(new GridLayout(3, 1, 0, 10)); form.setOpaque(false);
        JTextField s1 = ModernUI.createTextField("新街口");
        JTextField s2 = ModernUI.createTextField("大行宫");
        new StationAutoCompleter(s1, allStationNames); new StationAutoCompleter(s2, allStationNames);
        form.add(createFieldGroup("站点A", s1)); form.add(createFieldGroup("站点B", s2));
        JButton btn = ModernUI.createSuccessButton("查询直达");
        form.add(btn);
        
        JTextArea area = new JTextArea(); area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        btn.addActionListener(e -> {
            List<String> lines = service.getDirectLines(s1.getText(), s2.getText());
            if(lines.isEmpty()) { area.setText("无直达线路"); mapPanel.clearHighlight(); }
            else {
                area.setText("直达线路:\n" + String.join("\n", lines));
                List<String> path = new java.util.ArrayList<>(); path.add(s1.getText()); path.add(s2.getText());
                mapPanel.highlightPath(path);
            }
        });
        panel.add(form, BorderLayout.NORTH); panel.add(ModernUI.createModernScrollPane(area), BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createFieldGroup(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 5)); p.setOpaque(false);
        JLabel lbl = new JLabel(label); lbl.setFont(ModernUI.NORMAL_FONT); lbl.setForeground(Color.GRAY);
        p.add(lbl, BorderLayout.NORTH); p.add(comp, BorderLayout.CENTER);
        return p;
    }
}