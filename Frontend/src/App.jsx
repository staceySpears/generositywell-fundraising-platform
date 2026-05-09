import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout/Layout.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';

import LandingPage from './pages/LandingPage/LandingPage.jsx';
import NotFoundPage from './pages/NotFoundPage/NotFoundPage.jsx';
import LoginPage from './pages/LoginPage/LoginPage.jsx';
import RegisterPage from './pages/RegisterPage/RegisterPage.jsx';
import ForgotPasswordPage from './pages/ForgotPasswordPage/ForgotPasswordPage.jsx';
import SearchPage from './pages/SearchPage/SearchPage.jsx';
import CampaignsPage from './pages/CampaignsPage/CampaignsPage.jsx';
import CampaignDetailPage from './pages/CampaignDetailPage/CampaignDetailPage.jsx';
import DashboardPage from './pages/DashboardPage/DashboardPage.jsx';
import EventsPage from './pages/EventsPage/EventsPage.jsx';
import CalendarPage from './pages/CalendarPage/CalendarPage.jsx';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        {/* Public routes */}
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/campaigns" element={<CampaignsPage />} />
        <Route path="/campaigns/:id" element={<CampaignDetailPage />} />

        {/* Authenticated routes */}
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/events" element={<EventsPage />} />
          <Route path="/calendar" element={<CalendarPage />} />
        </Route>

        {/* Catch-all — renders inside Layout so the nav is still visible */}
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
