fetch('logs.json')
.then(response => response.json())
.then(logs => {
    const logLevels = logs.reduce((acc, log) => {
        acc[log.log_level] = (acc[log.log_level] || 0) + 1;
        return acc;
    }, {});

    const chartData = Object.entries(logLevels).map(([level, count]) => ({
        value: count,
        name: level
    }));

    const chart = echarts.init(document.getElementById('logLevelsPie'));
    chart.setOption({
        title: { text: 'Log Levels', left: 'center' },
        tooltip: { trigger: 'item' },
        series: [{
            type: 'pie',
            radius: '50%',
            data: chartData,
            itemStyle: {
                color: params => {
                    switch (params.name) {
                        case 'ERROR': return '#ff4d4f';
                        case 'WARN': return '#ffcc00';
                        case 'INFO': return '#4caf50';
                        default: return '#c4c4c4';
                    }
                }
            },
            emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' } }
        }]
    });
});

window.onload = function() {
    var chart = echarts.init(document.getElementById('appHealthContainer'));
    var healthValue = 75;

    var option = {
        title: {
            text: 'App Health',
            left: 'center',
            textStyle: {
                fontWeight: 'bold',
                fontSize: 16
            }
        },
        series: [{
            name: '',
            type: 'gauge',
            radius: '100%',
            min: 0,
            max: 100,
            splitNumber: 5,
            axisLine: {
                lineStyle: {
                    width: 20,
                    color: [
                        [0.2, '#f44336'],
                        [0.4, '#ff9800'],
                        [0.6, '#ffeb3b'],
                        [0.8, '#4caf50'],
                        [1, '#388e3c']
                    ]
                }
            },
            axisTick: { splitNumber: 8 },
            axisLabel: { fontSize: 10 },
            pointer: { length: '50%', width: 6 },
            detail: { formatter: '{value}%', fontSize: 24 },
            data: [{ width: 20, value: healthValue, name: '' }]
        }]
    };

    chart.setOption(option);
}

fetch('logs.json')
.then(response => response.json())
.then(logs => {
    const methodStatusData = {
        GET: { success: 0, fail: 0 },
        POST: { success: 0, fail: 0 },
        PUT: { success: 0, fail: 0 },
        DELETE: { success: 0, fail: 0 },
    };

    logs.forEach(log => {
        const method = log.http_method;
        const statusCode = log.status_code;

        if (methodStatusData[method]) {
            if (statusCode >= 200 && statusCode < 300) {
                methodStatusData[method].success += 1;
            } else {
                methodStatusData[method].fail += 1;
            }
        }
    });

    const chartData = Object.entries(methodStatusData).map(([method, { success, fail }]) => ({
        method,
        success,
        fail,
    }));

    const chart = echarts.init(document.getElementById('httpMethodStatusChart'));
    chart.setOption({
        title: { text: 'HTTP Methods vs Status Codes (Success vs Fail)', left: 'center' },
        tooltip: { trigger: 'item' },
        xAxis: { type: 'category', data: chartData.map(item => item.method) },
        yAxis: { type: 'value' },
        series: [
            {
                name: 'Success',
                type: 'bar',
                data: chartData.map(item => item.success),
                itemStyle: { color: '#4caf50' },
            },
            {
                name: 'Fail',
                type: 'bar',
                data: chartData.map(item => item.fail),
                itemStyle: { color: '#ff4d4f' },
            },
        ],
    });
})
.catch(error => console.error("Error fetching or processing logs:", error));

fetch('logs.json')
.then(response => response.json())
.then(logs => {
    const { dates, seriesData } = processLogData(logs);
    const chartSeries = Object.keys(seriesData).map(status => ({
        name: `Status ${status}`,
        type: 'line',
        data: dates.map(date => {
            const entry = seriesData[status].find(e => e.date === date);
            return entry ? entry.count : 0;
        }),
        lineStyle: { width: 2 },
        symbol: 'circle',
        areaStyle: { opacity: 0.1 },
    }));

    const chart = echarts.init(document.getElementById('errorLineChart'));
    const option = {
        title: { text: 'Error Trends', left: 'center' },
        tooltip: { trigger: 'axis' },
        legend: { data: Object.keys(seriesData).map(status => `Status ${status}`), top: '10%' },
        xAxis: { type: 'category', data: dates, name: 'Date', boundaryGap: false },
        yAxis: { type: 'value', name: 'Error Count' },
        series: chartSeries,
    };

    chart.setOption(option);
})
.catch(error => console.error('Error loading logs:', error));

