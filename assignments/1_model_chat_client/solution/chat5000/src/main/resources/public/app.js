const state = {
    token: localStorage.getItem('chat5000_token') || '',
    sessions: JSON.parse(localStorage.getItem('chat5000_sessions') || '[]'),
    activeSessionId: localStorage.getItem('chat5000_active_session') || '',
};

const el = {
    authGate: document.getElementById('authGate'),
    chatArea: document.getElementById('chatArea'),
    oauthForm: document.getElementById('oauthForm'),
    useTokenBtn: document.getElementById('useTokenBtn'),
    manualToken: document.getElementById('manualToken'),
    authError: document.getElementById('authError'),
    logoutBtn: document.getElementById('logoutBtn'),
    modelSelect: document.getElementById('modelSelect'),
    modeSelect: document.getElementById('modeSelect'),
    streamToggle: document.getElementById('streamToggle'),
    messages: document.getElementById('messages'),
    chatForm: document.getElementById('chatForm'),
    promptInput: document.getElementById('promptInput'),
    newSessionBtn: document.getElementById('newSessionBtn'),
    clearHistoryBtn: document.getElementById('clearHistoryBtn'),
    sessionList: document.getElementById('sessionList'),
    valkeyStatus: document.getElementById('valkeyStatus'),
    mCacheHits: document.getElementById('mCacheHits'),
    mRequests: document.getElementById('mRequests'),
};

function authHeaders(extra = {}) {
    return {
        ...extra,
        'Authorization': `Bearer ${state.token}`,
    };
}

function createSession(label) {
    const id = crypto.randomUUID();
    state.sessions.unshift({ id, label: label || `Session ${state.sessions.length + 1}` });
    state.activeSessionId = id;
    saveSessions();
    renderSessions();
    el.messages.innerHTML = '';
}

function saveSessions() {
    localStorage.setItem('chat5000_sessions', JSON.stringify(state.sessions));
    localStorage.setItem('chat5000_active_session', state.activeSessionId);
}

function renderSessions() {
    el.sessionList.innerHTML = '';
    state.sessions.forEach(s => {
        const li = document.createElement('li');
        li.className = `session-item ${s.id === state.activeSessionId ? 'active' : ''}`;
        li.textContent = s.label;
        li.onclick = () => selectSession(s.id);
        el.sessionList.appendChild(li);
    });
}

async function selectSession(id) {
    state.activeSessionId = id;
    saveSessions();
    renderSessions();
    await loadHistory(id);
}

async function loadHistory(sessionId) {
    el.messages.innerHTML = '';
    if (!sessionId || !state.token) return;
    const res = await fetch(`/api/v1/chat/history/${encodeURIComponent(sessionId)}`, {
        headers: authHeaders(),
    });
    if (res.status === 401) return signOut();
    if (!res.ok) return;
    const data = await res.json();
    (data.messages || []).forEach(m => appendMessage(m.role, m.content));
}

function appendMessage(role, content, meta = '') {
    const div = document.createElement('div');
    div.className = `message ${role}`;
    div.textContent = content;
    if (meta) {
        const metaEl = document.createElement('div');
        metaEl.className = 'message-meta';
        metaEl.textContent = meta;
        div.appendChild(metaEl);
    }
    el.messages.appendChild(div);
    el.messages.scrollTop = el.messages.scrollHeight;
    return div;
}

async function loadModels() {
    const res = await fetch('/api/v1/models', { headers: authHeaders() });
    if (res.status === 401) return signOut();
    if (!res.ok) return;
    const data = await res.json();
    el.modelSelect.innerHTML = '';
    (data.models || []).forEach(m => {
        const opt = document.createElement('option');
        opt.value = m.name;
        opt.textContent = m.parameterSize && m.parameterSize !== 'unknown'
            ? `${m.name} (${m.parameterSize})`
            : m.name;
        el.modelSelect.appendChild(opt);
    });
}

async function loadMetrics() {
    if (!state.token) return;
    const res = await fetch('/api/v1/metrics', { headers: authHeaders() });
    if (!res.ok) return;
    const m = await res.json();
    el.mCacheHits.textContent = Math.round(m.cacheHits || 0);
    el.mRequests.textContent = Math.round(m.chatRequests || 0);
    el.valkeyStatus.textContent = m.valkeyConnected ? 'Valkey: Connected' : 'Valkey: Ephemeral Fallback';
    el.valkeyStatus.className = `status-pill ${m.valkeyConnected ? 'ok' : ''}`;
}

