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
  People as PeopleIcon,
  Logout as LogoutIcon,
} from '@mui/icons-material';

import AddGroupForm from './AddGroupForm';
import GroupList from './GroupList';
import OwnerList from './OwnerList';

function Dashboard() {
  const [groups, setGroups] = useState([]);
  const [owners, setOwners] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [view, setView] = useState('groups');
  const navigate = useNavigate();

  const handleInvalidToken = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  const fetchGroups = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`https://thientranduc.id.vn:444/api/get-groups`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setGroups(response.data.groups || []);
      setError(null);
    } catch (err) {
      if (err.response?.data?.message === 'Invalid token') {
        handleInvalidToken();
      } else {
        setError(err.response?.data?.message || err.message);
      }
    } finally {
      setLoading(false);
    }
  };

  const fetchOwners = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`https://thientranduc.id.vn:444/api/get-owners`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setOwners(response.data.owners || []);
      setError(null);
    } catch (err) {
      if (err.response?.data?.message === 'Invalid token') {
        handleInvalidToken();
      } else {
        setError(err.response?.data?.message || err.message);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGroups();
    fetchOwners();
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
      <ListItem
        onClick={() => setView('groups')}
        selected={view === 'groups'}
        sx={{ bgcolor: view === 'groups' ? 'rgba(255, 255, 255, 0.1)' : 'transparent' }}
      >
        <ListItemIcon><GroupIcon sx={{ color: '#ffffff' }} /></ListItemIcon>
        <ListItemText primary="Groups" />
      </ListItem>
      <ListItem
        onClick={() => setView('owners')}
        selected={view === 'owners'}
        sx={{ bgcolor: view === 'owners' ? 'rgba(255, 255, 255, 0.1)' : 'transparent' }}
      >
        <ListItemIcon><PeopleIcon sx={{ color: '#ffffff' }} /></ListItemIcon>
        <ListItemText primary="Owners" />
      </ListItem>
      <ListItem onClick={handleLogout}>
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
            '& .MuiDrawer-paper': { width: 240, mt: 8 },
          }}
        >
          {drawer}
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
              {view === 'groups' ? 'Group Management' : 'Owner Management'}
            </Typography>
            {view === 'groups' && (
              <>
                <AddGroupForm onGroupAdded={fetchGroups} />
                <GroupList groups={groups} loading={loading} error={error} onRefetch={fetchGroups} />
              </>
            )}
            {view === 'owners' && (
              <OwnerList owners={owners} loading={loading} error={error} onRefetch={fetchOwners} />
            )}
          </Paper>
        </Fade>
      </Box>
    </Box>
  );
}

export default Dashboard;