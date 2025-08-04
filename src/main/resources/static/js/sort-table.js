const oldOrder=ORDER;
const oldSortBy=SORT_BY;
function sortButtonsOnClick(sortBy, div) {
    let order = 'desc';
    if(oldSortBy === sortBy){
        if(oldOrder === 'desc'){
            order = 'asc';
        }
    }

    location.href='/transactions?page='+page+'&sortBy='+sortBy+'&order=' + order+'&onlyTopUps='+onlyTopUps;
}

function displaySort() {
    let arrow=" ↓";
    if(oldOrder === 'asc'){
        arrow = ' ↑';
    }
    let cell;
    if(oldSortBy==="description"){
        cell = document.getElementById('descCell');
    }else if(oldSortBy==="amount"){
        cell = document.getElementById('totalCell');
    }else{
        cell = document.getElementById('dateCell');
    }
    cell.innerText = cell.innerText+arrow;
    //something better should be thought of
    //cell.style.textDecoration = 'underline';
}