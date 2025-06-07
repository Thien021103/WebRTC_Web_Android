const { transporter } = require("./otp");

async function mailNewPassword(email) {

  await transporter.sendMail({
    from: process.env.EMAIL_USER,
    to: email,
    subject: 'Your password has been changed',
    text: `Hello,\n\nYour password has been successfully changed.\n\nIf you did not request this change, please contact support immediately.\n\nBest regards,\nYour Team`,
  });
  console.log(`Password changed email sent to ${email}`);
  return true;
}

module.exports = { mailNewPassword };