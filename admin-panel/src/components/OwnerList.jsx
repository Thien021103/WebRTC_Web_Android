import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Typography,
} from '@mui/material';

function OwnerList({ owners, loading, error }) {
  if (loading) return <Typography variant="body1" align="center" sx={{ py: 2 }}>Loading owners...</Typography>;
  if (error) return <Typography variant="body1" color="error" align="center" sx={{ py: 2 }}>Error: {error}</Typography>;
  if (!Array.isArray(owners) || owners.length === 0) return <Typography variant="body1" align="center" sx={{ py: 2 }}>No owners found.</Typography>;

  return (
    <TableContainer component={Paper} sx={{ maxWidth: '100%', mt: 2, boxShadow: 3 }}>
      <Table stickyHeader>
        <TableHead>
          <TableRow>
            <TableCell sx={{ fontWeight: 600, bgcolor: '#f5f7fa' }}>Email</TableCell>
            <TableCell sx={{ fontWeight: 600, bgcolor: '#f5f7fa' }}>Created At</TableCell>
            <TableCell sx={{ fontWeight: 600, bgcolor: '#f5f7fa' }}>FCM Token</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {owners.map((owner, index) => (
            <TableRow key={owner.email} hover sx={{ bgcolor: index % 2 ? '#fafafa' : '#ffffff' }}>
              <TableCell>{owner.email}</TableCell>
              <TableCell>{new Date(owner.createdAt).toLocaleString()}</TableCell>
              <TableCell>{owner.fcmToken}</TableCell>

            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

export default OwnerList;