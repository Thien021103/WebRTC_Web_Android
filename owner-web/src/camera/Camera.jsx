import { useState, useEffect, useRef } from 'react';
import {
  Box,
  Typography,
  CircularProgress,
  Card,
  CardContent,
  Fade,
  Button,
  Snackbar,
  Alert,
} from '@mui/material';
import { Videocam as VideocamIcon, Link as ConnectIcon, Close as CloseIcon } from '@mui/icons-material';
import { useTheme } from '@mui/material';
import { useNavigate } from 'react-router-dom';

function Camera() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const theme = useTheme();
  const navigate = useNavigate();
  const wsRef = useRef(null);
  const pcRef = useRef(null);
  const videoRef = useRef(null);

  const handleCloseSnackbar = () => {
    setSnackbar((prev) => ({ ...prev, open: false }));
  };

  const handleConnect = () => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) return;
    setLoading(true);
    setError(null);

    wsRef.current = new WebSocket('wss://thientranduc.id.vn:444');

    wsRef.current.onopen = () => {
      const token = localStorage.getItem('token');
      if (!token) {
        setError('No session found. Please log in.');
        setSnackbar({ open: true, message: 'Please log in.', severity: 'error' });
        navigate('/login');
        wsRef.current.close();
        setLoading(false);
        return;
      }
      wsRef.current.send(`CONNECT user ${token}`);
      setSnackbar({ open: true, message: 'Connecting to camera server', severity: 'info' });
    };

    wsRef.current.onmessage = async (event) => {
      const message = event.data;
      if (message === 'PING') {
        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
          wsRef.current.send('PONG');
        }
      } else if (message.startsWith('STATE ')) {
        const state = message.substring(6).trim();
        handleStateChange(state);
      } else if (message.startsWith('OFFER\n')) {
        try {
          const sdp = message.substring(6);
          await handleOfferSDP(sdp);
        } catch (err) {
          setError('Failed to process video stream');
          setSnackbar({ open: true, message: 'Failed to process video stream', severity: 'error' });
          handleDisconnect();
        }
      } else {
        try {
          const data = JSON.parse(message);
          if (data.status === 'false' && data.message === 'Invalid token') {
            localStorage.removeItem('token');
            navigate('/login');
            setSnackbar({ open: true, message: 'Invalid session. Please log in.', severity: 'error' });
            handleDisconnect();
          } else {
            setError(data.message || 'Unknown server error');
            setSnackbar({ open: true, message: data.message || 'Unknown server error', severity: 'error' });
            handleStateChange('Error');
          }
        } catch (err) {
          setError('Invalid server response');
          setSnackbar({ open: true, message: 'Invalid server response', severity: 'error' });
          handleStateChange('Error');
        }
      }
    };

    wsRef.current.onclose = () => {
      handleDisconnect();
      setSnackbar({ open: true, message: 'Disconnected from camera server', severity: 'info' });
    };

    wsRef.current.onerror = () => {
      setError('Failed to connect to camera server');
      setSnackbar({ open: true, message: 'Failed to connect to camera server', severity: 'error' });
      handleDisconnect();
    };
  };

  const handleStateChange = (state) => {
    switch (state) {
      case 'Impossible':
        setLoading(false);
        setError('No camera available');
        setSnackbar({ open: true, message: 'No camera available', severity: 'warning' });
        break;
      case 'Ready':
        setLoading(false);
        setError(null);
        setSnackbar({ open: true, message: 'Camera ready, awaiting video stream', severity: 'info' });
        break;
      case 'Creating':
        setLoading(true);
        setError(null);
        setSnackbar({ open: true, message: 'Preparing video stream', severity: 'info' });
        break;
      case 'Active':
        setLoading(false);
        setError(null);
        setSnackbar({ open: true, message: 'Camera stream active', severity: 'success' });
        break;
      case 'Error':
        setLoading(false);
        setError('Camera server error');
        setSnackbar({ open: true, message: 'Camera server error', severity: 'error' });
        handleDisconnect();
        break;
      default:
        setError('Unknown camera state');
        setSnackbar({ open: true, message: 'Unknown camera state', severity: 'error' });
    }
  };

  const handleOfferSDP = async (sdp) => {
    pcRef.current = new RTCPeerConnection({
      iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
    });

    pcRef.current.ontrack = (event) => {
      if (videoRef.current && event.streams[0]) {
        videoRef.current.srcObject = event.streams[0];
        videoRef.current.play().catch((err) => {
          setError('Failed to play video stream');
          setSnackbar({ open: true, message: 'Failed to play video stream', severity: 'error' });
        });
      }
    };

    await pcRef.current.setRemoteDescription(new RTCSessionDescription({ type: 'offer', sdp }));
    const answer = await pcRef.current.createAnswer();
    await pcRef.current.setLocalDescription(answer);

    const token = localStorage.getItem('token');
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(`ANSWER user ${token}\n${answer.sdp}`);
    }

    setLoading(false);
  };

  const handleDisconnect = () => {
    if (pcRef.current) {
      pcRef.current.close();
      pcRef.current = null;
    }
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
    setLoading(false);
    setError(null);
  };

  useEffect(() => {
    return () => {
      handleDisconnect();
    };
  }, []);

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, minWidth: 1000, }}>
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
              <Box sx={{ display: 'flex', gap: 2 }}>
                <Button
                  variant="contained"
                  color="primary"
                  startIcon={loading ? <CircularProgress size={20} /> : <ConnectIcon />}
                  onClick={handleConnect}
                  disabled={loading || (wsRef.current && wsRef.current.readyState === WebSocket.OPEN)}
                  sx={{
                    px: 3,
                    py: 1,
                    borderRadius: 2,
                    fontWeight: 500,
                    transition: 'background-color 0.2s',
                    '&:hover': { bgcolor: 'primary.dark' },
                  }}
                >
                  Connect Camera
                </Button>
                <Button
                  variant="outlined"
                  color="error"
                  startIcon={<CloseIcon />}
                  onClick={handleDisconnect}
                  disabled={!(wsRef.current && wsRef.current.readyState === WebSocket.OPEN)}
                  sx={{
                    px: 3,
                    py: 1,
                    borderRadius: 2,
                    fontWeight: 500,
                    transition: 'all 0.2s',
                    '&:hover': { borderColor: 'error.dark', color: 'error.dark' },
                  }}
                >
                  Close Connection
                </Button>
              </Box>
            </Box>

            <Box sx={{ mb: 3, maxWidth: '100%', aspectRatio: '16/9' }}>
              {loading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
                  <CircularProgress />
                </Box>
              ) : error ? (
                <Typography variant="body1" color="error" align="center" sx={{ py: 4 }}>
                  Error: {error}
                </Typography>
              ) : videoRef.current?.srcObject ? (
                <video
                  ref={videoRef}
                  autoPlay
                  playsInline
                  muted
                  style={{ width: '100%', borderRadius: 8 }}
                ></video>
              ) : (
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
                  <VideocamIcon sx={{ fontSize: 48, color: 'text.secondary' }} />
                  <Typography variant="h6" sx={{ color: 'text.secondary', textAlign: 'center' }}>
                    No camera stream
                  </Typography>
                </Card>
              )}
            </Box>
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

export default Camera;