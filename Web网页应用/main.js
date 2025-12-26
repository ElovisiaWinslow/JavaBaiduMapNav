import { NANJING_CONFIG, MAP_STYLES, BAIDU_MAP_CONFIG } from './config.js';
import { loadBaiduMapAPI, formatCoordinates } from './utils.js';
import { MapControls } from './controls.js';
import { RoutePlanner } from './route-planner.js';
import { RouteSearchPanel } from './route-search-panel.js';
// 导入地铁管理器 (确保已创建该文件)
import { SubwayManager } from './subway-manager.js';

class NanjingMapApp {
    constructor() {
        this.map = null;
        this.controls = null;
        this.routePlanner = null;
        this.routePanel = null;
        // 地铁相关属性
        this.subwayManager = null;
        this.isSubwayMode = false;
        
        this.isSidebarCollapsed = false;
        this.init();
    }

    async init() {
        try {
            // 加载百度地图 GL API
            await new Promise((resolve, reject) => {
                loadBaiduMapAPI(resolve);
                setTimeout(() => {
                    if (!window.BMapGL) reject(new Error('百度地图API加载超时'));
                }, 10000);
            });

            this.initMap();
            
            // --- 修改点开始 ---
            // 1. 实例化管理器
            this.subwayManager = new SubwayManager(BAIDU_MAP_CONFIG.AK);
            // 2. 立即开始预加载！不要等点击！
            this.subwayManager.preload();
            // --- 修改点结束 ---

            this.initUI();
            this.initRoutePlanning(); 
            this.addControls();
            
            console.log('南京智慧出行应用启动成功');
            
        } catch (error) {
            console.error('初始化失败:', error);
            // 避免在非严重错误时弹窗打扰用户
            if (!window.BMapGL) alert('地图核心服务加载失败，请检查网络');
        }
    }

    initMap() {
        this.map = new BMapGL.Map('map-container', {
            minZoom: NANJING_CONFIG.MIN_ZOOM,
            maxZoom: NANJING_CONFIG.MAX_ZOOM,
            restrictCenter: false,
            enableAutoResize: true
        });

        const centerPoint = new BMapGL.Point(
            NANJING_CONFIG.CENTER.lng,
            NANJING_CONFIG.CENTER.lat
        );
        this.map.centerAndZoom(centerPoint, NANJING_CONFIG.DEFAULT_ZOOM);
        this.map.enableScrollWheelZoom(true);
        this.map.setTilt(50); // 默认开启3D视角
        this.map.setDisplayOptions(MAP_STYLES.DEFAULT.displayOptions);
    }

    initUI() {
        // 1. 绑定缩放滑块
        const zoomSlider = document.getElementById('zoom-slider');
        const zoomValue = document.getElementById('zoom-value');
        
        if (zoomSlider && zoomValue) {
            zoomSlider.value = this.map.getZoom();
            zoomValue.textContent = this.map.getZoom();

            this.map.addEventListener('zoomend', () => {
                const z = this.map.getZoom();
                zoomSlider.value = z;
                zoomValue.textContent = z;
            });

            zoomSlider.addEventListener('input', (e) => {
                const val = parseInt(e.target.value);
                zoomValue.textContent = val;
                this.map.setZoom(val);
            });
        }

        // 2. 侧边栏折叠逻辑
        const toggleBtn = document.getElementById('toggle-sidebar');
        const sidebar = document.getElementById('sidebar-card');
        
        if (toggleBtn && sidebar) {
            const icon = toggleBtn.querySelector('i');
            toggleBtn.addEventListener('click', () => {
                this.isSidebarCollapsed = !this.isSidebarCollapsed;
                sidebar.classList.toggle('collapsed');
                
                if (this.isSidebarCollapsed) {
                    icon.className = 'fas fa-chevron-right';
                } else {
                    icon.className = 'fas fa-chevron-left';
                }
            });
        }

        // 3. 定位按钮
        const locateBtn = document.getElementById('locate-nanjing');
        if (locateBtn) {
            locateBtn.addEventListener('click', () => {
                // 如果在地铁模式下点击定位，建议先切回地图模式
                if (this.isSubwayMode) {
                    this.toggleSubwayMode(document.getElementById('btn-subway-mode'));
                }
                this.map.flyTo(new BMapGL.Point(
                    NANJING_CONFIG.CENTER.lng,
                    NANJING_CONFIG.CENTER.lat
                ), 13);
            });
        }
        
        // 4. 样式切换按钮
        const styleBtn = document.getElementById('style-toggle-placeholder');
        if (styleBtn) {
            styleBtn.addEventListener('click', () => this.toggleMapStyle());
        }

        // 5. 地铁视图切换按钮 (新增)
        const subwayBtn = document.getElementById('btn-subway-mode');
        if (subwayBtn) {
            subwayBtn.addEventListener('click', () => {
                this.toggleSubwayMode(subwayBtn);
            });
        }
    }

    // 新增：切换地铁模式
    toggleSubwayMode(btn) {
        this.isSubwayMode = !this.isSubwayMode;
        
        if (this.isSubwayMode) {
            // 进入地铁模式
            btn.classList.add('active');
            btn.innerHTML = '<i class="fas fa-map"></i> 地图'; // 按钮变名为"地图"
            // 调用管理器显示地铁图
            this.subwayManager.toggle(true);
            
            // 可选：隐藏地图独有的控件以避免混淆
            const zoomControl = document.getElementById('zoom-slider');
            if (zoomControl) zoomControl.parentElement.parentElement.style.opacity = '0.3';
            
        } else {
            // 返回地图模式
            btn.classList.remove('active');
            btn.innerHTML = '<i class="fas fa-subway"></i> 地铁';
            this.subwayManager.toggle(false);
            
            const zoomControl = document.getElementById('zoom-slider');
            if (zoomControl) zoomControl.parentElement.parentElement.style.opacity = '1';
        }
    }

    addControls() {
        this.controls = new MapControls(this.map);
        this.controls.addScaleControl().addZoomControl().add3DControl();
    }
    
    toggleMapStyle() {
        const isNight = document.body.classList.toggle('night-mode');
        if (isNight) {
            // 夜晚模式逻辑
            this.map.setDisplayOptions(MAP_STYLES.SIMPLE.displayOptions);
            this.map.setMapStyleV2({     
                styleId: '46b9df205df4f66c9f65d6447d79b76e' // 示例深色样式ID，可替换
            });
        } else {
            // 恢复默认
            this.map.setMapStyleV2({ styleJson: [] });
            this.map.setDisplayOptions(MAP_STYLES.DEFAULT.displayOptions);
        }
    }

    initRoutePlanning() {
        this.routePlanner = new RoutePlanner(this.map);
        this.routePanel = new RouteSearchPanel(this.routePlanner, this.map);
    }
}

// 启动
window.addEventListener('DOMContentLoaded', () => {
    new NanjingMapApp();
});

export { NanjingMapApp };