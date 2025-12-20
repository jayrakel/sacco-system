import { createContext, useState, useEffect, useContext } from 'react';
import api from '../api';

const SettingsContext = createContext();

export const useSettings = () => useContext(SettingsContext);

export const SettingsProvider = ({ children }) => {
  const [settings, setSettings] = useState(() => {
      const cached = localStorage.getItem('sacco_settings');
      return cached ? JSON.parse(cached) : {
        SACCO_NAME: 'Sacco System',
        SACCO_TAGLINE: 'Empowering Your Future',
        SACCO_LOGO: '',
        SACCO_FAVICON: '',
        BRAND_COLOR_PRIMARY: '#059669',   // Default
        BRAND_COLOR_SECONDARY: '#0f172a'  // Default
      };
  });
  const [loading, setLoading] = useState(true);

  const getImageUrl = (filename) => {
    return filename ? `http://localhost:8082/uploads/settings/${filename}` : null;
  };

  useEffect(() => {
    const fetchSettings = async () => {
      try {
        console.log('🔄 Fetching settings from /api/settings...');
        const minLoadTime = new Promise(resolve => setTimeout(resolve, 2000));
        const apiCall = api.get('/api/settings');
        const [response] = await Promise.all([apiCall, minLoadTime]);

        console.log('✅ Settings response:', response.data);

        if (response.data.success) {
          const settingsMap = response.data.data.reduce((acc, curr) => {
            acc[curr.key] = curr.value;
            return acc;
          }, {});

          const newSettings = { ...settings, ...settingsMap };
          console.log('💾 Saving settings to localStorage:', newSettings);
          setSettings(newSettings);
          localStorage.setItem('sacco_settings', JSON.stringify(newSettings));
        } else {
          console.warn('⚠️ Settings fetch unsuccessful:', response.data);
        }
      } catch (error) {
        console.error("❌ Failed to load settings:");
        console.error('Error name:', error.name);
        console.error('Error message:', error.message);
        console.error('Error response:', error.response?.data);
        console.error('Error status:', error.response?.status);
        console.error('Full error:', error);
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

// Helper: Convert Hex (#ffffff) to RGB (255 255 255) for Tailwind
const hexToRgb = (hex) => {
    if (!hex) return null;
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ?
        `${parseInt(result[1], 16)} ${parseInt(result[2], 16)} ${parseInt(result[3], 16)}`
        : null;
}

export const SystemBranding = () => {
  const { settings, getImageUrl } = useSettings();

  useEffect(() => {
    // 1. Update Title & Favicon
    document.title = settings.SACCO_NAME;
    if (settings.SACCO_FAVICON) {
      const link = document.querySelector("link[rel~='icon']") || document.createElement('link');
      link.rel = 'icon';
      link.href = getImageUrl(settings.SACCO_FAVICON);
      document.getElementsByTagName('head')[0].appendChild(link);
    }

    // 2. ✅ Update CSS Variables (THEMING)
    const root = document.documentElement;
    const primaryRgb = hexToRgb(settings.BRAND_COLOR_PRIMARY);
    const secondaryRgb = hexToRgb(settings.BRAND_COLOR_SECONDARY);

    if (primaryRgb) root.style.setProperty('--color-primary', primaryRgb);
    if (secondaryRgb) root.style.setProperty('--color-secondary', secondaryRgb);

  }, [settings, getImageUrl]);

  return null;
};