// route-planner.js - 支持公交换乘详情解析版
export class RoutePlanner {
    constructor(mapInstance) {
        this.map = mapInstance;
        this.routeService = null;
        this.currentMode = null;
    }

    /**
     * 执行搜索
     * @param {Object} params {start, end, travelMode, policy}
     */
    search(params) {
        this.clearCurrentRoute();
        this.currentMode = params.travelMode;

        let ServiceClass;
        let options = {
            renderOptions: { 
                map: this.map, 
                autoViewport: true,
                enableDragging: true 
            },
            onSearchComplete: (results) => this.handleSearchResults(results)
        };
        
        let policy = null;
        if (params.policy && window[params.policy] !== undefined) {
            policy = window[params.policy];
        }

        switch (params.travelMode) {
            case 'transit':
                ServiceClass = BMapGL.TransitRoute;
                if (policy !== null) options.policy = policy;
                break;
            case 'walking':
                ServiceClass = BMapGL.WalkingRoute;
                break;
            case 'riding':
                ServiceClass = BMapGL.RidingRoute;
                break;
            case 'driving':
            default:
                ServiceClass = BMapGL.DrivingRoute;
                if (policy !== null) options.policy = policy;
                break;
        }

        this.routeService = new ServiceClass(this.map, options);

        if ((this.currentMode === 'driving' || this.currentMode === 'transit') && policy !== null) {
            this.routeService.setPolicy(policy);
        }

        const startPoint = new BMapGL.Point(params.start.lng, params.start.lat);
        const endPoint = new BMapGL.Point(params.end.lng, params.end.lat);
        
        this.routeService.search(startPoint, endPoint);
    }

    handleSearchResults(results) {
        if (this.routeService.getStatus() !== BMAP_STATUS_SUCCESS) {
            const event = new CustomEvent('routePlanError', { 
                detail: { message: '未找到合适的路线，请尝试调整起点/终点或策略' } 
            });
            document.dispatchEvent(event);
            return;
        }

        let plan = results.getPlan(0);
        let distance = plan.getDistance(true);
        let duration = plan.getDuration(true);
        let steps = [];

        // --- 核心修改：针对公交模式的特殊解析 ---
        if (this.currentMode === 'transit') {
            // 公交规划通常由 N 个 Line (车) 和 N+1 个 Route (步行) 组成
            // 顺序通常是：Walk(0) -> Line(0) -> Walk(1) -> Line(1) ...
            
            const numLines = plan.getNumLines();
            // 遍历所有乘车路段
            for (let i = 0; i < numLines; i++) {
                // 1. 获取这一段乘车前的步行 (前往站点或换乘)
                const walk = plan.getRoute(i);
                const line = plan.getLine(i);
                
                if (walk.getDistance(false) > 0) {
                    steps.push({
                        instruction: `步行 ${walk.getDistance(true)} 到达 <b>${line.getGetOnStop().title}</b>`,
                        distance: walk.getDistance(true),
                        type: 'walk' // 标记类型，方便UI显示不同图标
                    });
                }

                // 2. 获取乘车线路信息
                steps.push({
                    instruction: `乘坐 <b>${line.title}</b> <br><span class="sub-info">(${line.getGetOnStop().title} 上车 - ${line.getGetOffStop().title} 下车，途经 ${line.getNumViaStops()} 站)</span>`,
                    distance: line.getDistance(true),
                    type: line.type === 1 ? 'subway' : 'bus' // 简单区分地铁和公交 (1通常为地铁)
                });
            }

            // 3. 处理最后一段步行 (从末站到终点)
            const numRoutes = plan.getNumRoutes();
            if (numRoutes > numLines) {
                const lastWalk = plan.getRoute(numLines);
                if (lastWalk.getDistance(false) > 0) {
                    steps.push({
                        instruction: `步行 ${lastWalk.getDistance(true)} 到达终点`,
                        distance: lastWalk.getDistance(true),
                        type: 'walk'
                    });
                }
            }

        } else {
            // --- 驾车/步行/骑行的原有逻辑 ---
            if (plan.getRoute) {
                const route = plan.getRoute(0);
                for (let i = 0; i < route.getNumSteps(); i++) {
                    const step = route.getStep(i);
                    let desc = step.getDescription(false).replace(/<[^>]+>/g, "");
                    steps.push({
                        instruction: desc,
                        distance: step.getDistance(true),
                        type: 'normal'
                    });
                }
            }
        }

        const event = new CustomEvent('routePlanComplete', {
            detail: {
                distance: distance,
                duration: duration,
                steps: steps,
                originalPlan: plan
            }
        });
        document.dispatchEvent(event);
    }

    clearCurrentRoute() {
        if (this.routeService) {
            this.routeService.clearResults();
            this.routeService = null;
        }
        this.map.clearOverlays();
    }
}