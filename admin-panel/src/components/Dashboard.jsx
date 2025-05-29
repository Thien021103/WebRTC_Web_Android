import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
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
  Group as GroupIcon,
  Timeline as TimelineIcon,
  Logout as LogoutIcon,
} from '@mui/icons-material';
import AddGroupForm from './AddGroupForm';
import GroupList from './GroupList';

function Dashboard() {
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [mobileOpen, setMobileOpen] = useState(false);
  const navigate = useNavigate();

  const fetchGroups = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${import.meta.env.VITE_API_BASE_URL}/get-groups`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setGroups(response.data.groups || []);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGroups();
  }, []);

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  const drawer = (
    <List>
      <ListItem button selected>
        <ListItemIcon><GroupIcon sx={{ color: '#ffffff' }} /></ListItemIcon>
        <ListItemText primary="Groups" />
      </ListItem>
      <ListItem button disabled>
        <ListItemIcon><TimelineIcon sx={{ color: '#ffffff' }} /></ListItemIcon>
        <ListItemText primary="WebSocket Stats" />
      </ListItem>
      <ListItem button onClick={handleLogout}>
        <ListItemIcon><LogoutIcon sx={{ color: '#ffffff' }} /></ListItemIcon>
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
          <Typography variant="h6" noWrap sx={{ flexGrow: 1 }}>
            Admin Panel
          </Typography>
          <IconButton color="inherit">
            <img src="https://via.placeholder.com/40" alt="Profile" style={{ borderRadius: '50%' }} />
          </IconButton>
        </Toolbar>
      </AppBar>
      <Box sx={{ width: { sm: 240 }, flexShrink: { sm: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', sm: 'none' },
            '& .MuiDrawer-paper': { width: 240, mt: 8 },
          }}
        >
          { drawer }
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', sm: 'block' },
            '& .MuiDrawer-paper': { width: 240, mt: 8 },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>
      <Box component="main" sx={{ flexGrow: 1, p: 3, mt: 8, bgcolor: 'background.default' }}>
        <Fade in timeout={500}>
          <Paper elevation={3} sx={{ p: 4, borderRadius: 2, maxWidth: '100%' }}>
            <Typography
              variant="h4"
              sx={{
                fontWeight: 600,
                mb: 4,
                position: 'relative',
                '&:after': {
                  content: '""',
                  position: 'absolute',
                  bottom: -8,
                  left: 0,
                  width: 50,
                  height: 4,
                  background: 'linear-gradient(to right, #1976d2, #26a69a)',
                },
              }}
            >
              Group Management
            </Typography>
            <AddGroupForm onGroupAdded={fetchGroups} />
            <GroupList groups={groups} loading={loading} error={error} />
          </Paper>
        </Fade>
      </Box>
    </Box>
  );
}

export default Dashboard;