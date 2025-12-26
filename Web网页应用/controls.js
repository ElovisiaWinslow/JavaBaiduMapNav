import { createCustomControl } from './utils.js';

/**
 * 地图控件管理器
 */
export class MapControls {
    constructor(map) {
        this.map = map;
        this.controls = {};
    }

    /**
     * 添加比例尺控件
     */
    addScaleControl() {
        const scaleCtrl = new BMapGL.ScaleControl({
            anchor: BMAP_ANCHOR_BOTTOM_LEFT
        });
        this.map.addControl(scaleCtrl);
        this.controls.scale = scaleCtrl;
        return this;
    }

    /**
     * 添加缩放控件
     */
    addZoomControl() {
        const zoomCtrl = new BMapGL.ZoomControl({
            anchor: BMAP_ANCHOR_TOP_RIGHT
        });
        this.map.addControl(zoomCtrl);
        this.controls.zoom = zoomCtrl;
        return this;
    }

    /**
     * 添加3D控件
     */
    add3DControl() {
        const navi3DCtrl = new BMapGL.NavigationControl3D({
            anchor: BMAP_ANCHOR_TOP_RIGHT,
            offset: new BMapGL.Size(20, 80)
        });
        this.map.addControl(navi3DCtrl);
        this.controls.navigation3D = navi3DCtrl;
        return this;
    }

    /**
     * 添加定位控件
     */
    addLocationControl() {
        const locationCtrl = new BMapGL.LocationControl({
            anchor: BMAP_ANCHOR_BOTTOM_RIGHT,
            offset: new BMapGL.Size(20, 20)
        });
        
        locationCtrl.addEventListener('locationSuccess', (e) => {
            const address = e.addressComponent;
            const fullAddress = `${address.province}${address.city}${address.district}${address.street}${address.streetNumber}`;
            alert(`当前位置：${fullAddress}`);
        });
        
        locationCtrl.addEventListener('locationError', (e) => {
            alert(`定位失败：${e.message}`);
        });
        
        this.map.addControl(locationCtrl);
        this.controls.location = locationCtrl;
        return this;
    }

    /**
     * 添加城市列表控件
     */
    addCityListControl() {
        const cityControl = new BMapGL.CityListControl({
            anchor: BMAP_ANCHOR_TOP_LEFT,
            offset: new BMapGL.Size(10, 5)
        });
        this.map.addControl(cityControl);
        this.controls.cityList = cityControl;
        return this;
    }

    /**
     * 添加自定义控件 - 返回南京市
     */
    addReturnToNanjingControl() {
        const control = createCustomControl(
            '📍 返回南京市',
            () => {
                this.map.setCenter(new BMapGL.Point(118.778074, 32.057236));
                this.map.setZoom(12);
            },
            { 
                anchor: BMAP_ANCHOR_TOP_LEFT,
                offset: { x: 100, y: 10 },
                className: 'return-nanjing-control'
            }
        );
        
        this.map.addControl(control);
        this.controls.returnToNanjing = control;
        return this;
    }

    /**
     * 添加自定义控件 - 切换地图样式
     */
    addStyleToggleControl() {
        let isSimpleStyle = false;
        
        const control = createCustomControl(
            '🎨 简洁模式',
            () => {
                if (isSimpleStyle) {
                    this.map.setDisplayOptions({
                        poi: true,
                        poiText: true,
                        poiIcon: true,
                        building: true
                    });
                    control.innerHTML = '🎨 简洁模式';
                } else {
                    this.map.setDisplayOptions({
                        poi: false,
                        poiText: false,
                        poiIcon: false,
                        building: false
                    });
                    control.innerHTML = '🎨 恢复默认';
                }
                isSimpleStyle = !isSimpleStyle;
            },
            {
                anchor: BMAP_ANCHOR_TOP_LEFT,
                offset: { x: 210, y: 10 },
                className: 'style-toggle-control'
            }
        );
        
        this.map.addControl(control);
        this.controls.styleToggle = control;
        return this;
    }

    /**
     * 添加所有控件
     */
    addAllControls() {
        return this
            .addScaleControl()
            .addZoomControl()
            .add3DControl()
            .addLocationControl()
            .addCityListControl()
            .addReturnToNanjingControl()
            .addStyleToggleControl();
    }
}