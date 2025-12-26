// subway-manager.js - 修复版 (已解决滚轮穿透问题)

export class SubwayManager {
    constructor(ak) {
        this.ak = ak;
        this.subway = null;
        
        this.routePlanner = null;   
        this.lineHighlighter = null;
        
        this.container = document.getElementById('subway-container');
        this.uiContainer = null;
        
        this.isScriptLoaded = false;
        this.isMapInitialized = false;
        this.currentZoom = 0.5;
        this.linesData = []; 
    }
  
    preload() {
        if (window.BMapSub || document.getElementById('bmap-sub-script')) {
            this.isScriptLoaded = true;
            return;
        }
        console.log('开始预加载地铁API...');
        window.subwayApiLoaded = () => {
            console.log('地铁API资源下载完毕');
            this.isScriptLoaded = true;
        };
        const script = document.createElement('script');
        script.id = 'bmap-sub-script';
        script.type = 'text/javascript';
        script.src = `https://api.map.baidu.com/api?type=subway&v=1.0&ak=${this.ak}&callback=subwayApiLoaded`;
        document.head.appendChild(script);
    }
  
    toggle(show) {
        if (show) {
            this.container.style.zIndex = '100';
            this.container.style.opacity = '1';
            if (!this.isMapInitialized) this.showLoading(true);
            setTimeout(() => this.tryInitMap(), 100);
        } else {
            this.container.style.zIndex = '-1';
            this.container.style.opacity = '0';
        }
    }
  
    tryInitMap() {
        if (this.isMapInitialized && this.subway) {
            this.showLoading(false);
            return;
        }
        if (!this.isScriptLoaded && !window.BMapSub) {
            setTimeout(() => this.tryInitMap(), 200);
            return;
        }
        this.renderMap();
    }
  
    renderMap() {
        try {
            const list = BMapSub.SubwayCitiesList;
            const city = list.find(c => c.name === '南京');
            if (!city) throw new Error('未找到南京地铁数据');
  
            this.subway = new BMapSub.Subway('subway-container', city.citycode);
            this.container.style.backgroundColor = '#ffffff';
            this.subway.setZoom(this.currentZoom);
  
            this.subway.addEventListener('subwayloaded', () => {
                this.isMapInitialized = true;
                this.showLoading(false);
                this.linesData = this.subway.getLines(); 
                this.initComponents();
                this.addMouseWheelSupport();
            });
  
            const zoomControl = new BMapSub.ZoomControl({
                anchor: BMapSub.ANCHOR_BOTTOM_RIGHT,
                offset: new BMapSub.Size(20, 20)
            });
            this.subway.addControl(zoomControl);
  
        } catch (error) {
            console.error(error);
            this.showLoading(false);
        }
    }
  
    addMouseWheelSupport() {
        // 这是给整个地图容器加的缩放逻辑
        this.container.addEventListener('wheel', (e) => {
            e.preventDefault(); // 注意：这里禁止了默认滚动
            if (this._zooming) return;
            this._zooming = true;
            setTimeout(() => { this._zooming = false; }, 50);
            const delta = e.deltaY > 0 ? -0.1 : 0.1;
            let newZoom = this.currentZoom + delta;
            newZoom = Math.max(0.3, Math.min(newZoom, 1.0));
            this.currentZoom = newZoom;
            this.subway.setZoom(newZoom);
        }, { passive: false });
    }
  
    initComponents() {
        if (this.uiContainer) return;
        this.createSubwayUI();
  
        this.routePlanner = new BMapSub.Direction(this.subway, {
            renderOptions: {
                panel: "subway-result-panel",
                autoViewport: true
            }
        });

        this.lineHighlighter = new BMapSub.Direction(this.subway); 
        
        this.subway.addEventListener('tap', (e) => {
            if (e.station) {
                this.handleStationTap(e.station.name);
            }
        });
    }
  
