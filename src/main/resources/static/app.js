const state = {
    activity: null,
    activeTab: "commits"
};

const elements = {
    form: document.querySelector("#activityForm"),
    username: document.querySelector("#username"),
    owner: document.querySelector("#owner"),
    repo: document.querySelector("#repo"),
    branch: document.querySelector("#branch"),
    days: document.querySelector("#days"),
    sendToSlack: document.querySelector("#sendToSlack"),
    rangeButtons: document.querySelectorAll(".range-button"),
    fetchButton: document.querySelector("#fetchButton"),
    generateButton: document.querySelector("#generateButton"),
    historyButton: document.querySelector("#historyButton"),
    copyButton: document.querySelector("#copyButton"),
    messageArea: document.querySelector("#messageArea"),
    apiStatus: document.querySelector("#apiStatus"),
    activityMeta: document.querySelector("#activityMeta"),
    commitCount: document.querySelector("#commitCount"),
    prCount: document.querySelector("#prCount"),
    commitsList: document.querySelector("#commitsList"),
    prsList: document.querySelector("#prsList"),
    standupText: document.querySelector("#standupText"),
    standupMeta: document.querySelector("#standupMeta"),
    historyList: document.querySelector("#historyList"),
    tabs: document.querySelectorAll(".tab")
};

document.addEventListener("DOMContentLoaded", () => {
    wireEvents();
    checkHealth();
    renderEmptyActivity();
    fetchHistory({ silent: true });
});

function wireEvents() {
    elements.form.addEventListener("submit", async (event) => {
        event.preventDefault();
        await fetchActivity();
    });

    elements.generateButton.addEventListener("click", generateStandup);
    elements.historyButton.addEventListener("click", () => fetchHistory({ silent: false }));
    elements.copyButton.addEventListener("click", copyStandup);

    elements.rangeButtons.forEach((button) => {
        button.addEventListener("click", () => {
            elements.days.value = button.dataset.days;
            syncRangeButtons();
        });
    });

    elements.days.addEventListener("input", syncRangeButtons);

    elements.tabs.forEach((button) => {
        button.addEventListener("click", () => {
            state.activeTab = button.dataset.tab;
            renderTabs();
        });
    });
}

async function checkHealth() {
    try {
        await requestJson("/api/health");
        elements.apiStatus.textContent = "API online";
        elements.apiStatus.classList.add("ok");
        elements.apiStatus.classList.remove("error");
    } catch (error) {
        elements.apiStatus.textContent = "API offline";
        elements.apiStatus.classList.add("error");
        elements.apiStatus.classList.remove("ok");
    }
}

async function fetchActivity() {
    setBusy(true, "Fetching GitHub activity...");

    try {
        const activity = await requestJson(buildUrl("/api/github/activity"));
        state.activity = activity;
        renderActivity(activity);
        setMessage("Activity loaded.");
    } catch (error) {
        setMessage(error.message, true);
    } finally {
        setBusy(false);
    }
}

async function generateStandup() {
    setBusy(true, "Generating standup...");

    try {
        const response = await requestJson(buildUrl("/api/standup/generate", { includeSlack: true }));
        state.activity = response.activity;
        renderActivity(response.activity);
        renderStandup(response);
        await fetchHistory({ silent: true });
        setMessage("Standup generated and saved.");
    } catch (error) {
        setMessage(error.message, true);
    } finally {
        setBusy(false);
    }
}

async function fetchHistory({ silent }) {
    if (!silent) {
        setMessage("Loading history...");
    }

    const username = encodeURIComponent(elements.username.value.trim() || "JanWeeee");

    try {
        const history = await requestJson(`/api/standup/history/${username}`);
        renderHistory(history);
        if (!silent) {
            setMessage("History loaded.");
        }
    } catch (error) {
        renderHistory([]);
        if (!silent) {
            setMessage(error.message, true);
        }
    }
}

async function copyStandup() {
    const text = elements.standupText.textContent.trim();
    if (!text || text === "Generate a standup to see the result here.") {
        setMessage("No standup to copy yet.", true);
        return;
    }

    try {
        await navigator.clipboard.writeText(text);
        setMessage("Standup copied.");
    } catch (error) {
        setMessage("Copy failed. Select the text manually.", true);
    }
}

function buildUrl(basePath, options = {}) {
    const username = encodeURIComponent(elements.username.value.trim());
    const params = new URLSearchParams();

    params.set("days", normalizeDays());
    addParam(params, "owner", elements.owner.value);
    addParam(params, "repo", elements.repo.value);
    addParam(params, "branch", elements.branch.value);
    if (options.includeSlack && elements.sendToSlack.checked) {
        params.set("sendToSlack", "true");
    }

    return `${basePath}/${username}?${params.toString()}`;
}

function addParam(params, key, value) {
    const trimmed = value.trim();
    if (trimmed) {
        params.set(key, trimmed);
    }
}

