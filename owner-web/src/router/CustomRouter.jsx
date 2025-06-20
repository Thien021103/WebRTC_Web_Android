import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from '../components/Login';
import Dashboard from '../dashboard/Dashboard';

function PrivateRoute({ children }) {
  const token = localStorage.getItem('token');
  return token ? children : <Navigate to="/login" />;
}

function CustomRouter() {
  return (
    <Router basename="/owner">
      <Routes>
        <Route 
          path="/login"
          element={<Login />}
        />
        <Route
          path="/"
          element={
            <PrivateRoute>
              <Dashboard />
            </PrivateRoute>
          }
        />
        <Route 
          path="*"
          element={<Navigate to="/login" />}
        />
      </Routes>
    </Router>
  );
}

export default CustomRouter;