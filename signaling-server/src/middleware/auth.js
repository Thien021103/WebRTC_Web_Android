const jwt = require('jsonwebtoken');

const SECRET_KEY = process.env.JWT_SECRET || 'your-secret-key'; // Use env in production

const authMiddleware = (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ status: "false", message: 'No token provided' });
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, SECRET_KEY);
    req.user = decoded; // { email, groupId, deviceId }
    next();
  } catch (error) {
    res.status(401).json({ status: "false", message: 'Invalid token' });
  }
};

const wsAuthMiddleware = (message, client) => {
  const match = message.match(/^CONNECT user (\S+)$/);
  if (!match) return false;

  const token = match[1];
  try {
    const decoded = jwt.verify(token, SECRET_KEY);
    client._user = decoded; // Store user data
    return true;
  } catch (error) {
    console.error('WebSocket auth failed:', error.message);
    return false;
  }
};

module.exports = { authMiddleware, wsAuthMiddleware };