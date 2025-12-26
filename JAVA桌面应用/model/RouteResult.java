package model;

import java.util.List;

public class RouteResult {
    public List<String> stations;
    public List<String> lineSegments;
    public int totalTime;
    public int transferCount;
    public String strategyName;
    public List<String> transferPoints;

    public RouteResult(List<String> stations, List<String> lineSegments, int totalTime, 
                      int transferCount, String strategyName, List<String> transferPoints) {
        this.stations = stations;
        this.lineSegments = lineSegments;
        this.totalTime = totalTime;
        this.transferCount = transferCount;
        this.strategyName = strategyName;
        this.transferPoints = transferPoints;
    }
    
    @Override
    public String toString() {
        return String.format("<html><b>[%s]</b><br>路线: %s<br>总耗时: %d分钟 | 换乘: %d次</html>", 
            strategyName, String.join(" → ", stations), totalTime, transferCount);
    }
    
    public String formatResultWithTime(model.TransportGraph graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("   路线: ").append(String.join(" → ", stations)).append("\n");
        sb.append("   预估耗时: ").append(totalTime).append("分钟\n");
        sb.append("   换乘次数: ").append(transferCount).append("次\n");
        
        // 显示首末班车信息
        if (!lineSegments.isEmpty()) {
            sb.append("   涉及线路: ");
            java.util.Set<String> uniqueLines = new java.util.LinkedHashSet<>(lineSegments);
            for (String line : uniqueLines) {
                model.LineInfo info = graph.lineMetaMap.get(line);
                if (info != null) {
                    sb.append(line).append("(").append(info.firstTime).append("-")
                      .append(info.lastTime).append(") ");
                }
            }
            sb.append("\n");
        }
        
        // 显示换乘点
        if (!transferPoints.isEmpty()) {
            sb.append("   换乘点: ").append(String.join("、", transferPoints)).append("\n");
        }
        
        return sb.toString();
    }
}