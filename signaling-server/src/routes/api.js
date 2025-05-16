const express = require('express');
const router = express.Router();

const { handleLogin } = require('../controllers/login');
const { handleRegister } = require('../controllers/register');
const { handleLogout } = require('../controllers/logout');
const { handleLock } = require('../controllers/lock');
const { handleUnlock } = require('../controllers/unlock');

const { authMiddleware } = require('../middleware/auth');

router.post('/login', handleLogin);
router.post('/register', handleRegister);
router.post('/logout', authMiddleware, handleLogout);
router.post('/lock', authMiddleware, handleLock);
router.post('/unlock', authMiddleware, handleUnlock);
module.exports = router;