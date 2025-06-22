import { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  CircularProgress,
  Card,
  CardContent,
  Fade,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Snackbar,
  Alert,
} from '@mui/material';
import { Delete as DeleteIcon, Add as AddIcon, PeopleOutline as PeopleOutlineIcon } from '@mui/icons-material';
import { useTheme } from '@mui/material';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

function UserList({ onRefetch }) {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [deleteDialog, setDeleteDialog] = useState({ open: false, email: '' });
  const [addDialog, setAddDialog] = useState({ open: false });
  const [formData, setFormData] = useState({
    email: '',
    name: '',
    password: '',
    passwordConfirm: '',
  });
  const [formErrors, setFormErrors] = useState({});
  const theme = useTheme();
  const navigate = useNavigate(); // Initialize navigate

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`https://thientranduc.id.vn:444/api/get-users`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setUsers(response.data.users || []);
      setError(null);
    } catch (err) {
      if (err.response?.data?.status === "false" && err.response?.data?.message === 'Invalid token') {
        // Optional: Clear invalid token
        localStorage.removeItem('token');
        localStorage.removeItem('email');
        navigate('/login'); // Navigate to /login
      } else {
        setError(err.response?.data?.message || 'Failed to fetch users');
        setSnackbar({ open: true, message: errorMessage, severity: 'error' });
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleDelete = async (email) => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await axios.delete(`https://thientranduc.id.vn:444/api/delete-users`, {
        headers: { Authorization: `Bearer ${token}` },
        data: { email },
      });
      setSnackbar({
        open: true,
        message: response.data.message || `User ${email} deleted successfully`,
        severity: 'success',
      });
      await fetchUsers();
      onRefetch();
    } catch (err) {
      if (err.response?.data?.status === "false" && err.response?.data?.message === 'Invalid token') {
        // Optional: Clear invalid token
        localStorage.removeItem('token');
        localStorage.removeItem('email');
        navigate('/login'); // Navigate to /login
      } else {
        const errorMessage = err.response?.data?.message || 'Failed to delete user';
        setSnackbar({ open: true, message: errorMessage, severity: 'error' });
      }
    } finally {
      setLoading(false);
      setDeleteDialog({ open: false, email: '' });
    }
  };

  const handleAddUser = async () => {
    const errors = validateForm();
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await axios.post(
        `https://thientranduc.id.vn:444/api/register`,
        {
          email: formData.email,
          userName: formData.name,
          password: formData.password,
          ownerToken: token,
        },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setSnackbar({
        open: true,
        message: response.data.message || 'User added successfully',
        severity: 'success',
      });
      await fetchUsers();
      onRefetch();
      handleCloseAddDialog();
    } catch (err) {
      if (err.response?.data?.status === "false" && err.response?.data?.message === 'Invalid token') {
        // Optional: Clear invalid token
        localStorage.removeItem('token');
        localStorage.removeItem('email');
        navigate('/login'); // Navigate to /login
      } else {
        setSnackbar({
          open: true,
          message: err.response?.data?.message || 'Failed to add user',
          severity: 'error',
        });
      }
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDeleteDialog = (email) => {
    setDeleteDialog({ open: true, email });
  };

  const handleCloseDeleteDialog = () => {
    setDeleteDialog({ open: false, email: '' });
  };

  const handleOpenAddDialog = () => {
    setAddDialog({ open: true });
    setFormData({ email: '', name: '', password: '', passwordConfirm: '' });
    setFormErrors({});
  };

  const handleCloseAddDialog = () => {
    setAddDialog({ open: false });
  };

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setFormErrors((prev) => ({ ...prev, [name]: '' }));
  };

  const validateForm = () => {
    const errors = {};
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formData.email) errors.email = 'Email is required';
    else if (!emailRegex.test(formData.email)) errors.email = 'Invalid email format';
    if (!formData.name) errors.name = 'Name is required';
    if (!formData.password) errors.password = 'Password is required';
    if (!formData.passwordConfirm) errors.passwordConfirm = 'Password confirmation is required';
    else if (formData.password !== formData.passwordConfirm)
      errors.passwordConfirm = 'Passwords do not match';
    return errors;
  };

  const handleCloseSnackbar = () => {
    setSnackbar((prev) => ({ ...prev, open: false }));
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString('vi-VN', {
      timeZone: 'Asia/Ho_Chi_Minh',
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <Fade in timeout={600}>
        <Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" sx={{ fontWeight: 600, color: 'primary.main' }}>
              Group Users
            </Typography>
            <Button
              variant="contained"
              color="primary"
              startIcon={<AddIcon />}
              onClick={handleOpenAddDialog}
              sx={{
                px: 3,
                py: 1,
                borderRadius: 2,
                fontWeight: 500,
                transition: 'background-color 0.2s',
                '&:hover': { bgcolor: 'primary.dark' },
              }}
            >
              Add User
            </Button>
          </Box>

          {loading ? (
            <Typography variant="body1" align="center" sx={{ py: 4, color: 'text.secondary' }}>
              Loading users...
            </Typography>
          ) : error ? (
            <Typography variant="body1" color="error" align="center" sx={{ py: 4 }}>
              Error: {error}
            </Typography>
          ) : users.length === 0 ? (
            <Card
              sx={{
                borderRadius: 2,
                transition: 'transform 0.2s, box-shadow 0.2s',
                '&:hover': { transform: 'translateY(-4px)' },
                p: 4,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 3,
                minHeight: 200,
                justifyContent: 'center',
              }}
            >
              <PeopleOutlineIcon sx={{ fontSize: 48, color: 'text.secondary' }} />
              <Typography variant="h6" sx={{ color: 'text.secondary', textAlign: 'center' }}>
                No users found in this group
              </Typography>
            </Card>
          ) : (
            <Card
              sx={{
                borderRadius: 2,
                transition: 'transform 0.2s, box-shadow 0.2s',
                '&:hover': { transform: 'translateY(-4px)' },
              }}
            >
              <CardContent>
                <TableContainer>
                  <Table sx={{ minWidth: 650 }} aria-label="users table">
                    <TableHead>
                      <TableRow>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 600 }}>
                            Name
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 600 }}>
                            Email
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 600 }}>
                            Group ID
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 600 }}>
                            Created At
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 600 }}>
                            Actions
                          </Typography>
                        </TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {users.map((user) => (
                        <TableRow
                          key={user.email}
                          sx={{
                            '&:hover': { bgcolor: 'background.default' },
                            transition: 'background-color 0.2s',
                          }}
                        >
                          <TableCell>{user.name}</TableCell>
                          <TableCell>{user.email}</TableCell>
                          <TableCell>{user.groupId}</TableCell>
                          <TableCell>{formatDate(user.createdAt)}</TableCell>
                          <TableCell>
                            <Button
                              variant="outlined"
                              color="error"
                              startIcon={loading ? <CircularProgress size={20} /> : <DeleteIcon />}
                              onClick={() => handleOpenDeleteDialog(user.email)}
                              disabled={loading}
                              sx={{
                                px: 2,
                                py: 0.5,
                                borderRadius: 2,
                                fontWeight: 500,
                                transition: 'all 0.2s',
                                '&:hover': { bgcolor: 'error.light', borderColor: 'error.dark' },
                              }}
                            >
                              Delete
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </CardContent>
            </Card>
          )}
        </Box>
      </Fade>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialog.open}
        onClose={handleCloseDeleteDialog}
        aria-labelledby="delete-dialog-title"
      >
        <DialogTitle id="delete-dialog-title">Confirm Delete</DialogTitle>
        <DialogContent>
          <Typography variant="body1">
            Are you sure you want to delete the user <strong>{deleteDialog.email}</strong>?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDeleteDialog} color="secondary">
            Cancel
          </Button>
          <Button
            onClick={() => handleDelete(deleteDialog.email)}
            color="error"
            variant="contained"
            autoFocus
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>

      {/* Add User Dialog */}
      <Dialog
        open={addDialog.open}
        onClose={handleCloseAddDialog}
        aria-labelledby="add-dialog-title"
      >
        <DialogTitle id="add-dialog-title">Add New User</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <TextField
              label="Name"
              name="name"
              value={formData.name}
              onChange={handleFormChange}
              error={!!formErrors.name}
              helperText={formErrors.name}
              fullWidth
              variant="outlined"
            />
            <TextField
              label="Email"
              name="email"
              type="email"
              value={formData.email}
              onChange={handleFormChange}
              error={!!formErrors.email}
              helperText={formErrors.email}
              fullWidth
              variant="outlined"
            />
            <TextField
              label="Password"
              name="password"
              type="password"
              value={formData.password}
              onChange={handleFormChange}
              error={!!formErrors.password}
              helperText={formErrors.password}
              fullWidth
              variant="outlined"
            />
            <TextField
              label="Confirm Password"
              name="passwordConfirm"
              type="password"
              value={formData.passwordConfirm}
              onChange={handleFormChange}
              error={!!formErrors.passwordConfirm}
              helperText={formErrors.passwordConfirm}
              fullWidth
              variant="outlined"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseAddDialog} color="secondary">
            Cancel
          </Button>
          <Button
            onClick={handleAddUser}
            color="primary"
            variant="contained"
            disabled={loading}
            startIcon={loading ? <CircularProgress size={20} /> : null}
          >
            Add
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert
          onClose={handleCloseSnackbar}
          severity={snackbar.severity}
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}

export default UserList;