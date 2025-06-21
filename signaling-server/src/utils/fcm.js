const admin = require('firebase-admin');

const serviceAccount = require('../../firebase-admin-sdk.json');

const app = admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

async function sendFCMNotification(token, type) {
  try {
    if(type == 'notify') {
      const message = {
        data: {
          type: 'notify',
        },
        token: token,
      };
    } else if(type == 'human') {
      const message = {
        data: {
          type: 'notify',
        },
        token: token,
      };
    }
    const response = await admin.messaging(app).send(message);
    console.log('FCM notification sent:', response);
  } catch (error) {
    console.error('Error sending FCM notification:', error.message);
  }
}

module.exports = { sendFCMNotification };