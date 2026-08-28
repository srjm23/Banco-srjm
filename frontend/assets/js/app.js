(function () {
    "use strict";

    var API_ROOT = "/api";
    var state = {
        account: null,
        activeReference: "",
        transactions: [],
        adminAccounts: [],
        adminFilter: "ALL",
        customerView: "overview",
        statementMode: "full"
    };
    var toastTimer;
    var pixLookupTimer;
    var pixLookupSequence = 0;

    var accountBalance = document.getElementById("account-balance");
    var accountHolder = document.getElementById("account-holder");
    var accountDisplayReference = document.getElementById("account-display-reference");
    var accountStatus = document.getElementById("account-status");
    var accountNumber = document.getElementById("account-number");
    var accountDigit = document.getElementById("account-digit");
    var accountCreated = document.getElementById("account-created");
    var transactionsBody = document.getElementById("transactions-body");
    var textStatementLink = document.getElementById("text-statement-link");
    var statementGenerated = document.getElementById("statement-generated");
    var toast = document.getElementById("toast");
    var accountModal = document.getElementById("account-modal");
    var loginModal = document.getElementById("login-modal");
    var forgotPasswordModal = document.getElementById("forgot-password-modal");
    var resetPasswordModal = document.getElementById("reset-password-modal");
    var closeAccountModal = document.getElementById("close-account-modal");
    var closeAccountTrigger = document.getElementById("close-account-trigger");
    var loginTrigger = document.getElementById("login-trigger");
    var logoutButton = document.getElementById("logout-button");
    var landingPage = document.getElementById("landing-page");
    var authenticatedApp = document.getElementById("authenticated-app");

    function init() {
        document.getElementById("today-date").textContent = new Intl.DateTimeFormat("pt-BR", {
            weekday: "long",
            day: "2-digit",
            month: "long",
            year: "numeric"
        }).format(new Date());

        bindEvents();
        bindInputMasks();
        updateCurrentAccountInputs();
        restoreSession();
        openResetModalFromLink();
    }

    function bindEvents() {
        loginTrigger.addEventListener("click", openLoginModal);
        logoutButton.addEventListener("click", logout);

        document.querySelectorAll(".new-account-trigger, [data-action='account']").forEach(function (button) {
            button.addEventListener("click", openAccountModal);
        });

        document.querySelectorAll("[data-close-modal]").forEach(function (button) {
            button.addEventListener("click", closeAllModals);
        });

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                closeAllModals();
            }
        });

        document.querySelectorAll("[data-action]").forEach(function (button) {
            var action = button.dataset.action;
            if (action === "account") {
                return;
            }
            button.addEventListener("click", function () {
                showCustomerView("operations", false);
                activateOperation(action, true);
            });
        });

        document.querySelectorAll("[data-customer-view]").forEach(function (link) {
            link.addEventListener("click", function (event) {
                event.preventDefault();
                showCustomerView(link.dataset.customerView, true);
            });
        });

        document.querySelectorAll("[data-open-customer-view]").forEach(function (button) {
            button.addEventListener("click", function () {
                showCustomerView(button.dataset.openCustomerView, true);
            });
        });

        document.querySelectorAll(".operation-tab").forEach(function (button) {
            button.addEventListener("click", function () {
                activateOperation(button.dataset.operation, false);
            });
        });

        document.getElementById("create-account-form").addEventListener("submit", createAccount);
        document.querySelector("#create-account-form input[name='administrator']").addEventListener("change", updateAccountFormMode);
        document.getElementById("login-form").addEventListener("submit", login);
        document.getElementById("forgot-password-trigger").addEventListener("click", openForgotPasswordModal);
        document.getElementById("forgot-password-form").addEventListener("submit", requestPasswordReset);
        document.getElementById("reset-password-form").addEventListener("submit", resetPassword);
        document.getElementById("deposit-form").addEventListener("submit", deposit);
        document.getElementById("withdraw-form").addEventListener("submit", withdraw);
        document.getElementById("pix-form").addEventListener("submit", sendPix);
        document.querySelector("#pix-form input[name='destinationKey']").addEventListener("input", lookupPixRecipient);
        document.querySelectorAll(".pix-key-create").forEach(function (button) {
            button.addEventListener("click", function () { createPixKey(button.dataset.keyType, button); });
        });
        document.getElementById("refresh-statement").addEventListener("click", refreshStatement);
        document.getElementById("refresh-admin").addEventListener("click", loadAdminAccounts);
        document.querySelectorAll("[data-admin-filter]").forEach(function (button) {
            button.addEventListener("click", function () {
                state.adminFilter = button.dataset.adminFilter;
                document.querySelectorAll("[data-admin-filter]").forEach(function (item) {
                    item.classList.toggle("active", item === button);
                });
                renderAdminAccounts();
            });
        });
        closeAccountTrigger.addEventListener("click", openCloseAccountModal);
        document.getElementById("close-account-form").addEventListener("submit", closeActiveAccount);
    }

    function bindInputMasks() {
        document.querySelectorAll(".account-reference-input").forEach(function (input) {
            input.addEventListener("input", function () {
                input.value = formatAccountReferenceInput(input.value);
            });
        });

        document.querySelectorAll(".money-input").forEach(function (input) {
            input.addEventListener("input", function () {
                input.value = formatMoneyInput(input.value);
            });
        });
    }

    function formatAccountReferenceInput(value) {
        var digits = String(value || "").replace(/\D/g, "").slice(0, 8);
        if (digits.length < 4) {
            return digits;
        }
        return digits.slice(0, -1) + "-" + digits.slice(-1);
    }

    function formatMoneyInput(value) {
        var digits = String(value || "").replace(/\D/g, "").replace(/^0+(?=\d)/, "");
        if (!digits) {
            return "";
        }
        digits = digits.slice(0, 15).padStart(3, "0");
        var integerPart = digits.slice(0, -2).replace(/^0+(?=\d)/, "");
        var decimalPart = digits.slice(-2);
        return integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ".") + "," + decimalPart;
    }

    async function restoreSession() {
        try {
            var account = await api("/auth/me");
            await activateAuthenticatedAccount(account, true);
        } catch (error) {
            clearAuthenticatedAccount();
        }
    }

    async function login(event) {
        event.preventDefault();
        var form = event.currentTarget;
        var submitButton = form.querySelector("button[type='submit']");
        setLoading(submitButton, true);
        try {
            var account = await api("/auth/login", {
                method: "POST",
                body: JSON.stringify({
                    account: form.elements.account.value.trim(),
                    password: form.elements.password.value
                })
            });
            form.reset();
            closeAllModals();
            await activateAuthenticatedAccount(account, true);
            showToast("Login realizado com sucesso.", "success");
        } catch (error) {
            showToast(error.message, "error");
        } finally {
            setLoading(submitButton, false);
        }
    }

    async function logout() {
        try {
            await api("/auth/logout", { method: "DELETE" });
        } catch (error) {
            showToast(error.message, "error");
            return;
        }
        clearAuthenticatedAccount();
        showToast("Sessão encerrada.", "success");
    }

    function openForgotPasswordModal() {
        closeAllModals();
        forgotPasswordModal.classList.remove("hidden");
        window.setTimeout(function () {
            forgotPasswordModal.querySelector("input[name='account']").focus();
        }, 20);
    }

    async function requestPasswordReset(event) {
        event.preventDefault();
        var form = event.currentTarget;
        var button = form.querySelector("button[type='submit']");
        setLoading(button, true);
        try {
            var response = await api("/auth/forgot-password", {
                method: "POST",
                body: JSON.stringify({
                    account: form.elements.account.value.trim(),
                    email: form.elements.email.value.trim()
                })
            });
            form.reset();
            closeAllModals();
            showToast(response.message, "success");
        } catch (error) {
            showToast(error.message, "error");
        } finally {
            setLoading(button, false);
        }
    }

    function openResetModalFromLink() {
        var token = new URLSearchParams(window.location.search).get("resetToken");
        if (!token) return;
        closeAllModals();
        resetPasswordModal.querySelector("input[name='token']").value = token;
        resetPasswordModal.classList.remove("hidden");
    }

    async function resetPassword(event) {
        event.preventDefault();
        var form = event.currentTarget;
        if (form.elements.newPassword.value !== form.elements.confirmPassword.value) {
            showToast("As senhas informadas não são iguais.", "error");
            return;
        }
        var button = form.querySelector("button[type='submit']");
        setLoading(button, true);
        try {
            var response = await api("/auth/reset-password", {
                method: "POST",
                body: JSON.stringify({ token: form.elements.token.value, newPassword: form.elements.newPassword.value })
            });
            form.reset();
            window.history.replaceState({}, document.title, window.location.pathname);
            closeAllModals();
            openLoginModal();
            showToast(response.message + " Entre com a nova senha.", "success");
        } catch (error) {
            showToast(error.message, "error");
        } finally {
            setLoading(button, false);
        }
    }

    async function activateAuthenticatedAccount(account, silent) {
        state.account = account;
        state.activeReference = account.accountReference;
        loginTrigger.classList.add("hidden");
        logoutButton.classList.remove("hidden");
        setAccountCreationVisibility(false);
        landingPage.classList.add("hidden");
        authenticatedApp.classList.remove("hidden");
        await loadActiveAccount(state.activeReference, silent);
        if (account.administrator) {
            await loadAdminAccounts();
        }
    }

    function clearAuthenticatedAccount() {
        state.account = null;
        state.activeReference = "";
        state.transactions = [];
        state.adminAccounts = [];
        state.adminFilter = "ALL";
        state.customerView = "overview";
        state.statementMode = "full";
        loginTrigger.classList.remove("hidden");
        logoutButton.classList.add("hidden");
        setAccountCreationVisibility(true);
        landingPage.classList.remove("hidden");
        authenticatedApp.classList.add("hidden");
        authenticatedApp.classList.remove("administrator-view");
        document.getElementById("admin-nav-item").classList.add("hidden");
        document.getElementById("admin").classList.add("hidden");
        renderEmptyState();
    }

    function setAccountCreationVisibility(visible) {
        document.querySelectorAll(".new-account-trigger, [data-action='account']").forEach(function (element) {
            element.classList.toggle("hidden", !visible);
        });
    }

    async function api(path, options) {
        var requestOptions = options || {};
        var headers = Object.assign({ "Content-Type": "application/json" }, requestOptions.headers || {});
        var response;

        try {
            response = await fetch(API_ROOT + path, Object.assign({}, requestOptions, { headers: headers }));
        } catch (error) {
            throw new Error("Não foi possível conectar à API. Verifique se os containers estão em execução.");
        }

        var contentType = response.headers.get("content-type") || "";
        var payload = null;
        if (contentType.indexOf("application/json") !== -1) {
            payload = await response.json();
        }

        if (!response.ok) {
            throw new Error(extractErrorMessage(payload) || "Não foi possível concluir a operação.");
        }
        return payload;
    }

    function extractErrorMessage(payload) {
        if (!payload) {
            return "";
        }
        if (payload.fieldErrors && payload.fieldErrors.length) {
            return payload.fieldErrors[0].message;
        }
        return payload.message || payload.detail || "";
    }

    async function loadActiveAccount(reference, silent) {
        var normalizedReference = reference.trim();
        try {
            state.account = await api("/accounts/" + encodeURIComponent(normalizedReference));
            if (state.account.administrator) {
                state.transactions = [];
            } else {
                var statement = await api("/accounts/" + encodeURIComponent(normalizedReference) + "/statement?limit=500&order=desc");
                state.transactions = statement.transactions || [];
            }
            state.activeReference = state.account.accountReference;
            renderAccount();
            renderTransactions();
            if (!state.account.administrator) showCustomerView(state.customerView, false);
            if (!state.account.administrator) await loadPixKeys();
            if (!silent) {
                showToast("Conta atualizada.", "success");
            }
        } catch (error) {
            state.account = null;
            state.transactions = [];
            renderEmptyState();
            showToast(error.message, "error");
        }
    }

    async function refreshStatement() {
        if (!ensureSelectedAccount()) {
            return;
        }
        await loadActiveAccount(state.activeReference, false);
    }

    function renderEmptyState() {
        accountBalance.textContent = "R$ 0,00";
        accountHolder.textContent = "Nenhum cliente autenticado";
        accountDisplayReference.textContent = "Faça login para acessar sua conta";
        accountStatus.textContent = "Sem conta";
        accountStatus.className = "status-pill neutral";
        accountNumber.textContent = "—";
        accountDigit.textContent = "—";
        accountCreated.textContent = "—";
        closeAccountTrigger.disabled = true;
        textStatementLink.href = "#";
        statementGenerated.textContent = "Data e hora serão atualizadas ao consultar.";
        document.querySelectorAll(".statement-download").forEach(function (link) {
            link.href = "#";
            link.classList.add("disabled");
        });
        transactionsBody.innerHTML = "<tr class='empty-row'><td colspan='4'>Faça login para visualizar o extrato.</td></tr>";
        updateCurrentAccountInputs();
        setOperationAvailability(false);
    }

    function renderAccount() {
        var account = state.account;
        if (!account) {
            renderEmptyState();
            return;
        }

        accountBalance.textContent = formatCurrency(account.balance);
        accountHolder.textContent = account.client.fullName;
        accountDisplayReference.textContent = "Conta " + account.accountReference;
        accountNumber.textContent = account.number;
        accountDigit.textContent = account.checkDigit;
        accountCreated.textContent = formatDate(account.createdAt);
        accountStatus.textContent = formatStatus(account.status);
        accountStatus.className = "status-pill " + statusClass(account.status);
        closeAccountTrigger.disabled = account.status !== "ATIVA" || Number(account.balance) !== 0;
        textStatementLink.href = API_ROOT + "/accounts/" + encodeURIComponent(account.accountReference) + "/statement/text";
        statementGenerated.textContent = "Atualizado em " + formatDateTime(new Date().toISOString());
        document.querySelectorAll(".statement-download").forEach(function (link) {
            link.href = API_ROOT + "/accounts/" + encodeURIComponent(account.accountReference)
                + "/statement/download?format=" + encodeURIComponent(link.dataset.format) + "&limit=500";
            link.classList.remove("disabled");
        });
        updateCurrentAccountInputs();
        setOperationAvailability(account.status === "ATIVA");
        document.getElementById("admin-nav-item").classList.toggle("hidden", !account.administrator);
        document.getElementById("admin").classList.toggle("hidden", !account.administrator);
        authenticatedApp.classList.toggle("administrator-view", account.administrator);
    }

    async function loadAdminAccounts() {
        if (!state.account || !state.account.administrator) {
            return;
        }
        var body = document.getElementById("admin-accounts-body");
        try {
            var overview = await api("/accounts/admin/all");
            var accounts = overview.accounts || [];
            state.adminAccounts = accounts;
            document.getElementById("admin-total-balance").textContent = formatCurrency(overview.totalBalance);
            document.getElementById("admin-total-customers").textContent = String(overview.totalCustomers || 0);
            document.getElementById("admin-generated").textContent = "Atualizado em " + formatDateTime(new Date().toISOString());
            renderAdminAccounts();
        } catch (error) {
            showToast(error.message, "error");
        }
    }

    function renderAdminAccounts() {
        var body = document.getElementById("admin-accounts-body");
        var accounts = state.adminFilter === "ALL"
            ? state.adminAccounts
            : state.adminAccounts.filter(function (account) { return account.status === state.adminFilter; });
        if (!accounts.length) {
            body.innerHTML = "<tr class='empty-row'><td colspan='7'>Nenhuma conta encontrada neste filtro.</td></tr>";
            return;
        }
        body.innerHTML = accounts.map(function (account) {
                return "<tr>"
                    + "<td><strong>" + escapeHtml(account.accountReference) + "</strong></td>"
                    + "<td>" + escapeHtml(account.holderName) + "</td>"
                    + "<td>" + escapeHtml(account.email || "—") + "<small class='table-secondary'>" + escapeHtml(account.phone || "—") + "</small></td>"
                    + "<td><code class='encrypted-cpf'>" + escapeHtml(account.encryptedCpf || "—") + "</code></td>"
                    + "<td><span class='admin-status-pill " + statusClass(account.status) + "'>" + escapeHtml(formatStatus(account.status)) + "</span></td>"
                    + "<td>" + escapeHtml(formatDateTime(account.createdAt)) + "</td>"
                    + "<td class='balance-after'>" + escapeHtml(formatCurrency(account.balance)) + "</td>"
                    + "</tr>";
            }).join("");
    }

    function renderTransactions() {
        renderBalanceRecentTransactions();
        var visibleTransactions = state.statementMode === "recent"
            ? state.transactions.slice(0, 3)
            : state.transactions;
        if (!visibleTransactions.length) {
            transactionsBody.innerHTML = "<tr class='empty-row'><td colspan='4'>Ainda não existem movimentações nesta conta.</td></tr>";
            return;
        }

        transactionsBody.innerHTML = visibleTransactions.map(function (transaction) {
            var isCredit = transaction.direction === "C";
            var signal = isCredit ? "+" : "−";
            var typeLabel = transaction.transactionType === "PIX"
                ? (isCredit ? "PIX recebido" : "PIX enviado")
                : capitalize(transaction.transactionType.toLowerCase());
            var counterpart = transaction.counterpartyAccount
                ? "Conta " + escapeHtml(transaction.counterpartyAccount)
                : escapeHtml(transaction.description || "Movimentação bancária");
            return "<tr>"
                + "<td><div class='transaction-main'><span class='transaction-badge " + (isCredit ? "credit" : "debit") + "'>" + signal + "</span><span>"
                + escapeHtml(typeLabel) + "<small>" + counterpart + "</small></span></div></td>"
                + "<td>" + escapeHtml(formatDateTime(transaction.createdAt)) + "</td>"
                + "<td class='" + (isCredit ? "credit-value" : "debit-value") + "'>" + signal + formatCurrency(transaction.amount) + "</td>"
                + "<td class='balance-after'>" + formatCurrency(transaction.balanceAfter) + "</td>"
                + "</tr>";
        }).join("");
    }

    function renderBalanceRecentTransactions() {
        var output = document.getElementById("balance-recent-list");
        if (!output) return;
        if (!state.transactions.length) {
            output.innerHTML = "<p>Nenhuma movimentação recente.</p>";
            return;
        }
        output.innerHTML = state.transactions.slice(0, 3).map(function (transaction) {
            var credit = transaction.direction === "C";
            var label = transaction.transactionType === "PIX"
                ? (credit ? "PIX recebido" : "PIX enviado")
                : capitalize(transaction.transactionType.toLowerCase());
            return "<div class='balance-recent-item " + (credit ? "credit" : "debit") + "'>"
                + "<i>" + (credit ? "↗" : "↙") + "</i>"
                + "<span>" + escapeHtml(label) + "<small>" + escapeHtml(formatDateTime(transaction.createdAt)) + "</small></span>"
                + "<b>" + (credit ? "+ " : "− ") + escapeHtml(formatCurrency(transaction.amount)) + "</b>"
                + "</div>";
        }).join("");
    }

    function showCustomerView(view, scroll) {
        if (!view || (state.account && state.account.administrator)) return;

        state.customerView = view;
        state.statementMode = view === "recent" ? "recent" : "full";
        var targetSection = view === "recent" || view === "statement" ? "statement" : view;

        document.querySelectorAll("[data-customer-section]").forEach(function (section) {
            section.classList.toggle("hidden", section.dataset.customerSection !== targetSection);
        });
        document.querySelectorAll("[data-customer-view]").forEach(function (link) {
            link.classList.toggle("active", link.dataset.customerView === view);
        });

        var title = document.getElementById("statement-title");
        var actions = document.querySelector("#statement .statement-actions");
        if (title) title.textContent = view === "recent" ? "Últimas 3 transações" : "Extrato bancário completo";
        if (actions) actions.classList.toggle("hidden", view === "recent");
        renderTransactions();

        if (scroll) {
            var target = document.querySelector("[data-customer-section='" + targetSection + "']");
            if (target) target.scrollIntoView({ behavior: "smooth", block: "start" });
        }
    }

    function updateCurrentAccountInputs() {
        document.querySelectorAll(".current-account").forEach(function (input) {
            input.value = state.account ? state.account.accountReference : "";
        });
    }

    function setOperationAvailability(enabled) {
        document.querySelectorAll(".operation-panel button[type='submit']").forEach(function (button) {
            button.disabled = !enabled;
        });
    }

    function activateOperation(operation, scroll) {
        document.querySelectorAll(".operation-tab").forEach(function (button) {
            button.classList.toggle("active", button.dataset.operation === operation);
        });
        document.querySelectorAll(".operation-panel").forEach(function (panel) {
            panel.classList.toggle("active", panel.dataset.panel === operation);
        });
        if (scroll) {
            document.getElementById("operations").scrollIntoView({ behavior: "smooth", block: "start" });
        }
    }

    function ensureSelectedAccount() {
        if (!state.account || !state.activeReference) {
            showToast("Faça login antes de realizar esta operação.", "error");
            return false;
        }
        return true;
    }

    function ensureActiveAccount() {
        if (!ensureSelectedAccount()) {
            return false;
        }
        if (state.account.status !== "ATIVA") {
            showToast("A conta deve estar ativa para realizar movimentações.", "error");
            return false;
        }
        return true;
    }

    async function deposit(event) {
        event.preventDefault();
        if (!ensureActiveAccount()) {
            return;
        }

        var form = event.currentTarget;
        var payload = {
            account: state.activeReference,
            amount: normalizeAmount(form.elements.amount.value)
        };
        await submitOperation(form, "/transactions/deposits", payload, "Depósito realizado com sucesso.");
    }

    async function withdraw(event) {
        event.preventDefault();
        if (!ensureActiveAccount()) {
            return;
        }

        var form = event.currentTarget;
        var payload = {
            account: state.activeReference,
            amount: normalizeAmount(form.elements.amount.value),
            password: form.elements.password.value
        };
        await submitOperation(form, "/transactions/withdrawals", payload, "Saque realizado com sucesso.");
    }

    async function sendPix(event) {
        event.preventDefault();
        if (!ensureActiveAccount()) {
            return;
        }

        var form = event.currentTarget;
        var payload = {
            sourceAccount: state.activeReference,
            destinationKey: form.elements.destinationKey.value.trim(),
            amount: normalizeAmount(form.elements.amount.value),
            password: form.elements.password.value
        };
        await submitOperation(form, "/transactions/pix", payload, "PIX enviado com sucesso.");
        document.getElementById("pix-recipient-feedback").textContent = "";
    }

    function lookupPixRecipient(event) {
        window.clearTimeout(pixLookupTimer);
        var key = event.currentTarget.value.trim();
        var output = document.getElementById("pix-recipient-feedback");
        var sequence = ++pixLookupSequence;
        output.className = "pix-recipient-feedback";
        if (key.length < 3) {
            output.textContent = "";
            return;
        }
        output.textContent = "Verificando chave PIX...";
        pixLookupTimer = window.setTimeout(async function () {
            try {
                var recipient = await api("/transactions/pix/recipient?key=" + encodeURIComponent(key));
                if (sequence !== pixLookupSequence) return;
                output.className = "pix-recipient-feedback found";
                output.textContent = "Destinatário: " + recipient.holderName;
            } catch (error) {
                if (sequence !== pixLookupSequence) return;
                output.className = "pix-recipient-feedback not-found";
                output.textContent = "Chave PIX não encontrada ou indisponível.";
            }
        }, 450);
    }

    async function loadPixKeys() {
        var output = document.getElementById("pix-keys-list");
        if (!state.activeReference) {
            output.textContent = "Nenhuma chave cadastrada.";
            return;
        }
        try {
            var keys = await api("/accounts/" + encodeURIComponent(state.activeReference) + "/pix-keys");
            output.textContent = keys.length
                ? keys.map(function (key) { return keyTypeLabel(key.type) + ": " + key.value; }).join(" · ")
                : "Nenhuma chave cadastrada.";
            document.querySelectorAll(".pix-key-create").forEach(function (button) {
                button.disabled = state.account.status !== "ATIVA" || keys.some(function (key) { return key.type === button.dataset.keyType; });
            });
        } catch (error) {
            output.textContent = "Não foi possível carregar as chaves.";
        }
    }

    async function createPixKey(type, button) {
        if (!ensureActiveAccount()) return;
        setLoading(button, true);
        try {
            var key = await api("/accounts/" + encodeURIComponent(state.activeReference) + "/pix-keys", {
                method: "POST",
                body: JSON.stringify({ type: type })
            });
            showToast("Chave PIX criada: " + key.value, "success");
        } catch (error) {
            showToast(error.message, "error");
        } finally {
            setLoading(button, false);
            await loadPixKeys();
        }
    }

    function keyTypeLabel(type) {
        if (type === "EMAIL") return "E-mail";
        if (type === "PHONE") return "Telefone";
        if (type === "CPF") return "CPF";
        return "Aleatória";
    }

    async function submitOperation(form, endpoint, payload, successMessage) {
        var submitButton = form.querySelector("button[type='submit']");
        setLoading(submitButton, true);
        try {
            await api(endpoint, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            form.reset();
            updateCurrentAccountInputs();
            await loadActiveAccount(state.activeReference, true);
            showToast(successMessage, "success");
        } catch (error) {
            showToast(error.message, "error");
        } finally {
            setLoading(submitButton, false);
        }
    }

    async function createAccount(event) {
        event.preventDefault();
        var form = event.currentTarget;
        var payload = {
            holderName: form.elements.holderName.value.trim(),
            password: form.elements.password.value,
            administrator: form.elements.administrator.checked
        };
        if (payload.administrator) {
            payload.administratorToken = form.elements.administratorToken.value;
        } else {
            payload.email = form.elements.email.value.trim();
            payload.phone = form.elements.phone.value.replace(/\D/g, "");
            payload.cpf = form.elements.cpf.value.replace(/\D/g, "");
        }

        var submitButton = form.querySelector("button[type='submit']");
        setLoading(submitButton, true);
        try {
            var account = await api("/accounts", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            form.reset();
            updateAccountFormMode();
            closeAllModals();
            openLoginModal(account.accountReference);
            showToast("Conta " + account.accountReference + " criada. Use-a para entrar.", "success");
        } catch (error) {
            showToast(error.message, "error");
        } finally {
            setLoading(submitButton, false);
        }
    }

    function openCloseAccountModal() {
        if (!ensureActiveAccount()) {
            return;
        }
        if (Number(state.account.balance) !== 0) {
            showToast("Zere o saldo da conta antes de solicitar o encerramento.", "error");
            return;
        }
        closeAccountModal.classList.remove("hidden");
        window.setTimeout(function () {
            document.querySelector("#close-account-form input[name='password']").focus();
        }, 20);
    }

    async function closeActiveAccount(event) {
        event.preventDefault();
        if (!ensureActiveAccount()) {
            return;
        }

        var form = event.currentTarget;
        var submitButton = form.querySelector("button[type='submit']");
        setLoading(submitButton, true);
        try {
            await api("/accounts/" + encodeURIComponent(state.activeReference) + "/close", {
                method: "POST",
                body: JSON.stringify({ password: form.elements.password.value })
            });
            form.reset();
            closeAllModals();
            await loadActiveAccount(state.activeReference, true);
            showToast("Conta encerrada com sucesso. O extrato continua disponível.", "success");
        } catch (error) {
            showToast(error.message, "error");
        } finally {
            setLoading(submitButton, false);
        }
    }

    function openAccountModal() {
        closeAllModals();
        updateAccountFormMode();
        accountModal.classList.remove("hidden");
        window.setTimeout(function () {
            document.querySelector("#create-account-form input[name='holderName']").focus();
        }, 20);
    }

    function updateAccountFormMode() {
        var form = document.getElementById("create-account-form");
        var administrator = form.elements.administrator.checked;
        form.querySelectorAll(".customer-account-field").forEach(function (field) {
            field.classList.toggle("hidden", administrator);
            field.querySelector("input").required = !administrator;
        });
        var tokenField = form.querySelector(".administrator-token-field");
        tokenField.classList.toggle("hidden", !administrator);
        tokenField.querySelector("input").required = administrator;
        document.getElementById("account-modal-title").textContent = administrator
            ? "Crie uma conta administradora."
            : "Abra sua conta em poucos passos.";
    }

    function openLoginModal(accountReference) {
        closeAllModals();
        loginModal.classList.remove("hidden");
        var input = document.querySelector("#login-form input[name='account']");
        if (typeof accountReference === "string") {
            input.value = accountReference;
        }
        window.setTimeout(function () {
            input.focus();
        }, 20);
    }

    function closeAllModals() {
        document.querySelectorAll(".modal").forEach(function (modal) {
            modal.classList.add("hidden");
        });
    }

    function setLoading(button, loading) {
        if (!button) {
            return;
        }
        if (loading) {
            button.dataset.originalText = button.textContent;
            button.textContent = "Processando...";
            button.disabled = true;
        } else {
            button.textContent = button.dataset.originalText || button.textContent;
            button.disabled = false;
        }
    }

    function normalizeAmount(value) {
        var raw = String(value || "").trim();
        if (raw.indexOf(",") !== -1) {
            return raw.replace(/\./g, "").replace(",", ".");
        }
        return raw;
    }

    function formatCurrency(value) {
        return new Intl.NumberFormat("pt-BR", {
            style: "currency",
            currency: "BRL"
        }).format(Number(value || 0));
    }

    function formatDate(value) {
        if (!value) {
            return "—";
        }
        return new Intl.DateTimeFormat("pt-BR", {
            day: "2-digit",
            month: "short",
            year: "numeric"
        }).format(new Date(value));
    }

    function formatDateTime(value) {
        if (!value) {
            return "—";
        }
        return new Intl.DateTimeFormat("pt-BR", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        }).format(new Date(value));
    }

    function formatStatus(status) {
        var labels = {
            ATIVA: "Ativa",
            BLOQUEADA: "Bloqueada",
            ENCERRADA: "Encerrada"
        };
        return labels[status] || status;
    }

    function statusClass(status) {
        var classes = {
            ATIVA: "active",
            BLOQUEADA: "blocked",
            ENCERRADA: "closed"
        };
        return classes[status] || "neutral";
    }

    function capitalize(value) {
        return value.charAt(0).toUpperCase() + value.slice(1);
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function showToast(message, type) {
        window.clearTimeout(toastTimer);
        toast.textContent = message;
        toast.className = "toast show " + (type || "");
        toastTimer = window.setTimeout(function () {
            toast.className = "toast";
        }, 4400);
    }

    init();
}());
