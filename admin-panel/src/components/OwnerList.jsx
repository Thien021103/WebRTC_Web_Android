import { useState } from 'react';
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
} from '@mui/material';
import axios from 'axios';

function OwnerList({ owners, loading, error }) {
  const [buttonStates, setButtonStates] = useState({});

  if (loading) return <Typography variant="body1" align="center" sx={{ py: 2 }}>Loading owners...</Typography>;
  if (error) return <Typography variant="body1" color="error" align="center" sx={{ py: 2 }}>Error: {error}</Typography>;
  if (!Array.isArray(owners) || owners.length === 0) return <Typography variant="body1" align="center" sx={{ py: 2 }}>No owners found.</Typography>;

  const formatFcmToken = (token) => {
    if (!token) return '-';
    const chunkSize = 60;
    return token.match(new RegExp(`.{1,${chunkSize}}`, 'g')).join('\n');
  };

  const handleDeleteOwner = async (email, groupId, index) => {
    setButtonStates((prev) => ({ ...prev, [index]: { loading: true, error: null } }));
    try {
      const token = localStorage.getItem('token');
      await axios.delete(`https://thientranduc.id.vn:444/api/delete-owner`, {
        email,
        groupId,
      }, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setButtonStates((prev) => ({ ...prev, [index]: { loading: false, error: null } }));
    } catch (err) {
      const errorMsg = err.response?.data?.message || 'Failed to delete owner';
      setButtonStates((prev) => ({ ...prev, [index]: { loading: false, error: errorMsg } }));
      setTimeout(() => {
        setButtonStates((prev) => ({ ...prev, [index]: { loading: false, error: null } }));
      }, 3000);
    }
  };

  return (
    <TableContainer component={Paper} sx={{ maxWidth: '100%', mt: 2, boxShadow: 3 }}>
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
                  onClick={() => handleDeleteOwner(owner.email, owner.groupId, index)}
                  disabled={buttonStates[index]?.loading}
                  startIcon={buttonStates[index]?.loading && <CircularProgress size={16} color="inherit" />}
                >
                  Delete
                </Button>
                {buttonStates[index]?.error && (
                  <Typography variant="caption" color="error" sx={{ mt: 1, display: 'block' }}>
                    {buttonStates[index].error}
                  </Typography>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

export default OwnerList;