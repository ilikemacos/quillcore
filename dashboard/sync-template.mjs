// Copies the shared dashboard template (also used offline by `gradlew quillTest`) into the web app.
import { readFileSync, writeFileSync } from 'node:fs';
const t = readFileSync(new URL('../gradle/quill-test/dashboard.html', import.meta.url), 'utf8');
const loader = `<script>
window.quillLoad = (render) => {
  const id = new URLSearchParams(location.search).get('id');
  const app = document.getElementById('app');
  if (!id) { app.innerHTML = '<p>No run id. <a href="/">All runs</a></p>'; return; }
  fetch('/api/runs?id=' + encodeURIComponent(id)).then(r => r.ok ? r.json() : Promise.reject(r.status))
    .then(d => { render(d); document.title = 'QuillCore run ' + id;
      app.insertAdjacentHTML('afterbegin', '<p><a href="/">← All runs</a></p>'); })
    .catch(e => { app.innerHTML = '<p>Run not found (' + e + '). <a href="/">All runs</a></p>'; });
};
</script>
`;
writeFileSync(new URL('./public/run.html', import.meta.url), t.replace('<body>', '<body>\n' + loader));
console.log('public/run.html synced');
