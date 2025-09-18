/**
 * Populates the admin users table with user data
 * Limited to displaying 4 users at a time
 */
function populateUsersAdmin() {
    // Get reference to table element
    const transactionsTable = document.querySelector('.transactions-table');
    if (!transactionsTable) {
        console.error('Transactions table element not found.');
        return;
    }

    // Iterate through users and create table rows
    for (let i = 0; i < 4; i++) {
        if (i >= users.length) {
            break;
        }
        // Create main row container
        const row = document.createElement('div');
        row.className = 'table-row';

        // First pair of cells (ID and Email)
        const pairs1 = document.createElement('div');
        pairs1.className = 'table-pairs';

        // ID cell
        const idCell = document.createElement('div');
        idCell.className = 'table-cell';
        idCell.textContent = users[i].openID;

        // Email cell
        const emailCell = document.createElement('div');
        emailCell.className = 'table-cell';
        emailCell.textContent = users[i].email;

        pairs1.appendChild(idCell);
        pairs1.appendChild(emailCell);

        // Second pair of cells (Name and Balance)
        const pairs2 = document.createElement('div');
        pairs2.className = 'table-pairs';

        // Name cell
        const nameCell = document.createElement('div');
        nameCell.className = 'table-cell';
        nameCell.textContent = users[i].name;

        // Balance cell - format to 2 decimal places
        const balanceCell = document.createElement('div');
        balanceCell.className = 'table-cell';
        balanceCell.textContent = users[i].balance.toFixed(2).toString()+"EUR";

        pairs2.appendChild(nameCell);
        pairs2.appendChild(balanceCell);

        // View button cell
        const viewCell = document.createElement('div');
        viewCell.className = 'table-cell receipt-button-cell';
        const viewButton = document.createElement('button');
        viewButton.className = 'receipt-button';
        viewButton.textContent = 'View';

        // Handler for view button click
        viewButton.addEventListener('click', () => {
            console.log('View clicked for:', users[i]);
            location.href="/admin/user/"+users[i].id;
            //  Implement your receipt viewing logic (e.g., show modal, navigate)
        });
        viewCell.appendChild(viewButton);

        const banCell = document.createElement('div');
        banCell.className = 'table-cell ban-button-cell';
        const banButton = document.createElement('button');
        banButton.className = 'ban-button';
        banButton.textContent = 'Ban';

        banButton.textContent = users[i].banned ? 'Unban' : 'Ban';

        banButton.setAttribute('data-user-id', users[i].id);
        banButton.setAttribute('data-action', users[i].banned ? 'unban' : 'ban');
        banButton.addEventListener('click', () => {
            confirmBanAction(banButton)
        });
        banCell.appendChild(banButton);

        // Assemble final row
        row.appendChild(pairs1);
        row.appendChild(pairs2);
        row.appendChild(viewCell);
        row.appendChild(banCell);
        transactionsTable.appendChild(row);
    }
}

let banTarget = { userId: null, action: null };
function confirmBanAction(button) {
    const userId = button.getAttribute('data-user-id');

    if (userId === currentUserId.toString()) {
        triggerNotificationFromData({
            type: "message",
            message: "Warning: You cannot ban/unban your own account."
        });
        return;
    }

    banTarget.userId = userId;
    banTarget.action = button.getAttribute('data-action');

    document.getElementById("ban-modal-title").innerText = "Confirm " + banTarget.action;
    document.getElementById("ban-modal-message").innerText =
        `Are you sure you want to ${banTarget.action} this user?`;

    document.getElementById("ban-confirm-modal").classList.add("show");
}

function closeBanModal() {
    document.getElementById("ban-confirm-modal").classList.remove("show");
}

document.getElementById("ban-modal-confirm-button").addEventListener("click", () => {
    if (!banTarget.userId || !banTarget.action) return;

    fetch(`/admin/user/${banTarget.userId}/ban`, {
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': csrfToken
        }
    })
        .then(response => {
            if (response.ok) {
                location.reload();
            } else {
                return response.json().then(error => {
                    throw new Error(error.message || 'Failed to process request.');
                });
            }
        })
        .catch(error => {
            triggerNotificationFromData({
                type: "error",
                message: "Error: " + error.message
            });
        })
        .finally(() => closeBanModal());
});