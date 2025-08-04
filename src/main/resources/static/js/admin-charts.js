const months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

function getMonth(date){
    return months[new Date(date).getMonth()] + " " + new Date(date).getFullYear()
}

function generateLabels(data) {
    if(data.length<91){
        return data.map(row => row.date);
    }
    return Array.from(new Set(data.map(row => getMonth(row.date))));
}

function generateData(data) {
    if(data.length<91){
        return data.map(row => Number(row.amount).toFixed(2));
    }
    const grouped = Object.groupBy(data, (x) => getMonth(x.date));
    if(type === "balance"){
        return Object.values(grouped).map(group => (group.reduce((sum, item) => sum + Number(item.amount), 0) / group.length).toFixed(2));
    }
    return Object.values(grouped).map(group => group.reduce((sum, item) => sum + Number(item.amount), 0).toFixed(2));
}

function getLabelDescription(){
    if(type === "incoming-funds"){
        return 'Incoming Funds';
    }else if(type === "outcoming-funds"){
        return 'Outgoing Funds';
    }
    return 'Available Balance';
}

const chartData = {
    labels: generateLabels(stats),
    datasets: [
        {
            label: getLabelDescription(),
            data: generateData(stats),
            //borderColor: '#1E274A',
            //backgroundColor: '#2AA1A9',
            lineTension: 0.4,
            fill: true
        }
    ]
};
const chart=new Chart(
    document.getElementById('balance'),
    {
        type: 'line',
        data:chartData,
        options: {
            responsive: false,
            maintainAspectRatio: false
        }
    }
);

//chart js has pretty weird resizing
//its pain in the ass
let canvasWrap=document.getElementById('canvasWrap');
chart.resize(canvasWrap.clientWidth, canvasWrap.clientHeight);
chart.update();
window.addEventListener("resize", () => {
    let canvasWrap=document.getElementById('canvasWrap');
    chart.resize(canvasWrap.clientWidth, canvasWrap.clientHeight);
    chart.update();
});