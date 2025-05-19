const jwt = require('jsonwebtoken');
const Owner = require('../schemas/owner');
const User = require('../schemas/user');
const Group = require('../schemas/group');

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
      return res.status(401).json({ status: "false", message: 'Invalid token' });
    }

    req.user = decoded; // { email | id, groupId, isOwner }
    next();
  } catch (error) {
    console.log(error.message);
    res.status(401).json({ status: "false", message: 'Invalid or expired token' });
  }
};

const wsUserAuth = async (token, client) => {
  try {
    const decoded = jwt.verify(token, SECRET_KEY);
    let entity;
    if (decoded.isOwner) {
      entity = await Owner.findOne({ email: decoded.email, groupId: decoded.groupId, accessToken: token });
    } else {
      entity = await User.findOne({ id: decoded.id, groupId: decoded.groupId, accessToken: token });
    }
    if (!entity) {
      console.error('Invalid access token');
      return false;
    }
    client._groupId = decoded.groupId; // Store user data
    console.log(`New client on group: ${client._groupId}`)
    client._accessToken = token;
    return true;
  } catch (error) {
    console.error('WebSocket auth failed:', error.message);
    return false;
  }
};

const wsCameraAuth = async (token, client) => {
  try {
    const decoded = jwt.verify(token, SECRET_KEY);
    if (!decoded.cameraId || !decoded.groupId) {
      console.error('Invalid camera token: missing cameraId or groupId');
      return false;
    }
    console.log(decoded);
    const group = await Group.findOne({ cameraId: decoded.cameraId, groupId: decoded.groupId, cameraToken: token });
    if (!group) {
      console.error(`Invalid camera token: ${token}`);
      return false;
    }
    client._camera = decoded; // { cameraId, groupId }
    return true;
  } catch (error) {
    console.error('WebSocket camera auth failed:', error.message);
    return false;
  }
};

const wsControllerAuth = async (token, client) => {
  try {
    const decoded = jwt.verify(token, SECRET_KEY);
    if (!decoded.controllerId || !decoded.groupId) {
      console.error('Invalid controller token: missing controllerId or groupId');
      return false;
    }
    const group = await Group.findOne({ controllerId: decoded.controllerId, id: decoded.groupId, controllerToken: token });
    if (!group) {
      console.error('Invalid or revoked controller token');
      return false;
    }
    client._controller = decoded; // { controllerId, groupId }
    return true;
  } catch (error) {
    console.error('WebSocket controller auth failed:', error.message);
    return false;
  }
};

module.exports = { authMiddleware, wsUserAuth, wsCameraAuth, wsControllerAuth };