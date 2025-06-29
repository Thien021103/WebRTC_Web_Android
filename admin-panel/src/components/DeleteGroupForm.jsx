import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  TextField,
  Button,
  CircularProgress,
  Snackbar,
  Alert,
  DialogTitle,
  DialogContent,
  DialogActions,
  DialogContentText,
} from '@mui/material';
import axios from 'axios';

function DeleteGroupForm({ groupId, onRefetch, onClose, setActionState, currentIndex }) {
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const navigate = useNavigate();

  const handleInvalidToken = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!password.trim()) {
      setError('Password is required');
      return;
    }
    setLoading(true);
    setError('');
    setActionState('deleting');
    try {
      const token = localStorage.getItem('token');
      await axios.delete('https://thientranduc.id.vn:444/api/delete-group', {
        data: { groupId, password },
        headers: { Authorization: `Bearer ${token}` },
      });
      setSnackbar({ open: true, message: 'Group deleted successfully!', severity: 'success' });
      setPassword('');
      onRefetch();
      // onClose();
    } catch (err) {
      if (err.response?.data?.message === 'Invalid token') {
        handleInvalidToken();
      } else {
        const errorMsg = err.response?.data?.message || 'Failed to delete group';
        setSnackbar({ open: true, message: errorMsg, severity: 'error' });
      }
    } finally {
      setLoading(false);
      setActionState('');
    }
  };

  const handleSnackbarClose = () => {
    setSnackbar((prev) => ({ ...prev, open: false }));
  };

  return (
    <>
      <DialogTitle>Delete Group</DialogTitle>
      <DialogContent>
        <DialogContentText>
          Are you sure you want to delete the group with ID:
        </DialogContentText>
        <DialogContentText>
          "{groupId}"?
        </DialogContentText>
      </DialogContent>
      <DialogContent>
        <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1 }}>
          <TextField
            label="Password"
            type="password"
            fullWidth
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={!!error}
            helperText={error}
            disabled={loading}
            margin="normal"
            autoFocus
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={loading}>
          Cancel
        </Button>
        <Button
          type="submit"
          variant="contained"
          color="error"
          onClick={handleSubmit}
          disabled={loading}
          startIcon={loading && <CircularProgress size={16} color="inherit" />}
        >
          Delete
        </Button>
      </DialogActions>
      <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={handleSnackbarClose}>
        <Alert onClose={handleSnackbarClose} severity={snackbar.severity} sx={{ width: '100%' }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}

export default DeleteGroupForm;