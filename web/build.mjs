#!/usr/bin/env node
// Build the static web preview for the Quran Tadabbur app.
// Source: quran-app assets (inside PROJECT_DIR). Output: dist/ (inside PROJECT_DIR).
import { readFileSync, writeFileSync, mkdirSync, existsSync, copyFileSync, statSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const assets = join(root, 'quran-app', 'app', 'src', 'main', 'assets');
const outDir = join(root, 'dist');

// Rebuild when output is missing or any source asset is newer.
const outputs = [join(outDir, 'index.html')];
const inputs = ['quran_compact.json', 'tadabbur_surah.json', 'tadabbur_ayah.json'].map(f => join(assets, f));
let stale = outputs.some(f => !existsSync(f));
if (!stale) {
  const outTime = Math.min(...outputs.map(f => statSync(f).mtimeMs));
  stale = inputs.some(f => statSync(f).mtimeMs > outTime);
}
if (!stale && process.argv[2] !== '--force') {
  console.log('dist/ is up to date, skipping build.');
  process.exit(0);
}

const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
const AR = ['٠','١','٢','٣','٤','٥','٦','٧','٨','٩'];
const ar = n => String(n).replace(/[0-9]/g, d => AR[+d]);

const surahs = JSON.parse(readFileSync(join(assets, 'quran_compact.json'), 'utf8'));
const tadabbur = JSON.parse(readFileSync(join(assets, 'tadabbur_surah.json'), 'utf8'));
// Web preview uses simplified orthography: some Quranic small-high marks
// (U+06E1 and waqf signs) have no glyphs in headless-container fonts and
// render as tofu. The Android APK keeps the full Uthmani text untouched.
const normalize = s => String(s)
  .replace(/ا\u0653/g, 'آ')
  .replace(/ٓ/g, '')
  .replace(/ۡ/g, 'ْ')
  .replace(/[ۣۖۗۘۙۚۛۜ۟۠ۢۥۦ۪ۭۨ۫۬]/g, '')
  .replace(/۞/g, ' • ')
  .replace(/۩/g, ' (موضع سجدة) ');
for (const s of surahs) {
  s.arabicName = normalize(s.arabicName);
  s.ayahs = s.ayahs.map(normalize);
}
const tadMap = new Map(tadabbur.map(t => [t.number, t]));
const totalAyahs = surahs.reduce((a, s) => a + s.ayahs.length, 0);

const cards = surahs.map(s => {
  const t = tadMap.get(s.number);
  return `<article class="card" data-type="${esc(s.type)}" data-name="${esc(s.arabicName)} ${esc(s.englishName)} ${s.number}" data-n="${s.number}" tabindex="0" role="button" aria-label="${esc(s.arabicName)}">`
    + `<div class="num">${ar(s.number)}</div>`
    + `<div class="meta"><h3>${esc(s.arabicName)}</h3><p>${esc(s.type)} • ${ar(s.ayahs.length)} آيات • ${esc(s.translation || '')}</p>`
    + (t ? `<p class="axis">${esc(t.title)}</p>` : '') + `</div>`
    + `<span class="go">تلاوة وتدبر ←</span></article>`;
}).join('\n');

const html = `<!DOCTYPE html>
<html dir="rtl" lang="ar">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>قرآن تدبُّر | Quran Tadabbur</title>
<style>
:root{--emerald-dark:#0B3D2E;--emerald:#146B4D;--emerald-light:#1F8A63;--gold:#C9A227;--gold-light:#E9D189;--cream:#FAF6EE;--cream-dark:#F0E6D2;--ink:#1C2420;--soft:#4A5A52}
*{box-sizing:border-box}body{margin:0;font-family:"Segoe UI",Tahoma,Arial,sans-serif;background:var(--cream);color:var(--ink)}
.wrap{max-width:1060px;margin:0 auto;padding:16px}
.hero{background:linear-gradient(135deg,var(--emerald-dark),var(--emerald) 60%,var(--emerald-light));border-radius:24px;padding:28px 24px;color:#fff}
.hero h1{margin:0;font-size:30px}.hero p{color:var(--gold-light);margin:8px 0 0;font-size:15px;line-height:1.9}
.basmala{margin-top:14px;background:rgba(255,255,255,.15);border-radius:12px;padding:10px;text-align:center;font-size:20px}
.chips{display:flex;gap:8px;flex-wrap:wrap;margin:14px 0}.chip{background:#fff;border:1px solid var(--gold);color:var(--emerald-dark);border-radius:999px;padding:6px 14px;font-size:13px;font-weight:bold}
.toolbar{display:flex;gap:8px;margin:14px 0;flex-wrap:wrap}.toolbar input{flex:1;min-width:200px;padding:12px 14px;border-radius:14px;border:1px solid #d8cfae;font-size:15px;background:#fff}
.fbtn{border:1px solid var(--emerald);background:#fff;color:var(--emerald-dark);border-radius:999px;padding:8px 18px;cursor:pointer;font-weight:bold}.fbtn.on{background:var(--emerald-dark);color:#fff}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:10px}
.card{background:#fff;border-radius:16px;padding:14px;display:flex;gap:12px;align-items:center;cursor:pointer;border:1px solid #eee3c6}
.card:hover{border-color:var(--gold);box-shadow:0 4px 14px rgba(11,61,46,.12)}
.num{width:48px;height:48px;flex:none;border-radius:50%;background:radial-gradient(circle at 35% 30%,var(--gold-light),var(--gold));color:var(--emerald-dark);font-weight:bold;display:flex;align-items:center;justify-content:center;font-size:18px}
.meta{flex:1}.meta h3{margin:0;font-size:18px}.meta p{margin:2px 0 0;font-size:12.5px;color:var(--soft)}.axis{color:var(--emerald)!important;font-weight:bold}
.go{color:var(--gold);font-weight:bold;font-size:13px;white-space:nowrap}
#reader{background:#fff;border-radius:20px;padding:20px;margin-top:18px;border:2px solid var(--gold);display:none}
#reader.show{display:block}.ayah{font-size:22px;line-height:2.2;text-align:right;padding:12px 4px;border-bottom:1px dashed #e3d9b8}
.ayah:last-child{border-bottom:0}.an{color:var(--gold);font-weight:bold}
.tbox{background:var(--cream);border-radius:12px;padding:12px;margin:12px 0;font-size:15px;line-height:2}
.tbox h4{margin:0 0 6px;color:var(--emerald-dark)}.backbtn{background:var(--emerald-dark);color:#fff;border:0;border-radius:12px;padding:10px 22px;cursor:pointer;font-size:15px;font-weight:bold}
.note{background:#fff;border-radius:14px;padding:14px;margin-top:14px;font-size:13.5px;color:var(--soft);line-height:2}
footer{text-align:center;color:var(--soft);font-size:13px;padding:22px 0}
@media(max-width:600px){.hero h1{font-size:24px}.ayah{font-size:19px}}
</style>
</head>
<body>
<div class="wrap">
<header class="hero">
<h1>📖 قرآن تدبُّر</h1>
<p>القرآن الكريم كاملاً بالرسم العثماني • تدبر بأسلوب إيماني تربوي مستوحى من منهج الشيخ محمد المقرمي (وقفات + هدايات + عمل بالآية)</p>
<div class="basmala">﷽ بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ</div>
</header>
<div class="chips"><span class="chip">${ar(surahs.length)} سورة</span><span class="chip">${ar(totalAyahs)} آية</span><span class="chip">تدبر ${ar(tadabbur.length)} سورة</span><span class="chip">تطبيق أندرويد أوفلاين 🌙</span></div>
<div class="toolbar">
<input id="q" type="search" placeholder="ابحث باسم السورة أو رقمها…" aria-label="بحث">
<button class="fbtn on" data-f="الكل">الكل</button><button class="fbtn" data-f="مكية">مكية</button><button class="fbtn" data-f="مدنية">مدنية</button>
</div>
<section id="reader" aria-live="polite"></section>
<main class="grid" id="grid">
${cards}
</main>
<div class="note">⚠️ <b>تنبيه منهجي:</b> مادة التدبر محتوى تربوي أصلي مستوحى من <b>منهج</b> الشيخ محمد المقرمي في التدبر، وليست نقلاً حرفياً من كتبه. نسخة الويب هذه معاينة تعريفية؛ التطبيق الكامل للأندرويد (Kotlin + Compose، وضع ليلي، مفضلة، أذكار) جاهز في مجلد <b>releases</b> بصيغة APK.</div>
<footer>صُمم بواسطة Alkendi • الإصدار 1.0.0 • package: com.alkendi.quran</footer>
</div>
<script>
let Q=[],T={};
fetch('quran_compact.json').then(r=>r.json()).then(d=>{Q=d;bind()}).catch(()=>{});
fetch('tadabbur_surah.json').then(r=>r.json()).then(d=>{T={};d.forEach(x=>T[x.number]=x)}).catch(()=>{});
fetch('tadabbur_ayah.json').then(r=>r.json()).then(d=>{window.__TA=d||{}}).catch(()=>{window.__TA={}});
let filter='الكل';
function bind(){
  document.querySelectorAll('.fbtn').forEach(b=>b.onclick=()=>{document.querySelectorAll('.fbtn').forEach(x=>x.classList.remove('on'));b.classList.add('on');filter=b.dataset.f;apply()});
  document.getElementById('q').oninput=apply;
  document.querySelectorAll('.card').forEach(c=>{c.onclick=()=>openSurah(+c.dataset.n);c.onkeydown=e=>{if(e.key==='Enter')openSurah(+c.dataset.n)}});
}
function apply(){
  const q=document.getElementById('q').value.trim();
  document.querySelectorAll('.card').forEach(c=>{
    const okType=(filter==='الكل'||c.dataset.type===filter);
    const okQ=(!q||c.dataset.name.includes(q));
    c.style.display=(okType&&okQ)?'':'none';
  });
}
const AR='٠١٢٣٤٥٦٧٨٩', arn=n=>String(n).replace(/[0-9]/g,d=>AR[+d]);
function openSurah(n){
  const s=Q.find(x=>x.number===n); if(!s) return;
  const t=T[n]; const TA=window.__TA||{};
  let h='<button class="backbtn" onclick="document.getElementById(&quot;reader&quot;).classList.remove(&quot;show&quot;);window.scrollTo(0,0)">→ رجوع للسور</button>';
  h+='<h2>'+s.arabicName+'</h2><p>'+s.type+' • '+arn(s.ayahs.length)+' آيات</p>';
  if(t){h+='<div class="tbox"><h4>💡 المحور: '+t.title+'</h4><p>'+t.summary+'</p><p>'+t.tadabbur+'</p><p><b>✅ العمل:</b> '+t.amal+'</p><p><b>🤔 سؤال:</b> '+t.question+'</p></div>'}
  s.ayahs.forEach((a,i)=>{
    h+='<div class="ayah">'+a+' <span class="an">﴿'+arn(i+1)+'﴾</span></div>';
    const w=TA[n+':'+(i+1)];
    if(w) h+='<div class="tbox"><h4>وقفة تدبرية</h4><p>💡 '+w+'</p></div>';
  });
  const r=document.getElementById('reader'); r.innerHTML=h; r.classList.add('show'); r.scrollIntoView();
}
</script>
</div>
</body>
</html>`;

mkdirSync(outDir, { recursive: true });
writeFileSync(join(outDir, 'index.html'), html);
writeFileSync(join(outDir, 'quran_compact.json'), JSON.stringify(surahs));
for (const f of ['tadabbur_surah.json', 'tadabbur_ayah.json']) {
  copyFileSync(join(assets, f), join(outDir, f));
}
console.log(`Built dist/ with ${surahs.length} surahs, ${totalAyahs} ayahs.`);
