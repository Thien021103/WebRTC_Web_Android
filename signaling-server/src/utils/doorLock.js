const { transporter } = require("./otp");

async function mailDoorLock(email, status, userEmail) {

  await transporter.sendMail({
    from: process.env.EMAIL_USER,
    to: email,
    subject: `Your door has been ${status}`,
    text: `Hello,\n\nYour door has been ${status}.\nBy user with email ${userEmail}.\nIf you did not know this, please contact support immediately.\n\nBest regards.`,
  });
  console.log(`Door lock email sent to ${email}`);
  return true;
}

module.exports = { mailDoorLock };