function processLogData(logs) {
    const errorData = {};
    logs.forEach(log => {
        if (log.log_level === "ERROR") {
            const date = log.timestamp.split(":")[0];
            const status = log.status_code;
            if (!errorData[date]) errorData[date] = {};
            if (!errorData[date][status]) errorData[date][status] = 0;
            errorData[date][status]++;
        }
    });

    const dates = Object.keys(errorData).sort();
    const seriesData = {};
    dates.forEach(date => {
        Object.keys(errorData[date]).forEach(status => {
            if (!seriesData[status]) seriesData[status] = [];
            seriesData[status].push({ date, count: errorData[date][status] });
        });
    });

    return { dates, seriesData };
}

fetch('logs.json')
.then(response => response.json())
.then(logs => {
    const { dates, requestVolumes, errorCounts } = processLogData1(logs);
    const chart = echarts.init(document.getElementById('overlayChart'));
    const option = {
        title: { text: 'Requests vs. Errors', left: 'center' },
        tooltip: { trigger: 'axis' },
        legend: { data: ['Request Volume', 'Error Count'], top: '10%' },
        xAxis: { type: 'category', data: dates, name: 'Date' },
        yAxis: [
            { type: 'value', name: 'Request Volume', position: 'left' },
            {
                type: 'value',
                name: 'Error Count',
                position: 'right',
                axisLine: { lineStyle: { color: '#d14a61' } },
            },
        ],
        series: [
            {
                name: 'Request Volume',
                type: 'line',
                data: requestVolumes,
                lineStyle: { width: 2 },
                symbol: 'circle',
                areaStyle: { opacity: 0.1 },
            },
            {
                name: 'Error Count',
                type: 'bar',
                data: errorCounts,
                yAxisIndex: 1,
                itemStyle: { color: '#d14a61' },
            },
        ],
    };

    chart.setOption(option);
})
.catch(error => console.error('Error loading logs:', error));

function processLogData1(logs) {
    const requestData = {};
    const errorData = {};

    logs.forEach(log => {
        const date = log.timestamp.split(":")[0];
        if (!requestData[date]) requestData[date] = 0;
        if (!errorData[date]) errorData[date] = 0;

        requestData[date]++;
        if (log.log_level === "ERROR") {
            errorData[date]++;
        }
    });

    const dates = Object.keys(requestData).sort();
    const requestVolumes = dates.map(date => requestData[date]);
    const errorCounts = dates.map(date => errorData[date]);

    return { dates, requestVolumes, errorCounts };
}
fetch('logs.json')
    .then(response => response.json())
    .then(logs => {
        // Top Accessed URLs Data
        const topUrlsData = getTopAccessedUrls(logs);

        // Render Bar Chart for Top Accessed URLs
        renderTopUrlsChart(topUrlsData);

        // Flow Analysis Data
        const flowData = getFlowAnalysis(logs);

        // Render Sankey Diagram for Flow Analysis
        renderFlowAnalysisChart(flowData);
    })
    .catch(error => console.error('Error loading logs:', error));

// Function to get top accessed URLs
function getTopAccessedUrls(logs) {
    const urlCounts = {};

    logs.forEach(log => {
        const url = log.request;
        if (!urlCounts[url]) urlCounts[url] = 0;
        urlCounts[url]++;
    });

    // Sort by count and return the top 10 URLs
    return Object.entries(urlCounts)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10)
        .map(([url, count]) => ({ url, count }));
}

