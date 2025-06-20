import { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  CircularProgress,
  Card,
  CardContent,
  Chip,
  Fade,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
  Select,
  MenuItem,
  TextField,
  TablePagination,
  Snackbar,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import { Refresh as RefreshIcon, Lock as LockIcon, LockOpen as LockOpenIcon } from '@mui/icons-material';
import { useTheme } from '@mui/material';
import axios from 'axios';

function DoorHistory({ email, onRefetch }) {
  const [door, setDoor] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({
    state: '',
    startDate: '',
    endDate: '',
  });
  const [pagination, setPagination] = useState({
    page: 0, // 0-based for MUI
    rowsPerPage: 10,
  });
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [toggleDialog, setToggleDialog] = useState({ open: false });
  const [password, setPassword] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [toggleLoading, setToggleLoading] = useState(false);
  const theme = useTheme();

  const fetchDoorStatus = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`https://thientranduc.id.vn:444/api/door`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setDoor(response.data.door || null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch door status');
    }
  };

  const fetchDoorHistory = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const query = {
        state: filters.state || undefined,
        startDate: filters.startDate || undefined,
        endDate: filters.endDate || undefined,
        limit: pagination.rowsPerPage,
        page: pagination.page + 1, // Convert to 1-based for API
      };
      const response = await axios.get(`https://thientranduc.id.vn:444/api/door-history`, {
        headers: { Authorization: `Bearer ${token}` },
        params: query,
      });
      if (response.data.status === 'success') {
        setHistory(response.data.history || []);
        setError(null);
      } else {
        throw new Error(response.data.message || 'Failed to fetch door history');
      }
    } catch (err) {
      setError(err.message);
      setSnackbar({ open: true, message: err.message, severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDoorStatus();
    fetchDoorHistory();
  }, [pagination.page, pagination.rowsPerPage]);

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters((prev) => ({ ...prev, [name]: value }));
  };

  const handleApplyFilters = () => {
    setPagination((prev) => ({ ...prev, page: 0 }));
    fetchDoorHistory();
  };

  const handleRefresh = () => {
    setFilters({ state: '', startDate: '', endDate: '' });
    setPagination({ page: 0, rowsPerPage: 10 });
    fetchDoorStatus();
    fetchDoorHistory();
    onRefetch();
  };

  const handleChangePage = (event, newPage) => {
    setPagination((prev) => ({ ...prev, page: newPage }));
  };

  const handleChangeRowsPerPage = (event) => {
    setPagination({ page: 0, rowsPerPage: parseInt(event.target.value, 10) });
  };

  const handleCloseSnackbar = () => {
    setSnackbar((prev) => ({ ...prev, open: false }));
  };

  const handleOpenToggleDialog = () => {
    setToggleDialog({ open: true });
    setPassword('');
    setPasswordError('');
  };

  const handleCloseToggleDialog = () => {
    setToggleDialog({ open: false });
    setPassword('');
    setPasswordError('');
  };

  const handlePasswordChange = (e) => {
    setPassword(e.target.value);
    setPasswordError('');
  };

  const handleToggleDoor = async () => {
    if (!password) {
      setPasswordError('Password is required');
      return;
    }

    setToggleLoading(true);
    try {
      const token = localStorage.getItem('token');
      const newState = door.lock === 'Locked' ? 'Unlocked' : 'Locked';
      let url = "";
      if (newState === 'Locked') {
        url = "https://thientranduc.id.vn:444/api/lock";
      } 
      else {
        url = "https://thientranduc.id.vn:444/api/unlock";
      }
      const response = await axios.post(
        url,
        { password, email },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setSnackbar({
        open: true,
        message: response.data.message || `Door ${newState.toLowerCase()} successfully`,
        severity: 'success',
      });
      await fetchDoorStatus();
      await fetchDoorHistory();
      onRefetch();
      handleCloseToggleDialog();
    } catch (err) {
      setSnackbar({
        open: true,
        message: err.response?.data?.message || 'Failed to toggle door state',
        severity: 'error',
      });
    } finally {
      setToggleLoading(false);
    }
  };

  const getStateStyles = (state) => {
    const stateColors = {
      Locked: theme.palette.status.disconnected || '#d32f2f',
      Unlocked: theme.palette.status.connected || '#2e7d32',
    };
    return {
      bgcolor: stateColors[state] || theme.palette.status.inactive,
      color: '#fff',
    };
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString('vi-VN', {
      timeZone: 'Asia/Ho_Chi_Minh',
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  };

  if (error && !door && !history.length) {
    return (
      <Fade in timeout={500}>
        <Typography variant="body1" color="error" align="center" sx={{ py: 4 }}>
          Error: {error}
        </Typography>
      </Fade>
    );
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      {/* Current Door Status */}
      <Fade in timeout={600}>
        <Card
          sx={{
            borderRadius: 2,
            transition: 'transform 0.2s, box-shadow 0.2s',
            '&:hover': { transform: 'translateY(-4px)' },
          }}
        >
          <CardContent>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 2, color: 'primary.main' }}>
              Current Door Status
            </Typography>
            {door ? (
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                <Chip
                  label={door.lock}
                  sx={{
                    ...getStateStyles(door.lock),
                    px: 2,
                    fontWeight: 500,
                    width: 'fit-content',
                  }}
                />
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  <strong>Last Action By:</strong> {door.user || 'Unknown'}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  <strong>Time:</strong> {formatDate(door.time)}
                </Typography>
                <Button
                  variant="contained"
                  color="primary"
                  startIcon={door.lock === 'Locked' ? <LockOpenIcon /> : <LockIcon />}
                  onClick={handleOpenToggleDialog}
                  disabled={toggleLoading || !door}
                  sx={{
                    mt: 2,
                    px: 3,
                    py: 1,
                    borderRadius: 2,
                    fontWeight: 500,
                    transition: 'background-color 0.2s',
                    '&:hover': { bgcolor: 'primary.dark' },
                  }}
                >
                  {door.lock === 'Locked' ? 'Unlock Door' : 'Lock Door'}
                </Button>
              </Box>
            ) : (
              <Typography variant="body1" color="text.secondary">
                Loading status...
              </Typography>
            )}
          </CardContent>
        </Card>
      </Fade>

      {/* Filters and History */}
      <Fade in timeout={700}>
        <Card
          sx={{
            borderRadius: 2,
            transition: 'transform 0.2s, box-shadow 0.2s',
            '&:hover': { transform: 'translateY(-4px)' },
          }}
        >
          <CardContent>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
              <Typography variant="h6" sx={{ fontWeight: 600, color: 'primary.main' }}>
                Door History
              </Typography>
              <Button
                variant="outlined"
                color="secondary"
                startIcon={<RefreshIcon />}
                onClick={handleRefresh}
                sx={{
                  px: 3,
                  py: 1,
                  borderRadius: 2,
                  fontWeight: 500,
                  transition: 'all 0.2s',
                  '&:hover': { borderColor: 'secondary.dark', color: 'secondary.dark' },
                }}
              >
                Refresh
              </Button>
            </Box>

            {/* Filters */}
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 3 }}>
              <Select
                name="state"
                value={filters.state}
                onChange={handleFilterChange}
                displayEmpty
                sx={{ minWidth: 120 }}
                variant="outlined"
              >
                <MenuItem value="">All States</MenuItem>
                <MenuItem value="Locked">Locked</MenuItem>
                <MenuItem value="Unlocked">Unlocked</MenuItem>
              </Select>
              <TextField
                name="startDate"
                label="Start Date"
                type="date"
                value={filters.startDate}
                onChange={handleFilterChange}
                InputLabelProps={{ shrink: true }}
                variant="outlined"
                sx={{ maxWidth: 200 }}
              />
              <TextField
                name="endDate"
                label="End Date"
                type="date"
                value={filters.endDate}
                onChange={handleFilterChange}
                InputLabelProps={{ shrink: true }}
                variant="outlined"
                sx={{ maxWidth: 200 }}
              />
              <Button
                variant="contained"
                color="primary"
                onClick={handleApplyFilters}
                sx={{
                  px: 3,
                  py: 1,
                  borderRadius: 2,
                  fontWeight: 500,
                  transition: 'background-color 0.2s',
                  '&:hover': { bgcolor: 'primary.dark' },
                }}
              >
                Filter
              </Button>
            </Box>

            {/* History Table */}
            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                <CircularProgress />
              </Box>
            ) : history.length === 0 ? (
              <Typography variant="body1" align="center" sx={{ py: 4, color: 'text.secondary' }}>
                No history found.
              </Typography>
            ) : (
              <>
                <TableContainer>
                  <Table sx={{ minWidth: 500 }} aria-label="door history table">
                    <TableHead>
                      <TableRow>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 600 }}>
                            State
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 600 }}>
                            User
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 600 }}>
                            Time
                          </Typography>
                        </TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {history.map((entry, index) => (
                        <TableRow
                          key={`${entry.time}-${index}`}
                          sx={{
                            '&:hover': { bgcolor: 'background.default' },
                            transition: 'background-color 0.2s',
                          }}
                        >
                          <TableCell>
                            <Chip
                              label={entry.state}
                              sx={{
                                ...getStateStyles(entry.state),
                                px: 1,
                              }}
                            />
                          </TableCell>
                          <TableCell>{entry.user || 'Unknown'}</TableCell>
                          <TableCell>{formatDate(entry.time)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
                <TablePagination
                  rowsPerPageOptions={[10, 25, 50]}
                  component="div"
                  count={-1} // Unknown total count
                  rowsPerPage={pagination.rowsPerPage}
                  page={pagination.page}
                  onPageChange={handleChangePage}
                  onRowsPerPageChange={handleChangeRowsPerPage}
                  labelDisplayedRows={({ from, to, page }) => `Page ${page + 1}`}
                />
              </>
            )}
          </CardContent>
        </Card>
      </Fade>

      {/* Toggle Door Dialog */}
      <Dialog
        open={toggleDialog.open}
        onClose={handleCloseToggleDialog}
        aria-labelledby="toggle-dialog-title"
      >
        <DialogTitle id="toggle-dialog-title">
          {door?.lock === 'Locked' ? 'Unlock Door' : 'Lock Door'}
        </DialogTitle>
        <DialogContent>
          <TextField
            label="Password"
            type="password"
            value={password}
            onChange={handlePasswordChange}
            error={!!passwordError}
            helperText={passwordError}
            fullWidth
            variant="outlined"
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button
            onClick={handleCloseToggleDialog}
            color="secondary"
            variant="outlined"
            sx={{ px: 3, borderRadius: 2 }}
          >
            Cancel
          </Button>
          <Button
            onClick={handleToggleDoor}
            color="primary"
            variant="contained"
            disabled={toggleLoading}
            startIcon={toggleLoading ? <CircularProgress size={20} /> : null}
            sx={{ px: 3, borderRadius: 2 }}
          >
            Submit
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

export default DoorHistory;