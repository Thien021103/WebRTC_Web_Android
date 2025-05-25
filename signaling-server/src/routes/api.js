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
const { handleGetVideos, handleDeleteVideo } = require('../controllers/cloudVideo');
const { handleGetDoor } = require('../controllers/door');

router.post('/login', handleLogin);
router.post('/register', handleRegister);
router.post('/logout', authMiddleware, handleLogout);

router.post('/lock', authMiddleware, handleLock);
router.post('/unlock', authMiddleware, handleUnlock);

router.get('/get-users', authMiddleware, handleGetUsers);
router.delete('/delete-users', authMiddleware, handleDeleteUser);

router.get('/door', authMiddleware, handleGetDoor);
router.get('/door-history', authMiddleware, handleGetDoorHistory);

router.get('/get-videos', authMiddleware, handleGetVideos);
router.delete('/delete-videos', authMiddleware, handleDeleteVideo);

module.exports = router;