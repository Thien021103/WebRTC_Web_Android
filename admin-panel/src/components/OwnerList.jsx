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
  DialogContentText,
  DialogActions,
  Box,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import axios from 'axios';

function OwnerList({ owners, loading, error, onRefetch }) {
  const [actionState, setActionState] = useState('');
  const [currentIndex, setCurrentIndex] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedOwner, setSelectedOwner] = useState(null);
  const navigate = useNavigate();

  if (loading && !owners.length) return <Typography variant="body1" align="center" sx={{ py: 2 }}>Loading owners...</Typography>;
  if (error) return <Typography variant="body1" color="error" align="center" sx={{ py: 2 }}>Error: {error}</Typography>;
  if (!Array.isArray(owners) || owners.length === 0) return <Typography variant="body1" align="center" sx={{ py: 2 }}>No owners found.</Typography>;

  const handleInvalidToken = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  const formatFcmToken = (token) => {
    if (!token) return '-';
    const chunkSize = 60;
    return token.match(new RegExp(`.{1,${chunkSize}}`, 'g')).join('\n');
  };

  const handleOpenDialog = (owner, index) => {
    setSelectedOwner({ email: owner.email, groupId: owner.groupId, index });
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setSelectedOwner(null);
  };

  const handleDeleteOwner = async (email, groupId, index) => {
    setActionState('deleting');
    setCurrentIndex(index);
    try {
      const token = localStorage.getItem('token');
      await axios.delete(`https://thientranduc.id.vn:444/api/delete-owner`, {
        data: { email, groupId },
        headers: { Authorization: `Bearer ${token}` },
      });
      setSnackbar({ open: true, message: 'Owner deleted successfully!', severity: 'success' });
      onRefetch();
    } catch (err) {
      if (err.response?.data?.message === 'Invalid token') {
        handleInvalidToken();
      } else {
        const errorMsg = err.response?.data?.message || 'Failed to delete owner';
        setSnackbar({ open: true, message: errorMsg, severity: 'error' });
      }
    } finally {
      setActionState('');
      setCurrentIndex(null);
      handleCloseDialog();
    }
  };

  const handleSnackbarClose = () => {
    setSnackbar((prev) => ({ ...prev, open: false }));
  };

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
        <Button
          variant="contained"
          color="primary"
          startIcon={loading ? <CircularProgress size={16} color="inherit" /> : <RefreshIcon />}
          onClick={onRefetch}
          disabled={loading}
        >
          Refresh
        </Button>
      </Box>
      <TableContainer component={Paper} sx={{ maxWidth: '100%', boxShadow: 3 }}>
        <Table stickyHeader>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 600, bgcolor: '#f5f7fa' }}>Email</TableCell>
              <TableCell sx={{ fontWeight: 600, bgcolor: '#f5f7fa' }}>Created At</TableCell>
              <TableCell sx={{ fontWeight: 600, bgcolor: '#f5f7fa' }}>FCM Token</TableCell>
              <TableCell sx={{ fontWeight: 600, bgcolor: '#f5f7fa' }}>Group ID</TableCell>
              <TableCell sx={{ fontWeight: 600, bgcolor: '#f5f7fa' }}>Action</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {owners.map((owner, index) => (
              <TableRow key={owner.groupId} hover sx={{ bgcolor: index % 2 ? '#fafafa' : '#ffffff' }}>
                <TableCell>{owner.email}</TableCell>
                <TableCell>{new Date(owner.createdAt).toLocaleString()}</TableCell>
                <TableCell sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                  {formatFcmToken(owner.fcmToken)}
                </TableCell>
                <TableCell>{owner.groupId || '-'}</TableCell>
                <TableCell>
                  <Button
                    variant="contained"
                    color="error"
                    size="small"
                    onClick={() => handleOpenDialog(owner, index)}
                    disabled={actionState === 'deleting' && currentIndex === index}
                    startIcon={actionState === 'deleting' && currentIndex === index && <CircularProgress size={16} color="inherit" />}
                  >
                    Delete
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      <Dialog open={dialogOpen} onClose={handleCloseDialog}>
        <DialogTitle>Confirm Delete</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete the owner with email "{selectedOwner?.email}"?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button
            onClick={() => handleDeleteOwner(selectedOwner.email, selectedOwner.groupId, selectedOwner.index)}
            color="error"
          >
            Delete
          </Button>
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

export default OwnerList;