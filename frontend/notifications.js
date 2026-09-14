function connectNotifications(userId) {
  const socket = new SockJS('http://localhost:8080/ws');
  const stompClient = new StompJs.Client({
    webSocketFactory: () => socket,
    onConnect: () => {
      console.log('Connected to notifications for user ' + userId);
      stompClient.subscribe('/topic/notifications/' + userId, (message) => {
        showNotification(JSON.parse(message.body));
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