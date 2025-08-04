/**
 * Filter users by balance range using start and end price inputs
 * Redirects to user page with balance filter parameters
 */
function searchByBalance(){
    let startBalance = document.getElementById( "startBalance" );
    let endBalance = document.getElementById( "endBalance" );
    location.href = "/admin/users?"
        +"balanceAfter="+startBalance.value
        +"&balanceBefore="+endBalance.value;
}

/**
 * Filter users by email query string
 * Redirects to user page with user filter parameter
 */
function searchByEmail(){
    let usersEmailSearch = document.getElementById( "usersEmailSearch" );
    location.href = "/admin/users?"
        +"emailQuery="+usersEmailSearch.value;
}

/**
 * Filter users by name query string
 * Redirects to user page with user filter parameter
 */
function searchByName(){
    let usersNameSearch = document.getElementById( "usersNameSearch" );
    location.href = "/admin/users?"
        +"nameQuery="+usersNameSearch.value;
}


/**
 * Initialize filter form elements and setup event listeners
 */
const searchButton = document.getElementById("searchButt");
searchButton.addEventListener("click", searchByBalance);
const fieldEmail = document.getElementById("fieldEmail");
const fieldName = document.getElementById("fieldName");
const fieldBalance = document.getElementById("fieldBalance");
const fieldSelect = document.getElementById("fieldFilter");


function searchRemoveEventListeners(element) {
    element.removeEventListener("click", searchByBalance);
    element.removeEventListener("click", searchByEmail);
    element.removeEventListener("click", searchByName);
}

/**
 * Handle filter type selection changes
 * Shows relevant form fields and attaches appropriate search handler
 */
fieldSelect.addEventListener("change", (event)=>{
    fieldBalance.style.display = "none";
    fieldEmail.style.display = "none";
    fieldName.style.display = "none";
    //remove all event listeners
    searchRemoveEventListeners(searchButton);
    switch (fieldSelect.options[ fieldSelect.selectedIndex ].value ){
        case "balance":
            fieldBalance.style.display = "flex";
            searchButton.addEventListener("click",searchByBalance);
            break;
        case "email":
            fieldEmail.style.display = "flex";
            searchButton.addEventListener("click",searchByEmail);
            break;
        case "name":
            fieldName.style.display = "flex";
            searchButton.addEventListener("click",searchByName);
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
 * Load price filter values from URL parameters into balance input fields
 * Sets default values if no parameters present
 */
function setBalances(){
    let startBalanceString = getParam("balanceAfter", location.href);
    let endBalanceString = getParam("balanceBefore", location.href);
    let startBalance = document.getElementById( "startBalance" );
    let endBalance = document.getElementById( "endBalance" );
    if(startBalanceString != null && endBalanceString != null) {
        fieldSelect.selectedIndex=0;
        fieldSelect.dispatchEvent(new Event('change'));
        startBalance.value=parseFloat(startBalanceString).toFixed(2);
        endBalance.value=parseFloat(endBalanceString).toFixed(2);
    }else{
        startBalance.value=parseFloat("0").toFixed(2);
        endBalance.value=parseFloat("1000").toFixed(2);
    }
}

/**
 * Load user filter value from URL parameter into user search field
 */
function setEmail(){
    let emailString = getParam("emailQuery", location.href);
    if (emailString != null) {
        let emailSearch = document.getElementById("usersEmailSearch");
        fieldSelect.selectedIndex=1;
        fieldSelect.dispatchEvent(new Event('change'));
        emailSearch.value=emailString;
    }
}

/**
 * Load user filter value from URL parameter into user search field
 */
function setName(){
    let usersString = getParam("nameQuery", location.href);
    if (usersString != null) {
        let usersSearch = document.getElementById("usersNameSearch");
        fieldSelect.selectedIndex=2;
        fieldSelect.dispatchEvent(new Event('change'));
        usersSearch.value=usersString;
    }
}


setBalances();
setEmail();
setName();