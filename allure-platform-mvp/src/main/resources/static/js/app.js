document.addEventListener("DOMContentLoaded", () => {

  function showMessage(text, type = "success", timeout = 2500) {
    const t = document.getElementById("toast");
    t.textContent = text;
    t.className = "toast " + (type === "error" ? "error" : "success");
    t.classList.remove("hidden");
    clearTimeout(showMessage._hid);
    showMessage._hid = setTimeout(() => t.classList.add("hidden"), timeout);
  }

  function safeText(v) { return v == null ? "" : String(v); }

  const appIdInput = document.getElementById("appId");
  const releaseInput = document.getElementById("release");
  const zipInput = document.getElementById("zipfile");
  const uploadBtn = document.getElementById("uploadBtn");
  const loader = document.getElementById("uploadLoader");

  const appsSelect = document.getElementById("apps");
  const releasesSelect = document.getElementById("releases");
  const loadRunsBtn = document.getElementById("loadRuns");
  const runsTableBody = document.querySelector("#runsTable tbody");

  const viewer = document.getElementById("reportViewer");
  const viewerFrame = document.getElementById("reportFrame");
  const viewerClose = document.getElementById("closeViewer");
  const viewerDownload = document.getElementById("downloadReportLink");

  if (!appsSelect || !releasesSelect || !uploadBtn || !loadRunsBtn) {
    console.error("Required DOM elements missing");
    return;
  }

  viewer.classList.add("hidden");

  uploadBtn.addEventListener("click", async () => {
    const appId = appIdInput.value.trim();
    const release = releaseInput.value.trim();
    const file = zipInput.files[0];

    if (!appId || !release || !file) {
      showMessage("AppId, Release & ZIP required", "error");
      return;
    }

    loader.classList.remove("hidden");

    const fd = new FormData();
    fd.append("appId", appId);
    fd.append("release", release);
    fd.append("file", file);

    try {
      const res = await fetch("/api/upload", { method: "POST", body: fd });
      const json = await res.json();

      if (res.ok && !json.error) {
        showMessage("Upload successful", "success");
        appIdInput.value = "";
        releaseInput.value = "";
        zipInput.value = "";
        await loadApps();
      } else {
        const err = json && json.error ? json.error : `Upload failed (${res.status})`;
        showMessage(err, "error");
      }
    } catch (e) {
      console.error(e);
      showMessage("Upload failed (network)", "error");
    } finally {
      loader.classList.add("hidden");
    }
  });

  async function loadApps() {
    try {
      const res = await fetch("/api/apps");
      if (!res.ok) { showMessage("Failed to load apps", "error"); return; }
      const apps = await res.json();
      appsSelect.innerHTML = `<option value="">--select--</option>`;
      apps.forEach(a => {
        const o = document.createElement("option");
        o.value = a;
        o.textContent = a;
        appsSelect.appendChild(o);
      });
      releasesSelect.innerHTML = `<option value="">--select--</option>`;
      runsTableBody.innerHTML = "";
      viewer.classList.add("hidden");
    } catch (e) {
      console.error(e);
      showMessage("Failed to load apps", "error");
    }
  }

  appsSelect.addEventListener("change", async () => {
    const app = appsSelect.value;
    releasesSelect.innerHTML = `<option value="">--select--</option>`;
    runsTableBody.innerHTML = "";
    viewer.classList.add("hidden");
    if (!app) return;
    try {
      const res = await fetch("/api/releases?appId=" + encodeURIComponent(app));
      if (!res.ok) { showMessage("Failed to load releases", "error"); return; }
      const rels = await res.json();
      rels.forEach(r => {
        const o = document.createElement("option");
        o.value = r;
        o.textContent = r;
        releasesSelect.appendChild(o);
      });
    } catch (e) {
      console.error(e);
      showMessage("Failed to load releases", "error");
    }
  });

  loadRunsBtn.addEventListener("click", async () => {
    const app = appsSelect.value;
    const rel = releasesSelect.value;
    if (!app || !rel) { showMessage("Select app and release", "error"); return; }

    try {
      const res = await fetch(`/api/runs?appId=${encodeURIComponent(app)}&release=${encodeURIComponent(rel)}`);
      if (!res.ok) { showMessage("Failed to load runs", "error"); return; }
      const runs = await res.json();
      runsTableBody.innerHTML = "";
      viewer.classList.add("hidden");

      runs.forEach(r => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td>${safeText(r.runId)}</td>
          <td>${safeText(r.timestamp)}</td>
          <td>${safeText(r.passed)}</td>
          <td>${safeText(r.failed)}</td>
          <td>${safeText(r.broken)}</td>
          <td>${safeText(r.skipped)}</td>
          <td>${safeText(r.total)}</td>
          <td>${safeText(r.durationMs)}</td>
          <td>
            <button class="view-btn" data-path="${encodeURIComponent(r.htmlPath)}">View</button>
            <a class="btn-download" href="/api/download?key=${encodeURIComponent(r.htmlPath)}">Download</a>
          </td>`;
        runsTableBody.appendChild(tr);
      });

      document.querySelectorAll(".view-btn").forEach(b => {
        b.addEventListener("click", async () => {
          const p = decodeURIComponent(b.getAttribute("data-path"));
          openViewer(p);
        });
      });
    } catch (e) {
      console.error(e);
      showMessage("Failed to load runs", "error");
    }
  });

  async function openViewer(path) {
    viewer.classList.remove("hidden");
    viewerDownload.href = "/api/download?key=" + encodeURIComponent(path);
    try {
      const res = await fetch("/api/view?key=" + encodeURIComponent(path));
      if (!res.ok) { viewerFrame.srcdoc = `<p>Report not found (${res.status})</p>`; return; }
      const html = await res.text();
      viewerFrame.srcdoc = html;
    } catch (e) {
      console.error(e);
      viewerFrame.srcdoc = `<p>Failed to load report</p>`;
    }
  }

  viewerClose.addEventListener("click", () => {
    viewer.classList.add("hidden");
  });

  // init
  loadApps();
});
