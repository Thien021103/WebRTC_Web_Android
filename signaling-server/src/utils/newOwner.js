const { transporter } = require("./otp");

async function mailGroupId(email, groupId) {

  await transporter.sendMail({
    from: process.env.EMAIL_USER,
    to: email,
    subject: 'Your Group ID for Registration',
    text: `Your Group ID is: \n ${groupId}.`,
  });
  console.log(`Group ID sent to ${email}: ${groupId}`);
  return true;
}

module.exports = { mailGroupId };