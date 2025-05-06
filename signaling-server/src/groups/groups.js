/************************  
Groups are stored locally as JSON, presented as: 
{ id,
  state,
  clients: {
    camera: ws,
    user: ws,
    controller: ws
  },
  fcmToken,
}
************************/
const groups = new Map();

function notifyStateUpdate(groupId) {
  const group = groups.get(groupId);
  if (group) {
    const { camera, user, controller } = group.clients;
    const message = `STATE ${group.state}`;
    [camera, user, controller].forEach((client) => {
      if (client && client.readyState === client.OPEN) {
        client.send(message);
      }
    });
  }
}

module.exports = { groups, notifyStateUpdate };