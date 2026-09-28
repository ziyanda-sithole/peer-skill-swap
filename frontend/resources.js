async function searchWikipedia(query) {
  const params = new URLSearchParams({
    action: 'query',
    generator: 'search',
    gsrsearch: query,
    gsrlimit: '4',
    prop: 'extracts',
    exintro: '1',
    explaintext: '1',
    exsentences: '3',
    format: 'json',
    origin: '*',
  });
  const res = await fetch('https://en.wikipedia.org/w/api.php?' + params.toString());
  if (!res.ok) throw new Error('Wikipedia is not responding right now');
  const data = await res.json();
  const pages = data.query ? Object.values(data.query.pages) : [];
  return pages.sort((a, b) => a.index - b.index);
}

function renderResources(pages) {
  const container = document.getElementById('resourceResults');
  container.replaceChildren();
  if (pages.length === 0) {
    container.appendChild(el('p', 'muted', 'No articles found. Try different keywords.'));
    return;
  }
  pages.forEach((page) => {
    const card = el('div', 'resource-card');
    const link = el('a', null, page.title);
    link.href = 'https://en.wikipedia.org/?curid=' + page.pageid;
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    const heading = el('strong');
    heading.appendChild(link);
    card.appendChild(heading);
    card.appendChild(el('p', null, page.extract || 'No summary available.'));
    card.appendChild(el('span', 'muted', 'Source: Wikipedia'));
    container.appendChild(card);
  });
}

async function runResourceSearch(query) {
  const container = document.getElementById('resourceResults');
  container.replaceChildren(el('p', 'muted', 'Searching Wikipedia...'));
  try {
    renderResources(await searchWikipedia(query));
  } catch (err) {
    container.replaceChildren(el('p', 'error', err.message));
  }
}

function loadResourcesForRoom(session) {
  const input = document.getElementById('resourceQuery');
  input.value = session.topic + ' computer science';
  runResourceSearch(input.value);
}

document.getElementById('resourceForm').addEventListener('submit', (e) => {
  e.preventDefault();
  runResourceSearch(document.getElementById('resourceQuery').value.trim());
});