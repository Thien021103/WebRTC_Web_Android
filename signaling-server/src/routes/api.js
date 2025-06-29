const express = require('express');
const router = express.Router();

const { authMiddleware, adminAuth } = require('../middleware/auth');

const { handleLoginUser } = require('../controllers/loginUser');
const { handleLoginOwner } = require('../controllers/loginOwner');
const { handleRegister } = require('../controllers/register');
const { handleLogout } = require('../controllers/logout');
const { handleLock } = require('../controllers/lock');
const { handleUnlock } = require('../controllers/unlock');
const { handleGetUsers } = require('../controllers/getUser');
const { handleDeleteUser } = require('../controllers/deleteUser');
const { handleGetDoorHistory } = require('../controllers/getDoorHistory');
const { handleGetVideos, handleDeleteVideo } = require('../controllers/cloudVideo');
const { handleGetDoor } = require('../controllers/door');
const { handleRegisterOTP } = require('../controllers/registerOTP');
const { handleForgetOTP } = require('../controllers/forgetOTP');
const { handleAddGroup } = require('../controllers/addGroup');
const { handleAdminLogin } = require('../controllers/loginAdmin');
const { handleGetGroups } = require('../controllers/getGroups');
const { handleSendGroupId } = require('../controllers/sendGroupId');
const { handleGetOwners } = require('../controllers/getOwners');
const { handleGetGroup } = require('../controllers/group');
const { handleGetNotifications } = require('../controllers/notification');
const { handleChangePassword } = require('../controllers/changePassword');
const { handleDeleteOwner } = require('../controllers/deleteOwner');
const { handleLight } = require('../controllers/light');
const { handleAdminGetGroup } = require('../controllers/adminGetGroup');
const { handleResetPassword } = require('../controllers/resetPassword');
const { handleDeleteGroup } = require('../controllers/deleteGroup');

router.post('/login-user', handleLoginUser);
router.post('/login-owner', handleLoginOwner);

router.post('/register', handleRegister);
router.post('/logout', authMiddleware, handleLogout);

router.get('/check-token', authMiddleware, (req, res) => {
  res.json({ status: "success", message: "Valid" });
});

router.post('/change-password', authMiddleware, handleChangePassword);
router.post('/reset-password', handleResetPassword);

router.post('/forget-otp', handleForgetOTP);
router.post('/register-otp', handleRegisterOTP);

router.get('/light', authMiddleware, handleLight);

router.post('/lock', authMiddleware, handleLock);
router.post('/unlock', authMiddleware, handleUnlock);

router.get('/get-users', authMiddleware, handleGetUsers);
router.delete('/delete-users', authMiddleware, handleDeleteUser);

router.get('/door', authMiddleware, handleGetDoor);
router.get('/door-history', authMiddleware, handleGetDoorHistory);

router.get('/group', authMiddleware, handleGetGroup);

router.get('/notifications', authMiddleware, handleGetNotifications);

router.get('/get-videos', authMiddleware, handleGetVideos);
router.delete('/delete-videos', authMiddleware, handleDeleteVideo);

router.get('/get-owners', adminAuth, handleGetOwners);
router.delete('/delete-owner', adminAuth, handleDeleteOwner);

router.post('/admin-get-group', adminAuth, handleAdminGetGroup);
router.get('/get-groups', adminAuth, handleGetGroups);
router.post('/add-group', adminAuth, handleAddGroup);
router.delete('/delete-group', adminAuth, handleDeleteGroup);

router.post('/send-group-id', adminAuth, handleSendGroupId);
router.post('/login-admin', handleAdminLogin);


module.exports = router;