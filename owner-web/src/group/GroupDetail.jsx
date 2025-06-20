import { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Button,
  CircularProgress,
  Card,
  CardContent,
  Chip,
  useTheme,
} from '@mui/material';
import { Fade } from '@mui/material';
import axios from 'axios';

function GroupDetails({ onRefetch }) {
  const [group, setGroup] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const theme = useTheme();

  const fetchGroup = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`https://thientranduc.id.vn:444/api/group`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setGroup(response.data.group || null);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGroup();
  }, []);

  const getStateStyles = (state) => {
    const stateColors = {
      Active: theme.palette.status.active,
      Ready: theme.palette.status.ready,
      Creating: theme.palette.status.creating,
      Impossible: theme.palette.status.impossible,
      Error: theme.palette.status.error,
    };
    return {
      bgcolor: stateColors[state] || theme.palette.status.inactive,
      color: ['Maintenance'].includes(state) ? '#000' : '#fff',
    };
  };

  const getDeviceStatusStyles = (status) => {
    return {
      bgcolor:
        status === 'Connected'
          ? theme.palette.status.connected
          : theme.palette.status.disconnected,
      color: '#fff',
    };
  };

  if (loading) {
    return (
      <Fade in timeout={500}>
        <Typography variant="body1" align="center" sx={{ py: 4, color: 'text.secondary' }}>
          Loading group details...
        </Typography>
      </Fade>
    );
  }

  if (error) {
    return (
      <Fade in timeout={500}>
        <Typography variant="body1" color="error" align="center" sx={{ py: 4 }}>
          Error: {error}
        </Typography>
      </Fade>
    );
  }

  if (!group) {
    return (
      <Fade in timeout={500}>
        <Typography variant="body1" align="center" sx={{ py: 4, color: 'text.secondary' }}>
          No group assigned.
        </Typography>
      </Fade>
    );
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
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
              Group Information
            </Typography>
            <Typography variant="body1" sx={{ mb: 1 }}>
              <strong>Name:</strong> {group.name}
            </Typography>
            <Typography variant="body1" sx={{ mb: 1 }}>
              <strong>Owner:</strong> {group.owner}
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography variant="body1" sx={{ fontWeight: 500 }}>
                <strong>State:</strong>
              </Typography>
              <Chip
                label={group.state}
                sx={{
                  ...getStateStyles(group.state),
                  px: 1,
                }}
              />
            </Box>
          </CardContent>
        </Card>
      </Fade>

      <Fade in timeout={700}>
        <Card
          sx={{
            borderRadius: 2,
            transition: 'transform 0.2s',
            '&:hover': { transform: 'translateY(-4px)' },
          }}
        >
          <CardContent>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 2, color: 'primary.main' }}>
              Device Status
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Typography variant="body1" sx={{ fontWeight: 500 }}>
                  <strong>Camera:</strong>
                </Typography>
                <Chip
                  label={group.camera}
                  sx={{
                    ...getDeviceStatusStyles(group.camera),
                    px: 1,
                  }}
                />
              </Box>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Typography variant="body1" sx={{ fontWeight: 500 }}>
                  <strong>Controller:</strong>
                </Typography>
                <Chip
                  label={group.controller}
                  sx={{
                    ...getDeviceStatusStyles(group.controller),
                    px: 1,
                  }}
                />
              </Box>
            </Box>
          </CardContent>
        </Card>
      </Fade>

      <Fade in timeout={800}>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
          <Button
            variant="outlined"
            color="secondary"
            onClick={() => {
              onRefetch();
              fetchGroup();
            }}
            sx={{
              px: 3,
              py: 1,
              borderRadius: 2,
              fontWeight: 500,
              transition: 'border-color 0.2s, color 0.2s',
              '&:hover': { borderColor: 'secondary.dark', color: 'secondary.dark' },
            }}
          >
            Refresh
          </Button>
        </Box>
      </Fade>

      {actionError && (
        <Fade in timeout={500}>
          <Typography variant="caption" color="error" sx={{ mt: 2, display: 'block' }}>
            {actionError}
          </Typography>
        </Fade>
      )}
    </Box>
  );
}

export default GroupDetails;