function normalizeDays() {
    const parsed = Number.parseInt(elements.days.value, 10);
    if (Number.isNaN(parsed)) {
        return 1;
    }
    return Math.min(Math.max(parsed, 1), 30);
}

async function requestJson(url) {
    const response = await fetch(url, {
        headers: {
            Accept: "application/json"
        }
    });

    const contentType = response.headers.get("content-type") || "";
    const body = contentType.includes("application/json") ? await response.json() : await response.text();

    if (!response.ok) {
        const message = typeof body === "object" && body.message ? body.message : "Request failed.";
        throw new Error(message);
    }

    return body;
}

function renderActivity(activity) {
    const commits = activity.commits || [];
    const prs = activity.pullRequests || [];

    elements.activityMeta.textContent = `${activity.username} / last ${activity.days} ${activity.days === 1 ? "day" : "days"}`;
    elements.commitCount.textContent = activity.totalCommits ?? commits.length;
    elements.prCount.textContent = activity.totalPRs ?? prs.length;

    elements.commitsList.innerHTML = commits.length
        ? commits.map(renderCommit).join("")
        : emptyState("No commits found for this range.");

    elements.prsList.innerHTML = prs.length
        ? prs.map(renderPullRequest).join("")
        : emptyState("No pull requests found for this range.");

    renderTabs();
}

function renderEmptyActivity() {
    elements.commitsList.innerHTML = emptyState("Fetch activity to preview commits.");
    elements.prsList.innerHTML = emptyState("Fetch activity to preview pull requests.");
}

function renderCommit(commit) {
    return `
        <article class="list-item">
            <h3>${escapeHtml(commit.message || "Untitled commit")}</h3>
            <div class="item-meta">
                <span>${escapeHtml(commit.repository || "Unknown repository")}</span>
                <span>${formatDate(commit.date)}</span>
                <a href="${escapeAttribute(commit.url || "#")}" target="_blank" rel="noreferrer">View commit</a>
            </div>
        </article>
    `;
}

function renderPullRequest(pr) {
    return `
        <article class="list-item">
            <h3>${escapeHtml(pr.title || "Untitled pull request")}</h3>
            <div class="item-meta">
                <span>${escapeHtml(pr.repository || "Unknown repository")}</span>
                <span>${escapeHtml(pr.state || "unknown")}</span>
                <span>${formatDate(pr.updatedAt)}</span>
                <a href="${escapeAttribute(pr.url || "#")}" target="_blank" rel="noreferrer">View PR</a>
            </div>
        </article>
    `;
}

function renderStandup(response) {
    elements.standupText.textContent = response.standupText || "No standup text returned.";
    const delivery = response.slackDelivered === true
        ? " / Slack delivered"
        : response.slackDelivered === false
            ? " / Slack not sent"
            : "";
    elements.standupMeta.textContent = `Saved as #${response.id} at ${formatDate(response.generatedAt)}${delivery}`;
}

function renderHistory(history) {
    elements.historyList.innerHTML = history.length
        ? history.slice(0, 6).map(renderHistoryItem).join("")
        : emptyState("No saved standups yet.");
}

function renderHistoryItem(item) {
    return `
        <article class="history-item">
            <h3>#${item.id} for ${escapeHtml(item.username)}</h3>
            <div class="history-meta">
                <span>${formatDate(item.generatedAt)}</span>
                <span>${item.commitCount} commits</span>
                <span>${item.prCount} PRs</span>
            </div>
            <pre>${escapeHtml(item.standupText || "")}</pre>
        </article>
    `;
}

function renderTabs() {
    elements.tabs.forEach((button) => {
        button.classList.toggle("active", button.dataset.tab === state.activeTab);
    });

    elements.commitsList.classList.toggle("hidden", state.activeTab !== "commits");
    elements.prsList.classList.toggle("hidden", state.activeTab !== "prs");
}

function syncRangeButtons() {
    const value = elements.days.value;
    elements.rangeButtons.forEach((button) => {
        button.classList.toggle("active", button.dataset.days === value);
    });
}

function setBusy(isBusy, message) {
    elements.fetchButton.disabled = isBusy;
    elements.generateButton.disabled = isBusy;
    elements.historyButton.disabled = isBusy;

    if (message) {
        setMessage(message);
    }
}

function setMessage(message, isError = false) {
    elements.messageArea.textContent = message || "";
    elements.messageArea.classList.toggle("error", isError);
}

function emptyState(message) {
    return `<div class="empty-state">${escapeHtml(message)}</div>`;
}

function formatDate(value) {
    if (!value) {
        return "No date";
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return new Intl.DateTimeFormat(undefined, {
        dateStyle: "medium",
        timeStyle: "short"
    }).format(date);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function escapeAttribute(value) {
    return escapeHtml(value);
}
