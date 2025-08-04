
const oldOrder=ORDER;
const oldSortBy=SORT_BY;

/**
 * Sorts buttons based on the specified criteria when clicked and navigates to the updated view.
 *
 * @param {string} sortBy - The field by which the buttons should be sorted.
 * @param {string} pageName - The current page context, either 'transactions' or 'users'.
 * @param {string} filters - Additional filter parameters to include in the URL.
 * @return {void} This function does not return a value. It redirects to the updated view with sorting applied.
 */
function sortButtonsOnClick(sortBy, pageName, filters) {
    let order = 'desc';
    if(oldSortBy === sortBy){
        if(oldOrder === 'desc'){
            order = 'asc';
        }
    }

    //for now page is either transactions or users to navigate to correct view
    location.href='/admin/'+pageName+'?page='+page+'&sortBy='+sortBy+'&order=' + order + filters;
}

/**
 * Updates the column header with an arrow indicating the sort order (ascending or descending)
 * for the currently sorted column in a table.
 * Modifies the text content of the header cell corresponding to the sorted column
 * by appending an upward or downward arrow based on the order of sorting.
 *
 * @return {void} Does not return any value.
 */
function displaySort() {
    let arrow=" ↓";
    if(oldOrder === 'asc'){
        arrow = ' ↑';
    }
    let cell;
    switch(oldSortBy) {
        case "description":
            cell = document.getElementById('descCell');
            break;
        case "amount":
            cell = document.getElementById('totalCell');
            break;
        case "timestamp":
            cell = document.getElementById('dateCell');
            break;
        case "balance":
            cell = document.getElementById('balanceCell');
            break;
        case "name":
            cell = document.getElementById('nameCell');
            break;
        case "openID":
            cell = document.getElementById('openIDCell');
            break;
    }
    cell.innerText = cell.innerText+arrow;
}