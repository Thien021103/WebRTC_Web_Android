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
  TextField,
  TablePagination,
  Snackbar,
  Alert,
} from '@mui/material';
import { Refresh as RefreshIcon, Notifications as NotificationsIcon } from '@mui/icons-material';
import { useTheme } from '@mui/material';
import axios from 'axios';

function Notifications({ onRefetch }) {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({
    startDate: '',
    endDate: '',
  });
  const [pagination, setPagination] = useState({
    page: 0, // 0-based for MUI
    rowsPerPage: 10,
  });
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const theme = useTheme();

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const query = {
        startDate: filters.startDate || undefined,
        endDate: filters.endDate || undefined,
        limit: pagination.rowsPerPage,
        page: pagination.page + 1, // Convert to 1-based for API
      };
      const response = await axios.get(`https://thientranduc.id.vn:444/api/notifications`, {
        headers: { Authorization: `Bearer ${token}` },
        params: query,
      });
      if (response.data.status === 'success') {
        setNotifications(response.data.notifications || []);
        setError(null);
      } else {
        throw new Error(response.data.message || 'Failed to fetch notifications');
      }
    } catch (err) {
      const errorMessage = err.message || 'Failed to fetch notifications';
      setError(errorMessage);
      setSnackbar({ open: true, message: errorMessage, severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, [pagination.page, pagination.rowsPerPage]);

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters((prev) => ({ ...prev, [name]: value }));
  };

  const handleApplyFilters = () => {
    setPagination((prev) => ({ ...prev, page: 0 }));
    fetchNotifications();
  };

  const handleRefresh = () => {
    setFilters({ startDate: '', endDate: '' });
    setPagination({ page: 0, rowsPerPage: 10 });
    fetchNotifications();
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

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString('vi-VN', {
      timeZone: 'Asia/Ho_Chi_Minh',
      dateStyle: 'medium',
    });
  };

  const formatTime = (dateString) => {
    return new Date(dateString).toLocaleString('vi-VN', {
      timeZone: 'Asia/Ho_Chi_Minh',
      timeStyle: 'short',
    });
  };

  if (error && !notifications.length) {
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
                Notifications History
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

            {/* Notifications Table */}
            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                <CircularProgress />
              </Box>
            ) : notifications.length === 0 ? (
              <Card
                sx={{
                  borderRadius: 2,
                  p: 4,
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: 3,
                  minHeight: 200,
                  justifyContent: 'center',
                }}
              >
                <NotificationsIcon sx={{ fontSize: 48, color: 'text.secondary' }} />
                <Typography variant="h6" sx={{ color: 'text.secondary', textAlign: 'center' }}>
                  No notifications found
                </Typography>
              </Card>
            ) : (
              <>
                <TableContainer>
                  <Table sx={{ minWidth: 500 }} aria-label="notifications table">
                    <TableHead>
                      <TableRow>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 600 }}>
                            Date
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
                      {notifications.map((notification, index) => (
                        <TableRow
                          key={`${notification.time}-${index}`}
                          sx={{
                            '&:hover': { bgcolor: 'background.default' },
                            transition: 'background-color 0.2s',
                          }}
                        >
                          <TableCell>{formatDate(notification.time)}</TableCell>
                          <TableCell>{formatTime(notification.time)}</TableCell>
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

export default Notifications;