let openConversation = null;
let dmSubscription = null;
const renderedDmIds = new Set();

function conversationLabel(conversation) {
  const others = conversation.participants.filter((p) => p.userId !== currentUser.id);
  if (others.length === 0) return 'Just you';
  return others.map((p) => p.userName).join(', ');
}

async function loadConversations() {
  if (!currentUser) return;
  const container = document.getElementById('conversationsList');
  try {
    const conversations = await api('/api/conversations/mine?userId=' + currentUser.id);
    container.replaceChildren();
    if (conversations.length === 0) {
      container.appendChild(el('p', 'muted', 'No conversations yet.'));
      return;
    }
    conversations.forEach((conversation) => {
      const row = el('div', 'conversation-row');
      row.appendChild(el('span', null, conversationLabel(conversation)));
      row.appendChild(el('span', 'badge type', conversation.type === 'GROUP' ? 'GROUP' : '1:1'));
      const openBtn = el('button', null, 'Open');
      openBtn.onclick = () => openDm(conversation);
      row.appendChild(openBtn);
      container.appendChild(row);
    });
  } catch (err) {
    showToast('Could not load conversations: ' + err.message);
  }
}

async function messageUserByEmail(email) {
  try {
    const target = await api('/api/users/lookup?email=' + encodeURIComponent(email));
    const conversation = await api(
      '/api/conversations/direct?userId=' + currentUser.id + '&otherUserId=' + target.userId, 'POST');
    loadConversations();
    openDm(conversation);
  } catch (err) {
    showToast(err.message);
  }
}

async function messageUserById(userId) {
  try {
    const conversation = await api(
      '/api/conversations/direct?userId=' + currentUser.id + '&otherUserId=' + userId, 'POST');
    loadConversations();
    openDm(conversation);
  } catch (err) {
    showToast(err.message);
  }
}

function appendDmMessage(message) {
  if (renderedDmIds.has(message.id)) return;
  renderedDmIds.add(message.id);
  const box = document.getElementById('dmMessages');
  const row = el('div', 'chat-message' + (message.senderId === currentUser.id ? ' mine' : ''));
  row.appendChild(el('strong', null, message.senderName));
  row.appendChild(el('span', 'muted', ' ' + formatTime(message.createdAt)));
  row.appendChild(el('div', null, message.content));
  box.appendChild(row);
  box.scrollTop = box.scrollHeight;
}

function subscribeToDm() {
  if (!openConversation || !stompClient) return false;
  try {
    if (dmSubscription) dmSubscription.unsubscribe();
  } catch (e) { /* old connection already gone */ }
  try {
    dmSubscription = stompClient.subscribe(
      '/topic/conversations/' + openConversation.id + '/messages',
      (frame) => appendDmMessage(JSON.parse(frame.body)));
    return true;
  } catch (e) {
    return false;
  }
}

async function openDm(conversation) {
  if (openConversation) closeDm();
  openConversation = conversation;
  renderedDmIds.clear();

  document.getElementById('dmTitle').textContent = conversationLabel(conversation);
  document.getElementById('dmMembers').textContent =
    'With: ' + conversation.participants.map((p) => p.userName).join(', ');
  document.getElementById('dmMessages').replaceChildren();
  const panel = document.getElementById('dmPanel');
  panel.hidden = false;
  panel.scrollIntoView({ behavior: 'smooth' });

  try {
    const history = await api('/api/conversations/' + conversation.id + '/messages?userId=' + currentUser.id);
    history.forEach(appendDmMessage);
  } catch (err) {
    showToast(err.message);
  }

  if (!subscribeToDm()) {
    showToast('Live chat is still connecting. New messages will appear once it is ready.');
  }
}

function closeDm() {
  if (dmSubscription) {
    try { dmSubscription.unsubscribe(); } catch (e) { /* already closed */ }
    dmSubscription = null;
  }
  openConversation = null;
  document.getElementById('dmPanel').hidden = true;
}

document.getElementById('dmCloseBtn').addEventListener('click', closeDm);

document.getElementById('dmLeaveBtn').addEventListener('click', async () => {
  if (!openConversation || !confirm('Leave this conversation?')) return;
  try {
    await api('/api/conversations/' + openConversation.id + '/leave?userId=' + currentUser.id, 'DELETE');
    closeDm();
    loadConversations();
  } catch (err) {
    showToast(err.message);
  }
});

document.getElementById('dmForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const input = document.getElementById('dmInput');
  const content = input.value.trim();
  if (!content || !openConversation) return;
  try {
    const message = await api(
      '/api/conversations/' + openConversation.id + '/messages?senderId=' + currentUser.id,
      'POST', { content });
    input.value = '';
    appendDmMessage(message);
  } catch (err) {
    showToast(err.message);
  }
});

document.getElementById('startDirectForm').addEventListener('submit', (e) => {
  e.preventDefault();
  const input = document.getElementById('directEmailInput');
  messageUserByEmail(input.value.trim());
  input.value = '';
});

document.getElementById('startGroupForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const input = document.getElementById('groupEmailsInput');
  const emails = input.value.split(',').map((s) => s.trim()).filter(Boolean);
  try {
    const conversation = await api('/api/conversations/group?creatorId=' + currentUser.id, 'POST', {
      participantEmails: emails,
    });
    input.value = '';
    loadConversations();
    openDm(conversation);
  } catch (err) {
    showToast(err.message);
  }
});