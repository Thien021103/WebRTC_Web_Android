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
} from '@mui/material';

function AddGroupForm({ onGroupAdded }) {
  const [ownerEmail, setOwnerEmail] = useState('');
  const [error, setError] = useState(null);
  const [open, setOpen] = useState(false);

  const handleOpen = () => setOpen(true);
  const handleClose = () => {
    setOpen(false);
    setOwnerEmail('');
    setError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!ownerEmail) {
      setError('Email is required');
      return;
    }

    try {
      const token = localStorage.getItem('token');
      const response = await axios.post(`${import.meta.env.VITE_API_BASE_URL}/add-group`, {
        ownerEmail,
      }, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (response.status !== 201) {
        throw new Error('Failed to add group');
      }

      setOwnerEmail('');
      setError(null);
      onGroupAdded();
      handleClose();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <>
      <Button variant="contained" color="primary" onClick={handleOpen} sx={{ mb: 2 }}>
        Add New Group
      </Button>
      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
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
      </Dialog>
    </>
  );
}

export default AddGroupForm;