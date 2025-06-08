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

function GroupList({ groups, loading, error }) {
  const [buttonStates, setButtonStates] = useState({});

  if (loading) return <Typography variant="body1" align="center" sx={{ py: 2 }}>Loading groups...</Typography>;
  if (error) return <Typography variant="body1" color="error" align="center" sx={{ py: 2 }}>Error: {error}</Typography>;
  if (!Array.isArray(groups) || groups.length === 0) return <Typography variant="body1" align="center" sx={{ py: 2 }}>No groups found.</Typography>;

  const handleSendGroupId = async (email, id, index) => {
    setButtonStates((prev) => ({ ...prev, [index]: { loading: true, error: null } }));
    try {
      const token = localStorage.getItem('token');
      await axios.post(`https://thientranduc.id.vn:444/api/send-group-id`, {
        email,
        groupId: id,
      }, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setButtonStates((prev) => ({ ...prev, [index]: { loading: false, error: null } }));
    } catch (err) {
      const errorMsg = err.response?.data?.message || 'Failed to send group ID';
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
              <TableCell>
                <Button
                  variant="contained"
                  color="primary"
                  size="small"
                  onClick={() => handleSendGroupId(group.ownerEmail, group.id, index)}
                  disabled={buttonStates[index]?.loading}
                  startIcon={buttonStates[index]?.loading && <CircularProgress size={16} color="inherit" />}
                >
                  Mail Group ID to Owner
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

export default GroupList;