const admin = require('firebase-admin');

const serviceAccount = require('../../firebase-admin-sdk.json');

const app = admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

async function sendFCMNotification(token) {
  try {
    const message = {
      data: {
        show_signalling: 'true',
      },
      token: token,
    };
    const response = await admin.messaging(app).send(message);
    console.log('FCM notification sent:', response);
  } catch (error) {
    console.error('Error sending FCM notification:', error.message);
  }
}

module.exports = { sendFCMNotification };