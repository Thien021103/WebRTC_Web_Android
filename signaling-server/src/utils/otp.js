const nodemailer = require('nodemailer');

const otpStore = new Map(); // In-memory OTP store
const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: process.env.EMAIL_USER,
    pass: process.env.EMAIL_PASS,
  },
});

async function createAndSendOTP(email) {
  
  // 6-digit OTP
  const otp = Math.floor(100000 + Math.random() * 900000).toString();
  otpStore.set(
    email, 
    { otp, expires: Date.now() + 5 * 60 * 1000 }
  ); // 5-min expiry
  await transporter.sendMail({
    from: process.env.EMAIL_USER,
    to: email,
    subject: 'Your OTP for Registration',
    text: `Your OTP is ${otp}. It expires in 5 minutes.`,
  });
  console.log(`OTP sent to ${email}: ${otp}`);
  return true;
}

function verifyOTP(email, otp) {
  const record = otpStore.get(email);
  if (!record || record.otp !== otp || Date.now() > record.expires) {
    otpStore.delete(email);
    return false;
  }
  otpStore.delete(email);
  return true;
}

module.exports = { transporter, createAndSendOTP, verifyOTP };