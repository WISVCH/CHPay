/**
 * Populates the transactions table with transaction data for regular users.
 * Creates table rows with date, product description, amount, status, and receipt button
 * for the first 4 transactions in the transactions array.
 */
function populateTransactions() {
    const transactionsTable = document.querySelector('.transactions-table');
    if (!transactionsTable) {
        console.error('Transactions table element not found.');
        return;
    }


    for (let i = 0; i < 4; i++) {
        if(i>=transactions.length){
            break;
        }
        const row = document.createElement('div');
        row.className = 'table-row';

        const pairs1 = document.createElement('div');
        pairs1.className = 'table-pairs';

        const dateCell = document.createElement('div');
        dateCell.className = 'table-cell';
        dateCell.textContent = transactions[i].date;

        const productCell = document.createElement('div');
        productCell.className = 'table-cell';
        //productCell.textContent = transactions[i].description.substring(61);
        productCell.textContent = transactions[i].description;

        pairs1.appendChild(dateCell);
        pairs1.appendChild(productCell);

        const pairs2 = document.createElement('div');
        pairs2.className = 'table-pairs';

        const totalCell = document.createElement('div');
        totalCell.className = 'table-cell';
        totalCell.textContent = transactions[i].amount.toFixed(2);

        const statusCell = document.createElement('div');
        statusCell.className = 'table-cell';
        const statusSpan = document.createElement('span');
        statusSpan.className = `status ${transactions[i].status.toLowerCase()}`;  // Corrected to transaction.status
        statusSpan.textContent = transactions[i].status; // Corrected to transaction.status
        statusCell.appendChild(statusSpan);

        pairs2.appendChild(totalCell);
        pairs2.appendChild(statusCell);

        const receiptCell = document.createElement('div');
        receiptCell.className = 'table-cell receipt-button-cell';
        const receiptButton = document.createElement('button');
        receiptButton.className = 'receipt-button';
        receiptButton.textContent = 'Email Receipt';
        // Add event listener for the receipt button here:
        receiptButton.addEventListener('click', () => {
            emailReceipt(transactions[i].id);
        });
        receiptCell.appendChild(receiptButton);

        row.appendChild(pairs1);
        row.appendChild(pairs2);
        row.appendChild(receiptCell);
        transactionsTable.appendChild(row);
    }
}

function displayTypeFilterButtons(isOnlyTopUps) {
    if(!isOnlyTopUps){
        return;
    }
    const allButt=document.getElementById("allButt");
    const topUpButton = document.getElementById("topUpButt");
    allButt.classList.remove("active");
    topUpButton.classList.add("active");
}

function emailReceipt(transactionId) {
    triggerNotificationFromData({
        type: 'message',
        message: 'Sending receipt...'
    });
    fetch(`/transactions/email-receipt/${transactionId}`, {
        method: 'POST',
    })
        .then(response => {
            if (response.ok) {
                triggerNotificationFromData({
                    type: 'success',
                    message: 'Receipt emailed successfully.'
                });
            } else {
                triggerNotificationFromData({
                    type: 'error',
                    message: 'Failed to send receipt.'
                });
            }
        })
        .catch(error => {
            console.error("Error sending receipt:", error);
            alert("An error occurred while sending the receipt.");
        });
}

/**
 * Populates the transactions table with transaction data for admin users.
 * Creates table rows with date, openID, product description, amount, status, and view button
 * for the first 4 transactions in the transactions array.
 * Uses a different layout optimized for admin functionality.
 */
function populateTransactionsAdmin() {
    const transactionsTable = document.querySelector('.transactions-table');
    if (!transactionsTable) {
        console.error('Transactions table element not found.');
        return;
    }


    for (let i = 0; i < 4; i++) {
        if(i>=transactions.length){
            break;
        }
        const row = document.createElement('div');
        row.className = 'table-row';

        const pairs1 = document.createElement('div');
        pairs1.className = 'table-pairs table-pairs-admin';

        const dateCell = document.createElement('div');
        dateCell.className = 'table-cell';
        dateCell.textContent = transactions[i].date;

        const netIDCell = document.createElement('div');
        netIDCell.className = 'table-cell';
        netIDCell.textContent = transactions[i].userOpenID;

        pairs1.appendChild(dateCell);
        pairs1.appendChild(netIDCell);


        const pairs2 = document.createElement('div');
        pairs2.className = 'table-pairs table-pairs-admin';

        const productCell = document.createElement('div');
        productCell.className = 'table-cell';
        //productCell.textContent = transactions[i].description.substring(61);
        productCell.textContent = transactions[i].description;

        const totalCell = document.createElement('div');
        totalCell.className = 'table-cell';
        totalCell.textContent = transactions[i].amount.toFixed(2);


        pairs2.appendChild(productCell);
        pairs2.appendChild(totalCell);

        const pairs3 = document.createElement('div');
        pairs3.className = 'table-pairs table-pairs-admin';

        const statusCell = document.createElement('div');
        statusCell.className = 'table-cell';
        const statusSpan = document.createElement('span');
        statusSpan.className = `status ${transactions[i].status.toLowerCase()}`;  // Corrected to transaction.status
        statusSpan.textContent = transactions[i].status; // Corrected to transaction.status
        statusCell.appendChild(statusSpan);

        const receiptCell = document.createElement('div');
        receiptCell.className = 'table-cell receipt-button-cell';
        const receiptButton = document.createElement('button');
        receiptButton.className = 'receipt-button';
        receiptButton.textContent = 'View';
        // Add event listener for the receipt button here:
        receiptButton.addEventListener('click', () => {
            console.log('Receipt clicked for:', transactions[i]);
            location.href = "/admin/transaction/" + transactions[i].id;
            //  Implement your receipt viewing logic (e.g., show modal, navigate)
        });
        receiptCell.appendChild(receiptButton);

        pairs3.appendChild(statusCell);
        pairs3.appendChild(receiptCell);

        row.appendChild(pairs1);
        row.appendChild(pairs2);
        row.appendChild(pairs3);
        transactionsTable.appendChild(row);
    }
}