    createSubwayUI() {
        this.uiContainer = document.createElement('div');
        this.uiContainer.className = 'subway-ui-panel';
        
        // --- 核心修复：全方位阻止事件穿透 (新增 'wheel') ---
        const stopEventList = [
            'click', 'dblclick', 'mousedown', 'mouseup', 
            'touchstart', 'touchend', 'touchmove', 'pointerdown', 
            'wheel' // <--- 新增这一项，允许 UI 内部滚动
        ];
        
        stopEventList.forEach(eventType => {
            this.uiContainer.addEventListener(eventType, (e) => {
                e.stopPropagation(); // 阻止事件冒泡到 container
            }, { passive: false });
        });
        
        this.uiContainer.innerHTML = `
            <div class="subway-tabs">
                <button class="subway-tab-btn active" data-tab="search">路径查询</button>
                <button class="subway-tab-btn" data-tab="lines">线路详情</button>
            </div>

            <div id="tab-content-search" class="tab-content active">
                <div class="subway-search-box" style="border-bottom:none; padding-top:15px;">
                    <div class="subway-inputs">
                        <input type="text" id="sub-start" placeholder="起点 (如: 南京南站)">
                        <input type="text" id="sub-end" placeholder="终点 (如: 新街口)">
                    </div>
                    <div class="subway-actions">
                        <button class="btn" id="btn-sub-route">查询路线</button>
                        <button class="btn secondary" id="btn-sub-clear">重置</button>
                    </div>
                </div>
            </div>

            <div id="tab-content-lines" class="tab-content">
                <div style="padding: 15px;">
                    <select id="subway-line-select" class="line-select">
                        <option value="">-- 请选择地铁线路 --</option>
                    </select>
                    
                    <div class="line-details-container" id="line-detail-info">
                        <div style="text-align:center; color:#999; font-size:12px; margin-top:20px;">
                            请选择线路以查看站点列表
                        </div>
                    </div>
                </div>
            </div>
    
            <div id="subway-result-panel"></div>
    
            <div id="station-info-card" class="station-info-card" style="display:none;">
                <h4>
                    <span id="card-station-name">站点名称</span>
                    <button class="close-card-btn" id="close-card">×</button>
                </h4>
                <div class="station-tags">
                    <span class="tag">地铁站</span>
                </div>
                <div class="modal-btns">
                    <button class="btn-xs" id="set-as-start">设为起点</button>
                    <button class="btn-xs" id="set-as-end">设为终点</button>
                </div>
            </div>
        `;

        this.container.appendChild(this.uiContainer);
        this.renderLineOptions();
        this.bindEvents();
    }

    renderLineOptions() {
        const select = document.getElementById('subway-line-select');
        if (!select || !this.linesData) return;
        select.innerHTML = '<option value="">-- 请选择地铁线路 --</option>';
        this.linesData.forEach(line => {
            const option = document.createElement('option');
            option.value = line.name; 
            option.textContent = line.name; 
            select.appendChild(option);
        });
    }
  
