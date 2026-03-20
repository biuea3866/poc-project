// ── State ─────────────────────────────────────────────────────────
let reviews = [];
let currentReviewId = null;
let currentReview = null;
let dragStart = null;
let dragEnd = null;
let isDragging = false;
let saveTimeout = null;

// ── DOM ──────────────────────────────────────────────────────────
const $reviewList = document.getElementById('review-list');
const $reviewCount = document.getElementById('review-count');
const $emptyState = document.getElementById('empty-state');
const $reviewDetail = document.getElementById('review-detail');
const $detailTitle = document.getElementById('detail-title');
const $detailRepo = document.getElementById('detail-repo');
const $detailBranch = document.getElementById('detail-branch');
const $detailAuthor = document.getElementById('detail-author');
const $detailSize = document.getElementById('detail-size');
const $detailSeverity = document.getElementById('detail-severity');
const $reviewBodyEditor = document.getElementById('review-body-editor');
const $diffContainer = document.getElementById('diff-container');
const $actionStatus = document.getElementById('action-status');
const $toastContainer = document.getElementById('toast-container');

// ── API ──────────────────────────────────────────────────────────
async function fetchReviews() {
  const res = await fetch('/api/reviews');
  reviews = await res.json();
  renderReviewList();
}

async function fetchReviewDetail(id) {
  const res = await fetch(`/api/reviews/${id}`);
  if (!res.ok) return null;
  return res.json();
}

