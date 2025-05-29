import { CssBaseline, createTheme, ThemeProvider } from '@mui/material';
import CustomRouter from './components/CustomRouter';

const theme = createTheme({
  palette: {
    primary: { main: '#1976d2' },
    secondary: { main: '#26a69a' },
    background: { default: '#f5f7fa', paper: '#ffffff' },
  },
  typography: {
    fontFamily: '"Poppins", sans-serif',
    h4: { fontWeight: 600 },
    h6: { fontWeight: 500 },
  },
  components: {
    MuiDrawer: {
      styleOverrides: {
        paper: { backgroundColor: '#1976d2', color: '#ffffff' },
      },
    },
    MuiListItem: {
      styleOverrides: {
        root: {
          '&:hover': { backgroundColor: '#1565c0' },
          '&.Mui-selected': { backgroundColor: '#0d47a1' },
        },
      },
    },
  },
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