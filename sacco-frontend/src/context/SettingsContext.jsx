import { createContext, useState, useEffect, useContext } from 'react';
import api from '../api';

const SettingsContext = createContext();

export const useSettings = () => useContext(SettingsContext);

export const SettingsProvider = ({ children }) => {
  const [settings, setSettings] = useState({
    SACCO_NAME: 'Sacco System',
    SACCO_TAGLINE: 'Empowering Your Future',
    SACCO_LOGO: '',
    SACCO_FAVICON: ''
  });
  const [loading, setLoading] = useState(true);

  // Helper to get full image URL
  const getImageUrl = (filename) => {
    return filename ? `http://localhost:8080/uploads/settings/${filename}` : null;
  };

  useEffect(() => {
    const fetchSettings = async () => {
      try {
        const response = await api.get('/api/settings');
        if (response.data.success) {
          // Convert Array to Object for easier access
          const settingsMap = response.data.data.reduce((acc, curr) => {
            acc[curr.key] = curr.value;
            return acc;
          }, {});

          setSettings(prev => ({ ...prev, ...settingsMap }));
        }
      } catch (error) {
        console.error("Failed to load system settings", error);
      } finally {
        setLoading(false);
      }
    };

    fetchSettings();
  }, []);

  return (
    <SettingsContext.Provider value={{ settings, getImageUrl, loading }}>
      {children}
    </SettingsContext.Provider>
  );
};

// ✅ Component to Handle Global Head (Favicon & Title)
export const SystemBranding = () => {
  const { settings, getImageUrl } = useSettings();

  useEffect(() => {
    // 1. Update Document Title
    document.title = settings.SACCO_NAME;

    // 2. Update Favicon
    if (settings.SACCO_FAVICON) {
      const faviconUrl = getImageUrl(settings.SACCO_FAVICON);

      let link = document.querySelector("link[rel~='icon']");
      if (!link) {
        link = document.createElement('link');
        link.rel = 'icon';
        document.getElementsByTagName('head')[0].appendChild(link);
      }
      link.href = faviconUrl;
    }
  }, [settings, getImageUrl]);

  return null; // Renders nothing visibly
};