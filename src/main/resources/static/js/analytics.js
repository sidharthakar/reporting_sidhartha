const API_BASE = window.location.origin;

let analyticsChart;
const $ = id => document.getElementById(id);
const n = v => v == null ? 0 : Number(v);

async function apiFetch(path) {
  return fetch(`${API_BASE}${path}`);
}

function toast(msg) {
  const t = $("toast");
  t.textContent = msg;
  t.classList.remove("hidden");
  setTimeout(() => t.classList.add("hidden"), 2000);
}

/* LOAD APPS */
async function loadAnalyticsApps() {
  const apps = await (await apiFetch("/api/apps")).json();
  $("analyticsApp").innerHTML =
    `<option></option>` + apps.map(a => `<option>${a}</option>`).join("");
}
loadAnalyticsApps();

$("analyticsApp").onchange = async () => {
  $("analyticsRelease").innerHTML = `<option></option>`;
  if (!$("analyticsApp").value) return;

  const rel = await (await apiFetch(
    `/api/releases?appId=${$("analyticsApp").value}`
  )).json();

  $("analyticsRelease").innerHTML =
    `<option></option>` + rel.map(r => `<option>${r}</option>`).join("");
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

  if (!app || !from || !to)
    return toast("Select all filters");

  if ($("chartType").value === "appOverview") {
    const d = await (await apiFetch(
      `/api/charts/app?appId=${app}&from=${from}&to=${to}`
    )).json();
    drawOverview(d);
    return;
  }

  const rel = $("analyticsRelease").value;
  if (!rel) return toast("Select Release");

  const d = await (await apiFetch(
    `/api/charts/release?appId=${app}&release=${rel}&from=${from}&to=${to}`
  )).json();

  drawTrend(d);
};

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
