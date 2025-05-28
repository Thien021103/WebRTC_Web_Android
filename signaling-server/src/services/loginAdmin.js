const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const Admin = require('../schemas/admin');

const SECRET_KEY = process.env.JWT_SECRET || 'your-secret-key';

async function loginAdmin({ email, password }) {
  if (!email || !password ) {
    throw new Error('Missing required fields');
  }

  const admin = await Admin.findOne({ email: email, password: password });
  if (!admin) {
    throw new Error('Invalid info');
  }

  const isPasswordValid = await bcrypt.compare(password, admin.password);
  if (!isPasswordValid) {
    throw new Error('Invalid password');
  }

  const accessToken = jwt.sign({ email: email, isAdmin: true }, SECRET_KEY, { expiresIn: '7d' });
  await Admin.updateOne(
    { email, groupId }, 
    { $set: { accessToken: accessToken } }
  );

  console.log(`Admin logged in: ${email}`);
  return { accessToken };
}

module.exports = { loginAdmin };