package view;

import model.TransportGraph;
import model.Connection;
import model.GeoCoordinate;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisualMapPanel extends JPanel {
    // 视图模式常量
    public static final int VIEW_ALL = 0;
    public static final int VIEW_METRO = 1;
    public static final int VIEW_BUS = 2;

    private TransportGraph graph;
    private Map<String, Color> lineColors = new HashMap<>();
    private List<String> highlightedPath = null;
    
    private Set<String> metroTransferStations = new HashSet<>();
    private Set<String> busTransferStations = new HashSet<>();
    
    private int currentViewMode = VIEW_ALL;
    private Set<String> visibleStations = new HashSet<>();

    // 视图控制变量
    private double scale = 1.0;
    private int offsetX = 0, offsetY = 0;
    private Point dragStart = null;
    private boolean isDragging = false;
    private double minX, maxX, minY, maxY;
    
    // 色板
    private static final Color[] COLOR_PALETTE = {
        new Color(52, 152, 219), new Color(231, 76, 60), new Color(46, 204, 113),
        new Color(155, 89, 182), new Color(241, 196, 15), new Color(230, 126, 34),
        new Color(26, 188, 156), new Color(52, 73, 94),   new Color(211, 84, 0),
        new Color(192, 57, 43),  new Color(142, 68, 173), new Color(39, 174, 96),
        new Color(41, 128, 185), new Color(127, 140, 141), new Color(255, 0, 128)
    };

    public VisualMapPanel(TransportGraph graph) {
        this.graph = graph;
        this.setBackground(new Color(250, 252, 255));
        updateGraph(graph);
        
        // 鼠标交互
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    dragStart = e.getPoint(); isDragging = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging && dragStart != null) {
                    offsetX += e.getX() - dragStart.x;
                    offsetY += e.getY() - dragStart.y;
                    dragStart = e.getPoint();
                    repaint();
                }
            }
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double zoomFactor = 1.1;
                double oldScale = scale;
                if (e.getWheelRotation() < 0) scale = Math.min(8.0, scale * zoomFactor);
                else scale = Math.max(0.1, scale / zoomFactor);
                
                double f = scale / oldScale;
                int mx = e.getX(), my = e.getY();
                offsetX = (int)(mx - (mx - offsetX) * f);
                offsetY = (int)(my - (my - offsetY) * f);
                repaint();
            }
        };
        addMouseListener(ma); addMouseMotionListener(ma); addMouseWheelListener(ma);
    }
    
    public void setViewMode(int mode) {
        this.currentViewMode = mode;
        calculateVisibleStations(); 
        repaint();
    }

    // ★★★ 核心排序逻辑：实现用户的具体排序需求
    private Comparator<String> lineComparator = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            boolean isBus1 = o1.contains("公交");
            boolean isBus2 = o2.contains("公交");

            // 1. 公交排在最后
            if (!isBus1 && isBus2) return -1;
            if (isBus1 && !isBus2) return 1;

            // 2. 如果都是公交，按数字排序
            if (isBus1 && isBus2) {
                return extractNumber(o1) - extractNumber(o2);
            }

            // 3. 如果都是地铁
            // 判断是否是 S 线
            boolean isS1 = o1.toUpperCase().startsWith("S");
            boolean isS2 = o2.toUpperCase().startsWith("S");

            // 数字线排在 S 线前面
            if (!isS1 && isS2) return -1;
            if (isS1 && !isS2) return 1;

            // 同类地铁（都是数字线，或都是S线），按数字大小排序
            return extractNumber(o1) - extractNumber(o2);
        }

        // 辅助方法：从字符串中提取第一个数字
        private int extractNumber(String s) {
            Matcher m = Pattern.compile("\\d+").matcher(s);
            if (m.find()) {
                return Integer.parseInt(m.group());
            }
            return 0;
        }
    };

    private void calculateVisibleStations() {
        visibleStations.clear();
        if (graph == null) return;

        if (currentViewMode == VIEW_ALL) {
            visibleStations.addAll(graph.getAllStations());
            return;
        }

        for (Map.Entry<String, List<String>> entry : graph.lineStationsMap.entrySet()) {
            String lineName = entry.getKey();
            boolean isBus = lineName.contains("公交");
            if (currentViewMode == VIEW_METRO && isBus) continue;
            if (currentViewMode == VIEW_BUS && !isBus) continue;
            visibleStations.addAll(entry.getValue());
        }
    }

    private void calculateTransferStations() {
        metroTransferStations.clear();
        busTransferStations.clear();
        
        if (graph == null || graph.lineStationsMap == null) return;
        
        Map<String, Integer> metroCounts = new HashMap<>();
        Map<String, Integer> busCounts = new HashMap<>();
        
        for (Map.Entry<String, List<String>> entry : graph.lineStationsMap.entrySet()) {
            String lineName = entry.getKey();
            List<String> stations = entry.getValue();
            boolean isBus = lineName.contains("公交");
            
            Set<String> uniqueStations = new HashSet<>(stations);
            
            for (String s : uniqueStations) {
                if (isBus) {
                    busCounts.put(s, busCounts.getOrDefault(s, 0) + 1);
                } else {
                    metroCounts.put(s, metroCounts.getOrDefault(s, 0) + 1);
                }
            }
        }
        
        for (Map.Entry<String, Integer> entry : metroCounts.entrySet()) {
            if (entry.getValue() > 1) metroTransferStations.add(entry.getKey());
        }
        for (Map.Entry<String, Integer> entry : busCounts.entrySet()) {
            if (entry.getValue() > 1) busTransferStations.add(entry.getKey());
        }
    }

    private void refreshLineColors() {
        lineColors.clear();
        if (graph == null) return;
        List<String> lines = graph.getAllLines();
        
        // 使用新的排序逻辑来分配颜色，这样同类线路的颜色分布会更均匀
        Collections.sort(lines, lineComparator);
        
        int colorIdx = 0;
        for (String line : lines) {
            lineColors.put(line, COLOR_PALETTE[colorIdx % COLOR_PALETTE.length]);
            colorIdx++;
        }
    }

    private boolean shouldDrawVertically(String station) {
        if (!visibleStations.contains(station)) return false;
        if (graph.lineStationsMap == null) return false;
        for (List<String> stations : graph.lineStationsMap.values()) {
            int idx = stations.indexOf(station);
            if (idx == -1) continue;
            GeoCoordinate curr = graph.stationCoords.get(station);
            GeoCoordinate prev = (idx > 0) ? graph.stationCoords.get(stations.get(idx - 1)) : null;
            GeoCoordinate next = (idx < stations.size() - 1) ? graph.stationCoords.get(stations.get(idx + 1)) : null;
            if (curr == null) continue;
            double tolerance = 0.5;
            boolean isHorizontalLine = false;
            if (prev != null && next != null) { if (Math.abs(prev.latitude - next.latitude) < tolerance) isHorizontalLine = true; } 
            else if (prev != null) { if (Math.abs(prev.latitude - curr.latitude) < tolerance) isHorizontalLine = true; } 
            else if (next != null) { if (Math.abs(next.latitude - curr.latitude) < tolerance) isHorizontalLine = true; }
            if (isHorizontalLine) return true;
        }
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (graph == null) return;

        drawGrid(g2);

        // 1. 绘制线路
        for (String station : graph.adjList.keySet()) {
            if (!visibleStations.contains(station)) continue;
            GeoCoordinate c1 = graph.stationCoords.get(station);
            if (c1 == null) continue;
            Point p1 = coordToScreen(c1);

            for (Connection conn : graph.adjList.get(station)) {
                if (!visibleStations.contains(conn.toStation)) continue;
                
                boolean isBusLine = conn.lineName.contains("公交");
                if (currentViewMode == VIEW_METRO && isBusLine) continue;
                if (currentViewMode == VIEW_BUS && !isBusLine) continue;
                
                GeoCoordinate c2 = graph.stationCoords.get(conn.toStation);
                if (c2 == null || station.compareTo(conn.toStation) > 0) continue;
                Point p2 = coordToScreen(c2);

                boolean isHigh = highlightedPath != null && highlightedPath.contains(station) && highlightedPath.contains(conn.toStation);
                float baseWidth = isBusLine ? 1.5f : 3.5f;
                float strokeWidth = baseWidth * (float)Math.sqrt(scale);
                if(strokeWidth < 1f) strokeWidth = 1f;
                
                Color baseColor = lineColors.getOrDefault(conn.lineName, Color.GRAY);
                
                if (isHigh) {
                    g2.setStroke(new BasicStroke(strokeWidth * 2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(new Color(255, 50, 50, 100));
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                    g2.setColor(Color.RED);
                    g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                } else {
                    int alpha = isBusLine ? 150 : 220;
                    g2.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha));
                    g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

        // 2. 绘制站点
        for (String station : graph.stationCoords.keySet()) {
            if (!visibleStations.contains(station)) continue;
            
            GeoCoordinate c = graph.stationCoords.get(station);
            Point p = coordToScreen(c);
            
            boolean isHigh = highlightedPath != null && highlightedPath.contains(station);
            
            boolean showMetroTransfer = false;
            boolean showBusTransfer = false;
            
            if (currentViewMode == VIEW_ALL) {
                if (metroTransferStations.contains(station)) showMetroTransfer = true;
                else if (busTransferStations.contains(station)) showBusTransfer = true;
            } else if (currentViewMode == VIEW_METRO) {
                if (metroTransferStations.contains(station)) showMetroTransfer = true;
            } else if (currentViewMode == VIEW_BUS) {
                if (busTransferStations.contains(station)) showBusTransfer = true;
            }
            
            boolean isAnyTransfer = showMetroTransfer || showBusTransfer;
            
            int baseSize = isAnyTransfer ? 11 : 8;
            int size = (int)(baseSize * Math.sqrt(scale)); 
            if (size < 5) size = 5; if (size > 22) size = 22;
            
            if (isHigh) {
                g2.setColor(new Color(255, 0, 0, 80));
                g2.fillOval(p.x - size - 4, p.y - size - 4, size*2 + 8, size*2 + 8);
                g2.setColor(Color.WHITE);
                g2.fillOval(p.x - size/2, p.y - size/2, size, size);
                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(p.x - size/2, p.y - size/2, size, size);
                
            } else if (showMetroTransfer) {
                // ★ 地铁换乘：菱形，内部填充红色
                // ★★★ 这里的 WHITE 改为 RED
                g2.setColor(Color.RED); 
                Path2D.Double diamond = new Path2D.Double();
                double r = size / 1.7;
                diamond.moveTo(p.x, p.y - r - 2);
                diamond.lineTo(p.x + r + 2, p.y);
                diamond.lineTo(p.x, p.y + r + 2);
                diamond.lineTo(p.x - r - 2, p.y);
                diamond.closePath();
                g2.fill(diamond);
                
                g2.setColor(Color.BLACK); 
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(diamond);
                
            } else if (showBusTransfer) {
                // ★ 公交换乘：圆角矩形，内部填充红色
                // ★★★ 这里的 WHITE 改为 RED
                g2.setColor(Color.RED);
                int r = size / 2 + 1;
                g2.fillRoundRect(p.x - r, p.y - r, r*2, r*2, 4, 4);
                
                g2.setColor(new Color(52, 152, 219));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(p.x - r, p.y - r, r*2, r*2, 4, 4);
                
            } else {
                g2.setColor(Color.WHITE);
                g2.fillOval(p.x - size/2, p.y - size/2, size, size);
                g2.setColor(Color.GRAY);
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(p.x - size/2, p.y - size/2, size, size);
            }
            
            if (scale > 0.5) {
                g2.setFont(new Font("微软雅黑", isAnyTransfer ? Font.BOLD : Font.PLAIN, Math.max(10, (int)(12 * Math.sqrt(scale)))));
                g2.setColor(isHigh ? Color.RED : (isAnyTransfer ? Color.BLACK : Color.DARK_GRAY));
                
                int textX = p.x + size/2 + 4;
                int textY = p.y + size/2 + 4;
                
                if (shouldDrawVertically(station)) {
                    FontMetrics fm = g2.getFontMetrics();
                    int lineHeight = fm.getHeight() - 2;
                    for (int i = 0; i < station.length(); i++) {
                        String charStr = String.valueOf(station.charAt(i));
                        g2.drawString(charStr, textX, textY + fm.getAscent() + (i * lineHeight));
                    }
                } else {
                    g2.drawString(station, textX, textY + g2.getFontMetrics().getAscent()/2);
                }
            }
        }
        
        drawLegend(g2);
    }
    
    private void drawGrid(Graphics2D g2) {
        if (scale < 0.4) return;
        g2.setColor(new Color(235, 235, 240));
        g2.setStroke(new BasicStroke(1f));
        int step = (int)(100 * scale);
        for (int i = offsetX % step; i < getWidth(); i += step) g2.drawLine(i, 0, i, getHeight());
        for (int i = offsetY % step; i < getHeight(); i += step) g2.drawLine(0, i, getWidth(), i);
    }

    private void drawLegend(Graphics2D g2) {
        List<String> allLines = new ArrayList<>();
        for (String line : lineColors.keySet()) {
            boolean isBus = line.contains("公交");
            if (currentViewMode == VIEW_METRO && isBus) continue;
            if (currentViewMode == VIEW_BUS && !isBus) continue;
            allLines.add(line);
        }
        
        // ★★★ 应用新的排序逻辑
        Collections.sort(allLines, lineComparator);
        
        int lineCount = allLines.size();
        if (lineCount == 0) return;

        int padding = 15; int lineHeight = 20; int colWidth = 110; 
        int titleHeight = 90;
        int maxLinesPerCol = 10; 
        int cols = (int) Math.ceil((double)lineCount / maxLinesPerCol); if (cols < 1) cols = 1;
        int panelW = padding * 2 + cols * colWidth;
        int panelH = padding * 2 + titleHeight + Math.min(lineCount, maxLinesPerCol) * lineHeight;
        
        int x = 20; int y = 20;
        
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fillRoundRect(x, y, panelW, panelH, 12, 12);
        g2.setColor(new Color(200, 200, 200));
        g2.drawRoundRect(x, y, panelW, panelH, 12, 12);
        
        int curX = x + padding; int curY = y + padding + 15;
        g2.setFont(new Font("微软雅黑", Font.BOLD, 14));
        g2.setColor(Color.DARK_GRAY);
        String title = (currentViewMode == VIEW_BUS) ? "公交图例" : (currentViewMode == VIEW_METRO ? "地铁图例" : "全图例");
        g2.drawString(title, curX, curY);
        
        curY += 25;
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        g2.setColor(Color.WHITE); g2.fillOval(curX, curY-6, 10, 10);
        g2.setColor(Color.DARK_GRAY); g2.drawOval(curX, curY-6, 10, 10);
        g2.drawString("站点", curX + 15, curY+4);
        
        g2.setColor(Color.RED); g2.fillOval(curX + 60, curY-6, 10, 10);
        g2.drawString("高亮", curX + 75, curY+4);
        
        curY += 20;
        
        int legendIconX = curX + 5;
        
        if (currentViewMode == VIEW_ALL || currentViewMode == VIEW_METRO) {
            // ★★★ 图例也同步改为红色填充
            g2.setColor(Color.RED);
            Path2D.Double d = new Path2D.Double();
            d.moveTo(legendIconX, curY - 6); d.lineTo(legendIconX + 5, curY-1);
            d.lineTo(legendIconX, curY + 4); d.lineTo(legendIconX - 5, curY-1); d.closePath();
            g2.fill(d); g2.setColor(Color.BLACK); g2.draw(d);
            
            g2.setColor(Color.BLACK);
            g2.drawString("地铁换乘", curX + 15, curY+3);
            
            if (currentViewMode == VIEW_ALL) curY += 20;
        }
        
        if (currentViewMode == VIEW_ALL || currentViewMode == VIEW_BUS) {
            // ★★★ 图例也同步改为红色填充
            g2.setColor(Color.RED);
            g2.fillRoundRect(legendIconX - 4, curY - 5, 9, 9, 2, 2);
            g2.setColor(new Color(52, 152, 219));
            g2.drawRoundRect(legendIconX - 4, curY - 5, 9, 9, 2, 2);
            
            g2.setColor(Color.BLACK);
            g2.drawString("公交换乘", curX + 15, curY+3);
        }
        
        curY += 20; g2.setColor(new Color(220, 220, 220)); g2.drawLine(x+5, curY, x+panelW-5, curY); curY += 20;
        
        int startYForLines = curY;
        for (int i = 0; i < lineCount; i++) {
            String lineName = allLines.get(i);
            Color c = lineColors.get(lineName);
            int colIndex = i / maxLinesPerCol;
            int rowIndex = i % maxLinesPerCol;
            int drawX = curX + colIndex * colWidth;
            int drawY = startYForLines + rowIndex * lineHeight;
            g2.setColor(c); g2.setStroke(new BasicStroke(3f));
            g2.drawLine(drawX, drawY - 4, drawX + 25, drawY - 4);
            g2.setColor(Color.DARK_GRAY); g2.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            String dispName = lineName.length() > 6 ? lineName.substring(0, 5)+".." : lineName;
            g2.drawString(dispName, drawX + 30, drawY);
        }
    }

    private void calculateBounds() {
        minX = Double.MAX_VALUE; maxX = Double.MIN_VALUE;
        minY = Double.MAX_VALUE; maxY = Double.MIN_VALUE;
        if(graph == null || graph.stationCoords.isEmpty()) {
            minX=0; maxX=1000; minY=0; maxY=800; return;
        }
        for (GeoCoordinate c : graph.stationCoords.values()) {
            minX = Math.min(minX, c.longitude); maxX = Math.max(maxX, c.longitude);
            minY = Math.min(minY, c.latitude); maxY = Math.max(maxY, c.latitude);
        }
        double padX = (maxX-minX)*0.1 + 1; 
        double padY = (maxY-minY)*0.1 + 1;
        minX -= padX; maxX += padX; minY -= padY; maxY += padY;
    }

    private Point coordToScreen(GeoCoordinate coord) {
        double w = getWidth(), h = getHeight();
        double nx = (coord.longitude - minX) / (maxX - minX);
        double ny = 1.0 - (coord.latitude - minY) / (maxY - minY);
        return new Point((int)(nx * w * scale) + offsetX, (int)(ny * h * scale) + offsetY);
    }
    
    public void highlightPath(List<String> path) { this.highlightedPath = path; repaint(); }
    public void clearHighlight() { this.highlightedPath = null; repaint(); }
    public void resetView() { scale = 1.0; offsetX = 0; offsetY = 0; repaint(); }
    
    public void updateGraph(TransportGraph g) { 
        this.graph = g; 
        calculateTransferStations(); 
        calculateBounds(); 
        refreshLineColors();
        calculateVisibleStations(); 
        repaint(); 
    }
    
    public void refreshBounds() { calculateBounds(); repaint(); }
}