// Function to render top accessed URLs chart
function renderTopUrlsChart(data) {
    const chart = echarts.init(document.getElementById('topUrlsChart'));

    const option = {
        title: {
            text: 'Top Accessed URLs',
            left: 'center',
        },
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'shadow',
            },
        },
        xAxis: {
            type: 'category',
            data: data.map(item => item.url),
            axisLabel: {
                rotate: 45,
                interval: 0,
            },
            name: 'URLs',
        },
        yAxis: {
            type: 'value',
            name: 'Access Count',
        },
        series: [
            {
                data: data.map(item => item.count),
                type: 'bar',
                itemStyle: {
                    color: '#5470C6',
                },
            },
        ],
    };

    chart.setOption(option);
}

function getFlowAnalysis(logs) {
    const flowMap = {};

    logs.forEach(log => {
        const referrer = log.referrer || 'Direct';
        const target = log.request;

        const key = `${referrer} -> ${target}`;
        if (!flowMap[key]) flowMap[key] = 0;
        flowMap[key]++;
    });

    const nodes = new Set();
    const links = [];

    Object.entries(flowMap).forEach(([flow, count]) => {
        const [source, target] = flow.split(' -> ');
        nodes.add(source);
        nodes.add(target);
        links.push({ source, target, value: count });
    });

    return {
        nodes: Array.from(nodes).map(name => ({ name })),
        links,
    };
}

function renderFlowAnalysisChart(data) {
    const chart = echarts.init(document.getElementById('flowAnalysisChart'));

    const option = {
        title: {
            text: 'Flow Analysis',
            left: 'center',
        },
        tooltip: {
            trigger: 'item',
            formatter: ({ data }) =>
                data.source
                    ? `${data.source} -> ${data.target}: ${data.value}`
                    : `${data.name}`,
        },
        series: [
            {
                type: 'sankey',
                data: data.nodes,
                links: data.links,
                emphasis: {
                    focus: 'adjacency',
                },
                lineStyle: {
                    color: 'gradient',
                    curveness: 0.5,
                },
                label: {
                    color: '#000',
                },
            },
        ],
    };

    chart.setOption(option);
}
fetch('logs.json')
            .then(response => response.json())
            .then(data => {
                // Filter the data into error and success categories
                const errorData = data.filter(item => item.status_code >= 400);
                const successData = data.filter(item => item.status_code < 400);
                console.log('Error Data (4xx, 5xx):', errorData);
                console.log('Success Data (2xx, 3xx):', successData);
                // Prepare data for the chart
                const errorPoints = errorData.map(item => [item.response_size, item.status_code]);
                const successPoints = successData.map(item => [item.response_size, item.status_code]);
                console.log(successPoints)
                // Initialize ECharts
                const chart = echarts.init(document.getElementById('r-size-vs-errors'));

                const option = {
                    title: {
                        text: 'Response Size vs Status Code',
                        left: 'center',
                        top: '10px'
                    },
                    tooltip: {
                        trigger: 'item',
                        formatter: function (params) {
                            return `Status Code: ${params.value[1]}<br>Response Size: ${params.value[0]} bytes`;
                        }
                    },
                    xAxis: {
                        type: 'value',
                        name: 'Response Size (Bytes)',
                        nameLocation: 'middle',
                        min:4900,
                        max:5100,
                        nameGap: 30
                    },
                    yAxis: {
                        type: 'value',
                        name: 'Status Code',
                        nameLocation: 'middle',
                        min:190,
                        max:500,
                        nameGap: 30
                    },
                    series: [
                        {
                            name: 'Errors (4xx, 5xx)',
                            type: 'scatter',
                            data: errorPoints,
                            itemStyle: {
                                color: '#ff3333', // Red for error codes
                            },
                            symbolSize: 10,
                            label: {
                                show: false
                            }
                        },
                        {
                            name: 'Successes (2xx, 3xx)',
                            type: 'scatter',
                            data: successPoints,
                            itemStyle: {
                                color: '#33cc33', // Green for success codes
                            },
                            symbolSize: 10,
                            label: {
                                show: false
                            }
                        }
                    ]
                };

                // Set the option for the chart
                chart.setOption(option);
            })
            .catch(error => {
                console.error('Error fetching the logs:', error);
            });