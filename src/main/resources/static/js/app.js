/* =========================================================
   API BASE (AUTO-DETECT HOST, PORT, PROTOCOL)
========================================================= */
const API_BASE = window.location.origin;

/* =========================================================
   HELPERS
========================================================= */
let analyticsChart;
const $ = id => document.getElementById(id);
const n = v => v == null ? 0 : Number(v);

async function apiFetch(path, options = {}) {
  return fetch(`${API_BASE}${path}`, options);
}

function toast(msg) {
  const t = $("toast");
  t.textContent = msg;
  t.classList.remove("hidden");
  setTimeout(() => t.classList.add("hidden"), 2000);
}

/* =========================================================
   SIDEBAR
========================================================= */
$("openAnalytics").onclick = () => {
  $("reportViewer").classList.add("hidden");
  $("analyticsSection").classList.toggle("hidden");
};

/* =========================================================
   UPLOAD
========================================================= */
$("uploadBtn").onclick = async () => {
  const fd = new FormData();

  const execDate = $("executionDate").value;
  if (!execDate) return toast("Select execution date");
  if (!$("zipfile").files.length) return toast("Select Allure ZIP");

  fd.append("appId", $("appId").value);
  fd.append("release", $("release").value);
  fd.append("executionDate", execDate);
  fd.append("file", $("zipfile").files[0]);

  try {
    const r = await apiFetch("/api/upload", {
      method: "POST",
      body: fd
    });

    if (!r.ok) throw new Error(await r.text());

    toast("Uploaded successfully");

    $("appId").value = "";
    $("release").value = "";
    $("executionDate").value = "";
    $("zipfile").value = "";

    await refreshAllDropdowns();
  } catch (e) {
    console.error(e);
    toast("Upload failed");
  }
};

/* DASHBOARD (RUNS) */
async function loadApps() {
  const apps = await (await apiFetch("/api/apps")).json();
  $("apps").innerHTML =
    `<option></option>` +
    apps.map(a => `<option>${a}</option>`).join("");
}

loadApps();

$("apps").onchange = async () => {
  $("releases").innerHTML = `<option></option>`;
  if (!$("apps").value) return;

  const rel = await (await apiFetch(
    `/api/releases?appId=${$("apps").value}`
  )).json();

  $("releases").innerHTML =
    `<option></option>` +
    rel.map(r => `<option>${r}</option>`).join("");
};

$("loadRuns").onclick = async () => {
  const app = $("apps").value;
  const release = $("releases").value;

  if (!app) return toast("Select App");
  if (!release) return toast("Select Release");

  const runs = await (await apiFetch(
    `/api/runs?appId=${app}&release=${release}`
  )).json();

  $("runsTable").querySelector("tbody").innerHTML =
    runs.map(r => `
      <tr>
        <td>${r.runId}</td>
        <td>${r.timestamp}</td>
        <td>${r.passed}</td>
        <td>${r.failed}</td>
        <td>${r.broken}</td>
        <td>${r.skipped}</td>
        <td>${r.total}</td>
        <td>${r.durationMs}</td>
        <td>
          <button data-view="${encodeURIComponent(r.htmlPath)}">View</button>
          <button data-del="${r.runId}" class="danger">Delete</button>
        </td>
      </tr>
    `).join("");

  // View report
  document.querySelectorAll("button[data-view]").forEach(b => {
    b.onclick = async () => {
      $("reportViewer").classList.remove("hidden");
      $("reportFrame").srcdoc =
        await (await apiFetch(`/api/view?key=${b.dataset.view}`)).text();
      $("downloadReportLink").href =
        `${API_BASE}/api/download?key=${b.dataset.view}`;
    };
  });

  // Delete run
  document.querySelectorAll("button[data-del]").forEach(b => {
    b.onclick = () => deleteRun(b.dataset.del);
  });
};

$("closeViewer").onclick = () => {
  $("reportViewer").classList.add("hidden");
};

/*ANALYTICS*/
async function loadAnalyticsApps() {
  const apps = await (await apiFetch("/api/apps")).json();
  $("analyticsApp").innerHTML =
    `<option></option>` +
    apps.map(a => `<option>${a}</option>`).join("");
}

loadAnalyticsApps();

$("analyticsApp").onchange = async () => {
  $("analyticsRelease").innerHTML = `<option></option>`;
  if (!$("analyticsApp").value) return;

  const rel = await (await apiFetch(
    `/api/releases?appId=${$("analyticsApp").value}`
  )).json();

  $("analyticsRelease").innerHTML =
    `<option></option>` +
    rel.map(r => `<option>${r}</option>`).join("");
};

$("chartType").onchange = () => {
  $("analyticsRelease").disabled =
    $("chartType").value === "appOverview";
};

