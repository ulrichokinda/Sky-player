import { initializeApp } from 'firebase/app';
import { getAnalytics, isSupported } from 'firebase/analytics';
import { 
  getAuth, 
  onAuthStateChanged as firebaseOnAuthStateChanged, 
  signInWithEmailAndPassword as firebaseSignInWithEmailAndPassword,
  createUserWithEmailAndPassword as firebaseCreateUserWithEmailAndPassword,
  signOut as firebaseSignOut,
  GoogleAuthProvider,
  signInWithPopup as firebaseSignInWithPopup,
  sendPasswordResetEmail as firebaseSendPasswordResetEmail,
  sendEmailVerification as firebaseSendEmailVerification,
  updatePassword as firebaseUpdatePassword,
  EmailAuthProvider,
  reauthenticateWithCredential
} from 'firebase/auth';
import { 
  getFirestore, 
  collection as firestoreCollection, 
  doc as firestoreDoc, 
  getDoc as firestoreGetDoc,
  getDocs as firestoreGetDocs,
  setDoc as firestoreSetDoc,
  addDoc as firestoreAddDoc,
  updateDoc as firestoreUpdateDoc,
  deleteDoc as firestoreDeleteDoc,
  query as firestoreQuery,
  where as firestoreWhere,
  onSnapshot as firestoreOnSnapshot,
  getDocFromServer,
  Timestamp,
  initializeFirestore,
  persistentLocalCache,
  persistentMultipleTabManager
} from 'firebase/firestore';
import { 
  getStorage, 
  ref as storageRef, 
  uploadBytes as storageUploadBytes, 
  getDownloadURL as storageGetDownloadURL,
  deleteObject as storageDeleteObject
} from 'firebase/storage';

// Import the Firebase configuration
import firebaseConfig from '../firebase-applet-config.json';

// Initialize Firebase SDK
const app = initializeApp(firebaseConfig);

// Initialize Firestore with settings for better connectivity
const databaseId = firebaseConfig.firestoreDatabaseId || '(default)';
export const db = initializeFirestore(app, {
  experimentalForceLongPolling: true,
}, databaseId);

export const auth = getAuth(app);
export const storage = getStorage(app);
export const googleProvider = new GoogleAuthProvider();

// Initialize Analytics
export const analytics = isSupported().then(yes => yes ? getAnalytics(app) : null);

// Auth Helpers
export const onAuthStateChanged = (authObj: any, callback: (user: any) => void) => {
  return firebaseOnAuthStateChanged(authObj, callback);
};

export const signInWithEmailAndPassword = firebaseSignInWithEmailAndPassword;
export const createUserWithEmailAndPassword = firebaseCreateUserWithEmailAndPassword;
export const signOut = firebaseSignOut;
export const signInWithPopup = firebaseSignInWithPopup;
export const sendPasswordResetEmail = firebaseSendPasswordResetEmail;
export const sendEmailVerification = firebaseSendEmailVerification;
export const updatePassword = firebaseUpdatePassword;
export { EmailAuthProvider, reauthenticateWithCredential };

// Firestore Helpers
export const collection = firestoreCollection;
export const doc = firestoreDoc;
export const getDoc = firestoreGetDoc;
export const getDocs = firestoreGetDocs;
export const setDoc = firestoreSetDoc;
export const addDoc = firestoreAddDoc;
export const updateDoc = firestoreUpdateDoc;
export const deleteDoc = firestoreDeleteDoc;
export const query = firestoreQuery;
export const where = firestoreWhere;
export const onSnapshot = firestoreOnSnapshot;

// Storage Helpers
export const ref = storageRef;
export const uploadBytes = storageUploadBytes;
export const getDownloadURL = storageGetDownloadURL;
export const deleteObject = storageDeleteObject;

export enum OperationType {
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete',
  LIST = 'list',
  GET = 'get',
  WRITE = 'write',
}

export interface FirestoreErrorInfo {
  error: string;
  operationType: OperationType;
  path: string | null;
  authInfo: {
    userId?: string;
    email?: string | null;
    emailVerified?: boolean;
    isAnonymous?: boolean;
    tenantId?: string | null;
    providerInfo: {
      providerId: string;
      displayName: string | null;
      email: string | null;
      photoUrl: string | null;
    }[];
  }
}

export function handleFirestoreError(error: unknown, operationType: OperationType, path: string | null) {
  const errInfo: FirestoreErrorInfo = {
    error: error instanceof Error ? error.message : String(error),
    authInfo: {
      userId: auth.currentUser?.uid,
      email: auth.currentUser?.email,
      emailVerified: auth.currentUser?.emailVerified,
      isAnonymous: auth.currentUser?.isAnonymous,
      tenantId: auth.currentUser?.tenantId,
      providerInfo: auth.currentUser?.providerData.map(provider => ({
        providerId: provider.providerId,
        displayName: provider.displayName,
        email: provider.email,
        photoUrl: provider.photoURL
      })) || []
    },
    operationType,
    path
  }
  console.error('Firestore Error: ', JSON.stringify(errInfo));
  throw new Error(JSON.stringify(errInfo));
}

// Validate Connection to Firestore
async function testConnection() {
  try {
    await getDocFromServer(firestoreDoc(db, 'test', 'connection'));
    console.log("Firebase connection successful");
  } catch (error: any) {
    // If it's a permission error, it means we ARE talking to Firebase, 
    // but the test doc isn't public (which is fine).
    if (error?.code === 'permission-denied') {
      console.log("Firebase: Connection established (Auth verified)");
    } else {
      console.error("Firebase Connection Error Details:", error.message);
    }
  }
}
testConnection();
