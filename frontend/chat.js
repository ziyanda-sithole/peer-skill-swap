let openSession = null;
let chatSubscription = null;
const renderedMessageIds = new Set();

function formatTime(iso) {
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function appendMessage(message) {
  if (renderedMessageIds.has(message.id)) return;
  renderedMessageIds.add(message.id);
  const box = document.getElementById('chatMessages');
  const row = el('div', 'chat-message' + (message.senderId === currentUser.id ? ' mine' : ''));
  row.appendChild(el('strong', null, message.senderName));
  row.appendChild(el('span', 'muted', ' ' + formatTime(message.createdAt)));
  row.appendChild(el('div', null, message.content));
  box.appendChild(row);
  box.scrollTop = box.scrollHeight;
}

function subscribeToChat() {
  if (!openSession || !stompClient) return false;
  try {
    if (chatSubscription) chatSubscription.unsubscribe();
  } catch (e) { /* the old connection is already gone */ }
  try {
    chatSubscription = stompClient.subscribe('/topic/sessions/' + openSession.id + '/chat', (frame) => {
      appendMessage(JSON.parse(frame.body));
    });
    return true;
  } catch (e) {
    return false;
  }
}

async function openRoom(session) {
  if (openSession) closeRoom();
  openSession = session;
  renderedMessageIds.clear();

  document.getElementById('roomTitle').textContent = session.title;
  document.getElementById('roomTopic').textContent =
    'Topic: ' + session.topic + ' · hosted by ' + session.hostName;
  document.getElementById('chatMessages').replaceChildren();
  const panel = document.getElementById('roomPanel');
  panel.hidden = false;
  panel.scrollIntoView({ behavior: 'smooth' });

  try {
    const history = await api('/api/sessions/' + session.id + '/messages?userId=' + currentUser.id);
    history.forEach(appendMessage);
  } catch (err) {
    showToast(err.message);
  }

  if (!subscribeToChat()) {
    showToast('Live chat is still connecting. New messages will appear once it is ready.');
  }
}

function closeRoom() {
  if (chatSubscription) {
    try { chatSubscription.unsubscribe(); } catch (e) { /* connection already closed */ }
    chatSubscription = null;
  }
  openSession = null;
  document.getElementById('roomPanel').hidden = true;
}

document.getElementById('closeRoomBtn').addEventListener('click', closeRoom);

document.getElementById('chatForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const input = document.getElementById('chatInput');
  const content = input.value.trim();
  if (!content || !openSession) return;
  try {
    const message = await api(
      '/api/sessions/' + openSession.id + '/messages?senderId=' + currentUser.id,
      'POST', { content });
    input.value = '';
    appendMessage(message);
  } catch (err) {
    showToast(err.message);
  }
});