/**
 * 异步加载百度地图API
 * @param {Function} callback - 加载完成后的回调函数
 */
export function loadBaiduMapAPI(callback) {
    if (window.BMapGL) {
        callback();
        return;
    }

    const script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = `//api.map.baidu.com/api?type=webgl&v=1.0&ak=rYLd21LtZx3csKPshxntAeUdlvPMhZ3m&callback=baiduMapLoaded`;
    
    window.baiduMapLoaded = callback;
    document.head.appendChild(script);
}

/**
 * 创建自定义信息窗口内容
 * @param {Object} data - 标记点数据
 * @returns {string} HTML字符串
 */
export function createInfoWindowContent(data) {
    return `
        <div class="info-window">
            <h4>${data.name}</h4>
            <p class="category">${data.category}</p>
            <p class="description">${data.description}</p>
            <div class="info-footer">
                <span>经度: ${data.position.lng.toFixed(6)}</span>
                <span>纬度: ${data.position.lat.toFixed(6)}</span>
            </div>
        </div>
    `;
}

/**
 * 创建自定义控件
 * @param {string} text - 控件文本
 * @param {Function} onClick - 点击回调函数
 * @param {Object} options - 配置选项
 * @returns {BMapGL.Control} 自定义控件实例
 */
export function createCustomControl(text, onClick, options = {}) {
    const { 
        anchor = BMAP_ANCHOR_TOP_LEFT, 
        offset = { x: 10, y: 10 },
        className = 'custom-control'
    } = options;

    // 定义控件类
    function CustomControl() {
        this.defaultAnchor = anchor;
        this.defaultOffset = new BMapGL.Size(offset.x, offset.y);
    }

    // 继承BMapGL.Control
    CustomControl.prototype = new BMapGL.Control();

    // 实现initialize方法
    CustomControl.prototype.initialize = function(map) {
        const div = document.createElement('div');
        div.className = className;
        div.innerHTML = text;
        
        // 样式设置
        Object.assign(div.style, {
            cursor: 'pointer',
            padding: '8px 12px',
            backgroundColor: 'white',
            borderRadius: '4px',
            boxShadow: '0 2px 6px 0 rgba(27, 142, 236, 0.5)',
            fontSize: '14px',
            color: '#333',
            userSelect: 'none'
        });

        // 点击事件
        div.onclick = onClick;
        div.onmouseover = () => {
            div.style.backgroundColor = '#f5f5f5';
        };
        div.onmouseout = () => {
            div.style.backgroundColor = 'white';
        };

        map.getContainer().appendChild(div);
        return div;
    };

    return new CustomControl();
}

/**
 * 格式化坐标显示
 * @param {Object} point - 坐标点
 * @returns {string} 格式化后的字符串
 */
export function formatCoordinates(point) {
    return `(${point.lng.toFixed(6)}, ${point.lat.toFixed(6)})`;
}

/**
 * 计算两点间距离（简化版）
 * @param {Object} point1 - 点1
 * @param {Object} point2 - 点2
 * @returns {number} 距离（公里）
 */
export function calculateDistance(point1, point2) {
    const R = 6371; // 地球半径（公里）
    const dLat = (point2.lat - point1.lat) * Math.PI / 180;
    const dLng = (point2.lng - point1.lng) * Math.PI / 180;
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
              Math.cos(point1.lat * Math.PI / 180) * Math.cos(point2.lat * Math.PI / 180) *
              Math.sin(dLng/2) * Math.sin(dLng/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
}