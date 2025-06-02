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
} from '@mui/material';
import axios from 'axios';

function GroupList({ groups, loading, error }) {
  if (loading) return <Typography variant="body1" align="center" sx={{ py: 2 }}>Loading groups...</Typography>;
  if (error) return <Typography variant="body1" color="error" align="center" sx={{ py: 2 }}>Error: {error}</Typography>;
  if (!Array.isArray(groups) || groups.length === 0) return <Typography variant="body1" align="center" sx={{ py: 2 }}>No groups found.</Typography>;

  const handleSendGroupId = async (email, groupId) => {
    try {
      const token = localStorage.getItem('token');
      await axios.post(`https://thientranduc.id.vn:444/api/send-group-id`, {
        email,
        groupId,
      }, {
        headers: { Authorization: `Bearer ${token}` },
      });
    } catch (err) {
      console.error('Failed to send group ID:', err.response?.data?.message || err.message);
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
                  onClick={() => handleSendGroupId(group.ownerEmail, group.id)}
                >
                  Mail Group ID to Owner
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

export default GroupList;