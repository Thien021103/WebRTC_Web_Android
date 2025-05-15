const express = require('express');
const router = express.Router();
const { handleLogin } = require('../handlers/login');
const { handleRegister } = require('../handlers/register');
const { handleLogout } = require('../handlers/logout');
const { handleLock } = require('../handlers/lock');
const { handleUnlock } = require('../handlers/unlock');

router.post('/login', handleLogin);
router.post('/register', handleRegister);
router.post('/logout', handleLogout);
router.post('/lock', handleLock);
router.post('/unlock', handleUnlock);

module.exports = router;