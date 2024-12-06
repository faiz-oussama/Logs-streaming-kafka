// Utility functions
const formatDate = (date) => {
    return new Date(date).toLocaleString('en-US', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
    });
};

const getLogLevelClass = (level) => {
    switch(level.toUpperCase()) {
        case 'ERROR': return 'badge badge-error';
        case 'WARN': return 'badge badge-warn';
        case 'INFO': return 'badge badge-info';
        default: return 'badge badge-info';
    }
};

// DOM Elements
const tbody = document.querySelector('.logs-table tbody');
const searchInput = document.querySelector('.search-input');
const startDate = document.getElementById('start-date');
const endDate = document.getElementById('end-date');

// State
let logs = [];
let filteredLogs = [];

// Event Listeners
searchInput.addEventListener('input', handleSearch);
startDate.addEventListener('change', handleDateFilter);
endDate.addEventListener('change', handleDateFilter);

// Handlers
function handleSearch(e) {
    const searchTerm = e.target.value.toLowerCase();
    filterAndRenderLogs(searchTerm);
}

function handleDateFilter() {
    const start = startDate.value ? new Date(startDate.value) : null;
    const end = endDate.value ? new Date(endDate.value) : null;
    
    if (start && end) {
        filteredLogs = logs.filter(log => {
            const logDate = new Date(log.timestamp);
            return logDate >= start && logDate <= end;
        });
    } else {
        filteredLogs = [...logs];
    }
    
    renderLogs(filteredLogs);
}

function filterAndRenderLogs(searchTerm = '') {
    filteredLogs = logs.filter(log => 
        log.clientIp.toLowerCase().includes(searchTerm) ||
        log.action.toLowerCase().includes(searchTerm) ||
        log.requestType.toLowerCase().includes(searchTerm) ||
        log.logLevel.toLowerCase().includes(searchTerm)
    );
    renderLogs(filteredLogs);
}

// Render Functions
function renderLogs(logsToRender) {
    tbody.innerHTML = '';
    
    logsToRender.forEach(log => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${log.clientIp}</td>
            <td>${formatDate(log.timestamp)}</td>
            <td>${log.requestType}</td>
            <td>${log.statusCode}</td>
            <td>${log.action}</td>
            <td><span class="${getLogLevelClass(log.logLevel)}">${log.logLevel}</span></td>
        `;
        tbody.appendChild(row);
    });
}

// WebSocket Connection
function connectWebSocket() {
    const ws = new WebSocket('ws://localhost:8080/logs');
    
    ws.onopen = () => {
        console.log('Connected to WebSocket');
    };
    
    ws.onmessage = (event) => {
        const newLog = JSON.parse(event.data);
        logs.unshift(newLog); // Add to beginning of array
        if (logs.length > 100) logs.pop(); // Keep only latest 100 logs
        
        // Apply current filters
        const searchTerm = searchInput.value.toLowerCase();
        filterAndRenderLogs(searchTerm);
    };
    
    ws.onclose = () => {
        console.log('WebSocket connection closed');
        // Attempt to reconnect after 5 seconds
        setTimeout(connectWebSocket, 5000);
    };
    
    ws.onerror = (error) => {
        console.error('WebSocket error:', error);
    };
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    // Set default date range (last 24 hours)
    const now = new Date();
    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);
    
    startDate.value = yesterday.toISOString().split('T')[0];
    endDate.value = now.toISOString().split('T')[0];
    
    // Connect to WebSocket
    connectWebSocket();
});