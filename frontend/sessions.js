function sessionCard(session) {
  const card = el('div', 'session-card');
  const top = el('div');
  top.appendChild(el('span', 'badge ' + session.visibility, session.visibility));
  top.appendChild(el('span', 'badge type', session.type === 'ONE_ON_ONE' ? '1:1' : 'GROUP'));
  top.appendChild(el('strong', null, ' ' + session.title));
  card.appendChild(top);
  card.appendChild(el('div', 'muted', 'Topic: ' + session.topic + ' · hosted by ' + session.hostName));
  card.appendChild(el('p', null, session.description));
  return card;
}

function membershipAction(session, membership) {
  if (!membership) {
    const btn = el('button', null, 'Request to join');
    btn.onclick = () => requestToJoin(session.id);
    return btn;
  }
  const labels = {
    APPROVED: "You're in (" + membership.role.toLowerCase() + ')',
    PENDING: 'Request pending',
    DECLINED: 'Request declined',
  };
  return el('span', 'muted', labels[membership.status]);
}

function renderBoard(sessions, membershipBySession) {
  const container = document.getElementById('boardList');
  container.replaceChildren();
  if (sessions.length === 0) {
    container.appendChild(el('p', 'muted', 'No public sessions yet. Host the first one!'));
    return;
  }
  sessions.forEach((session) => {
    const card = sessionCard(session);
    card.appendChild(membershipAction(session, membershipBySession[session.id]));
    container.appendChild(card);
  });
}

function renderMySessions(sessions, membershipBySession) {
  const container = document.getElementById('mySessionsList');
  container.replaceChildren();
  if (sessions.length === 0) {
    container.appendChild(el('p', 'muted', "You haven't joined any sessions yet."));
    return;
  }
  sessions.forEach((session) => {
    const membership = membershipBySession[session.id];
    const role = membership ? membership.role : 'MEMBER';
    const card = sessionCard(session);
    card.appendChild(el('div', 'muted', 'Your role: ' + role));
    const openBtn = el('button', null, 'Open');
    openBtn.onclick = () => openRoom(session);
    card.appendChild(openBtn);

    if (role === 'HOST' || role === 'ADMIN') {
      const requestsBox = el('div');
      card.appendChild(requestsBox);
      loadPendingRequests(session.id, requestsBox);
    }
    if (role !== 'HOST') {
      const leaveBtn = el('button', 'secondary', 'Leave');
      leaveBtn.onclick = () => leaveSession(session.id);
      card.appendChild(leaveBtn);
    }
    container.appendChild(card);
  });
}

async function loadPendingRequests(sessionId, container) {
  try {
    const requests = await api('/api/sessions/' + sessionId + '/requests?actingUserId=' + currentUser.id);
    requests.forEach((request) => {
      const row = el('div', 'request-row');
      row.appendChild(el('span', null, request.userName + ' wants to join'));
      const approve = el('button', null, 'Approve');
      approve.onclick = () => decideRequest(sessionId, request.userId, 'approve');
      const decline = el('button', 'secondary', 'Decline');
      decline.onclick = () => decideRequest(sessionId, request.userId, 'decline');
      row.append(approve, decline);
      container.appendChild(row);
    });
  } catch (err) {
    container.appendChild(el('p', 'error', err.message));
  }
}

async function loadSessions() {
  if (!currentUser) return;
  try {
    const [publicSessions, mySessions, memberships] = await Promise.all([
      api('/api/sessions'),
      api('/api/sessions/mine?userId=' + currentUser.id),
      api('/api/sessions/memberships?userId=' + currentUser.id),
    ]);
    const membershipBySession = {};
    memberships.forEach((m) => { membershipBySession[m.sessionId] = m; });
    renderBoard(publicSessions, membershipBySession);
    renderMySessions(mySessions, membershipBySession);
  } catch (err) {
    showToast('Could not load sessions: ' + err.message);
  }
}

async function requestToJoin(sessionId) {
  try {
    await api('/api/sessions/' + sessionId + '/join?userId=' + currentUser.id, 'POST');
    showToast('Request sent to the host');
    loadSessions();
  } catch (err) {
    showToast(err.message);
  }
}

async function decideRequest(sessionId, userId, decision) {
  try {
    await api('/api/sessions/' + sessionId + '/members/' + userId + '/' + decision
      + '?actingUserId=' + currentUser.id, 'POST');
    loadSessions();
  } catch (err) {
    showToast(err.message);
  }
}

async function leaveSession(sessionId) {
  if (!confirm('Leave this session?')) return;
  try {
    await api('/api/sessions/' + sessionId + '/leave?userId=' + currentUser.id, 'DELETE');
    if (openSession && openSession.id === sessionId) closeRoom();
    loadSessions();
  } catch (err) {
    showToast(err.message);
  }
}

document.getElementById('sessionForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    await api('/api/sessions?hostId=' + currentUser.id, 'POST', {
      title: document.getElementById('sessionTitle').value,
      topic: document.getElementById('sessionTopic').value,
      description: document.getElementById('sessionDescription').value,
      visibility: document.getElementById('sessionVisibility').value,
      type: document.getElementById('sessionType').value,
    });
    e.target.reset();
    showToast('Session created');
    loadSessions();
  } catch (err) {
    showToast(err.message);
  }
});