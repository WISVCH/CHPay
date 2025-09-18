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

const searchButton = document.getElementById("searchButt");
const fieldSelect = document.getElementById("fieldStat");
const fieldTime = document.getElementById("fieldTime");

/**
 * Redirects the user to a statistics page based on the selected stat and time range.
 */
function redirectStat(){
    let stat = fieldSelect.options[ fieldSelect.selectedIndex ].value;
    let daysBack = fieldTime.options[ fieldTime.selectedIndex ].value;
    location.href = "/admin/user/"+userId+"/stats/"+stat+"?daysBack="+daysBack;
}
searchButton.addEventListener("click", redirectStat);


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

function setStatSelect(){
    if(type!=null){
        if(type==="outcoming-funds"){
            fieldSelect.selectedIndex=1;
        }else{
            fieldSelect.selectedIndex=0;
        }
        fieldSelect.dispatchEvent(new Event('change'));
    }
}

setTimeSelect();
setStatSelect();
