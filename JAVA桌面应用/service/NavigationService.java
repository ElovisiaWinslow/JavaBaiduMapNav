package service;

import model.*;
import java.util.*;

public class NavigationService {
    private TransportGraph graph;

    // 策略常量定义
    public static final int STRATEGY_TIME = 0;     // 时间最短
    public static final int STRATEGY_TRANSFER = 1; // 换乘最少
    public static final int STRATEGY_BUS_ONLY = 2; // 只坐公交
    public static final int STRATEGY_METRO_ONLY = 3;// 只坐地铁

    public NavigationService(TransportGraph graph) {
        this.graph = graph;
    }

    public RouteResult search(String start, String end, int strategy) {
        if (!graph.adjList.containsKey(start) || !graph.adjList.containsKey(end)) return null;

        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));
        Map<String, Integer> minCost = new HashMap<>();

        pq.add(new Node(start, 0, null, null, null));
        minCost.put(start, 0);
        Node finalNode = null;

        while (!pq.isEmpty()) {
            Node curr = pq.poll();
            if (curr.cost > minCost.getOrDefault(curr.station, Integer.MAX_VALUE)) continue;
            if (curr.station.equals(end)) { finalNode = curr; break; }

            if (graph.adjList.get(curr.station) != null) {
                for (Connection edge : graph.adjList.get(curr.station)) {
                    // ★★★ 新增：类型过滤逻辑 ★★★
                    boolean isBusLine = edge.lineName.contains("公交");
                    
                    // 如果是只坐公交，但当前线不是公交 -> 跳过
                    if (strategy == STRATEGY_BUS_ONLY && !isBusLine) continue;
                    
                    // 如果是只坐地铁，但当前线是公交 -> 跳过
                    if (strategy == STRATEGY_METRO_ONLY && isBusLine) continue;

                    // 计算权重
                    boolean transfer = isTransfer(curr, edge);
                    int weight = 0;

                    if (strategy == STRATEGY_TRANSFER) {
                        // 换乘最少策略：换乘代价极大，路程代价极小
                        weight = 1 + (transfer ? 1000 : 0);
                    } else {
                        // 时间最短、只坐公交、只坐地铁：都优先考虑时间
                        // 基础时间 + 换乘罚时
                        weight = edge.timeCost + (transfer ? 5 : 0);
                    }

                    int newCost = curr.cost + weight;
                    if (newCost < minCost.getOrDefault(edge.toStation, Integer.MAX_VALUE)) {
                        minCost.put(edge.toStation, newCost);
                        Node next = new Node(edge.toStation, newCost, edge.lineName, edge, curr);
                        // 记录实际耗时
                        next.realTime = curr.realTime + edge.timeCost + (transfer ? 5 : 0);
                        pq.add(next);
                    }
                }
            }
        }
        return finalNode == null ? null : buildResult(finalNode, strategy);
    }
    
    private boolean isTransfer(Node curr, Connection edge) {
        return curr.arriveLine != null && !curr.arriveLine.equals(edge.lineName);
    }

    private RouteResult buildResult(Node node, int strategy) {
        LinkedList<String> path = new LinkedList<>();
        LinkedList<String> lineSegments = new LinkedList<>();
        LinkedList<String> transferPoints = new LinkedList<>();
        
        // 换乘计数逻辑 (初始为0)
        int transfers = 0;
        
        Node temp = node;
        Node prevNode = null;
        
        while(temp != null) {
            path.addFirst(temp.station);
            if (temp.arriveLine != null) lineSegments.addFirst(temp.arriveLine);
            
            if (prevNode != null && prevNode.arriveLine != null && temp.arriveLine != null && 
                !prevNode.arriveLine.equals(temp.arriveLine)) {
                transfers++; 
                transferPoints.addFirst(temp.station);
            }
            prevNode = temp; temp = temp.parent;
        }
        
        String stName;
        switch (strategy) {
            case STRATEGY_TIME: stName = "时间最短"; break;
            case STRATEGY_TRANSFER: stName = "换乘最少"; break;
            case STRATEGY_BUS_ONLY: stName = "只坐公交"; break;
            case STRATEGY_METRO_ONLY: stName = "只坐地铁"; break;
            default: stName = "自定义";
        }
        
        return new RouteResult(path, lineSegments, node.realTime, transfers, stName, transferPoints);
    }

    public List<String> getDirectLines(String station1, String station2) {
        List<String> directLines = new ArrayList<>();
        if (graph == null || graph.lineStationsMap == null) return directLines;
        for (Map.Entry<String, List<String>> entry : graph.lineStationsMap.entrySet()) {
            List<String> stations = entry.getValue();
            if (stations.contains(station1) && stations.contains(station2)) {
                directLines.add(entry.getKey());
            }
        }
        return directLines;
    }

    static class Node {
        String station;
        int cost;
        int realTime;
        String arriveLine;
        Connection edge;    // 到达当前站点所经过的边
        Node parent;
        public Node(String s, int c, String l, Connection edge, Node p) {
            this.station = s; this.cost = c; this.arriveLine = l; this.edge = edge; this.parent = p;
            this.realTime = (p == null) ? 0 : p.realTime;
        }
    }
}