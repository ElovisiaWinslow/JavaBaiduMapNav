package model;

import javax.swing.*;
import java.io.*;
import java.util.*;

public class TransportGraph {
    // 邻接表：存储站点间的连接关系
    public Map<String, List<Connection>> adjList = new HashMap<>();
    // 线路-站点映射：存储每条线路包含的有序站点列表
    public Map<String, List<String>> lineStationsMap = new HashMap<>();
    // 线路元数据：存储首末班时间等
    public Map<String, LineInfo> lineMetaMap = new HashMap<>();
    // 站点坐标映射
    public Map<String, GeoCoordinate> stationCoords = new HashMap<>();

    // ========== 1. 增加 (Add) 功能 ==========

    public boolean addStationWithCoord(String station, int x, int y) {
        if (station == null || station.trim().isEmpty()) return false;
    
        // ★★★ 保护现有坐标 ★★★
        // 只有当传入的坐标有效（非0,0），或者该站点在系统中完全不存在时，才执行 put 更新
        boolean exists = stationCoords.containsKey(station);
        if (!exists || (x != 0 || y != 0)) {
            stationCoords.put(station, new GeoCoordinate(x, y));
        }
    
        adjList.putIfAbsent(station, new ArrayList<>()); 
        return true;
    }

    public boolean addRouteWithCoords(String lineName, String s1, int x1, int y1, 
                                    String s2, int x2, int y2, 
                                    int time, String firstTime, String lastTime) {
        // 1. 确保站点及坐标存在
        addStationWithCoord(s1, x1, y1);
        addStationWithCoord(s2, x2, y2);
    
        // 2. 添加物理连接 (双向边，使用指定的时间)
        addConnectionToGraph(s1, s2, lineName, time);
        addConnectionToGraph(s2, s1, lineName, time);

        // 3. 更新线路元数据
        lineStationsMap.computeIfAbsent(lineName, k -> new ArrayList<>());
        if (!lineMetaMap.containsKey(lineName)) {
            lineMetaMap.put(lineName, new LineInfo(lineName, firstTime, lastTime));
        }
        
        // 4. 更新线路的站点有序列表
        List<String> stops = lineStationsMap.get(lineName);
        boolean hasS1 = stops.contains(s1);
        boolean hasS2 = stops.contains(s2);

        if (!hasS1 && !hasS2) {
            stops.add(s1); stops.add(s2);
        } else if (hasS1 && !hasS2) {
            stops.add(stops.indexOf(s1) + 1, s2);
        } else if (!hasS1 && hasS2) {
            stops.add(stops.indexOf(s2), s1);
        }
        return true;
    }

    private void addConnectionToGraph(String from, String to, String line, int time) {
        List<Connection> conns = adjList.get(from);
        // 查重：避免重复添加同一条线同一方向的边
        for (Connection c : conns) {
            if (c.toStation.equals(to) && c.lineName.equals(line)) {
                c.timeCost = time; // 允许更新时间
                return;
            }
        }
        conns.add(new Connection(from, to, line, time));
    }

    // ========== 2. 删除 (Delete) 功能 ==========