async function enterAuthenticatedApp() {
    el.authGate.classList.add('hidden');
    el.chatArea.classList.remove('hidden');
    el.logoutBtn.classList.remove('hidden');
    if (state.sessions.length === 0) {
        createSession('Default Session');
    } else if (!state.activeSessionId) {
        state.activeSessionId = state.sessions[0].id;
    }
    renderSessions();
    await Promise.all([loadModels(), loadHistory(state.activeSessionId), loadMetrics()]);
}

function signOut() {
    state.token = '';
    localStorage.removeItem('chat5000_token');
    el.authGate.classList.remove('hidden');
    el.chatArea.classList.add('hidden');
    el.logoutBtn.classList.add('hidden');
}

el.oauthForm.onsubmit = async (e) => {
    e.preventDefault();
    el.authError.classList.add('hidden');
    const body = {
        grant_type: 'client_credentials',
        client_id: document.getElementById('clientId').value,
        client_secret: document.getElementById('clientSecret').value,
    };
    const res = await fetch('/oauth/token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });
    const data = await res.json();
    if (!res.ok) {
        el.authError.textContent = data.message || 'Authentication failed';
        el.authError.classList.remove('hidden');
        return;
    }
    state.token = data.access_token;
    localStorage.setItem('chat5000_token', state.token);
    await enterAuthenticatedApp();
};

el.useTokenBtn.onclick = async () => {
    const token = el.manualToken.value.trim();
    if (!token) return;
    state.token = token;
    localStorage.setItem('chat5000_token', token);
    await enterAuthenticatedApp();
};

el.logoutBtn.onclick = signOut;
el.newSessionBtn.onclick = () => createSession();

el.clearHistoryBtn.onclick = async () => {
    if (!state.activeSessionId) return;
    await fetch(`/api/v1/chat/history/${encodeURIComponent(state.activeSessionId)}`, {
        method: 'DELETE',
        headers: authHeaders(),
    });
    el.messages.innerHTML = '';
};

el.promptInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        el.chatForm.requestSubmit();
    }
});

el.chatForm.onsubmit = async (e) => {
    e.preventDefault();
    const prompt = el.promptInput.value.trim();
    if (!prompt) return;

    const mode = el.modeSelect.value;
    const model = el.modelSelect.value;
    const useStream = el.streamToggle.checked;

    el.promptInput.value = '';
    appendMessage('user', prompt);

    if (useStream) {
        const assistantDiv = appendMessage('assistant', '…');
        let text = '';
        const res = await fetch('/api/v1/chat/stream', {
            method: 'POST',
            headers: authHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify({
                sessionId: mode === 'session' ? state.activeSessionId : null,
                prompt,
                model,
                stream: true,
            }),
        });
        if (res.status === 401) return signOut();
        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        while (true) {
            const { value, done } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop() || '';
            for (const line of lines) {
                if (!line.startsWith('data:')) continue;
                const jsonStr = line.slice(5).trim();
                if (!jsonStr) continue;
                try {
                    const chunk = JSON.parse(jsonStr);
                    if (chunk.delta) {
                        text += chunk.delta;
                        assistantDiv.textContent = text;
                        el.messages.scrollTop = el.messages.scrollHeight;
                    }
                    if (chunk.done && !assistantDiv.querySelector('.message-meta')) {
                        const meta = document.createElement('div');
                        meta.className = 'message-meta';
                        meta.textContent = `${chunk.model} · ${chunk.latencyMs}ms · streamed`;
                        assistantDiv.appendChild(meta);
                    }
                } catch (_) { }
            }
        }
        await loadMetrics();
        return;
    }

    const endpoint = mode === 'single' ? '/api/v1/chat/ask' : '/api/v1/chat/session';
    const res = await fetch(endpoint, {
        method: 'POST',
        headers: authHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({
            sessionId: mode === 'session' ? state.activeSessionId : null,
            prompt,
            model,
        }),
    });
    if (res.status === 401) return signOut();
    const data = await res.json();
    if (!res.ok) {
        appendMessage('assistant', `Error: ${data.message || res.statusText}`);
        return;
    }
    const meta = `${data.model} · ${data.latencyMs}ms · ${data.cached ? 'Valkey Cache HIT' : 'Ollama Live'}`;
    appendMessage('assistant', data.content, meta);
    await loadMetrics();
};

if (state.token) {
    enterAuthenticatedApp();
}
