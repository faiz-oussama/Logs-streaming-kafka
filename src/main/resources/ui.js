document.addEventListener('DOMContentLoaded', () => {
    const darkModeBtn = document.getElementById('darkmode-btn');
    
    darkModeBtn.addEventListener('click', () => {
        document.body.classList.toggle('dark-mode');
        darkModeBtn.textContent = document.body.classList.contains('dark-mode') ? '☀️' : '🌙';
    });
});

const kpiValueElement = document.getElementById('kpiValue');

async function updateKPI() {
    try {
        const response = await fetch('logs.json');
        const data = await response.json();
        const logCount = data.length;
        
        // Call the function to animate the count from 0 to logCount
        animateKPI(kpiValueElement, logCount);
    } catch (error) {
        console.error("Error fetching or parsing log data:", error);
    }
}

// Function to animate the KPI count from 0 to the actual value
function animateKPI(element, targetValue) {
    let currentValue = 0;
    const step = Math.ceil(targetValue / 100); // This determines the animation speed
    const interval = setInterval(() => {
        currentValue += step;
        if (currentValue >= targetValue) {
            currentValue = targetValue; // Ensure it reaches the exact target value
            clearInterval(interval);    // Stop the animation
        }
        element.textContent = currentValue; // Update the element with the current value
    }, 20); // Adjust the interval time for smoother/faster animation
}

updateKPI();
