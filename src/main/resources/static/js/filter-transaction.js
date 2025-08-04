import Datepicker from '/webjars/vanillajs-datepicker/js/Datepicker.js';

const pickerStart = document.querySelector('input[id="startDate"]');
const datepickerStart = new Datepicker(pickerStart, {
    format: 'yyyy-mm-dd',
    defaultViewDate:Date.now(),
    maxDate: Date.now()
});
const pickerEnd = document.querySelector('input[id="endDate"]');
const datepickerEnd = new Datepicker(pickerEnd, {
    format: 'yyyy-mm-dd',
    defaultViewDate:Date.now(),
    maxDate: Date.now()
});


// When start date changes, ensure end date is not before it
pickerStart.addEventListener('changeDate', function () {
    const startDate = datepickerStart.getDate();

    const endDate = datepickerEnd.getDate();

    // If end date is not set, skip setting start date
    if(!endDate) return;

    // If end date is before start date, set end date to start date
    if (endDate.getTime() < startDate.getTime()) {
        datepickerEnd.setDate(startDate);
    }
});

// When end date changes, ensure start date is not after it
pickerEnd.addEventListener('changeDate', function () {
    const endDate = datepickerEnd.getDate();

    const startDate = datepickerStart.getDate();

    // If start date is not set, skip setting end date
    if(!startDate) return;

    // If start date is not set or is after end date, set start date to end date
    if (startDate.getTime() > endDate.getTime()) {
        datepickerStart.setDate(endDate);
    }
});

/**
 * Filter transactions by date range using start and end dates from datepickers
 * Redirects to transactions page with date filter parameters
 */
function searchByDate(){
    let startDateString = datepickerStart.getDate('yyyy-mm-dd');
    let endDateString = datepickerEnd.getDate('yyyy-mm-dd');
    location.href = "/admin/transactions?"
        +"startDate="+startDateString
        +"&endDate="+endDateString;
}

/**
 * Filter transactions by amount range using start and end price inputs
 * Redirects to transactions page with price filter parameters
 */
function searchByAmount(){
    let startPrice = document.getElementById( "startPrice" );
    let endPrice = document.getElementById( "endPrice" );
    location.href = "/admin/transactions?"
        +"startPrice="+startPrice.value
        +"&endPrice="+endPrice.value;
}

/**
 * Filter transactions by user query string
 * Redirects to transactions page with user filter parameter
 */
function searchByOpenIDUsers(){
    let usersOpenIDSearch = document.getElementById( "usersOpenIDSearch" );
    location.href = "/admin/transactions?"
        +"userOpenIdQuery="+usersOpenIDSearch.value;
}

/**
 * Filter transactions by user query string
 * Redirects to transactions page with user filter parameter
 */
function searchByNameUsers(){
    let usersNameSearch = document.getElementById( "usersNameSearch" );
    location.href = "/admin/transactions?"
        +"userNameQuery="+usersNameSearch.value;
}

/**
 * Filter transactions by description query string
 * Redirects to transactions page with description filter parameter
 */
function searchByDescription(){
    let descSearch = document.getElementById( "descSearch" );
    location.href = "/admin/transactions?"
        +"descQuery="+descSearch.value;
}

/**
 * Redirects the user to the transactions page with query parameters for type and status based on selected values from dropdown elements.
 *
 * @return {void} This method does not return a value, it performs a redirect.
 */
function searchByTypeAndStatus(){
    let fieldType = document.getElementById( "fieldType" );
    let fieldStatus = document.getElementById( "fieldStatus");
    let valType = fieldType.options[ fieldType.selectedIndex ].value;
    let valStatus = fieldStatus.options[ fieldStatus.selectedIndex ].value;
    location.href = "/admin/transactions?"
        +"type="+valType+"&status="+valStatus;
}


/**
 * Initialize filter form elements and setup event listeners
 */
const searchButton = document.getElementById("searchButt");
searchButton.addEventListener("click", searchByDate);
const fieldOpenID = document.getElementById("fieldOpenID");
const fieldName = document.getElementById("fieldName");
const fieldDate = document.getElementById("fieldDate");
const fieldPrice = document.getElementById("fieldPrice");
const fieldDescription = document.getElementById("fieldDescription");
const fieldTypeAndStatus = document.getElementById("fieldTypeAndStatus");
const fieldSelect = document.getElementById("fieldFilter");


function searchRemoveEventListeners(element){
    element.removeEventListener("click",searchByDate);
    element.removeEventListener("click",searchByOpenIDUsers);
    element.removeEventListener("click",searchByNameUsers);
    element.removeEventListener("click",searchByAmount);
    element.removeEventListener("click",searchByDescription);
    element.removeEventListener("click",searchByTypeAndStatus);
}

/**
 * Handle filter type selection changes
 * Shows relevant form fields and attaches appropriate search handler
 */
