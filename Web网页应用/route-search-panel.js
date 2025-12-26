// route-search-panel.js - 适配图标渲染的升级版
export class RouteSearchPanel {
    constructor(routePlannerInstance, mapInstance) {
        this.planner = routePlannerInstance;
        this.map = mapInstance;
        this.panelElement = null;
        this.currentMode = 'driving';
        this.selectedPoints = {
            start: null,
            end: null,
            waypoints: []
        };
        this.currentSelectType = null;
        this.currentSelectIndex = -1;
        this.mapClickHandler = this.handleMapClick.bind(this);

        this.initPanel();
        this.bindCustomEvents();
        this.updatePolicyOptions('driving');
    }

    initPanel() {
        this.panelElement = document.createElement('div');
        this.panelElement.className = 'route-controls-panel';
        this.panelElement.innerHTML = `
            <h3><i class="fas fa-route"></i> 路线规划</h3>
            <div class="input-group">
                <label>起点：</label>
                <input type="text" id="route-start" placeholder="输入地名 (如: 南京站)">
                <button class="btn-small" id="btn-set-start"><i class="fas fa-map-marker-alt"></i></button>
                <button class="btn-clear" id="btn-clear-start">×</button>
            </div>
            <div class="input-group">
                <label>终点：</label>
                <input type="text" id="route-end" placeholder="输入地名 (如: 总统府)">
                <button class="btn-small" id="btn-set-end"><i class="fas fa-map-marker-alt"></i></button>
                <button class="btn-clear" id="btn-clear-end">×</button>
            </div>
            <div id="waypoints-container"></div>
            
            <div class="mode-selector" style="flex-wrap: wrap;">
                <label style="width: 100%; margin-bottom: 5px;">出行方式与偏好：</label>
                <select id="travel-mode" style="width: 48%;">
                    <option value="driving">🚗 驾车</option>
                    <option value="transit">🚌 公交</option>
                    <option value="walking">🚶 步行</option>
                    <option value="riding">🚴 骑行</option>
                </select>
                <select id="route-policy" style="width: 48%;">
                </select>
            </div>

            <div class="button-group">
                <button class="btn" id="btn-search-route">开始规划</button>
                <button class="btn secondary" id="btn-clear-route">清除</button>
            </div>

            <div class="route-result" id="route-result" style="display: none;">
                <div class="result-header">
                    <h4>规划结果</h4>
                    <div class="result-summary">
                        <span class="tag-info"><i class="fas fa-road"></i> <span id="result-distance">--</span></span>
                        <span class="tag-info"><i class="fas fa-clock"></i> <span id="result-duration">--</span></span>
                    </div>
                </div>
                <div class="result-steps-container">
                    <ul id="route-steps-list" class="steps-list"></ul>
                </div>
            </div>
        `;

        const sidebar = document.querySelector('.floating-sidebar') || document.querySelector('.sidebar');
        if (sidebar) {
            sidebar.insertBefore(this.panelElement, sidebar.firstChild);
        }
        this.bindPanelEvents();
    }

    bindPanelEvents() {
        document.getElementById('btn-set-start').addEventListener('click', () => this.setSelectMode('start'));
        document.getElementById('btn-set-end').addEventListener('click', () => this.setSelectMode('end'));
        document.getElementById('btn-clear-start').addEventListener('click', () => this.clearPoint('start'));
        document.getElementById('btn-clear-end').addEventListener('click', () => this.clearPoint('end'));
        
        document.getElementById('travel-mode').addEventListener('change', (e) => {
            this.currentMode = e.target.value;
            this.updatePolicyOptions(this.currentMode);
        });

        document.getElementById('btn-search-route').addEventListener('click', () => this.executeSearch());
        document.getElementById('btn-clear-route').addEventListener('click', () => this.clearAll());
    }
    
    updatePolicyOptions(mode) {
        const policySelect = document.getElementById('route-policy');
        policySelect.innerHTML = '';
        
        let options = [];
        if (mode === 'driving') {
            options = [
                { val: 'BMAP_DRIVING_POLICY_DEFAULT', text: '默认策略' },
                { val: 'BMAP_DRIVING_POLICY_FIRST_HIGHWAYS', text: '优先高速' },
                { val: 'BMAP_DRIVING_POLICY_AVOID_HIGHWAYS', text: '避开高速' },
                { val: 'BMAP_DRIVING_POLICY_AVOID_CONGESTION', text: '避开拥堵' }
            ];
            policySelect.disabled = false;
        } else if (mode === 'transit') {
            options = [
                { val: 'BMAP_TRANSIT_POLICY_RECOMMEND', text: '推荐方案' },
                { val: 'BMAP_TRANSIT_POLICY_LEAST_TIME', text: '时间最短' },
                { val: 'BMAP_TRANSIT_POLICY_LEAST_TRANSFER', text: '少换乘' },
                { val: 'BMAP_TRANSIT_POLICY_LEAST_WALKING', text: '少步行' },
                { val: 'BMAP_TRANSIT_POLICY_AVOID_SUBWAYS', text: '不乘地铁' }
            ];
            policySelect.disabled = false;
        } else {
            options = [{ val: '', text: '标准路线' }];
            policySelect.disabled = true;
        }
        
        options.forEach(opt => {
            const el = document.createElement('option');
            el.value = opt.val;
            el.textContent = opt.text;
            policySelect.appendChild(el);
        });
    }