$("loadChart").onclick = async () => {
  analyticsChart?.destroy();

  const app = $("analyticsApp").value;
  const from = $("fromDate").value;
  const to = $("toDate").value;

  if (!app) return toast("Select App");
  if (!from || !to) return toast("Select date range");

  try {
    if ($("chartType").value === "appOverview") {
      const res = await apiFetch(
        `/api/charts/app?appId=${app}&from=${from}&to=${to}`
      );
      drawOverview(await res.json());
      return;
    }

    const release = $("analyticsRelease").value;
    if (!release) return toast("Select Release");

    const res = await apiFetch(
      `/api/charts/release?appId=${app}&release=${release}&from=${from}&to=${to}`
    );
    drawTrend(await res.json());
  } catch (e) {
    console.error(e);
    toast("No analytics data");
  }
};

/* CHARTS*/
function drawTrend(d) {
  analyticsChart = new Chart($("analyticsChart"), {
    type: "bar",
    data: {
      labels: d.map(x => x.execution_date),
      datasets: [
        { label: "Passed", data: d.map(x => n(x.passed)), backgroundColor: "#4caf50" },
        { label: "Failed", data: d.map(x => n(x.failed)), backgroundColor: "#f44336" },
        { label: "Broken", data: d.map(x => n(x.broken)), backgroundColor: "#ff9800" },
        { label: "Skipped", data: d.map(x => n(x.skipped)), backgroundColor: "#9e9e9e" }
      ]
    },
    options: { scales: { x: { stacked: true }, y: { stacked: true } } }
  });
}

function drawOverview(d) {
  analyticsChart = new Chart($("analyticsChart"), {
    type: "bar",
    data: {
      labels: d.map(x => x.release),
      datasets: [
        { label: "Pass %", data: d.map(x => Math.round(x.passPercent)), backgroundColor: "#4caf50" },
        { label: "Failed", data: d.map(x => x.failed), backgroundColor: "#f44336" },
        { label: "Broken", data: d.map(x => x.broken), backgroundColor: "#ff9800" },
        { label: "Skipped", data: d.map(x => x.skipped), backgroundColor: "#9e9e9e" }
      ]
    }
  });
}

/* DELETE + REFRESH*/
async function refreshAllDropdowns() {
  await loadApps();
  await loadAnalyticsApps();

  $("apps").value = "";
  $("analyticsApp").value = "";

  $("releases").innerHTML = `<option></option>`;
  $("analyticsRelease").innerHTML = `<option></option>`;
}

async function deleteRun(runId) {
  if (!confirm("Delete this run?")) return;

  const res = await apiFetch(`/api/admin/run/${runId}`, {
    method: "DELETE"
  });

  if (res.ok) {
    toast("Run deleted");
    await refreshAllDropdowns();
    $("runsTable").querySelector("tbody").innerHTML = "";
  } else {
    toast("Delete failed");
  }
}

/*    DELETE SECTION */
async function loadDeleteApps() {
  const apps = await (await apiFetch("/api/apps")).json();
  $("deleteApp").innerHTML = `<option></option>` + apps.map(a => `<option>${a}</option>`).join("");
}
loadDeleteApps();

$("deleteApp").onchange = async () => {
  $("deleteRelease").innerHTML = `<option></option>`;
  $("deleteRun").innerHTML = `<option></option>`;
  if (!$("deleteApp").value) return;

  const rel = await (await apiFetch(`/api/releases?appId=${$("deleteApp").value}`)).json();
  $("deleteRelease").innerHTML = `<option></option>` + rel.map(r => `<option>${r}</option>`).join("");
};

$("deleteRelease").onchange = async () => {
  $("deleteRun").innerHTML = `<option></option>`;
  if (!$("deleteRelease").value) return;

  const runs = await (await apiFetch(`/api/runs?appId=${$("deleteApp").value}&release=${$("deleteRelease").value}`)).json();
  $("deleteRun").innerHTML = `<option></option>` + runs.map(r => `<option value="${r.runId}">${r.runId}</option>`).join("");
};

$("deleteRunBtn").onclick = async () => {
  const runId = $("deleteRun").value;
  if (!runId || !confirm(`Delete run ${runId}?`)) return;
  await apiFetch(`/api/admin/run/${runId}`, { method: "DELETE" });
  toast("Run deleted");
  await refreshAllDropdowns();
  await loadDeleteApps();
};

$("deleteReleaseBtn").onclick = async () => {
  const app = $("deleteApp").value;
  const rel = $("deleteRelease").value;
  if (!app || !rel || !confirm(`Delete release ${rel}?`)) return;
  await apiFetch(`/api/admin/release?appId=${app}&release=${rel}`, { method: "DELETE" });
  toast("Release deleted");
  await refreshAllDropdowns();
  await loadDeleteApps();
};

$("deleteAppBtn").onclick = async () => {
  const app = $("deleteApp").value;
  if (!app || !confirm(`Delete app ${app}?`)) return;
  await apiFetch(`/api/admin/app/${app}`, { method: "DELETE" });
  toast("App deleted");
  await refreshAllDropdowns();
  await loadDeleteApps();
};

async function refreshAllDropdowns() {
  await loadApps();
  await loadAnalyticsApps();
  $("apps").value = "";
  $("analyticsApp").value = "";
  $("releases").innerHTML = `<option></option>`;
  $("analyticsRelease").innerHTML = `<option></option>`;
}

function goToDashboard() {
  window.location.href = "/analytics.html";
}


