// HTTP integration checks. Run only against the synthetic accounts created by bootstrap-demo.
const assert=require('node:assert/strict');const pool=require('../config/database');
const base=process.env.DEMO_API_URL||'http://127.0.0.1:3000/api';
async function request(path,method='GET',body,token){const r=await fetch(base+path,{method,headers:{...(body instanceof FormData?{}:{'Content-Type':'application/json'}),...(token?{Authorization:'Bearer '+token}:{})},body:body instanceof FormData?body:body?JSON.stringify(body):undefined});return {status:r.status,data:await r.json()};}
(async()=>{
 const password=process.env.DEMO_PASSWORD;assert(password);
 const owner=(await request('/auth/login','POST',{email:'demo-user@example.test',password})).data.data.token;
 const other=(await request('/auth/login','POST',{email:'demo-other@example.test',password})).data.data.token;
 assert.equal((await request('/admin/stats','GET',null,owner)).status,403,'User token must not become admin by matching numeric IDs');
 const folder=await request('/folders','POST',{name:'Synthetic-'+Date.now()},owner);assert.equal(folder.status,201);
 const id=folder.data.data.folder?.id||folder.data.data.id;
 assert.equal((await request(`/folders/${id}/contents`,'GET',null,other)).status,404);
 const form=new FormData();form.append('file',new Blob(['Generated synthetic portfolio fixture\n'],{type:'text/plain'}),'synthetic.txt');form.append('folder_id',id);
 const uploaded=await request('/files/upload','POST',form,owner);assert.equal(uploaded.status,201,JSON.stringify(uploaded.data));
 const file=uploaded.data.data.file;
 assert.equal((await request('/files/'+file.id,'DELETE',null,other)).status,404);
 assert.equal((await request('/files/info/'+file.share_token)).status,200);
 assert.equal((await request('/files/info/invalid-synthetic-token')).status,404);
 const bad=new FormData();bad.append('file',new Blob(['<?php echo 1; ?>'],{type:'text/plain'}),'synthetic.php');assert.notEqual((await request('/files/upload','POST',bad,owner)).status,201);
 await pool.query('UPDATE users SET storage_used=1099511627776 WHERE email=$1',['demo-other@example.test']);
 const quota=new FormData();quota.append('file',new Blob(['synthetic']),'quota.txt');assert.equal((await request('/files/upload','POST',quota,other)).status,400);
 await pool.query('UPDATE users SET storage_used=0 WHERE email=$1',['demo-other@example.test']);
 console.log('PASS: user/admin boundary, folder isolation, upload, shared token, foreign deletion, executable rejection and quota rejection.');
})().catch(e=>{console.error(e.message);process.exitCode=1}).finally(()=>pool.end());