fieldSelect.addEventListener("change", (event)=>{
    fieldDate.style.display = "none";
    fieldOpenID.style.display = "none";
    fieldName.style.display = "none";
    fieldPrice.style.display = "none";
    fieldDescription.style.display = "none";
    fieldTypeAndStatus.style.display = "none";
    //remove all event listeners
    searchRemoveEventListeners(searchButton);
    switch (fieldSelect.options[ fieldSelect.selectedIndex ].value ){
        case "date":
            fieldDate.style.display = "flex";
            searchButton.addEventListener("click",searchByDate);
            break;
        case "userOpenId":
            fieldOpenID.style.display = "flex";
            searchButton.addEventListener("click",searchByOpenIDUsers);
            break;
        case "userName":
            fieldName.style.display = "flex";
            searchButton.addEventListener("click",searchByNameUsers);
            break;
        case "price":
            fieldPrice.style.display = "flex";
            searchButton.addEventListener("click",searchByAmount);
            break;
        case "description":
            fieldDescription.style.display = "flex";
            searchButton.addEventListener("click",searchByDescription);
            break;
        case "type":
            fieldTypeAndStatus.style.display = "flex";
            searchButton.addEventListener("click",searchByTypeAndStatus);
            break;
    }
});


/**
 * Extract parameter value from URL query string
 * @param {string} name - Parameter name to search for
 * @param {string} url - URL string to search in, defaults to current location
 * @returns {string|null} Parameter value if found, null otherwise
 */
function getParam( name, url ) {
    if (!url) url = location.href;
    name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
    let regexS = "[\\?&]"+name+"=([^&#]*)";
    let regex = new RegExp( regexS );
    let results = regex.exec( url );
    return results == null ? null : results[1];
}

//load into the filter fields the params by which the table is already filtered

/**
 * Load date filter values from URL parameters into date picker fields
 */
function setDates(){
    let startDateString = getParam("startDate", location.href);
    let endDateString = getParam("endDate", location.href);

    if(startDateString != null && endDateString != null) {
        datepickerStart.setDate(Date.parse(startDateString));
        datepickerEnd.setDate(Date.parse(endDateString));
    }
}

/**
 * Load price filter values from URL parameters into price input fields
 * Sets default values if no parameters present
 */
function setPrices(){
    let startPriceString = getParam("startPrice", location.href);
    let endPriceString = getParam("endPrice", location.href);
    let startPrice = document.getElementById( "startPrice" );
    let endPrice = document.getElementById( "endPrice" );
    if(startPriceString != null && endPriceString != null) {
        fieldSelect.selectedIndex=1;
        fieldSelect.dispatchEvent(new Event('change'));
        startPrice.value=parseFloat(startPriceString).toFixed(2);
        endPrice.value=parseFloat(endPriceString).toFixed(2);
    }else{
        startPrice.value=parseFloat("0").toFixed(2);
        endPrice.value=parseFloat("1000").toFixed(2);
    }
}

/**
 * Load user filter value from URL parameter into user search field
 */
function setOpenIDUsers(){
    let usersString = getParam("userOpenIdQuery", location.href);
    if (usersString != null) {
        usersString = usersString.replace("%20", " ");
        let usersSearch = document.getElementById("usersOpenIDSearch");
        fieldSelect.selectedIndex=2;
        fieldSelect.dispatchEvent(new Event('change'));
        usersSearch.value=usersString;
    }
}

/**
 * Load user filter value from URL parameter into user search field
 */
function setNameUsers(){
    let usersString = getParam("userNameQuery", location.href);
    if (usersString != null) {
        let usersSearch = document.getElementById("usersNameSearch");
        fieldSelect.selectedIndex=3;
        fieldSelect.dispatchEvent(new Event('change'));
        usersSearch.value=usersString;
    }
}

/**
 * Load description filter value from URL parameter into description search field
 */
function setDescription() {
    let descString = getParam("descQuery", location.href);
    let descSearch = document.getElementById( "descSearch" );
    if(descString != null) {
        descString = descString.replace("%20", " ");
        fieldSelect.selectedIndex=4;
        fieldSelect.dispatchEvent(new Event('change'));
        descSearch.value=descString;
    }
}

function setTypeAndStatus() {
    let typeString = getParam("type", location.href);
    let statusString = getParam("status", location.href);
    let fieldType = document.getElementById( "fieldType" );
    let fieldStatus = document.getElementById( "fieldStatus");
    if(typeString != null || statusString != null) {
        fieldSelect.selectedIndex=5;
        fieldSelect.dispatchEvent(new Event('change'));
    }
    if(typeString != null) {
        switch (typeString) {
            case "all":fieldType.selectedIndex=0;break;
            case "top_up":fieldType.selectedIndex=1;break;
            case "refund":fieldType.selectedIndex=2;break;
            case "payment":fieldType.selectedIndex=3;break;
            case "external_payment":fieldType.selectedIndex=4;break;
        }
    }
    if(statusString != null) {
        switch (statusString) {
            case "all":fieldStatus.selectedIndex=0;break;
            case "successful":fieldStatus.selectedIndex=1;break;
            case "pending":fieldStatus.selectedIndex=2;break;
            case "failed":fieldStatus.selectedIndex=3;break;
            case "refunded":fieldStatus.selectedIndex=4;break;
            case "partially_refunded":fieldStatus.selectedIndex=5;break;
        }
    }
}
setPrices();
setDates();
setOpenIDUsers();
setNameUsers();
setDescription();
setTypeAndStatus();