    public boolean deleteStation(String station) {
        if (!stationCoords.containsKey(station)) return false;
        
        // 1. 移除连接
        if (adjList.containsKey(station)) {
            for (Connection conn : adjList.get(station)) {
                List<Connection> neighborConns = adjList.get(conn.toStation);
                if (neighborConns != null) {
                    neighborConns.removeIf(c -> c.toStation.equals(station));
                }
            }
        }
        adjList.remove(station);
        
        // 2. 移除线路引用
        Iterator<Map.Entry<String, List<String>>> it = lineStationsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<String>> entry = it.next();
            entry.getValue().remove(station);
            if (entry.getValue().isEmpty()) {
                lineMetaMap.remove(entry.getKey());
                it.remove();
            }
        }
        // 3. 移除坐标
        stationCoords.remove(station);
        return true;
    }

    public boolean deleteSection(String lineName, String startStation, String endStation) {
        if (!lineStationsMap.containsKey(lineName)) return false;
        List<String> stations = lineStationsMap.get(lineName);
        
        int idx1 = stations.indexOf(startStation);
        int idx2 = stations.indexOf(endStation);
        if (idx1 == -1 || idx2 == -1 || idx1 == idx2) return false;
        
        int start = Math.min(idx1, idx2);
        int end = Math.max(idx1, idx2);

        // 删除物理连接
        for (int i = start; i < end; i++) {
            String sA = stations.get(i);
            String sB = stations.get(i+1);
            removeEdge(sA, sB, lineName);
            removeEdge(sB, sA, lineName);
        }

        // 删除孤岛站点
        List<String> potentialOrphans = new ArrayList<>();
        for (int i = start + 1; i < end; i++) potentialOrphans.add(stations.get(i));

        stations.subList(start + 1, end).clear();
        for (String orphan : potentialOrphans) checkAndRemoveOrphanStation(orphan);
        return true;
    }
    
    private void removeEdge(String s1, String s2, String lineName) {
        if (adjList.containsKey(s1)) {
            adjList.get(s1).removeIf(c -> c.toStation.equals(s2) && c.lineName.equals(lineName));
        }
    }

    public boolean deleteLine(String lineName) {
        if (!lineStationsMap.containsKey(lineName)) return false;
        List<String> stations = new ArrayList<>(lineStationsMap.get(lineName));
        
        for (String station : stations) {
            if (adjList.containsKey(station)) {
                adjList.get(station).removeIf(c -> c.lineName.equals(lineName));
            }
        }
        lineStationsMap.remove(lineName);
        lineMetaMap.remove(lineName);
        for (String station : stations) checkAndRemoveOrphanStation(station);
        return true;
    }

    private void checkAndRemoveOrphanStation(String station) {
        boolean belongsToAnyLine = false;
        for (List<String> lines : lineStationsMap.values()) {
            if (lines.contains(station)) {
                belongsToAnyLine = true;
                break;
            }
        }
        if (!belongsToAnyLine) {
            adjList.remove(station);
            stationCoords.remove(station);
        }
    }

    // ========== 3. 修改 (Modify) 功能 ==========

    public boolean renameStation(String oldName, String newName) {
        if (!stationCoords.containsKey(oldName) || stationCoords.containsKey(newName)) return false;
        
        stationCoords.put(newName, stationCoords.remove(oldName));
        adjList.put(newName, adjList.remove(oldName));
        
        for (List<Connection> list : adjList.values()) {
            for (Connection c : list) {
                if (c.fromStation.equals(oldName)) c.fromStation = newName;
                if (c.toStation.equals(oldName)) c.toStation = newName;
            }
        }
        for (List<String> stops : lineStationsMap.values()) {
            Collections.replaceAll(stops, oldName, newName);
        }
        return true;
    }

    public boolean renameLine(String oldName, String newName) {
        if (!lineStationsMap.containsKey(oldName) || lineStationsMap.containsKey(newName)) return false;
        
        lineStationsMap.put(newName, lineStationsMap.remove(oldName));
        LineInfo info = lineMetaMap.remove(oldName);
        if (info != null) {
            info.lineName = newName;
            lineMetaMap.put(newName, info);
        }
        for (List<Connection> list : adjList.values()) {
            for (Connection c : list) {
                if (c.lineName.equals(oldName)) c.lineName = newName;
            }
        }
        return true;
    }
    
    public boolean updateStationCoord(String station, int x, int y) {
        if(!stationCoords.containsKey(station)) return false;
        stationCoords.put(station, new GeoCoordinate(x, y));
        return true;
    }

    /**
     * 管理员功能：修改两个站点之间的耗时
     */
    public boolean updateConnectionTime(String lineName, String s1, String s2, int newTime) {
        boolean found = false;
        // 正向
        if (adjList.containsKey(s1)) {
            for (Connection c : adjList.get(s1)) {
                if (c.toStation.equals(s2) && c.lineName.equals(lineName)) {
                    c.timeCost = newTime;
                    found = true;
                }
            }
        }
        // 反向
        if (adjList.containsKey(s2)) {
            for (Connection c : adjList.get(s2)) {
                if (c.toStation.equals(s1) && c.lineName.equals(lineName)) {
                    c.timeCost = newTime;
                    found = true;
                }
            }
        }
        return found;
    }

    // ========== 4. 查询辅助 ==========

    public List<String> getAllLines() { return new ArrayList<>(lineStationsMap.keySet()); }
    public List<String> getAllStations() { return new ArrayList<>(stationCoords.keySet()); }
    
    public int[] getStationCoords(String station) {
        GeoCoordinate gc = stationCoords.get(station);
        if (gc != null) return new int[]{(int)gc.longitude, (int)gc.latitude};
        return null;
    }

    /**
     * 获取指定线路两站之间的耗时（用于路书）
     */
    public int getConnectionTime(String from, String to, String lineName) {
        if (adjList.containsKey(from)) {
            for (Connection c : adjList.get(from)) {
                if (c.toStation.equals(to) && c.lineName.equals(lineName)) {
                    return c.timeCost;
                }
            }
        }
        return 0; // 未找到
    }

    // ========== 5. 文件 IO (支持 A,time,B 格式) ==========

    public void initData() {
        if (!loadFromFile("routes.txt")) {
            // 初始化失败时的默认行为
        }
    }

    public void saveToFile(String filename) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            // 1. 保存线路 (含时间权重)
            for (String lineName : lineStationsMap.keySet()) {
                LineInfo info = lineMetaMap.get(lineName);
                List<String> stops = lineStationsMap.get(lineName);
                
                StringBuilder sb = new StringBuilder();
                sb.append("LINE|").append(lineName).append("|")
                  .append(info != null ? info.firstTime : "06:00").append("|")
                  .append(info != null ? info.lastTime : "22:00").append("|");
                
                for (int i = 0; i < stops.size(); i++) {
                    String currentStation = stops.get(i);
                    sb.append(currentStation);
                    
                    if (i < stops.size() - 1) {
                        String nextStation = stops.get(i + 1);
                        int time = findTimeCost(currentStation, nextStation, lineName);
                        sb.append(",").append(time).append(",");
                    }
                }
                out.println(sb.toString());
            }
            
            // 2. 保存坐标
            for (String station : stationCoords.keySet()) {
                GeoCoordinate coord = stationCoords.get(station);
                out.printf("COORD|%s|%.6f|%.6f%n", station, coord.longitude, coord.latitude);
            }
        } catch (IOException e) { 
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "保存失败: " + e.getMessage());
        }
    }
    
    // 内部辅助：查找两站在线路上的当前耗时
    private int findTimeCost(String from, String to, String lineName) {
        if (adjList.containsKey(from)) {
            for (Connection c : adjList.get(from)) {
                if (c.toStation.equals(to) && c.lineName.equals(lineName)) {
                    return c.timeCost;
                }
            }
        }
        return 2; // 默认值
    }
    
    public boolean loadFromFile(String filename) {
        File f = new File(filename);
        if (!f.exists()) return false;
        
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            adjList.clear();
            lineStationsMap.clear();
            lineMetaMap.clear();
            stationCoords.clear();
            
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 2) continue;
                
                if (parts[0].equals("LINE") && parts.length >= 5) {
                    String lineName = parts[1];
                    String firstTime = parts[2];
                    String lastTime = parts[3];
                    
                    String[] segments = parts[4].split(",");
                    
                    // 单个站点情况
                    if (segments.length == 1) {
                        addRouteWithCoords(lineName, segments[0], 0, 0, segments[0], 0, 0, 0, firstTime, lastTime);
                    }
                    
                    // 解析 A,time,B,time,C 结构
                    for (int i = 0; i < segments.length - 2; i += 2) {
                        try {
                            String s1 = segments[i];
                            int time = Integer.parseInt(segments[i+1]);
                            String s2 = segments[i+2];
                            addRouteWithCoords(lineName, s1, 0, 0, s2, 0, 0, time, firstTime, lastTime);
                        } catch (NumberFormatException e) {
                            System.err.println("数据格式错误: " + lineName);
                        }
                    }
                } else if (parts[0].equals("COORD") && parts.length >= 4) {
                    try {
                        String station = parts[1];
                        double lon = Double.parseDouble(parts[2]);
                        double lat = Double.parseDouble(parts[3]);
                        stationCoords.put(station, new GeoCoordinate(lon, lat));
                    } catch (Exception e) {}
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}