const jwt = require('jsonwebtoken');
const Owner = require('../schemas/owner');
const User = require('../schemas/user');

const SECRET_KEY = process.env.JWT_SECRET || 'your-secret-key'; // Use env in production

const authMiddleware = async (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ status: "false", message: 'No token provided' });
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, SECRET_KEY);
    let entity;

    if (decoded.isOwner) {
      entity = await Owner.findOne({ email: decoded.email, groupId: decoded.groupId, accessToken: token });
    } else {
      entity = await User.findOne({ id: decoded.id, groupId: decoded.groupId, accessToken: token });
    }

    if (!entity) {
      return res.status(401).json({ status: "false", message: 'Invalid or revoked token' });
    }

    req.user = decoded; // { email | id, groupId, isOwner }
    next();
  } catch (error) {
    console.log(error.message);
    res.status(401).json({ status: "false", message: 'Invalid or expired token' });
  }
};

const wsAuthMiddleware = (token, client) => {
  try {
    const decoded = jwt.verify(token, SECRET_KEY);
    client._user = decoded; // Store user data
    client._accessToken = token;
    return true;
  } catch (error) {
    console.error('WebSocket auth failed:', error.message);
    return false;
  }
};

module.exports = { authMiddleware, wsAuthMiddleware };