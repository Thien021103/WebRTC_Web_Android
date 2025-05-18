const express = require('express');
const router = express.Router();

const { authMiddleware } = require('../middleware/auth');

const { handleLogin } = require('../controllers/login');
const { handleRegister } = require('../controllers/register');
const { handleLogout } = require('../controllers/logout');
const { handleLock } = require('../controllers/lock');
const { handleUnlock } = require('../controllers/unlock');
const { handleGetUsers } = require('../controllers/getUser');
const { handleDeleteUser } = require('../controllers/deleteUser');
const { handleGetDoorHistory } = require('../controllers/getDoorHistory');

router.post('/login', handleLogin);
router.post('/register', handleRegister);
router.post('/logout', authMiddleware, handleLogout);

router.post('/lock', authMiddleware, handleLock);
router.post('/unlock', authMiddleware, handleUnlock);

router.get('/users', authMiddleware, handleGetUsers);
router.delete('/users', authMiddleware, handleDeleteUser);

router.get('/door-history', authMiddleware, handleGetDoorHistory);


module.exports = router;