import { useState, useEffect, useRef } from 'react';
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
  Snackbar,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import { Refresh as RefreshIcon, VideoLibrary as VideoLibraryIcon, PlayArrow as PlayArrowIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useTheme } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

function Videos({ onRefetch }) {
  const [videos, setVideos] = useState([]);
  const [selectedVideo, setSelectedVideo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({
    startDate: '',
    endDate: '',
  });
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [deleteDialog, setDeleteDialog] = useState({ open: false, publicId: '' });
  const theme = useTheme();
  const playerRef = useRef(null);
  const navigate = useNavigate();

  // Initialize Cloudinary Video Player
  useEffect(() => {
    if (typeof cloudinary !== 'undefined' && !playerRef.current) {
      playerRef.current = cloudinary.videoPlayer('video-player', {
        cloud_name: 'dvarse6wk',
        controls: true,
        fluid: true,
        sourceTypes: ['hls', 'mp4'],
      });
    }
  }, []);

  const fetchVideos = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`https://thientranduc.id.vn:444/api/get-videos`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (response.data.status === 'success') {
        setVideos(response.data.videos || []);
        setError(null);
      } else {
        throw new Error(response.data.message || 'Failed to fetch videos');
      }
    } catch (err) {
      if (err.response?.data?.status === false && err.response?.data?.message === 'Invalid token') {
        localStorage.removeItem('token');
        navigate('/login');
      } else {
        const errorMessage = err.response?.data?.message || 'Failed to fetch videos';
        setError(errorMessage);
        setSnackbar({ open: true, message: errorMessage, severity: 'error' });
      }
    } finally {
      setLoading(false);
    }
  };

  const deleteVideo = async (publicId) => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await axios.delete(`https://thientranduc.id.vn:444/api/delete-videos`, {
        headers: { Authorization: `Bearer ${token}` },
        data: { publicId },
      });
      if (response.data.status === 'success') {
        setSnackbar({ open: true, message: 'Video deleted successfully', severity: 'success' });
        fetchVideos();
      } else {
        throw new Error(response.data.message || 'Failed to delete video');
      }
    } catch (err) {
      if (err.response?.data?.status === false && err.response?.data?.message === 'Invalid token') {
        localStorage.removeItem('token');
        navigate('/login');
      } else {
        const errorMessage = err.response?.data?.message || 'Failed to delete video';
        setSnackbar({ open: true, message: errorMessage, severity: 'error' });
      }
    } finally {
      setLoading(false);
      setDeleteDialog({ open: false, publicId: '' });
    }
  };

  useEffect(() => {
    fetchVideos();
  }, []);

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters((prev) => ({ ...prev, [name]: value }));
  };

  const handleApplyFilters = () => {
    fetchVideos();
  };

  const handleRefresh = () => {
    setFilters({ startDate: '', endDate: '' });
    setSelectedVideo(null);
    if (playerRef.current) {
      playerRef.current.stop();
    }
    fetchVideos();
    onRefetch();
  };

  const handleCloseSnackbar = () => {
    setSnackbar((prev) => ({ ...prev, open: false }));
  };

  const handlePlayVideo = (video) => {
    setSelectedVideo(video);
    if (playerRef.current) {
      playerRef.current.source(video.public_id, {
        sourceTypes: ['hls', 'mp4'],
      }).play();
    }
  };

  const handleOpenDeleteDialog = (publicId) => {
    setDeleteDialog({ open: true, publicId });
  };

  const handleCloseDeleteDialog = () => {
    setDeleteDialog({ open: false, publicId: '' });
  };

  const handleConfirmDelete = () => {
    if (deleteDialog.publicId) {
      deleteVideo(deleteDialog.publicId);
    }
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

  // Client-side filtering
  const filteredVideos = videos.filter((video) => {
    const createdAt = new Date(video.created_at);
    const start = filters.startDate ? new Date(filters.startDate) : null;
    const end = filters.endDate ? new Date(filters.endDate) : null;
    return (
      (!start || createdAt >= start) &&
      (!end || createdAt <= new Date(end.getTime() + 24 * 60 * 60 * 1000 - 1))
    );
  });

  if (error && !videos.length) {
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
                Videos
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

            {/* Video Player */}
            <Box sx={{ mb: 3, maxWidth: '100%', aspectRatio: '16/9' }}>
              <video
                id="video-player"
                className="cld-video-player cld-fluid"
                controls
              ></video>
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

            {/* Videos Table */}
            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                <CircularProgress />
              </Box>
            ) : filteredVideos.length === 0 ? (
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
                <VideoLibraryIcon sx={{ fontSize: 48, color: 'text.secondary' }} />
                <Typography variant="h6" sx={{ color: 'text.secondary', textAlign: 'center' }}>
                  No videos found
                </Typography>
              </Card>
            ) : (
              <TableContainer>
                <Table sx={{ minWidth: 500 }} aria-label="videos table">
                  <TableHead>
                    <TableRow>
                      <TableCell>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          Name
                        </Typography>
                      </TableCell>
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
                      <TableCell>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          Action
                        </Typography>
                      </TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {filteredVideos.map((video, index) => (
                      <TableRow
                        key={`${video.public_id}-${index}`}
                        sx={{
                          '&:hover': { bgcolor: 'background.default' },
                          transition: 'background-color 0.2s',
                        }}
                      >
                        <TableCell>{video.name}</TableCell>
                        <TableCell>{formatDate(video.created_at)}</TableCell>
                        <TableCell>{formatTime(video.created_at)}</TableCell>
                        <TableCell>
                          <Button
                            variant="contained"
                            color="primary"
                            startIcon={<PlayArrowIcon />}
                            onClick={() => handlePlayVideo(video)}
                            sx={{ borderRadius: 2, mr: 1 }}
                          >
                            Play
                          </Button>
                          <Button
                            variant="outlined"
                            color="error"
                            startIcon={<DeleteIcon />}
                            onClick={() => handleOpenDeleteDialog(video.public_id)}
                            sx={{ 
                              borderRadius: 2,
                              '&:hover': { bgcolor: 'error.light', borderColor: 'error.dark' },
                              transition: 'background-color 0.2s',
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
            )}
          </CardContent>
        </Card>
      </Fade>

      <Dialog
        open={deleteDialog.open}
        onClose={handleCloseDeleteDialog}
        aria-labelledby="delete-dialog-title"
      >
        <DialogTitle id="delete-dialog-title">Confirm Delete</DialogTitle>
        <DialogContent>
          <Typography variant="body1">
            Are you sure you want to delete the video <strong>{deleteDialog.publicId}</strong>?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDeleteDialog} color="secondary">
            Cancel
          </Button>
          <Button
            onClick={handleConfirmDelete}
            color="error"
            variant="contained"
            autoFocus
          >
            Delete
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

export default Videos;