    bindEvents() {
        const tabs = this.uiContainer.querySelectorAll('.subway-tab-btn');
        tabs.forEach(tab => {
            tab.addEventListener('click', (e) => {
                tabs.forEach(t => t.classList.remove('active'));
                this.uiContainer.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
                e.target.classList.add('active');
                const targetId = `tab-content-${e.target.dataset.tab}`;
                document.getElementById(targetId).classList.add('active');
            });
        });

        const lineSelect = document.getElementById('subway-line-select');
        if (lineSelect) {
            lineSelect.addEventListener('change', (e) => {
                const lineName = e.target.value;
                if (!lineName) return;

                this.clearDirection(this.routePlanner);
                document.getElementById('subway-result-panel').innerHTML = ''; 

                try { this.subway.closeInfoWindow(); } catch(err) {}

                const lineData = this.linesData.find(l => l.name === lineName);
                if (lineData && lineData.stations && lineData.stations.length > 0) {
                    this.renderStationList(lineData);

                    const startStation = lineData.stations[0].name;
                    const endStation = lineData.stations[lineData.stations.length - 1].name;
                    this.lineHighlighter.search(startStation, endStation);
                }
            });
        }

        document.getElementById('btn-sub-route').addEventListener('click', () => {
            const start = document.getElementById('sub-start').value.trim();
            const end = document.getElementById('sub-end').value.trim();
            
            this.clearDirection(this.lineHighlighter);
            if(lineSelect) lineSelect.value = ''; 
            
            if(start && end && this.routePlanner) {
                document.getElementById('station-info-card').style.display = 'none';
                this.routePlanner.search(start, end);
            }
        });
  
        document.getElementById('btn-sub-clear').addEventListener('click', () => {
            this.clearDirection(this.routePlanner);
            this.clearDirection(this.lineHighlighter);
            
            this.subway.setCenter('新街口'); 
            this.subway.setZoom(0.5);
            
            document.getElementById('sub-start').value = '';
            document.getElementById('sub-end').value = '';
            if(lineSelect) lineSelect.value = '';

            document.getElementById('station-info-card').style.display = 'none';
            document.getElementById('subway-result-panel').innerHTML = ''; 
            document.getElementById('line-detail-info').innerHTML = 
                '<div style="text-align:center; color:#999; font-size:12px; margin-top:20px;">请选择线路以查看站点列表</div>';
            
            try { this.subway.closeInfoWindow(); } catch(err) {}
        });
  
        document.getElementById('close-card').addEventListener('click', () => {
            document.getElementById('station-info-card').style.display = 'none';
        });
  
        document.getElementById('set-as-start').addEventListener('click', () => {
            const name = document.getElementById('card-station-name').innerText;
            document.getElementById('sub-start').value = name;
            tabs[0].click(); 
            document.getElementById('station-info-card').style.display = 'none';
        });
  
        document.getElementById('set-as-end').addEventListener('click', () => {
            const name = document.getElementById('card-station-name').innerText;
            document.getElementById('sub-end').value = name;
            tabs[0].click();
            document.getElementById('station-info-card').style.display = 'none';
        });
    }

    clearDirection(drctInstance) {
        if (!drctInstance) return;
        try {
            if (typeof drctInstance.clear === 'function') {
                drctInstance.clear();
            } else if (typeof drctInstance.clearResults === 'function') {
                drctInstance.clearResults();
            }
        } catch (e) {
            console.warn('Direction clear failed:', e);
        }
    }

    renderStationList(lineData) {
        const container = document.getElementById('line-detail-info');
        if (!container) return;
        
        let stationsHtml = '';
        if (lineData.stations && lineData.stations.length > 0) {
            stationsHtml = lineData.stations.map(station => `
                <div class="station-list-item" data-name="${station.name}">
                    <div class="station-dot"></div>
                    <span>${station.name}</span>
                </div>
            `).join('');
        }

        container.innerHTML = `
            <div class="line-header-badge">
                <div class="line-color-dot" style="background-color: #4facfe"></div>
                <span class="line-name-title">${lineData.name}</span>
            </div>
            <div style="font-size:12px; color:#999; margin-bottom:10px; padding:0 5px;">
                起止：${lineData.stations[0].name} <i class="fas fa-arrow-right"></i> ${lineData.stations[lineData.stations.length-1].name}
            </div>
            <div style="max-height: 250px; overflow-y: auto;">
                ${stationsHtml}
            </div>
        `;

        const items = container.querySelectorAll('.station-list-item');
        items.forEach(item => {
            item.addEventListener('click', (e) => {
                const stationName = item.dataset.name;
                this.handleStationTap(stationName);
            });
        });
    }
  
    handleStationTap(name) {
        const card = document.getElementById('station-info-card');
        document.getElementById('card-station-name').innerText = name;
        card.style.display = 'block';
  
        const detail = new BMapSub.DetailInfo(this.subway);
        detail.search(name);
        this.subway.setCenter(name);
    }
  
    showLoading(show) {
        let loader = document.getElementById('subway-loader');
        if (!loader && show) {
            loader = document.createElement('div');
            loader.id = 'subway-loader';
            Object.assign(loader.style, {
                position: 'absolute', top: 0, left: 0, width: '100%', height: '100%',
                background: 'rgba(255,255,255,0.95)',
                display: 'flex', justifyContent: 'center', alignItems: 'center',
                color: '#66a6ff', fontSize: '16px', zIndex: '2000'
            });
            loader.innerHTML = '<i class="fas fa-spinner fa-spin" style="margin-right:10px;"></i> 加载中...';
            this.container.appendChild(loader);
        }
        if (loader) loader.style.display = show ? 'flex' : 'none';
    }
}