    bindCustomEvents() {
        document.addEventListener('routePlanComplete', (e) => this.displayRouteResults(e.detail));
        document.addEventListener('routePlanError', (e) => alert(e.detail.message));
    }

    setSelectMode(type, index = -1) {
        this.currentSelectType = type;
        this.currentSelectIndex = index;
        this.map.setDefaultCursor('crosshair');
        alert(`请在地图上点击选择【${type === 'start' ? '起点' : '终点'}】`);
        this.map.addEventListener('click', this.mapClickHandler);
    }

    handleMapClick(e) {
        if (!this.currentSelectType) return;
        const latlng = e.latlng;
        
        if (this.currentSelectType === 'start') this.selectedPoints.start = latlng;
        if (this.currentSelectType === 'end') this.selectedPoints.end = latlng;
        
        const inputId = this.currentSelectType === 'start' ? 'route-start' : 'route-end';
        const input = document.getElementById(inputId);
        input.value = `${latlng.lng.toFixed(4)}, ${latlng.lat.toFixed(4)}`;
        
        this.map.removeEventListener('click', this.mapClickHandler);
        this.map.setDefaultCursor('default');
        this.currentSelectType = null;
    }

    async executeSearch() {
        const startInput = document.getElementById('route-start').value;
        const endInput = document.getElementById('route-end').value;

        if (!startInput || !endInput) {
            alert('请输入或选择起点和终点！');
            return;
        }

        const start = await this.resolveLocation(startInput, 'start');
        const end = await this.resolveLocation(endInput, 'end');

        if (!start || !end) {
            alert('无法解析地址，请检查输入或使用地图选点');
            return;
        }

        const policyVal = document.getElementById('route-policy').value;
        const params = {
            start: start,
            end: end,
            travelMode: this.currentMode,
            policy: policyVal
        };

        this.planner.search(params);
    }

    resolveLocation(input, type) {
        return new Promise((resolve) => {
            if (this.selectedPoints[type] && input.includes(',')) {
                resolve(this.selectedPoints[type]);
                return;
            }
            const myGeo = new BMapGL.Geocoder();
            myGeo.getPoint(input, (point) => {
                if (point) resolve(point);
                else resolve(null);
            }, '南京市');
        });
    }

    displayRouteResults(details) {
        const resultDiv = document.getElementById('route-result');
        document.getElementById('result-distance').textContent = details.distance;
        document.getElementById('result-duration').textContent = details.duration;
        
        const listContainer = document.getElementById('route-steps-list');
        listContainer.innerHTML = '';

        if (details.steps && details.steps.length > 0) {
            details.steps.forEach((step, index) => {
                const li = document.createElement('li');
                li.className = 'step-item';
                
                // --- 核心修改：动态图标逻辑 ---
                let iconHtml = '';
                let iconClass = '';
                
                if (step.type === 'walk') {
                    iconHtml = '<i class="fas fa-walking"></i>';
                    iconClass = 'step-icon-walk';
                } else if (step.type === 'bus' || step.type === 'subway' || step.type === 'transit') {
                    iconHtml = '<i class="fas fa-bus"></i>';
                    iconClass = 'step-icon-transit';
                } else {
                    iconHtml = index + 1; // 驾车/默认情况显示数字
                    iconClass = 'step-icon-normal';
                }
                
                li.innerHTML = `
                    <div class="step-icon ${iconClass}">${iconHtml}</div>
                    <div class="step-content">
                        <div class="step-text">${step.instruction}</div>
                        <div class="step-meta" ${step.distance ? '' : 'style="display:none"'}>${step.distance}</div>
                    </div>
                `;
                listContainer.appendChild(li);
            });
        } else {
            listContainer.innerHTML = '<li class="step-item" style="color:#999; justify-content:center;">暂无详细路书信息</li>';
        }

        resultDiv.style.display = 'block';
    }

    clearPoint(type) {
        this.selectedPoints[type] = null;
        document.getElementById(`route-${type}`).value = '';
    }

    clearAll() {
        this.planner.clearCurrentRoute();
        this.selectedPoints = { start: null, end: null, waypoints: [] };
        document.getElementById('route-start').value = '';
        document.getElementById('route-end').value = '';
        document.getElementById('route-result').style.display = 'none';
        document.getElementById('route-steps-list').innerHTML = '';
    }
}