import 'dotenv/config';
import admin from 'firebase-admin';
import { getFirestore } from 'firebase-admin/firestore';
import fs from 'fs';

const firebaseConfig = JSON.parse(fs.readFileSync('firebase-applet-config.json', 'utf8'));
const projectId = firebaseConfig.projectId;
const databaseId = firebaseConfig.firestoreDatabaseId || '(default)';

const saContent = process.env.FIREBASE_SERVICE_ACCOUNT;
if (saContent) {
  admin.initializeApp({
    credential: admin.credential.cert(JSON.parse(saContent)),
    projectId
  });
} else {
  admin.initializeApp({ projectId });
}

const db = getFirestore(admin.app(), databaseId);

async function check() {
  const mac = 'D9:F1:53:29:72:3C';
  console.log('Querying for MAC:', mac);
  const snapshot = await db.collection('activations').where('target_mac', '==', mac).get();
  console.log(`Found ${snapshot.docs.length} documents for MAC ${mac}`);
  snapshot.docs.forEach(doc => {
    console.log(doc.id, '=>', doc.data());
  });
  
  console.log('--- ALL ACTIVATIONS ---');
  const all = await db.collection('activations').get();
  all.docs.forEach(doc => {
    console.log(doc.id, '=> MAC:', doc.data().target_mac);
  });
}
check().catch(console.error);