async function saveReview(id, patch) {
  await fetch(`/api/reviews/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(patch),
  });
}

async function addComment(reviewId, comment) {
  const res = await fetch(`/api/reviews/${reviewId}/comments`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(comment),
  });
  return res.json();
}

async function deleteComment(reviewId, commentId) {
  await fetch(`/api/reviews/${reviewId}/comments/${commentId}`, { method: 'DELETE' });
}

async function publishReview(id) {
  const res = await fetch(`/api/reviews/${id}/publish`, { method: 'POST' });
  return res.json();
}

// ── Render: Review List ──────────────────────────────────────────
function renderReviewList() {
  $reviewCount.textContent = reviews.length;
  $reviewList.innerHTML = '';

  for (const r of reviews) {
    const div = document.createElement('div');
    div.className = `review-item${r.id === currentReviewId ? ' active' : ''}`;
    div.dataset.id = r.id;
    div.onclick = () => selectReview(r.id);

    const sc = r.severityCounts || {};
    let badgesHtml = '';
    for (const sev of ['P1','P2','P3','P4','P5','ASK']) {
      if (sc[sev] > 0) badgesHtml += `<span class="badge badge-${sev.toLowerCase()}">${sev}:${sc[sev]}</span>`;
    }

    div.innerHTML = `
      <div class="title">#${r.prNumber} ${r.title}</div>
      <div class="meta">
        <span>${r.repo}</span>
        <span>${r.author}</span>
        <span class="badge badge-status-${r.status}">${r.status}</span>
        <span class="badge badge-event-${r.event.toLowerCase()}">${r.event}</span>
      </div>
      <div class="badges">${badgesHtml}</div>
    `;

    $reviewList.appendChild(div);
  }
}

// ── Render: Review Detail ────────────────────────────────────────
async function selectReview(id) {
  currentReviewId = id;
  currentReview = await fetchReviewDetail(id);
  if (!currentReview) return;

  $emptyState.style.display = 'none';
  $reviewDetail.style.display = 'flex';

  const pr = currentReview.prInfo;
  const scope = currentReview.scope;
  const summary = currentReview.summary;

  $detailTitle.textContent = `#${pr.number} ${pr.title}`;
  $detailRepo.textContent = `${pr.owner}/${pr.repo}`;
  $detailBranch.textContent = `${pr.branch} → ${pr.baseBranch}`;
  $detailAuthor.textContent = pr.author;
  $detailSize.textContent = `${scope.size} (+${scope.totalAdditions} -${scope.totalDeletions}) | ${scope.totalFiles} files | ${scope.prType}`;

  // Severity badges
  const sc = summary.severityCounts;
  $detailSeverity.innerHTML = '';
  for (const sev of ['P1','P2','P3','P4','P5','ASK']) {
    if (sc[sev] > 0) {
      $detailSeverity.innerHTML += `<span class="badge badge-${sev.toLowerCase()}">${sev}: ${sc[sev]}</span>`;
    }
  }

  // Review body
  $reviewBodyEditor.value = currentReview.reviewBody;

  // Event selector
  document.querySelector(`input[name="event"][value="${currentReview.event}"]`).checked = true;

  // Summary findings (non-file-specific)
  renderSummaryFindings(currentReview);

  // Diff
  renderDiff(currentReview);

  // Update sidebar active
  document.querySelectorAll('.review-item').forEach(el => {
    el.classList.toggle('active', el.dataset.id === id);
  });

  $actionStatus.textContent = currentReview.status === 'posted' ? 'Posted' : '';
  document.getElementById('btn-publish').disabled = currentReview.status === 'posted';

  // Show review request path
  fetch(`/api/reviews/${id}/review-request-path`).then(r => r.json()).then(data => {
    const el = document.getElementById('review-request-info');
    el.textContent = data.fullPath;
    el.title = data.fullPath;
  }).catch(() => {});
}

// ── Render: Summary Findings ─────────────────────────────────────
function renderSummaryFindings(review) {
  const $summary = document.getElementById('summary-findings');
  $summary.innerHTML = '';

  // All items come from inline comments — every item is clickable
  const comments = review.inlineComments || [];
  if (comments.length === 0) return;

  const buckets = ['P1','P2','P3','P4','P5','ASK'].map(label => ({
    label,
    items: comments.filter(c => c.severity === label),
  })).filter(b => b.items.length > 0);

  for (const bucket of buckets) {
    const div = document.createElement('div');
    div.style.marginBottom = '16px';

    const header = document.createElement('h4');
    header.style.cssText = 'font-size:13px;margin-bottom:8px;display:flex;align-items:center;gap:6px';
    header.innerHTML = `<span class="badge badge-${bucket.label.toLowerCase()}">${bucket.label}</span><span style="color:var(--text-secondary)">${bucket.items.length}건</span>`;
    div.appendChild(header);

    const ul = document.createElement('ul');
    ul.style.cssText = 'list-style:none;padding:0;display:flex;flex-direction:column;gap:4px';

    for (const comment of bucket.items) {
      const li = document.createElement('li');
      li.style.cssText = 'padding:8px 12px;background:var(--bg-secondary);border:1px solid var(--border);border-radius:6px;font-size:12px;line-height:1.5;cursor:pointer;transition:border-color 0.15s';
      li.onmouseenter = () => { li.style.borderColor = 'var(--link)'; };
      li.onmouseleave = () => { li.style.borderColor = 'var(--border)'; };
      li.onclick = () => scrollToComment(comment.id);

      const file = comment.path ? comment.path.split('/').pop() : '';
      const lineInfo = comment.line > 1 ? `:${comment.line}` : '';
      const location = file ? `<span style="color:var(--text-muted)">${file}${lineInfo}</span> ` : '';

      li.innerHTML = `${location}${escapeHtml(comment.body.split('\\n')[0])}`;
      ul.appendChild(li);
    }

    div.appendChild(ul);
    $summary.appendChild(div);
  }
}

// ── Render: Diff ─────────────────────────────────────────────────
function renderDiff(review) {
  $diffContainer.innerHTML = '';

  // Build file map from changedFiles
  const fileMap = {};
  for (const f of (review.changedFiles || [])) {
    fileMap[f.filename] = f;
  }

  // Collect comments by file
  const commentsByFile = {};
  for (const c of review.inlineComments) {
    if (!commentsByFile[c.path]) commentsByFile[c.path] = [];
    commentsByFile[c.path].push(c);
  }

  // Render each changed file
  const rendered = new Set();
  const files = review.changedFiles || [];

  for (const file of files) {
    rendered.add(file.filename);
    renderFileSection(file.filename, file, commentsByFile[file.filename] || []);
  }

  // Also render files from findings that might not be in changedFiles
  for (const c of review.inlineComments) {
    if (c.path && !rendered.has(c.path)) {
      rendered.add(c.path);
      renderFileSection(c.path, null, commentsByFile[c.path] || []);
    }
  }
}

function renderFileSection(filename, fileData, comments) {
  const fileDiv = document.createElement('div');
  fileDiv.className = 'diff-file';
  fileDiv.dataset.filename = filename;

  const additions = fileData?.additions ?? 0;
  const deletions = fileData?.deletions ?? 0;

  fileDiv.innerHTML = `
    <div class="diff-file-header">
      <span class="filename">${filename}</span>
      <span class="stats">
        ${additions > 0 ? `<span class="add">+${additions}</span> ` : ''}
        ${deletions > 0 ? `<span class="del">-${deletions}</span> ` : ''}
        ${comments.length > 0 ? `<span style="margin-left:8px">${comments.length} comment(s)</span>` : ''}
      </span>
    </div>
  `;

  const table = document.createElement('table');
  table.className = 'diff-table';

  // Render diff patch lines
  if (fileData?.patch) {
    const lines = fileData.patch.split('\n');
    let newLine = 0;

    for (const line of lines) {
      const hunkMatch = line.match(/^@@\s+-\d+(?:,\d+)?\s+\+(\d+)/);
      if (hunkMatch) {
        newLine = parseInt(hunkMatch[1], 10) - 1;
        const tr = document.createElement('tr');
        tr.className = 'diff-line-hunk';
        tr.innerHTML = `<td class="diff-line-num" style="cursor:default"></td><td class="diff-line-content">${escapeCode(line)}</td>`;
        table.appendChild(tr);
        continue;
      }

      const isAdd = line.startsWith('+');
      const isDel = line.startsWith('-');
      if (!isDel) newLine++;

      const tr = document.createElement('tr');
      const lineClass = isAdd ? 'diff-line-add' : isDel ? 'diff-line-del' : '';
      const lineNum = isDel ? '' : newLine;

      const btn = !isDel && lineNum ? `<button class="add-comment-btn" onclick="showCommentForm('${filename}', ${lineNum})">+</button>` : '';
      tr.innerHTML = `<td class="diff-line-num ${lineClass}" data-line="${lineNum}" data-file="${filename}">${lineNum}${btn}</td><td class="diff-line-content ${lineClass}">${escapeCode(line)}</td>`;
      table.appendChild(tr);

      // Insert inline comments at matching line
      if (lineNum) {
        const lineComments = comments.filter(c => c.line === lineNum);
        for (const comment of lineComments) {
          table.appendChild(buildCommentRow(comment));
        }
      }
    }
  }

  // Render comments not attached to specific lines
  if (!fileData?.patch && comments.length > 0) {
    for (const comment of comments) {
      table.appendChild(buildCommentRow(comment));
    }
  }

  fileDiv.appendChild(table);
  $diffContainer.appendChild(fileDiv);

  // Drag-select for multiline comments
  setupDragSelect(table, filename);
}

function setupDragSelect(table, filename) {
  let dragStartLine = null;
  let dragCurrentLine = null;

  table.addEventListener('mousedown', (e) => {
    const cell = e.target.closest('.diff-line-num');
    if (!cell || !cell.dataset.line) return;
    const line = parseInt(cell.dataset.line);
    if (!line) return;

    dragStartLine = line;
    dragCurrentLine = line;
    isDragging = true;
    e.preventDefault();
    clearDragHighlight(table);
    highlightRange(table, line, line);
  });

  table.addEventListener('mousemove', (e) => {
    if (!isDragging || dragStartLine === null) return;
    const cell = e.target.closest('.diff-line-num');
    if (!cell || !cell.dataset.line) return;
    const line = parseInt(cell.dataset.line);
    if (!line || line === dragCurrentLine) return;

    dragCurrentLine = line;
    clearDragHighlight(table);
    const start = Math.min(dragStartLine, dragCurrentLine);
    const end = Math.max(dragStartLine, dragCurrentLine);
    highlightRange(table, start, end);
  });

  table.addEventListener('mouseup', () => {
    if (!isDragging || dragStartLine === null) return;
    const start = Math.min(dragStartLine, dragCurrentLine);
    const end = Math.max(dragStartLine, dragCurrentLine);
    isDragging = false;

    if (start !== end) {
      showCommentForm(filename, end, start);
    }

    dragStartLine = null;
    dragCurrentLine = null;
    setTimeout(() => clearDragHighlight(table), 100);
  });
}

function highlightRange(table, start, end) {
  const cells = table.querySelectorAll('.diff-line-num');
  for (const cell of cells) {
    const line = parseInt(cell.dataset.line);
    if (line >= start && line <= end) {
      cell.parentElement.classList.add('diff-line-selected');
    }
  }
}

function clearDragHighlight(table) {
  table.querySelectorAll('.diff-line-selected').forEach(el => el.classList.remove('diff-line-selected'));
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML.replace(/\n/g, '<br>');
}

function escapeCode(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// ── Comment Form ─────────────────────────────────────────────────
function showCommentForm(filename, line, startLine) {
  // Remove existing form
  const existingForm = document.querySelector('.comment-form-active');
  if (existingForm) existingForm.remove();

  const fileDiv = document.querySelector(`[data-filename="${filename}"]`);
  if (!fileDiv) return;

  const form = document.createElement('div');
  form.className = 'comment-form comment-form-active';
  const lineLabel = startLine && startLine !== line ? `L${startLine}-${line}` : line ? `L${line}` : '';

  form.innerHTML = `
    <div style="font-size:11px;color:var(--text-secondary);margin-bottom:6px">${lineLabel || 'File-level comment'}</div>
    <textarea placeholder="Write a comment..." id="new-comment-body"></textarea>
    <input type="hidden" id="new-comment-line" value="${line || 1}">
    <input type="hidden" id="new-comment-start-line" value="${startLine || ''}">
    <div class="form-actions">
      <div>
        <select id="new-comment-severity">
          <option value="P1">P1 - 즉시 수정</option>
          <option value="P2">P2 - 반드시 수정</option>
          <option value="P3" selected>P3 - 수정 권장</option>
          <option value="P4">P4 - 개선 제안</option>
          <option value="P5">P5 - 사소한 개선</option>
          <option value="ASK">ASK - 질문</option>
        </select>
      </div>
      <div>
        <button class="btn btn-secondary btn-sm" onclick="this.closest('.comment-form-active').remove()">Cancel</button>
        <button class="btn btn-primary btn-sm" onclick="submitComment('${filename}')">Add</button>
      </div>
    </div>
  `;

  fileDiv.appendChild(form);
  form.querySelector('textarea').focus();
}

async function submitComment(filename) {
  const body = document.getElementById('new-comment-body').value.trim();
  if (!body) return;

  const severity = document.getElementById('new-comment-severity').value;
  const line = parseInt(document.getElementById('new-comment-line').value) || 1;
  const startLineVal = document.getElementById('new-comment-start-line').value;
  const startLine = startLineVal ? parseInt(startLineVal) : undefined;

  const comment = await addComment(currentReviewId, {
    path: filename,
    line,
    startLine,
    body,
    severity,
  });

  // Refresh
  currentReview = await fetchReviewDetail(currentReviewId);
  renderDiff(currentReview);
  await fetchReviews();
}

function scrollToComment(commentId) {
  const row = document.querySelector(`tr[data-comment-id="${commentId}"]`);
  if (!row) return;
  row.scrollIntoView({ behavior: 'smooth', block: 'center' });
  row.classList.add('comment-highlight');
  setTimeout(() => row.classList.remove('comment-highlight'), 2000);
}

function buildCommentRow(comment) {
  const ctr = document.createElement('tr');
  ctr.className = 'comment-thread';
  ctr.dataset.commentId = comment.id;
  ctr.innerHTML = `<td></td><td style="padding:8px 16px"><div class="comment-item"><div class="comment-header"><div><span class="badge badge-${comment.severity.toLowerCase()}">${comment.severity}</span> <span class="badge ${comment.source === 'auto' ? 'badge-auto' : 'badge-user'}">${comment.source === 'auto' ? 'Auto' : 'You'}</span>${comment.line > 1 ? ` <span style="color:var(--text-muted);margin-left:4px">L${comment.startLine ? comment.startLine + '-' : ''}${comment.line}</span>` : ''}</div><div class="comment-actions"><button onclick="onEditComment('${comment.id}')" title="Edit" style="font-size:13px">&#9998;</button> <button onclick="onDeleteComment('${comment.id}')" title="Delete">&#10005;</button></div></div><div class="comment-body" id="comment-body-${comment.id}">${escapeHtml(comment.body)}</div></div></td>`;
  return ctr;
}

function onEditComment(commentId) {
  const bodyEl = document.getElementById(`comment-body-${commentId}`);
  if (!bodyEl) return;

  const comment = currentReview.inlineComments.find(c => c.id === commentId);
  if (!comment) return;

  const item = bodyEl.closest('.comment-item');
  const existingEditor = item.querySelector('.edit-area');
  if (existingEditor) return;

  bodyEl.style.display = 'none';

  const editArea = document.createElement('div');
  editArea.className = 'edit-area';
  editArea.innerHTML = `<textarea id="edit-text-${commentId}" style="width:100%;min-height:60px;background:var(--bg-primary);border:1px solid var(--border);border-radius:4px;color:var(--text-primary);padding:8px;font-size:12px;resize:vertical;margin-bottom:6px">${comment.body}</textarea><div style="display:flex;gap:6px;align-items:center"><select id="edit-severity-${commentId}" style="background:var(--bg-tertiary);border:1px solid var(--border);color:var(--text-primary);padding:4px 8px;border-radius:4px;font-size:12px"><option value="P1"${comment.severity === 'P1' ? ' selected' : ''}>P1</option><option value="P2"${comment.severity === 'P2' ? ' selected' : ''}>P2</option><option value="P3"${comment.severity === 'P3' ? ' selected' : ''}>P3</option><option value="P4"${comment.severity === 'P4' ? ' selected' : ''}>P4</option><option value="P5"${comment.severity === 'P5' ? ' selected' : ''}>P5</option><option value="ASK"${comment.severity === 'ASK' ? ' selected' : ''}>ASK</option></select><button class="btn btn-primary btn-sm" onclick="onSaveEdit('${commentId}')">Save</button><button class="btn btn-secondary btn-sm" onclick="onCancelEdit('${commentId}')">Cancel</button></div>`;
  item.appendChild(editArea);
  editArea.querySelector('textarea').focus();
}

async function onSaveEdit(commentId) {
  const newBody = document.getElementById(`edit-text-${commentId}`).value.trim();
  const newSeverity = document.getElementById(`edit-severity-${commentId}`).value;
  if (!newBody) return;

  const comment = currentReview.inlineComments.find(c => c.id === commentId);
  if (!comment) return;

  comment.body = newBody;
  comment.severity = newSeverity;

  await saveReview(currentReviewId, { inlineComments: currentReview.inlineComments });
  currentReview = await fetchReviewDetail(currentReviewId);
  renderDiff(currentReview);
  await fetchReviews();
}

function onCancelEdit(commentId) {
  const bodyEl = document.getElementById(`comment-body-${commentId}`);
  if (bodyEl) bodyEl.style.display = '';
  const item = bodyEl?.closest('.comment-item');
  const editArea = item?.querySelector('.edit-area');
  if (editArea) editArea.remove();
}

async function onDeleteComment(commentId) {
  if (!confirm('Delete this comment?')) return;
  await deleteComment(currentReviewId, commentId);
  currentReview = await fetchReviewDetail(currentReviewId);
  renderDiff(currentReview);
}

// ── Event Handlers ───────────────────────────────────────────────
document.querySelectorAll('input[name="event"]').forEach(input => {
  input.addEventListener('change', (e) => {
    if (!currentReviewId) return;
    debouncedSave({ event: e.target.value });
  });
});

$reviewBodyEditor.addEventListener('input', () => {
  if (!currentReviewId) return;
  debouncedSave({ reviewBody: $reviewBodyEditor.value });
});

function debouncedSave(patch) {
  if (saveTimeout) clearTimeout(saveTimeout);
  saveTimeout = setTimeout(async () => {
    await saveReview(currentReviewId, patch);
    $actionStatus.textContent = 'Saved';
    setTimeout(() => { if ($actionStatus.textContent === 'Saved') $actionStatus.textContent = ''; }, 2000);
  }, 500);
}

document.getElementById('btn-save').addEventListener('click', async () => {
  if (!currentReviewId) return;
  const event = document.querySelector('input[name="event"]:checked')?.value;
  await saveReview(currentReviewId, {
    reviewBody: $reviewBodyEditor.value,
    event,
  });
  $actionStatus.textContent = 'Saved';
});

document.getElementById('btn-publish').addEventListener('click', async () => {
  if (!currentReviewId) return;
  if (!confirm('Post this review to GitHub?')) return;

  $actionStatus.textContent = 'Publishing...';
  const result = await publishReview(currentReviewId);

  if (result.posted) {
    $actionStatus.textContent = 'Posted!';
    document.getElementById('btn-publish').disabled = true;
    await fetchReviews();
  } else {
    $actionStatus.textContent = `Failed: ${result.message}`;
  }
});

// ── SSE ──────────────────────────────────────────────────────────
function connectSSE() {
  const evtSource = new EventSource('/api/events');

  evtSource.onmessage = async (e) => {
    try {
      const data = JSON.parse(e.data);
      if (data.type === 'new_review') {
        showToast(`New review: ${data.repo} #${data.pr}`);
        await fetchReviews();

        // Auto-select if no current review
        if (!currentReviewId) {
          selectReview(data.id);
        }
      }
    } catch (err) {
      // ignore parse errors from keepalive
    }
  };

  evtSource.onerror = () => {
    setTimeout(connectSSE, 5000);
  };
}

function showToast(message) {
  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.textContent = message;
  toast.onclick = () => toast.remove();
  $toastContainer.appendChild(toast);
  setTimeout(() => toast.remove(), 5000);

  // Desktop notification
  if (Notification.permission === 'granted') {
    new Notification('PR Review', { body: message });
  }
}

// ── Init ─────────────────────────────────────────────────────────
async function init() {
  if (Notification.permission === 'default') {
    Notification.requestPermission();
  }
  await fetchReviews();
  connectSSE();
}

init();
