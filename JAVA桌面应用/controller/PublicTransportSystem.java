package controller;

import model.TransportGraph;
import service.NavigationService;
import view.*;
import javax.swing.*;
import java.awt.*;

public class PublicTransportSystem extends JFrame {
    private TransportGraph graph;
    private NavigationService service;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private LoginPanel loginPanel;
    private UserPanel userPanel;
    private AdminPanel adminPanel;
    
    public PublicTransportSystem() {
        // 使用系统外观，但ModernUI会覆盖具体组件样式
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        initData();
        initUI();
    }
    
    private void initData() {
        graph = new TransportGraph();
        graph.initData(); // 加载 routes.txt
        service = new NavigationService(graph);
    }
    
    private void initUI() {
        setTitle("南京公共交通导航系统 - 2025专业版");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        loginPanel = new LoginPanel(this);
        // 初始化时创建
        userPanel = new UserPanel(this, graph, service);
        adminPanel = new AdminPanel(this, graph, service);
        
        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(userPanel, "USER");
        mainPanel.add(adminPanel, "ADMIN");
        add(mainPanel);
    }
    
    public void showUserPanel() {
        cardLayout.show(mainPanel, "USER");
    }
    
    public void showAdminLogin() {
        JPasswordField pf = new JPasswordField();
        int ok = JOptionPane.showConfirmDialog(this, pf, "输入管理员密码 (默认123456)", JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) {
            if ("123456".equals(new String(pf.getPassword()))) showAdminPanel();
            else JOptionPane.showMessageDialog(this, "密码错误", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void showAdminPanel() {
        cardLayout.show(mainPanel, "ADMIN");
    }
    
    public void showLoginPanel() {
        cardLayout.show(mainPanel, "LOGIN");
    }
    
    public void exitAdminPanel() {
        int confirm = JOptionPane.showConfirmDialog(this, "保存更改并退出?", "确认", JOptionPane.YES_NO_CANCEL_OPTION);
        if (confirm == JOptionPane.CANCEL_OPTION) return;
        
        if (confirm == JOptionPane.YES_OPTION) {
            graph.saveToFile("routes.txt");
        }
        
        // 关键：重新加载 UserPanel 以获取最新的地图数据
        mainPanel.remove(userPanel);
        userPanel = new UserPanel(this, graph, service);
        mainPanel.add(userPanel, "USER");
        
        showLoginPanel();
    }
}