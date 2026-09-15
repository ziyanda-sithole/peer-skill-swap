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

function setCurrentUser(user) {
  currentUser = user;
  localStorage.setItem('skillswapUser', JSON.stringify(user));
  render();
}

function logout() {
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

document.getElementById('postForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    await api(`/api/posts?userId=${currentUser.id}`, 'POST', {
      type: document.getElementById('postType').value,
      skillTag: document.getElementById('postSkillTag').value,
      description: document.getElementById('postDescription').value,
    });
    e.target.reset();
    loadPosts();
  } catch (err) {
    alert(err.message);
  }
});

async function loadPosts() {
  const posts = await api('/api/posts');
  const container = document.getElementById('postsList');
  container.innerHTML = '';
  posts.forEach((post) => {
    const card = document.createElement('div');
    card.className = 'post-card';
    card.innerHTML = `
      <span class="badge ${post.type}">${post.type}</span>
      <strong>${post.skillTag}</strong> — ${post.description}
      <div class="muted">by ${post.authorName}</div>
    `;
    const respondBtn = document.createElement('button');
    respondBtn.textContent = 'Respond';
    respondBtn.onclick = () => respondToPost(post.id);
    card.appendChild(respondBtn);
    container.appendChild(card);
  });
}

async function respondToPost(postId) {
  const message = prompt('Optional message:') || '';
  try {
    await api(`/api/posts/${postId}/respond?responderId=${currentUser.id}`, 'POST', { message });
    alert('Response sent!');
    loadMatches();
  } catch (err) {
    alert(err.message);
  }
}

async function loadMatches() {
  if (!currentUser) return;
  const matches = await api(`/api/matches?userId=${currentUser.id}`);
  const container = document.getElementById('matchesList');
  container.innerHTML = '';
  matches.forEach((match) => {
    const card = document.createElement('div');
    card.className = 'match-card';
    const isMine = match.posterId === currentUser.id;
    card.innerHTML = `
      <strong>${match.skillTag}</strong> (${match.postType})
      <div class="muted">${match.posterName} ↔ ${match.responderName} — ${match.status}</div>
      ${match.message ? `<div>"${match.message}"</div>` : ''}
    `;
    if (isMine && match.status === 'PENDING') {
      const acceptBtn = document.createElement('button');
      acceptBtn.textContent = 'Accept';
      acceptBtn.onclick = () => acceptMatch(match.id);
      card.appendChild(acceptBtn);
    }
    container.appendChild(card);
  });
}

async function acceptMatch(matchId) {
  try {
    await api(`/api/matches/${matchId}/accept?userId=${currentUser.id}`, 'PATCH');
    loadMatches();
  } catch (err) {
    alert(err.message);
  }
}

function connectNotifications() {
  if (!currentUser) return;
  const socket = new SockJS(BACKEND_URL + '/ws');
  stompClient = new StompJs.Client({
    webSocketFactory: () => socket,
    onConnect: () => {
      stompClient.subscribe('/topic/notifications/' + currentUser.id, (message) => {
        const payload = JSON.parse(message.body);
        showNotification(payload);
        loadPosts();
        loadMatches();
      });
    },
  });
  stompClient.activate();
}

function showNotification(payload) {
  const list = document.getElementById('notifications');
  const item = document.createElement('li');
  item.textContent = payload.message;
  list.prepend(item);
}

function render() {
  const authSection = document.getElementById('authSection');
  const appSection = document.getElementById('appSection');
  const userBar = document.getElementById('userBar');

  if (currentUser) {
    authSection.hidden = true;
    appSection.hidden = false;
    userBar.innerHTML = `Logged in as <strong>${currentUser.name}</strong> <button id="logoutBtn">Logout</button>`;
    document.getElementById('logoutBtn').onclick = logout;
    loadPosts();
    loadMatches();
    connectNotifications();
  } else {
    authSection.hidden = false;
    appSection.hidden = true;
    userBar.innerHTML = '';
  }
}

render();