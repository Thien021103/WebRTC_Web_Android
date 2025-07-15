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
import { Videocam as VideocamIcon, Link as ConnectIcon, Close as CloseIcon, VolumeUp, VolumeOff, Mic, MicOff } from '@mui/icons-material';
import { useTheme } from '@mui/material';
import { useNavigate } from 'react-router-dom';

function Camera() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [hasStream, setHasStream] = useState(false);
  const [isMuted, setIsMuted] = useState(false); // Playback audio
  const [isMicEnabled, setIsMicEnabled] = useState(true); // Microphone track
  const theme = useTheme();
  const navigate = useNavigate();
  const wsRef = useRef(null);
  const pcRef = useRef(null);
  const videoRef = useRef(null);
  const localStreamRef = useRef(null);

  const handleCloseSnackbar = () => {
    setSnackbar((prev) => ({ ...prev, open: false }));
  };

  const toggleMute = () => {
    if (videoRef.current) {
      videoRef.current.muted = !isMuted;
      setIsMuted(!isMuted);
    }
  };

  const toggleMic = () => {
    if (localStreamRef.current) {
      const audioTrack = localStreamRef.current.getAudioTracks()[0];
      if (audioTrack) {
        audioTrack.enabled = !isMicEnabled;
        setIsMicEnabled(!isMicEnabled);
      }
    }
  };

  const handleConnect = () => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) return;
    setLoading(true);
    setError(null);
    setHasStream(false);
    setIsMuted(false);
    setIsMicEnabled(true);

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
      console.log('WebSocket message:', message);
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
          await handleOfferSDP(sdp + '\n');
        } catch (err) {
          console.error('Error processing SDP:', err);
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
        setError('Awaiting camera access');
        setSnackbar({ open: true, message: 'Awaiting camera access', severity: 'warning' });
        break;
      case 'Ready':
        setLoading(true);
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
        setHasStream(true);
        setSnackbar({ open: true, message: 'Camera stream active', severity: 'success' });
        break;
      case 'Error':
        setLoading(false);
        setError('Camera server error');
        setHasStream(false);
        setSnackbar({ open: true, message: 'Camera server error', severity: 'error' });
        handleDisconnect();
        break;
      default:
        setError('Unknown camera state');
        setHasStream(false);
        setSnackbar({ open: true, message: 'Unknown camera state', severity: 'error' });
    }
  };

  async function waitGatheringComplete() {
    return new Promise((resolve) => {
      if (pcRef.current.iceGatheringState === 'complete') {
        resolve();
      } else {
        pcRef.current.addEventListener('icegatheringstatechange', () => {
          if (pcRef.current.iceGatheringState === 'complete') {
            resolve();
          }
        });
      }
    });
  }

  const handleOfferSDP = async (sdp) => {
    pcRef.current = new RTCPeerConnection({
      iceServers: [
        // { urls: 'stun:stun.l.google.com:19302' },
        {
          urls: "turn:103.149.28.136:3478",
          username: "camera1",
          credential: "password1",
        }
      ],
    });

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      console.log('Local microphone stream:', stream, 'audio tracks:', stream.getAudioTracks());
      localStreamRef.current = stream;
      stream.getAudioTracks().forEach((track) => {
        console.log('Adding audio track to RTCPeerConnection:', track);
        pcRef.current.addTrack(track, stream);
      });
    } catch (err) {
      console.error('Failed to get microphone stream:', err);
      setError('Microphone access denied');
      setSnackbar({ open: true, message: 'Microphone access denied', severity: 'error' });
      setIsMicEnabled(false);
    }

    pcRef.current.ontrack = (event) => {
      console.log('ontrack event, streams:', event.streams, 'audio tracks:', event.streams[0].getAudioTracks());
      if (videoRef.current && event.streams[0]) {
        console.log('Assigning stream to videoRef:', videoRef.current);
        videoRef.current.srcObject = event.streams[0];
        videoRef.current.play().catch((err) => {
          console.error('Failed to play video:', err);
          setError('Failed to play video stream');
        });
      } else {
        console.warn('videoRef.current or streams missing:', {
          videoRef: videoRef.current,
          streams: event.streams[0],
        });
      }
    };

    await pcRef.current.setRemoteDescription(new RTCSessionDescription({ type: 'offer', sdp }));
    await pcRef.current.setLocalDescription(await pcRef.current.createAnswer());
    await waitGatheringComplete();
    const answer = pcRef.current.localDescription;

    const token = localStorage.getItem('token');
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(`ANSWER user ${token}\n${answer.sdp}`);
    } else {
      console.warn('WebSocket not open, cannot send ANSWER');
    }

    setLoading(false);
  };

  const handleDisconnect = () => {
    console.log('Handling disconnection, videoRef:', videoRef.current);
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
    if (localStreamRef.current) {
      localStreamRef.current.getTracks().forEach((track) => track.stop());
      localStreamRef.current = null;
    }
    setHasStream(false);
    setIsMuted(false);
    setIsMicEnabled(true);
    setLoading(false);
    setError(null);
  };

  useEffect(() => {
    console.log('Component mounted, videoRef:', videoRef.current);
    return () => {
      console.log('Component unmounting');
      handleDisconnect();
    };
  }, []);

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, width: '100%', maxWidth: '90vw', minWidth: 1000, ml: 2 }}>
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
              <Typography variant="h5" sx={{ fontWeight: 600, color: 'primary.main' }}>
                Live Camera
              </Typography>
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
                {hasStream && (
                  <Button
                    variant="outlined"
                    color="secondary"
                    startIcon={isMuted ? <VolumeOff /> : <VolumeUp />}
                    onClick={toggleMute}
                    sx={{
                      px: 3,
                      py: 1,
                      borderRadius: 2,
                      fontWeight: 500,
                      transition: 'all 0.2s',
                      '&:hover': { borderColor: 'secondary.dark', color: 'secondary.dark' },
                    }}
                  >
                    {isMuted ? 'Unmute' : 'Mute'}
                  </Button>
                )}
                {hasStream && (
                  <Button
                    variant="outlined"
                    color="secondary"
                    startIcon={isMicEnabled ? <Mic /> : <MicOff />}
                    onClick={toggleMic}
                    sx={{
                      px: 3,
                      py: 1,
                      borderRadius: 2,
                      fontWeight: 500,
                      transition: 'all 0.2s',
                      '&:hover': { borderColor: 'secondary.dark', color: 'secondary.dark' },
                    }}
                  >
                    {isMicEnabled ? 'Disable Mic' : 'Enable Mic'}
                  </Button>
                )}
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
              ) : (
                !hasStream && (
                  <Box
                    sx={{
                      p: 4,
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: 3,
                      width: '100%',
                      justifyContent: 'center',
                    }}
                  >
                    <VideocamIcon sx={{ fontSize: 48, color: 'text.secondary' }} />
                    <Typography variant="h6" sx={{ color: 'text.secondary', textAlign: 'center' }}>
                      No camera stream
                    </Typography>
                  </Box>
                )
              )}
              <video
                ref={videoRef}
                autoPlay
                playsInline
                style={{ width: '100%', maxWidth: '100%', borderRadius: 8, display: hasStream ? 'block' : 'none' }}
              />
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