let currentIndex = 0;
const batchSize = 1;

function fetchLogsData() {
    $.ajax({
        url: 'logs.json',
        method: 'GET',
        dataType: 'json',
        success: function(data) {
            const tableBody = document.querySelector('.table100 tbody');
            const logsToDisplay = data.slice(currentIndex, currentIndex + batchSize);

            logsToDisplay.forEach(log => {
                const row = document.createElement('tr');

                const cellIP = document.createElement('td');
                cellIP.textContent = log.client_ip || 'N/A';
                cellIP.classList.add('column1');

                const cellTimestamp = document.createElement('td');
                cellTimestamp.textContent = log.timestamp || 'N/A';
                cellTimestamp.classList.add('column2');

                const cellRequestType = document.createElement('td');
                cellRequestType.textContent = log.http_method || 'N/A';
                cellRequestType.classList.add('column3');

                const cellStatusCode = document.createElement('td');
                cellStatusCode.textContent = log.status_code || 'N/A';
                cellStatusCode.classList.add('column4');

                const cellAction = document.createElement('td');
                cellAction.textContent = log.action || 'N/A';
                cellAction.classList.add('column5');

                const cellLogLevel = document.createElement('td');
                const logLevelSpan = document.createElement('span');
                logLevelSpan.textContent = log.log_level || 'N/A';
                logLevelSpan.classList.add(log.log_level.toLowerCase());
                cellLogLevel.appendChild(logLevelSpan);

                row.appendChild(cellIP);
                row.appendChild(cellTimestamp);
                row.appendChild(cellRequestType);
                row.appendChild(cellStatusCode);
                row.appendChild(cellAction);
                row.appendChild(cellLogLevel);

                // Apply color based on log level
                if (log.log_level === 'WARN') {
                    row.classList.add('warn');
                } else if (log.log_level === 'ERROR') {
                    row.classList.add('error');
                } else if (log.log_level === 'INFO') {
                    row.classList.add('info');
                }

                tableBody.prepend(row);
            });

            currentIndex += batchSize;
            if (currentIndex >= data.length) {
                currentIndex = 0; // Reset to start if end is reached
            }
        },
        error: function(xhr, status, error) {
            console.error('Error loading logs data:', error);
        }
    });
}

// Polling interval set to 2 seconds
setInterval(fetchLogsData, 2000);

document.addEventListener('DOMContentLoaded', fetchLogsData);