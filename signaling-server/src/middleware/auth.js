const jwt = require('jsonwebtoken');
const Owner = require('../schemas/owner');
const User = require('../schemas/user');
const Group = require('../schemas/group');
const Admin = require('../schemas/admin');

const SECRET_KEY = process.env.JWT_SECRET || 'your-secret-key'; // Use env in production

const authMiddleware = async (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ status: "false", message: 'Invalid token' });
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, SECRET_KEY);
    let entity;

    if (decoded.isOwner) {
      entity = await Owner.findOne({ email: decoded.email, groupId: decoded.groupId, accessToken: token });
    } else {
      entity = await User.findOne({ email: decoded.email, groupId: decoded.groupId, accessToken: token });
    }

    if (!entity) {
      return res.status(401).json({ status: "false", message: 'Invalid token' });
    }

    req.user = decoded; // { email, groupId, isOwner }
    next();
  } catch (error) {
    console.log(error.message);
    res.status(401).json({ status: "false", message: 'Invalid token' });
  }
};

const wsUserAuth = async (token, client) => {
  try {
    const decoded = jwt.verify(token, SECRET_KEY);
    let entity;
    if (decoded.isOwner) {
      entity = await Owner.findOne({ email: decoded.email, groupId: decoded.groupId, accessToken: token });
    } else {
      entity = await User.findOne({ email: decoded.email, groupId: decoded.groupId, accessToken: token });
    }
    if (!entity) {
      console.error('Invalid access token');
      return false;
    }
    client._tmpGroupId = decoded.groupId;
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
    const group = await Group.findOne({ cameraId: decoded.cameraId, id: decoded.groupId, cameraToken: token });
    if (!group) {
      console.error(`Invalid camera token: ${token}`);
      return false;
    }
    client._tmpGroupId = decoded.groupId;
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
    client._tmpGroupId = decoded.groupId;
    return true;
  } catch (error) {
    console.error('WebSocket controller auth failed:', error.message);
    return false;
  }
};

const adminAuth = async (req, res, next) => {
  const token = req.header('Authorization')?.replace('Bearer ', '');
  if (!token) {
    return res.status(401).json({ status: 'false', message: 'Invalid token' });
  }

  try {
    const decoded = jwt.verify(token, SECRET_KEY);
    let entity;
    if (decoded.isAdmin) {
      entity = await Admin.findOne({ email: decoded.email, accessToken: token });
    } else {
      return res.status(401).json({ status: 'false', message: 'Invalid token' });
    }

    if (!entity) {
      return res.status(401).json({ status: 'false', message: 'Invalid token' });
    }
    req.user = decoded;
    next();
  } catch (error) {
    res.status(401).json({ status: 'false', message: 'Invalid token' });
  }
}

module.exports = { authMiddleware, wsUserAuth, wsCameraAuth, wsControllerAuth, adminAuth };