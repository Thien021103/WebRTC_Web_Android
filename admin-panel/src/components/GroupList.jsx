import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Typography,
  Button,
  CircularProgress,
  Snackbar,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Chip,
  Box,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import axios from 'axios';

function GroupList({ groups, loading, error }) {
  const [actionState, setActionState] = useState('');
  const [currentIndex, setCurrentIndex] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [modalOpen, setModalOpen] = useState(false);
  const [groupData, setGroupData] = useState(null);
  const [selectedGroupId, setSelectedGroupId] = useState(null);
  const navigate = useNavigate();

  if (loading) return <Typography variant="body1" align="center" sx={{ py: 2 }}>Loading groups...</Typography>;
  if (error) return <Typography variant="body1" color="error" align="center" sx={{ py: 2 }}>Error: {error}</Typography>;
  if (!Array.isArray(groups) || groups.length === 0) return <Typography variant="body1" align="center" sx={{ py: 2 }}>No groups found.</Typography>;

  const getStateColor = (value) => {
    switch (value) {
      case 'Yes': return { bgcolor: 'success.main', color: 'white' };
      case 'No': return { bgcolor: 'error.main', color: 'white' };
      case 'Impossible': return { bgcolor: 'warning.main', color: 'white' };
      case 'Error': return { bgcolor: 'error.main', color: 'white' };
      case 'Ready': return { bgcolor: 'info.main', color: 'white' };
      case 'Creating': return { bgcolor: 'info.main', color: 'white' };
      case 'Disconnected': return { bgcolor: 'error.main', color: 'white' };
      case 'Connected': return { bgcolor: 'success.main', color: 'white' };
      default: return { bgcolor: 'success.main', color: 'white' };
    }
  };

  const handleInvalidToken = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  const fetchGroupDetails = async (id, index) => {
    setActionState('fetching');
    setCurrentIndex(index);
    try {
      const token = localStorage.getItem('token');
      const response = await axios.post(`https://thientranduc.id.vn:444/api/admin-get-group`, {
        groupId: id,
      }, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (response.data.status === 'success') {
        setGroupData(response.data.group);
        setModalOpen(true);
      } else {
        throw new Error(response.data.message || 'Failed to fetch group details');
      }
    } catch (err) {
      if (err.response?.data?.message === 'Invalid token') {
        handleInvalidToken();
      } else {
        setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to fetch group details', severity: 'error' });
      }
    } finally {
      setActionState('');
      setCurrentIndex(null);
    }
  };

  const handleSendGroupId = async (email, id, index) => {
    setActionState('sending');
    setCurrentIndex(index);
    try {
      const token = localStorage.getItem('token');
      await axios.post(`https://thientranduc.id.vn:444/api/send-group-id`, {
        email,
        groupId: id,
      }, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setSnackbar({ open: true, message: 'Group ID mailed successfully!', severity: 'success' });
    } catch (err) {
      if (err.response?.data?.message === 'Invalid token') {
        handleInvalidToken();
      } else {
        setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to send group ID', severity: 'error' });
      }
    } finally {
      setActionState('');
      setCurrentIndex(null);
    }
  };

  const handleViewDetails = (id, index) => {
    setSelectedGroupId(id);
    fetchGroupDetails(id, index);
  };

  const handleRefresh = () => {
    if (selectedGroupId) {
      const index = groups.findIndex((group) => group.id === selectedGroupId);
      if (index !== -1) fetchGroupDetails(selectedGroupId, index);
    }
  };

  const handleModalClose = () => {
    setModalOpen(false);
    setGroupData(null);
    setSelectedGroupId(null);
  };

  const handleSnackbarClose = () => {
    setSnackbar((prev) => ({ ...prev, open: false }));
  };

  return (
    <>
      <TableContainer component={Paper} sx={{ maxWidth: '100%', mt: 2, boxShadow: 3 }}>
        <Table stickyHeader>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 600, bgcolor: '#f5f7fa' }}>ID</TableCell>
              <TableCell sx={{ fontWeight: 600, bgcolor: '#f5f7fa' }}>Owner Email</TableCell>
              <TableCell sx={{ fontWeight: 600, bgcolor: '#f5f7fa' }}>Created At</TableCell>
              <TableCell sx={{ fontWeight: 600, bgcolor: '#f5f7fa' }}>Camera ID</TableCell>
              <TableCell sx={{ fontWeight: 600, bgcolor: '#f5f7fa' }}>Controller ID</TableCell>
              <TableCell sx={{ fontWeight: 600, bgcolor: '#f5f7fa' }}>Action</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {groups.map((group, index) => (
              <TableRow key={group.id} hover sx={{ bgcolor: index % 2 ? '#fafafa' : '#ffffff' }}>
                <TableCell>{group.id}</TableCell>
                <TableCell>{group.ownerEmail}</TableCell>
                <TableCell>{new Date(group.createdAt).toLocaleString()}</TableCell>
                <TableCell>{group.cameraId}</TableCell>
                <TableCell>{group.controllerId}</TableCell>
                <TableCell sx={{ display: 'flex', gap: 1 }}>
                  <Button
                    variant="contained"
                    color="primary"
                    size="small"
                    onClick={() => handleSendGroupId(group.ownerEmail, group.id, index)}
                    disabled={actionState === 'sending' && currentIndex === index}
                    startIcon={actionState === 'sending' && currentIndex === index && <CircularProgress size={16} color="inherit" />}
                  >
                    Mail Group ID
                  </Button>
                  <Button
                    variant="outlined"
                    color="info"
                    size="small"
                    onClick={() => handleViewDetails(group.id, index)}
                    disabled={actionState === 'fetching' && currentIndex === index}
                    startIcon={actionState === 'fetching' && currentIndex === index && <CircularProgress size={16} color="inherit" />}
                  >
                    View Details
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      <Dialog
        open={modalOpen}
        onClose={handleModalClose}
        sx={{ '& .MuiDialog-paper': { width: '600px', maxWidth: '90vw', p: 2 } }}
      >
        <DialogTitle>Group Details</DialogTitle>
        <DialogContent>
          {groupData && (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Typography component="div"><strong>Owner:</strong> {groupData.owner}</Typography>
              <Typography component="div">
                <strong>State:</strong>{' '}
                <Chip
                  label={groupData.state}
                  sx={{ ...getStateColor(groupData.state), fontWeight: 500 }}
                />
              </Typography>
              <Typography component="div">
                <strong>Owner registered:</strong>{' '}
                <Chip
                  label={groupData.registered}
                  sx={{ ...getStateColor(groupData.registered), fontWeight: 500 }}
                />
              </Typography>
              <Typography component="div">
                <strong>Camera:</strong>{' '}
                <Chip
                  label={groupData.camera}
                  sx={{ ...getStateColor(groupData.camera), fontWeight: 500 }}
                />
              </Typography>
              <Typography component="div">
                <strong>Controller:</strong>{' '}
                <Chip
                  label={groupData.controller}
                  sx={{ ...getStateColor(groupData.controller), fontWeight: 500 }}
                />
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button
            variant="contained"
            color="primary"
            startIcon={actionState === 'fetching' ? <CircularProgress size={16} color="inherit" /> : <RefreshIcon />}
            onClick={handleRefresh}
            disabled={actionState === 'fetching'}
          >
            Refresh
          </Button>
          <Button onClick={handleModalClose}>Close</Button>
        </DialogActions>
      </Dialog>
      <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={handleSnackbarClose}>
        <Alert onClose={handleSnackbarClose} severity={snackbar.severity} sx={{ width: '100%' }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}

export default GroupList;