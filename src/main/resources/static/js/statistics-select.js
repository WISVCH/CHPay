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


const fieldSelect = document.getElementById("fieldStat");
const fieldTime = document.getElementById("fieldTime");

/**
 * Redirects the user to a statistics page based on the selected stat and time range.
 */
function redirectStat(){
    let stat = fieldSelect.options[ fieldSelect.selectedIndex ].value;
    let daysBack = fieldTime.options[ fieldTime.selectedIndex ].value;
    location.href = "/admin/stats/"+stat+"?daysBack="+daysBack;
}

/**
 * Sets up event listeners on specific fields that trigger a redirect operation
 * when their values are changed. This method ensures that the appropriate redirection
 * logic, implemented in the redirectStat function, is executed whenever the input
 * values are modified in the monitored fields.
 *
 * @return {void} Does not return any value.
 */
function setSystemStatRedirect(){
    fieldSelect.addEventListener("change", (event)=>{
        redirectStat();
    });
    fieldTime.addEventListener("change", (event)=>{
        redirectStat();
    });
}

/**
 * Sets the selected index of the `fieldTime` element based on the `daysBack` parameter from the URL.
 * If `daysBack` is not provided, a default index of 2 is applied.
 * After updating the index, a 'change' event is dispatched to notify of the change.
 *
 * @return {void} This method does not return a value.
 */
function setTimeSelect(){
    let daysBack=getParam("daysBack", location.href);
    if(daysBack!=null){
        if(daysBack<10){
            fieldTime.selectedIndex=0;
        }else if(daysBack<45){
            fieldTime.selectedIndex=1;
        }else if(daysBack<105){
            fieldTime.selectedIndex=2;
        }else if(daysBack<195){
            fieldTime.selectedIndex=3;
        }else if(daysBack<225){
            fieldTime.selectedIndex=4;
        }else if(daysBack<400){
            fieldTime.selectedIndex=5;
        }else{
            fieldTime.selectedIndex=6;
        }
        fieldTime.dispatchEvent(new Event('change'));
        return;
    }
    fieldTime.selectedIndex=2;
    fieldTime.dispatchEvent(new Event('change'));
}

/**
 * Updates the selected index of the `fieldSelect` element based on the value of the `type` variable.
 * If `type` is "incoming-funds", sets the selected index to 1.
 * If `type` is "outcoming-funds", sets the selected index to 2.
 * Defaults to setting the selected index to 0 if `type` is any other value or null.
 * Dispatches a 'change' event on the `fieldSelect` element after updating the selected index.
 *
 * @return {void} This method does not return a value.
 */
function setStatSelect(){
    if(type!=null){
        if(type==="incoming-funds"){
            fieldSelect.selectedIndex=1;
        }else if(type==="outcoming-funds"){
            fieldSelect.selectedIndex=2;
        }else{
            fieldSelect.selectedIndex=0;
        }
        fieldSelect.dispatchEvent(new Event('change'));
    }
}

setTimeSelect();
setStatSelect();
setSystemStatRedirect();





