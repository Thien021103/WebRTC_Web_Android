const nodemailer = require('nodemailer');

// In-memory OTP store
const registerOtpStore = new Map();
const forgetOtpStore = new Map();

const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: process.env.EMAIL_USER,
    pass: process.env.EMAIL_PASS,
  },
});

async function createForgetOTP(email) {
  
  // 6-digit OTP
  const otp = Math.floor(100000 + Math.random() * 900000).toString();
  forgetOtpStore.set(
    email, 
    { otp, expires: Date.now() + 5 * 60 * 1000 }
  ); // 5-min expiry
  await transporter.sendMail({
    from: process.env.EMAIL_USER,
    to: email,
    subject: 'Your OTP for new password',
    text: `Your OTP is ${otp}. It expires in 5 minutes.`,
  });
  console.log(`OTP sent to ${email}: ${otp}`);
  return true;
}

async function createRegisterOTP(email) {
  
  // 6-digit OTP
  const otp = Math.floor(100000 + Math.random() * 900000).toString();
  registerOtpStore.set(
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

function verifyForgetOTP(email, otp) {
  const record = forgetOtpStore.get(email);
  if (!record || record.otp !== otp || Date.now() > record.expires) {
    forgetOtpStore.delete(email);
    return false;
  }
  forgetOtpStore.delete(email);
  return true;
}

function verifyRegisterOTP(email, otp) {
  const record = registerOtpStore.get(email);
  if (!record || record.otp !== otp || Date.now() > record.expires) {
    registerOtpStore.delete(email);
    return false;
  }
  registerOtpStore.delete(email);
  return true;
}

module.exports = { transporter, createRegisterOTP, createForgetOTP, verifyRegisterOTP, verifyForgetOTP };