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

/**
 * Converts provided JSON data to CSV format and triggers a download of the CSV file.
 *
 * @param {Object} jsonData - The JSON data to be converted into CSV format.
 * @param {string} statType - The type or category of statistics being processed, used for the file naming.
 * @param {string|null} userName - The name of the user (optional) for whom the data is being generated.
 * @param {string|null} userOpenId - The OpenID of the user (optional), used for user-specific data mappings.
 * @return {void} This function does not return any value; it triggers a file download in the browser.
 */
function downloadJSONAsCSV(jsonData, statType, userName, userOpenId) {
    // Convert JSON data to CSV
    let csvData='';
    if(userName==null || userOpenId==null) {
        csvData = jsonToCsv(jsonData); // Add .items.data
    }else{
        csvData = jsonToCsvForUser(jsonData, userName, userOpenId);
    }
    // Create a CSV file and allow the user to download it
    let blob = new Blob(["\uFEFF"+csvData], {type: 'text/csv'});
    let url = window.URL.createObjectURL(blob);
    let a = document.getElementById('downloadLink');

    //get the dates for the file title
    let today=new Date().toISOString().slice(0, 10);
    let startDayDate=new Date();
    let daysBack=parseInt(getParam("daysBack", location.href));
    if(isNaN(daysBack)){
        daysBack=90;
    }
    startDayDate.setDate(new Date().getDate()-daysBack);
    let startDay=startDayDate.toISOString().slice(0, 10);
    //define the user string for title if applicable
    if(userName!=null){
        userName=' for '+userName;
    }else{
        userName='';
    }
    a.href = url;
    a.download = statType+' '+startDay+' to '+today+userName+'.csv';
}

/**
 * Converts a JSON array into a CSV formatted string.
 *
 * @param {Array<Object>} jsonData - An array of JSON objects, where each object represents a row of data.
 *                                    Each object should have properties matching the expected column headers (e.g., "date", "amount", "type").
 * @return {string} A string representing the data in CSV format, including the header row followed by data rows.
 */
function jsonToCsv(jsonData) {
    jsonData.map(function (row) {
        row.amount = row.amount.toFixed(2);
    })
    let csv = '';
    // Get the headers
    let headers = ["date","amount","type"];
    csv += headers.join(';') + '\n';
    // Add the data
    jsonData.forEach(function (row) {
        let data = headers.map(header => JSON.stringify(row[header])).join(';'); // Add JSON.stringify statement
        csv += data + '\n';
    });
    return csv;
}

/**
 * Converts JSON data to a CSV format, appending user-specific information such as name and OpenID to each row.
 *
 * @param {Object[]} jsonData - An array of objects containing data to be converted into CSV format.
 * @param {string} userName - The name of the user to be appended to each row in the CSV output.
 * @param {string} userOpenId - The OpenID of the user to be appended to each row in the CSV output.
 * @return {string} A string representing the CSV formatted data including the user-specific details.
 */
function jsonToCsvForUser(jsonData, userName, userOpenId) {
    jsonData.map(function (row) {
        row.amount = row.amount.toFixed(2);
    })
    let csv = '';
    // Get the headers
    let headers = ["date","amount","type"];
    csv += headers.join(';') + ';name;openID\n';
    // Add the data
    jsonData.forEach(function (row) {
        let data = headers.map(header => JSON.stringify(row[header])).join(';'); // Add JSON.stringify statement
        csv += data +';'+ userName+';'+userOpenId+'\n';
    });
    return csv;
}