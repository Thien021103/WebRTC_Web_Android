const express = require('express');
const router = express.Router();

const { handleLogin } = require('../controllers/login');
const { handleRegister } = require('../controllers/register');
const { handleLogout } = require('../controllers/logout');
const { handleLock } = require('../controllers/lock');
const { handleUnlock } = require('../controllers/unlock');

router.post('/login', handleLogin);
router.post('/register', handleRegister);
router.post('/logout', handleLogout);
router.post('/lock', handleLock);
router.post('/unlock', handleUnlock);

module.exports = router;