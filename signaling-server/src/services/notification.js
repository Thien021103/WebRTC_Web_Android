const Notification = require('../schemas/notification');
const Group = require('../schemas/group');

async function getNotifications(decoded, query) {
  const { groupId, isOwner, email } = decoded;
  const { startDate, endDate, limit = 50, page = 1 } = query;

  // Validate groupId
  const dbGroup = await Group.findOne({ id: groupId });
  if (!dbGroup) {
    throw new Error('Group not found');
  }

  // Build query
  const queryObj = { groupId };
  if (startDate) {
    const start = new Date(startDate);
    if (isNaN(start.getTime())) {
      throw new Error('Invalid startDate');
    }
    queryObj.time = { $gte: start };
  }
  if (endDate) {
    const end = new Date(endDate);
    if (isNaN(end.getTime())) {
      throw new Error('Invalid endDate');
    }
    queryObj.time = queryObj.time || {};
    queryObj.time.$lte = end;
  }

  // Pagination
  const limitNum = Math.min(parseInt(limit, 10), 50);
  const pageNum = Math.max(parseInt(page, 10), 1);
  const skip = (pageNum - 1) * limitNum;

  // Fetch notifications
  const notifications = await Notification.find(queryObj)
    .select('groupId time -_id')
    .sort({ time: -1 })
    .skip(skip)
    .limit(limitNum)
    .lean();

  return notifications;
}

module.exports = { getNotifications };