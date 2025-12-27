let analyticsChart;
const $ = id => document.getElementById(id);
const n = v => v == null ? 0 : Number(v);

function toast(msg){
  const t = $("toast");
  t.textContent = msg;
  t.classList.remove("hidden");
  setTimeout(()=>t.classList.add("hidden"),2000);
}

/* ---------- Sidebar ---------- */
$("openAnalytics").onclick = () => {
  $("reportViewer").classList.add("hidden");
  $("analyticsSection").classList.toggle("hidden");
};

/* ---------- Upload ---------- */
$("uploadBtn").onclick = async ()=>{
  const fd = new FormData();

  const execDate = $("executionDate").value;
  if (!execDate) return toast("Select execution date");

  fd.append("appId", $("appId").value);
  fd.append("release", $("release").value);
  fd.append("executionDate", execDate);
  fd.append("file", $("zipfile").files[0]);

  const r = await fetch("/api/upload", { method:"POST", body:fd });

  if (r.ok) {
    toast("Uploaded");

    // ✅ Clear form
    $("appId").value = "";
    $("release").value = "";
    $("executionDate").value = "";
    $("zipfile").value = "";

    loadApps();
    loadAnalyticsApps();
  }
};
/* ---------- Load Chart ---------- */
$("loadChart").onclick = async ()=>{
  analyticsChart?.destroy();

  const app=$("analyticsApp").value;
  if(!app) return toast("Select App");

  const from = $("fromDate").value;
  const to = $("toDate").value;
  if (!from || !to) return toast("Select From and To dates");

  if($("chartType").value==="appOverview"){
    const d = await (await fetch(
      `/api/charts/app?appId=${app}&from=${from}&to=${to}`
    )).json();
    drawOverview(d); return;
  }

  let url=`/api/charts/release?appId=${app}&release=${$("analyticsRelease").value}&from=${from}&to=${to}`;
  drawTrend(await (await fetch(url)).json());
};


/* ---------- Dashboard ---------- */
async function loadApps(){
  const apps = await (await fetch("/api/apps")).json();
  $("apps").innerHTML = `<option></option>`+apps.map(a=>`<option>${a}</option>`).join("");
}
loadApps();

$("apps").onchange = async ()=>{
  const rel = await (await fetch(`/api/releases?appId=${$("apps").value}`)).json();
  $("releases").innerHTML = `<option></option>`+rel.map(r=>`<option>${r}</option>`).join("");
};

$("loadRuns").onclick = async ()=>{
  let url=`/api/runs?appId=${$("apps").value}`;
  if($("releases").value) url+=`&release=${$("releases").value}`;

  const runs = await (await fetch(url)).json();
  $("runsTable").querySelector("tbody").innerHTML =
    runs.map(r=>`
      <tr>
        <td>${r.runId}</td>
        <td>${r.timestamp}</td>
        <td>${r.passed}</td>
        <td>${r.failed}</td>
        <td>${r.broken}</td>
        <td>${r.skipped}</td>
        <td>${r.total}</td>
        <td>${r.durationMs}</td>
        <td><button data-k="${encodeURIComponent(r.htmlPath)}">View</button></td>
      </tr>`).join("");

  document.querySelectorAll("button[data-k]").forEach(b=>{
    b.onclick=async()=>{
      $("reportViewer").classList.remove("hidden");
      $("reportFrame").srcdoc = await (await fetch(`/api/view?key=${b.dataset.k}`)).text();
      $("downloadReportLink").href=`/api/download?key=${b.dataset.k}`;
    }
  });
};

/* ---------- Analytics ---------- */
async function loadAnalyticsApps(){
  const apps = await (await fetch("/api/apps")).json();
  $("analyticsApp").innerHTML = `<option></option>`+apps.map(a=>`<option>${a}</option>`).join("");
}
loadAnalyticsApps();

$("analyticsApp").onchange = async ()=>{
  const rel = await (await fetch(`/api/releases?appId=${$("analyticsApp").value}`)).json();
  $("analyticsRelease").innerHTML = `<option></option>`+rel.map(r=>`<option>${r}</option>`).join("");
};

$("chartType").onchange = ()=>{
  $("analyticsRelease").disabled = $("chartType").value==="appOverview";
};

$("loadChart").onclick = async ()=>{
  analyticsChart?.destroy();

  const app = $("analyticsApp").value;
  if (!app) return toast("Select App");

  const from = $("fromDate").value;
  const to = $("toDate").value;

  if (!from || !to) {
    return toast("Select From and To dates");
  }

  if ($("chartType").value === "appOverview") {
    const res = await fetch(
      `/api/charts/app?appId=${app}&from=${from}&to=${to}`
    );

    if (!res.ok) {
      toast("No data for selected range");
      return;
    }

    const d = await res.json();
    if (!Array.isArray(d)) return toast("Invalid data");

    drawOverview(d);
    return;
  }

  const release = $("analyticsRelease").value;
  if (!release) return toast("Select Release");

  const res = await fetch(
    `/api/charts/release?appId=${app}&release=${release}&from=${from}&to=${to}`
  );

  if (!res.ok) {
    toast("No data for selected range");
    return;
  }

  const d = await res.json();
  if (!Array.isArray(d)) return toast("Invalid data");

  drawTrend(d);
};


function drawTrend(d){
  analyticsChart = new Chart($("analyticsChart"),{
    type:"bar",
    data:{
      labels:d.map(x=>x.execution_date),
      datasets:[
        {label:"Passed",data:d.map(x=>n(x.passed)),backgroundColor:"#4caf50"},
        {label:"Failed",data:d.map(x=>n(x.failed)),backgroundColor:"#f44336"},
        {label:"Broken",data:d.map(x=>n(x.broken)),backgroundColor:"#ff9800"},
        {label:"Skipped",data:d.map(x=>n(x.skipped)),backgroundColor:"#9e9e9e"}
      ]
    },
    options:{scales:{x:{stacked:true},y:{stacked:true}}}
  });
}

function drawOverview(d){
  analyticsChart = new Chart($("analyticsChart"),{
    type:"bar",
    data:{
      labels:d.map(x=>x.release),
      datasets:[
        {label:"Pass %",data:d.map(x=>Math.round(x.passPercent)),backgroundColor:"#4caf50"},
        {label:"Failed",data:d.map(x=>x.failed),backgroundColor:"#f44336"},
        {label:"Broken",data:d.map(x=>x.broken),backgroundColor:"#ff9800"},
        {label:"Skipped",data:d.map(x=>x.skipped),backgroundColor:"#9e9e9e"}
      ]
    }
  });
}
