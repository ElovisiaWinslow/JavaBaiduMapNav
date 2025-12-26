package view;

import controller.PublicTransportSystem;
import model.TransportGraph;
import service.NavigationService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminPanel extends JPanel {
    private PublicTransportSystem controller;
    private TransportGraph graph;
    private NavigationService service;
    private VisualMapPanel mapPanel;
    private JTextArea logArea;

    // ★★★ 修复：将下拉框提升为成员变量，以便全局刷新
    private JComboBox<String> addLineCombo;
    private JComboBox<String> deleteSectionLineCombo;
    private JComboBox<String> deleteLineCombo;
    private JComboBox<String> modifyLineNameCombo;
    private JComboBox<String> modifyTimeLineCombo;

    public AdminPanel(PublicTransportSystem controller, TransportGraph graph, NavigationService service) {
        this.controller = controller;
        this.graph = graph;
        this.service = service;
        this.mapPanel = new VisualMapPanel(graph);
        ModernUI.decoratePanel(this);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 顶部工具栏
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topBar.setBackground(ModernUI.TEXT_COLOR);
        JButton btnBack = ModernUI.createButton("← 退出", ModernUI.TEXT_COLOR); btnBack.setForeground(Color.WHITE);
        JButton btnSave = ModernUI.createSuccessButton("💾 保存");
        JButton btnRefresh = ModernUI.createButton("刷新地图", ModernUI.PRIMARY_COLOR);
        
        btnBack.addActionListener(e -> controller.exitAdminPanel());
        btnSave.addActionListener(e -> { graph.saveToFile("routes.txt"); log("保存成功"); });
        btnRefresh.addActionListener(e -> { mapPanel.updateGraph(graph); mapPanel.refreshBounds(); });
        topBar.add(btnBack); topBar.add(btnSave); topBar.add(btnRefresh);

        // 左侧 Tabs
        JTabbedPane leftTabs = new JTabbedPane();
        leftTabs.setFont(ModernUI.NORMAL_FONT);
        leftTabs.addTab("增加", wrapInCard(createAddPanel()));
        leftTabs.addTab("删除", wrapInCard(createDeletePanel()));
        leftTabs.addTab("修改", wrapInCard(createModifyPanel()));

        // 日志区域
        logArea = new JTextArea(6, 20); logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("操作日志"));

        JPanel leftContainer = new JPanel(new BorderLayout());
        leftContainer.add(leftTabs, BorderLayout.CENTER);
        leftContainer.add(logScroll, BorderLayout.SOUTH);

        // 分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftContainer, mapPanel);
        splitPane.setDividerLocation(420); splitPane.setBorder(null);

        add(topBar, BorderLayout.NORTH); add(splitPane, BorderLayout.CENTER);
    }
    
    // ★★★ 修复：新增方法，用于刷新所有Tabs中的下拉框
    private void refreshAllCombos() {
        if (addLineCombo != null) updateLineCombo(addLineCombo);
        if (deleteSectionLineCombo != null) updateLineCombo(deleteSectionLineCombo);
        if (deleteLineCombo != null) updateLineCombo(deleteLineCombo);
        if (modifyLineNameCombo != null) updateLineCombo(modifyLineNameCombo);
        if (modifyTimeLineCombo != null) updateLineCombo(modifyTimeLineCombo);
    }

    private JPanel wrapInCard(JPanel content) {
        JPanel w = new JPanel(new BorderLayout()); w.setBackground(ModernUI.BG_COLOR);
        w.setBorder(new EmptyBorder(10, 10, 10, 10)); w.add(content); return w;
    }
    
    private JPanel createFormRow(String lbl, JComponent f) {
        JPanel p = new JPanel(new BorderLayout(5, 5)); p.setOpaque(false);
        JLabel l = new JLabel(lbl); l.setFont(ModernUI.NORMAL_FONT);
        p.add(l, BorderLayout.NORTH); p.add(f, BorderLayout.CENTER); return p;
    }
    
    // ========== 增加模块 ==========
    private JPanel createAddPanel() {
        JPanel panel = ModernUI.createCardPanel(); panel.setLayout(new BorderLayout(0, 10));
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT)); radios.setOpaque(false);
        JRadioButton rb1 = new JRadioButton("孤立站点", true); JRadioButton rb2 = new JRadioButton("连接/线路");
        ButtonGroup bg = new ButtonGroup(); bg.add(rb1); bg.add(rb2); radios.add(rb1); radios.add(rb2);
        
        CardLayout cl = new CardLayout(); JPanel content = new JPanel(cl); content.setOpaque(false);
        
        // 孤立站点
        JPanel p1 = new JPanel(new GridLayout(4, 1, 0, 15)); p1.setOpaque(false);
        JTextField tN = ModernUI.createTextField(); JTextField tX = ModernUI.createTextField("500"); JTextField tY = ModernUI.createTextField("500");
        p1.add(createFormRow("站点名", tN)); p1.add(createFormRow("X", tX)); p1.add(createFormRow("Y", tY));
        JButton btn1 = ModernUI.createPrimaryButton("添加");
        btn1.addActionListener(e -> {
             if(graph.addStationWithCoord(tN.getText(), Integer.parseInt(tX.getText()), Integer.parseInt(tY.getText()))) {
                 log("添加点: " + tN.getText()); mapPanel.updateGraph(graph);
             }
        });
        p1.add(btn1);
        
        // 连接
        JPanel p2 = new JPanel(new GridLayout(6, 1, 0, 10)); p2.setOpaque(false);
        addLineCombo = new JComboBox<>(); addLineCombo.setEditable(true); updateLineCombo(addLineCombo);
        JTextField s1 = ModernUI.createTextField(), s2 = ModernUI.createTextField(), tm = ModernUI.createTextField("3");
        new StationAutoCompleter(s1, graph.getAllStations()); new StationAutoCompleter(s2, graph.getAllStations());
        p2.add(createFormRow("线路(可新)", addLineCombo)); p2.add(createFormRow("站A", s1)); p2.add(createFormRow("站B", s2)); p2.add(createFormRow("时间", tm));
        JButton btn2 = ModernUI.createPrimaryButton("添加连接");
        btn2.addActionListener(e -> {
            String l = (String)addLineCombo.getSelectedItem();
            if(graph.addRouteWithCoords(l, s1.getText(), 0, 0, s2.getText(), 0, 0, Integer.parseInt(tm.getText()), "06:00", "22:00")) {
                log("添加连接: " + s1.getText()+"-"+s2.getText()); 
                mapPanel.updateGraph(graph);
                // ★★★ 修复：调用全局刷新，让删除和修改页面也能看到新线路
                refreshAllCombos();
            }
        });
        p2.add(btn2);
        
        content.add(p1, "A"); content.add(p2, "B");
        rb1.addActionListener(e -> cl.show(content, "A")); rb2.addActionListener(e -> cl.show(content, "B"));
        panel.add(radios, BorderLayout.NORTH); panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    // ========== 删除模块 ==========
    private JPanel createDeletePanel() {
        JPanel panel = ModernUI.createCardPanel(); panel.setLayout(new BorderLayout(0, 10));
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT)); radios.setOpaque(false);
        JRadioButton r1 = new JRadioButton("站点", true), r2 = new JRadioButton("区间"), r3 = new JRadioButton("线路");
        ButtonGroup bg = new ButtonGroup(); bg.add(r1); bg.add(r2); bg.add(r3); radios.add(r1); radios.add(r2); radios.add(r3);
        
        CardLayout cl = new CardLayout(); JPanel content = new JPanel(cl); content.setOpaque(false);
        
        JPanel p1 = new JPanel(new GridLayout(3,1,0,15)); p1.setOpaque(false);
        JTextField t1 = ModernUI.createTextField(); new StationAutoCompleter(t1, graph.getAllStations());
        JButton b1 = ModernUI.createDangerButton("删除站点");
        b1.addActionListener(e -> { if(graph.deleteStation(t1.getText())) { log("删除点:"+t1.getText()); mapPanel.updateGraph(graph); }});
        p1.add(createFormRow("站点名", t1)); p1.add(b1);
        
        JPanel p2 = new JPanel(new GridLayout(4,1,0,10)); p2.setOpaque(false);
        deleteSectionLineCombo = new JComboBox<>(); updateLineCombo(deleteSectionLineCombo);
        JTextField t2a = ModernUI.createTextField(), t2b = ModernUI.createTextField();
        new StationAutoCompleter(t2a, graph.getAllStations()); new StationAutoCompleter(t2b, graph.getAllStations());
        JButton b2 = ModernUI.createDangerButton("删除区间");
        b2.addActionListener(e -> { if(graph.deleteSection((String)deleteSectionLineCombo.getSelectedItem(), t2a.getText(), t2b.getText())) { log("删除区间成功"); mapPanel.updateGraph(graph); }});
        p2.add(createFormRow("线路", deleteSectionLineCombo)); p2.add(createFormRow("起", t2a)); p2.add(createFormRow("止", t2b)); p2.add(b2);

        JPanel p3 = new JPanel(new GridLayout(3,1,0,15)); p3.setOpaque(false);
        deleteLineCombo = new JComboBox<>(); updateLineCombo(deleteLineCombo);
        JButton b3 = ModernUI.createDangerButton("删除整线");
        b3.addActionListener(e -> { 
            if(graph.deleteLine((String)deleteLineCombo.getSelectedItem())) { 
                log("删除线成功"); 
                mapPanel.updateGraph(graph); 
                // ★★★ 修复：删除线路后也要刷新所有下拉框
                refreshAllCombos(); 
            }
        });
        p3.add(createFormRow("线路", deleteLineCombo)); p3.add(b3);

        content.add(p1,"A"); content.add(p2,"B"); content.add(p3,"C");
        r1.addActionListener(e->cl.show(content,"A")); r2.addActionListener(e->cl.show(content,"B")); r3.addActionListener(e->cl.show(content,"C"));
        panel.add(radios, BorderLayout.NORTH); panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    // ========== 修改模块 (含修改时间) ==========
    private JPanel createModifyPanel() {
        JPanel panel = ModernUI.createCardPanel(); panel.setLayout(new BorderLayout(0, 10));
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT)); radios.setOpaque(false);
        
        JRadioButton r1 = new JRadioButton("站点", true); 
        JRadioButton r2 = new JRadioButton("线路");
        JRadioButton r3 = new JRadioButton("耗时");
        
        ButtonGroup bg = new ButtonGroup(); bg.add(r1); bg.add(r2); bg.add(r3);
        radios.add(r1); radios.add(r2); radios.add(r3);
        
        CardLayout cl = new CardLayout(); JPanel content = new JPanel(cl); content.setOpaque(false);
        
        // Mode 1: 站点
        JPanel p1 = new JPanel(new GridLayout(5,1,0,10)); p1.setOpaque(false);
        JTextField old = ModernUI.createTextField(), nw = ModernUI.createTextField(), nx = ModernUI.createTextField(), ny = ModernUI.createTextField();
        new StationAutoCompleter(old, graph.getAllStations());
        JButton b1 = ModernUI.createPrimaryButton("提交修改");
        b1.addActionListener(e -> {
            if(!nw.getText().isEmpty()) graph.renameStation(old.getText(), nw.getText());
            if(!nx.getText().isEmpty()) graph.updateStationCoord(nw.getText().isEmpty()?old.getText():nw.getText(), Integer.parseInt(nx.getText()), Integer.parseInt(ny.getText()));
            mapPanel.updateGraph(graph); log("修改站点");
        });
        p1.add(createFormRow("原名", old)); p1.add(createFormRow("新名", nw)); p1.add(createFormRow("新X", nx)); p1.add(createFormRow("新Y", ny)); p1.add(b1);
        
        // Mode 2: 线路
        JPanel p2 = new JPanel(new GridLayout(3,1,0,15)); p2.setOpaque(false);
        modifyLineNameCombo = new JComboBox<>(); updateLineCombo(modifyLineNameCombo);
        JTextField t2 = ModernUI.createTextField();
        JButton b2 = ModernUI.createPrimaryButton("重命名");
        b2.addActionListener(e -> { 
            if(graph.renameLine((String)modifyLineNameCombo.getSelectedItem(), t2.getText())) { 
                log("改线名成功"); 
                mapPanel.updateGraph(graph); 
                // ★★★ 修复：改名后刷新列表
                refreshAllCombos(); 
            }
        });
        p2.add(createFormRow("线路", modifyLineNameCombo)); p2.add(createFormRow("新名", t2)); p2.add(b2);
        
        // Mode 3: 耗时
        JPanel p3 = new JPanel(new GridLayout(5,1,0,10)); p3.setOpaque(false);
        modifyTimeLineCombo = new JComboBox<>(); updateLineCombo(modifyTimeLineCombo);
        JTextField ts1 = ModernUI.createTextField(), ts2 = ModernUI.createTextField(), tNewTime = ModernUI.createTextField();
        new StationAutoCompleter(ts1, graph.getAllStations()); new StationAutoCompleter(ts2, graph.getAllStations());
        JButton b3 = ModernUI.createPrimaryButton("更新耗时");
        
        b3.addActionListener(e -> {
            try {
                int time = Integer.parseInt(tNewTime.getText());
                String line = (String)modifyTimeLineCombo.getSelectedItem();
                String sA = ts1.getText(); String sB = ts2.getText();
                if(graph.updateConnectionTime(line, sA, sB, time)) {
                    log("更新: " + sA + "-" + sB + " = " + time + "分");
                } else {
                    log("失败: 连接不存在");
                }
            } catch(NumberFormatException ex) { log("时间必须是整数"); }
        });
        
        p3.add(createFormRow("线路", modifyTimeLineCombo)); 
        p3.add(createFormRow("站点A", ts1)); 
        p3.add(createFormRow("站点B", ts2)); 
        p3.add(createFormRow("新耗时", tNewTime)); 
        p3.add(b3);
        
        content.add(p1, "A"); content.add(p2, "B"); content.add(p3, "C");
        r1.addActionListener(e->cl.show(content,"A")); r2.addActionListener(e->cl.show(content,"B")); r3.addActionListener(e->cl.show(content,"C"));
        panel.add(radios, BorderLayout.NORTH); panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private void updateLineCombo(JComboBox<String> cb) {
        cb.removeAllItems(); for(String l : graph.getAllLines()) cb.addItem(l);
    }
    private void log(String s) { logArea.append(s+"\n"); }
}