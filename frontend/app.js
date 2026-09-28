const BACKEND_URL = (location.hostname === 'localhost' || location.hostname === '127.0.0.1')
  ? 'http://localhost:8080'
  : 'https://peer-skill-swap.onrender.com';

let currentUser = JSON.parse(localStorage.getItem('skillswapUser') || 'null');
let stompClient = null;

async function api(path, method = 'GET', body = null) {
  const res = await fetch(BACKEND_URL + path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error((data && data.error) || 'Something went wrong');
  }
  return data;
}

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function showToast(message) {
  const container = document.getElementById('toastContainer');
  const toast = el('div', 'toast', message);
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 5000);
}

function showNotification(payload) {
  document.getElementById('notifications').prepend(el('li', null, payload.message));
}

function setCurrentUser(user) {
  currentUser = user;
  localStorage.setItem('skillswapUser', JSON.stringify(user));
  render();
}

function logout() {
  closeRoom();
  currentUser = null;
  localStorage.removeItem('skillswapUser');
  if (stompClient) stompClient.deactivate();
  render();
}

function showAuthError(message) {
  document.getElementById('authError').textContent = message;
}

document.getElementById('registerForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    const user = await api('/api/auth/register', 'POST', {
      name: document.getElementById('regName').value,
      email: document.getElementById('regEmail').value,
      password: document.getElementById('regPassword').value,
    });
    setCurrentUser(user);
  } catch (err) {
    showAuthError(err.message);
  }
});

document.getElementById('loginForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    const user = await api('/api/auth/login', 'POST', {
      email: document.getElementById('loginEmail').value,
      password: document.getElementById('loginPassword').value,
    });
    setCurrentUser(user);
  } catch (err) {
    showAuthError(err.message);
  }
});

function connectNotifications() {
  if (!currentUser) return;
  if (stompClient) stompClient.deactivate();
  stompClient = new StompJs.Client({
    webSocketFactory: () => new SockJS(BACKEND_URL + '/ws'),
    onConnect: () => {
      stompClient.subscribe('/topic/notifications/' + currentUser.id, (message) => {
        const payload = JSON.parse(message.body);
        const isOpenChat = payload.type === 'NEW_CHAT_MESSAGE' && openSession
          && payload.data && payload.data.sessionId === openSession.id;
        if (isOpenChat) return;
        showNotification(payload);
        showToast(payload.message);
        if (['JOIN_REQUEST', 'REQUEST_APPROVED', 'REQUEST_DECLINED'].includes(payload.type)) {
          loadSessions();
        }
      });
      subscribeToChat();
    },
  });
  stompClient.activate();
}

function render() {
  const authSection = document.getElementById('authSection');
  const appSection = document.getElementById('appSection');
  const userBar = document.getElementById('userBar');
  userBar.replaceChildren();

  if (currentUser) {
    authSection.hidden = true;
    appSection.hidden = false;
    const logoutBtn = el('button', 'secondary', 'Logout');
    logoutBtn.onclick = logout;
    userBar.append('Logged in as ', el('strong', null, currentUser.name), ' ', logoutBtn);
    loadSessions();
    connectNotifications();
  } else {
    authSection.hidden = false;
    appSection.hidden = true;
    document.getElementById('notifications').replaceChildren();
    document.getElementById('boardList').replaceChildren();
    document.getElementById('mySessionsList').replaceChildren();
  }
}

render();