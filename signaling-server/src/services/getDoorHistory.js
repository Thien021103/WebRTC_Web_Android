const Door = require('../schemas/door');

async function getDoorHistory(decoded, query) {
  const { groupId, isOwner, email, id } = decoded;
  const { state, startDate, endDate, limit = 50, page = 1 } = query;

  // Build query
  const queryObj = { groupId };
  if (state) {
    if (!['Locked', 'Unlocked'].includes(state)) {
      throw new Error('Invalid state value');
    }
    queryObj.state = state;
  }
  if (startDate) {
    const start = new Date(startDate);
    if (isNaN(start.getTime())) {
      throw new Error('Invalid startDate');
    }
    queryObj.timestamp = { $gte: start };
  }
  if (endDate) {
    const end = new Date(endDate);
    if (isNaN(end.getTime())) {
      throw new Error('Invalid endDate');
    }
    queryObj.timestamp = queryObj.timestamp || {};
    queryObj.timestamp.$lte = end;
  }

  // Pagination
  const limitNum = Math.min(parseInt(limit, 10), 50);
  const pageNum = Math.max(parseInt(page, 10), 1);
  const skip = (pageNum - 1) * limitNum;

  // Fetch door history
  const history = await Door.find(queryObj)
    .select('state user timestamp -_id')
    .sort({ timestamp: -1 }) // Newest first
    .skip(skip)
    .limit(limitNum)
    .lean();

  console.log(`${isOwner ? 'Owner' : 'User'} ${isOwner ? email : id} retrieved ${history.length} door history entries for group: ${groupId}`);
  return history;
}

module.exports = { getDoorHistory };