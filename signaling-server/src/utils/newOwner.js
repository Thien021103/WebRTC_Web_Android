const { transporter } = require("./otp");

async function mailGroupId(email, groupId) {

  await transporter.sendMail({
    from: process.env.EMAIL_USER,
    to: email,
    subject: 'Your Group ID for Registration',
    text: `You have been added to a group, the group ID is: \n ${groupId}.\n\nBest regards.`,
  });
  console.log(`Group ID sent to ${email}: ${groupId}`);
  return true;
}

module.exports = { mailGroupId };