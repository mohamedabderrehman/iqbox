// Run only against a disposable database; initialize tables through config/initDatabase.
const pool=require('../config/database');
const init=require('../config/initDatabase');
const bcrypt=require('bcryptjs');
(async()=>{
 const password=process.env.DEMO_PASSWORD;
 if(!password || password.length<12) throw new Error('Set DEMO_PASSWORD (12+ characters) for synthetic accounts');
 await init();const hash=await bcrypt.hash(password,10);
 await pool.query("INSERT INTO admins(username,email,password_hash) VALUES('demo-admin','admin@example.test',$1) ON CONFLICT(email) DO UPDATE SET password_hash=EXCLUDED.password_hash",[hash]);
 for(const name of ['demo-user','demo-other']) {
  const r=await pool.query('INSERT INTO users(username,email,password_hash,referral_code) VALUES($1,$2,$3,$4) ON CONFLICT(email) DO UPDATE SET password_hash=EXCLUDED.password_hash RETURNING id',[name,name+'@example.test',hash,'SYNTHETIC-'+name]);
  await pool.query('INSERT INTO user_wallets(user_id,balance,total_earned,total_withdrawn) VALUES($1,0,0,0) ON CONFLICT(user_id) DO NOTHING',[r.rows[0].id]);
 }
 console.log('Synthetic accounts created; use the HTTP upload flow to create example files.');
})().catch(e=>{console.error(e.message);process.exitCode=1}).finally(()=>pool.end());
