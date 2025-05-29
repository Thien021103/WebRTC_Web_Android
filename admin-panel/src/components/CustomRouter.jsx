import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './Login';
import Dashboard from './Dashboard';

function PrivateRoute({ children }) {
  const token = localStorage.getItem('token');
  return token ? children : <Navigate to="/login" />;
}

function CustomRouter() {
  return (
    <Router basename="/admin">
      <Routes>
        <Route 
          path = "/login"
          element = {
            <Login />
          } 
        />
        <Route
          path = "/"
          element = {
            <PrivateRoute>
              <Dashboard />
            </PrivateRoute>
          }
        />
        <Route 
          path="*"
          element = {
            <Navigate to="/login" />
          }
        />
      </Routes>
    </Router>
  );
}

export default CustomRouter;