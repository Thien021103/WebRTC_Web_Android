import { CssBaseline, createTheme, ThemeProvider } from '@mui/material';
import CustomRouter from './router/CustomRouter';

const theme = createTheme({
  palette: {
    primary: { main: '#1F7D53' },
    secondary: { main: '#1976d2' },
    background: { default: '#f5f7fa', paper: '#ffffff' },
    status: {
      active: '#3F7D58',
      ready: '#1976d2',
      impossible: '#f57c00',
      creating: '#fbc02d',
      error: '#d32f2f',
      connected: '#2e7d32', // success.main equivalent
      disconnected: '#d32f2f', // error.main equivalent
    },
  },
  typography: {
    fontFamily: '"Poppins", sans-serif',
    h4: { fontWeight: 600 },
    h6: { fontWeight: 500 },
  },
  components: {
    MuiListItem: {
      styleOverrides: {
        root: {
          '&:hover': { backgroundColor: '#e3f2fd' },
          '&.Mui-selected': { backgroundColor: '#bbdefb' },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: '8px',
          padding: '0 8px',
          fontWeight: 500,
        },
      },
    },
  }
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <CustomRouter />
    </ThemeProvider>
  );
}

export default App;