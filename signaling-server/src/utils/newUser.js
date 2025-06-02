const { transporter } = require("./otp");

async function mailNewUser(email, name, ownerEmail) {

  await transporter.sendMail({
    from: process.env.EMAIL_USER,
    to: email,
    subject: 'You had been added to a group, by Owner',
    text: `Your Owner's email is: ${ownerEmail}.\nYour name will be ${name}.\nPlease contact them for further details.`,
  });
  console.log(`Owner email sent to ${email}: ${ownerEmail}`);
  return true;
}

module.exports = { mailNewUser };