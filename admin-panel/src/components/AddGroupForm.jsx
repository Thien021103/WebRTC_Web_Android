import { useState } from 'react';
import { v4 as uuidv4 } from 'uuid';
import axios from 'axios';
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Typography,
  Snackbar,
  Alert,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';

function AddGroupForm({ onGroupAdded }) {
  const [ownerEmail, setOwnerEmail] = useState('');
  const [error, setError] = useState(null);
  const [open, setOpen] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const navigate = useNavigate();
  
  const handleInvalidToken = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  const handleOpen = () => setOpen(true);
  const handleClose = () => {
    setOpen(false);
    setOwnerEmail('');
    setError(null);
  };

  const handleSnackbarClose = () => {
    setSnackbar((prev) => ({ ...prev, open: false }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!ownerEmail) {
      setError('Email is required');
      return;
    }

    try {
      const token = localStorage.getItem('token');
      const response = await axios.post('https://thientranduc.id.vn:444/api/add-group', {
        ownerEmail,
      }, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (response.status !== 201) {
        throw new Error('Failed to add group');
      }

      setOwnerEmail('');
      setSnackbar({ open: true, message: 'Group added successfully!', severity: 'success' });
      setError(null);
      onGroupAdded();
      // handleClose();
    } catch (err) {
      if (err.response?.data?.message === 'Invalid token') {
        setSnackbar({ open: true, message: err.response?.data?.message, severity: 'error' });
        handleInvalidToken();
      } else {
        setSnackbar({ open: true, message: err.response?.data?.message, severity: 'error' });
        setError(err.response?.data?.message || err.message);
      }
    }
  };

  return (
    <>
      <Button variant="contained" color="secondary" onClick={handleOpen} sx={{ mb: 2 }}>
        Add New Group
      </Button>
      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth sx={{ '& .MuiDialog-paper': { p: 2 } }}>
        <DialogTitle>Add New Group</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Owner Email"
            type="email"
            fullWidth
            variant="outlined"
            value={ownerEmail}
            onChange={(e) => setOwnerEmail(e.target.value)}
            placeholder="Enter owner email"
            error={!!error}
          />
          {error && (
            <Typography color="error" variant="body2" sx={{ mt: 1 }}>
              {error}
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose} color="secondary">
            Cancel
          </Button>
          <Button onClick={handleSubmit} variant="contained" color="primary">
            Add Group
          </Button>
        </DialogActions>
        <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={handleSnackbarClose}>
          <Alert onClose={handleSnackbarClose} severity={snackbar.severity} sx={{ width: '100%' }}>
            {snackbar.message}
          </Alert>
        </Snackbar>
      </Dialog>
    </>
  );
}

export default AddGroupForm;