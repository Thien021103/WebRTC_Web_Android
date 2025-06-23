import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Drawer,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Typography,
  Toolbar,
  AppBar,
  IconButton,
  Paper,
  Fade,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Monitor as MonitorIcon,
  People as PeopleIcon,
  History as HistoryIcon,
  Logout as LogoutIcon,
  Notifications as NotificationsIcon,
  VideoLibrary as VideoLibraryIcon,
  Videocam as VideocamIcon,
} from '@mui/icons-material';
import GroupDetails from '../group/GroupDetail';
import UserList from '../user-list/UserList';
import DoorHistory from '../door/DoorHistory';
import Notifications from '../notifications/Notifications';
import Videos from '../videos/Videos';
import Camera from '../camera/Camera';

function Dashboard() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [view, setView] = useState('group');

  const navigate = useNavigate();

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('email');
    navigate('/login');
  };

  const handleRefetch = () => {
    // Placeholder for coordination
  };

  const drawer = (
    <List>
      <ListItem
        onClick={() => setView('group')}
        selected={view === 'group'}
      >
        <ListItemIcon><MonitorIcon /></ListItemIcon>
        <ListItemText primary="Doorbell System" />
      </ListItem>
      <ListItem
        onClick={() => setView('users')}
        selected={view === 'users'}
      >
        <ListItemIcon><PeopleIcon /></ListItemIcon>
        <ListItemText primary="Group Users" />
      </ListItem>
      <ListItem
        onClick={() => setView('history')}
        selected={view === 'history'}
      >
        <ListItemIcon><HistoryIcon /></ListItemIcon>
        <ListItemText primary="Door History" />
      </ListItem>
      <ListItem
        onClick={() => setView('notifications')}
        selected={view === 'notifications'}
      >
        <ListItemIcon><NotificationsIcon /></ListItemIcon>
        <ListItemText primary="Notifications" />
      </ListItem>
      <ListItem
        onClick={() => setView('videos')}
        selected={view === 'videos'}
      >
        <ListItemIcon><VideoLibraryIcon /></ListItemIcon>
        <ListItemText primary="Videos" />
      </ListItem>
      <ListItem 
        onClick={() => setView('camera')} 
        selected={view === 'camera'}
      >
        <ListItemIcon><VideocamIcon /></ListItemIcon>
        <ListItemText primary="Camera" />
      </ListItem>
      <ListItem onClick={handleLogout}>
        <ListItemIcon><LogoutIcon /></ListItemIcon>
        <ListItemText primary="Logout" />
      </ListItem>
    </List>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
        <Toolbar>
          <IconButton
            color="inherit"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { sm: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h5" noWrap sx={{ flexGrow: 1 }}>
            Owner Portal
          </Typography>
        </Toolbar>
      </AppBar>
      <Box component="nav" sx={{ width: { sm: 240 }, flexShrink: { sm: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', sm: 'none' },
            '& .MuiDrawer-paper': { width: 240, mt: 8, boxSizing: 'border-box' },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', sm: 'block' },
            '& .MuiDrawer-paper': { width: 240, mt: 8, boxSizing: 'border-box' },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>
      <Box component="main" sx={{ flexGrow: 1, p: 3, mt: 8 }}>
        <Fade in timeout={500}>
          <Paper elevation={3} sx={{ p: 4, borderRadius: 2, maxWidth: '100%' }}>
            <Typography
              variant="h4"
              sx={{
                mb: 4,
                position: 'relative',
                '&:after': {
                  content: '""',
                  position: 'absolute',
                  bottom: -8,
                  left: 0,
                  width: 50,
                  height: 4,
                  bgcolor: 'primary.main',
                },
              }}
            >
              {view === 'group' ? 'Doorbell System' : view === 'users' ? 'Group Users' : view === 'history' ? 'Door History' : view === 'notifications' ? 'Notifications' : view === 'videos' ? 'Videos' : 'Live Camera'}
            </Typography>
            {view === 'group' && <GroupDetails onRefetch={handleRefetch} />}
            {view === 'users' && <UserList onRefetch={handleRefetch} />}
            {view === 'history' && <DoorHistory email={localStorage.getItem('email')} onRefetch={handleRefetch} />}
            {view === 'notifications' && <Notifications onRefetch={handleRefetch} />}
            {view === 'videos' && <Videos onRefetch={handleRefetch} />}
            {view === 'camera' && <Camera />}
          </Paper>
        </Fade>
      </Box>
    </Box>
  );
}

export default Dashboard;