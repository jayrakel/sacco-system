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
        BRAND_COLOR_PRIMARY: '#059669',
        BRAND_COLOR_SECONDARY: '#0f172a'
      };
  });
  const [loading, setLoading] = useState(true);

  // ✅ Smart Image URL Generator
  const getImageUrl = (filename) => {
    if (!filename) return null;

    // If it's a full URL (e.g. external link), return as is
    if (filename.startsWith('http')) return filename;

    // Base URL for uploads (Direct server access, bypassing /api prefix)
    const baseUrl = 'http://localhost:8082/uploads';

    // If the filename already includes a folder (e.g., "profiles/pic.jpg"), use it directly
    if (filename.includes('/')) {
        return `${baseUrl}/${filename}`;
    }

    // Default Fallback: If no folder is specified, assume it's a System Setting
    return `${baseUrl}/settings/${filename}`;
  };

  const fetchSettings = async () => {
    try {
      // ✅ FIX: Removed '/api' prefix.
      // api.js adds '/api', so we just request '/settings' to get '/api/settings'
      const response = await api.get('/settings');

      if (response.data.success) {
        const settingsMap = response.data.data.reduce((acc, curr) => {
          acc[curr.key] = curr.value;
          return acc;
        }, {});

        const newSettings = { ...settings, ...settingsMap };
        setSettings(newSettings);
        localStorage.setItem('sacco_settings', JSON.stringify(newSettings));
      }
    } catch (error) {
      console.error("Failed to load settings:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSettings();
  }, []);

  return (
    <SettingsContext.Provider value={{ settings, getImageUrl, loading, fetchSettings }}>
      {children}
    </SettingsContext.Provider>
  );
};

// Helper: Convert Hex to RGB for Tailwind
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
    document.title = settings.SACCO_NAME;
    if (settings.SACCO_FAVICON) {
      const link = document.querySelector("link[rel~='icon']") || document.createElement('link');
      link.rel = 'icon';
      link.href = getImageUrl(settings.SACCO_FAVICON);
      document.getElementsByTagName('head')[0].appendChild(link);
    }

    const root = document.documentElement;
    const primaryRgb = hexToRgb(settings.BRAND_COLOR_PRIMARY);
    const secondaryRgb = hexToRgb(settings.BRAND_COLOR_SECONDARY);

    if (primaryRgb) root.style.setProperty('--color-primary', primaryRgb);
    if (secondaryRgb) root.style.setProperty('--color-secondary', secondaryRgb);

  }, [settings, getImageUrl]);

  return null;
};