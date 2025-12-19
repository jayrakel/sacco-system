# SACCO Frontend - Modular Structure Alignment

## Frontend Architecture Overview

The frontend follows the same modular monolith principles as the backend, organized by feature modules with clear separation of concerns.

## Recommended Frontend Structure

```
sacco-frontend/src/
├── api/                          # API client layer
│   ├── client.js                # Axios configuration
│   ├── index.js                 # Central API export
│   ├── endpoints/
│   │   ├── member.js
│   │   ├── savings.js
│   │   ├── loan.js
│   │   ├── finance.js
│   │   ├── payment.js
│   │   ├── admin.js
│   │   ├── notification.js
│   │   └── reporting.js
│   └── interceptors/
│       ├── authInterceptor.js
│       ├── errorInterceptor.js
│       └── loggingInterceptor.js
│
├── modules/                      # Feature modules
│   ├── core/                     # Shared across modules
│   │   ├── components/
│   │   │   ├── layouts/
│   │   │   │   ├── MainLayout.jsx
│   │   │   │   └── AuthLayout.jsx
│   │   │   ├── common/
│   │   │   │   ├── Header.jsx
│   │   │   │   ├── Sidebar.jsx
│   │   │   │   ├── Footer.jsx
│   │   │   │   └── Loading.jsx
│   │   │   └── ui/
│   │   │       ├── Button.jsx
│   │   │       ├── Input.jsx
│   │   │       ├── Modal.jsx
│   │   │       ├── Toast.jsx
│   │   │       └── Card.jsx
│   │   ├── hooks/
│   │   │   ├── useAuth.js
│   │   │   ├── useFetch.js
│   │   │   ├── useForm.js
│   │   │   └── useLocalStorage.js
│   │   ├── context/
│   │   │   ├── AuthContext.jsx
│   │   │   ├── NotificationContext.jsx
│   │   │   └── SettingsContext.jsx
│   │   ├── utils/
│   │   │   ├── formatters.js
│   │   │   ├── validators.js
│   │   │   ├── constants.js
│   │   │   └── helpers.js
│   │   └── styles/
│   │       ├── variables.css
│   │       ├── globals.css
│   │       └── animations.css
│   │
│   ├── member/                   # Member module
│   │   ├── pages/
│   │   │   ├── MemberDashboard.jsx
│   │   │   ├── AddMember.jsx
│   │   │   └── MemberDetail.jsx
│   │   ├── components/
│   │   │   ├── MemberProfile.jsx
│   │   │   ├── MemberForm.jsx
│   │   │   ├── MemberList.jsx
│   │   │   └── MemberCard.jsx
│   │   ├── hooks/
│   │   │   ├── useMember.js
│   │   │   └── useMembers.js
│   │   ├── store/
│   │   │   └── memberSlice.js
│   │   ├── services/
│   │   │   └── memberService.js
│   │   └── styles/
│   │       └── member.css
│   │
│   ├── savings/                  # Savings module
│   │   ├── pages/
│   │   │   ├── SavingsDashboard.jsx
│   │   │   ├── SavingsAccounts.jsx
│   │   │   └── SavingsDetail.jsx
│   │   ├── components/
│   │   │   ├── SavingsOverview.jsx
│   │   │   ├── DepositForm.jsx
│   │   │   ├── WithdrawForm.jsx
│   │   │   └── TransactionHistory.jsx
│   │   ├── hooks/
│   │   │   ├── useSavings.js
│   │   │   └── useSavingsAccounts.js
│   │   ├── store/
│   │   │   └── savingsSlice.js
│   │   └── services/
│   │       └── savingsService.js
│   │
│   ├── loan/                     # Loan module
│   │   ├── pages/
│   │   │   ├── LoansDashboard.jsx
│   │   │   ├── LoanProducts.jsx
│   │   │   ├── LoanApplication.jsx
│   │   │   └── LoanDetail.jsx
│   │   ├── components/
│   │   │   ├── LoanApplicationForm.jsx
│   │   │   ├── RepaymentSchedule.jsx
│   │   │   ├── LoanCard.jsx
│   │   │   └── LoanReview.jsx
│   │   ├── hooks/
│   │   │   ├── useLoans.js
│   │   │   └── useLoanApplication.js
│   │   ├── store/
│   │   │   └── loanSlice.js
│   │   └── services/
│   │       └── loanService.js
│   │
│   ├── finance/                  # Finance module
│   │   ├── pages/
│   │   │   ├── FinanceDashboard.jsx
│   │   │   ├── Reports.jsx
│   │   │   └── Transactions.jsx
│   │   ├── components/
│   │   │   ├── FinanceOverview.jsx
│   │   │   ├── ChartWidget.jsx
│   │   │   ├── ReportTable.jsx
│   │   │   └── TransactionList.jsx
│   │   ├── hooks/
│   │   │   ├── useFinanceData.js
│   │   │   └── useReports.js
│   │   ├── store/
│   │   │   └── financeSlice.js
│   │   └── services/
│   │       └── financeService.js
│   │
│   ├── payment/                  # Payment module
│   │   ├── components/
│   │   │   ├── PaymentForm.jsx
│   │   │   ├── PaymentHistory.jsx
│   │   │   └── PaymentStatus.jsx
│   │   ├── hooks/
│   │   │   └── usePayment.js
│   │   ├── store/
│   │   │   └── paymentSlice.js
│   │   └── services/
│   │       └── paymentService.js
│   │
│   ├── admin/                    # Admin module
│   │   ├── pages/
│   │   │   ├── AdminDashboard.jsx
│   │   │   └── SystemSettings.jsx
│   │   ├── components/
│   │   │   ├── AuditLogs.jsx
│   │   │   ├── AssetManager.jsx
│   │   │   ├── SystemConfig.jsx
│   │   │   └── UserManagement.jsx
│   │   ├── hooks/
│   │   │   └── useAdmin.js
│   │   └── services/
│   │       └── adminService.js
│   │
│   ├── reporting/                # Reporting module
│   │   ├── pages/
│   │   │   └── ReportsDashboard.jsx
│   │   ├── components/
│   │   │   ├── ReportBuilder.jsx
│   │   │   ├── ReportViewer.jsx
│   │   │   └── ScheduledReports.jsx
│   │   └── services/
│   │       └── reportingService.js
│   │
│   └── auth/                     # Authentication module
│       ├── pages/
│       │   ├── Login.jsx
│       │   ├── Register.jsx
│       │   └── VerifyEmail.jsx
│       ├── components/
│       │   ├── LoginForm.jsx
│       │   ├── RegisterForm.jsx
│       │   └── ProtectedRoute.jsx
│       ├── hooks/
│       │   ├── useAuth.js
│       │   └── useLogin.js
│       └── services/
│           └── authService.js
│
├── router/                       # Route configuration
│   ├── index.js
│   ├── routes.js
│   └── ProtectedRoute.jsx
│
├── store/                        # Redux store
│   ├── index.js
│   ├── rootReducer.js
│   └── middleware/
│       └── errorMiddleware.js
│
├── types/                        # TypeScript/JSDoc types
│   ├── member.types.js
│   ├── loan.types.js
│   ├── savings.types.js
│   └── common.types.js
│
├── App.jsx                       # Root component
├── main.jsx                      # Entry point
└── index.css                     # Global styles
```

## Module Component Guidelines

### Core Module Layout Structure

```jsx
// src/modules/core/components/layouts/MainLayout.jsx
export const MainLayout = ({ children }) => {
  return (
    <div className="main-layout">
      <Header />
      <div className="layout-body">
        <Sidebar />
        <main className="content">
          {children}
        </main>
      </div>
      <Footer />
    </div>
  );
};
```

### Feature Module Page Structure

```jsx
// src/modules/member/pages/MemberDashboard.jsx
import { MainLayout } from '../../core/components/layouts/MainLayout';
import { useMembers } from '../hooks/useMembers';

export const MemberDashboard = () => {
  const { members, loading, error } = useMembers();

  return (
    <MainLayout>
      <div className="member-dashboard">
        <h1>Member Management</h1>
        {/* Module-specific content */}
      </div>
    </MainLayout>
  );
};
```

### API Service Pattern

```javascript
// src/modules/member/services/memberService.js
import client from '../../../api/client';

const API_BASE = '/api/v1/members';

export const memberService = {
  // Create new member
  createMember: async (memberData) => {
    const response = await client.post(API_BASE, memberData);
    return response.data;
  },

  // Get member by ID
  getMember: async (id) => {
    const response = await client.get(`${API_BASE}/${id}`);
    return response.data;
  },

  // Get all members with pagination
  getAllMembers: async (page = 0, size = 10) => {
    const response = await client.get(API_BASE, {
      params: { page, size }
    });
    return response.data;
  },

  // Update member
  updateMember: async (id, memberData) => {
    const response = await client.put(`${API_BASE}/${id}`, memberData);
    return response.data;
  },

  // Deactivate member
  deactivateMember: async (id) => {
    const response = await client.delete(`${API_BASE}/${id}`);
    return response.data;
  },

  // Get active members count
  getActiveMemberCount: async () => {
    const response = await client.get(`${API_BASE}/count/active`);
    return response.data;
  }
};
```

### Custom Hook Pattern

```javascript
// src/modules/member/hooks/useMembers.js
import { useState, useEffect } from 'react';
import { memberService } from '../services/memberService';

export const useMembers = (page = 0, size = 10) => {
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [totalElements, setTotalElements] = useState(0);

  useEffect(() => {
    fetchMembers();
  }, [page, size]);

  const fetchMembers = async () => {
    try {
      setLoading(true);
      const response = await memberService.getAllMembers(page, size);
      setMembers(response.data.content);
      setTotalElements(response.data.totalElements);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return {
    members,
    loading,
    error,
    totalElements,
    refetch: fetchMembers
  };
};
```

### Redux Store Slice Pattern

```javascript
// src/modules/member/store/memberSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { memberService } from '../services/memberService';

export const fetchMembers = createAsyncThunk(
  'member/fetchMembers',
  async ({ page, size }) => {
    const response = await memberService.getAllMembers(page, size);
    return response.data;
  }
);

const memberSlice = createSlice({
  name: 'member',
  initialState: {
    members: [],
    loading: false,
    error: null,
    totalElements: 0,
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchMembers.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchMembers.fulfilled, (state, action) => {
        state.loading = false;
        state.members = action.payload.content;
        state.totalElements = action.payload.totalElements;
      })
      .addCase(fetchMembers.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message;
      });
  }
});

export default memberSlice.reducer;
```

## Communication Between Modules

### 1. Direct Service Communication (for shared data)

```jsx
// Savings module retrieving member data
import { memberService } from '../../member/services/memberService';

const SavingsOverview = () => {
  const [member, setMember] = useState(null);
  
  useEffect(() => {
    memberService.getMember(memberId).then(setMember);
  }, [memberId]);
  
  return <div>{member?.fullName}'s Savings</div>;
};
```

### 2. State Management Communication (using Redux)

```javascript
// Store configuration
import { configureStore } from '@reduxjs/toolkit';
import memberReducer from './modules/member/store/memberSlice';
import savingsReducer from './modules/savings/store/savingsSlice';
import loanReducer from './modules/loan/store/loanSlice';

export const store = configureStore({
  reducer: {
    member: memberReducer,
    savings: savingsReducer,
    loan: loanReducer,
  }
});
```

### 3. Context API Communication

```jsx
// Shared notification context
import { createContext, useContext, useState } from 'react';

const NotificationContext = createContext();

export const NotificationProvider = ({ children }) => {
  const [notifications, setNotifications] = useState([]);

  const notify = (message, type = 'info') => {
    setNotifications(prev => [...prev, { message, type, id: Date.now() }]);
  };

  return (
    <NotificationContext.Provider value={{ notify }}>
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotification = () => useContext(NotificationContext);
```

## API Client Configuration

```javascript
// src/api/client.js
import axios from 'axios';
import { useAuth } from '../modules/auth/hooks/useAuth';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

const client = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for auth
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor for error handling
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized access
      localStorage.removeItem('authToken');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default client;
```

## Environment Configuration

```bash
# .env
REACT_APP_API_URL=http://localhost:8080
REACT_APP_ENV=development
REACT_APP_LOG_LEVEL=debug

# .env.production
REACT_APP_API_URL=https://api.sacco.prod
REACT_APP_ENV=production
REACT_APP_LOG_LEVEL=error
```

## Build & Development Commands

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "lint": "eslint src --ext .js,.jsx",
    "lint:fix": "eslint src --ext .js,.jsx --fix",
    "format": "prettier --write \"src/**/*.{js,jsx,json,css}\"",
    "test": "vitest",
    "test:ui": "vitest --ui",
    "type-check": "tsc --noEmit"
  }
}
```

## Performance Optimization

### Code Splitting by Module

```javascript
// src/router/routes.js
import { lazy, Suspense } from 'react';

const MemberDashboard = lazy(() => import('../modules/member/pages/MemberDashboard'));
const SavingsDashboard = lazy(() => import('../modules/savings/pages/SavingsDashboard'));
const LoansDashboard = lazy(() => import('../modules/loan/pages/LoansDashboard'));

export const routes = [
  {
    path: '/members',
    component: () => (
      <Suspense fallback={<Loading />}>
        <MemberDashboard />
      </Suspense>
    )
  },
  // ... other routes
];
```

## Testing Strategy

```javascript
// src/modules/member/__tests__/memberService.test.js
import { describe, it, expect, vi } from 'vitest';
import { memberService } from '../services/memberService';

describe('memberService', () => {
  it('should fetch all members', async () => {
    const members = await memberService.getAllMembers();
    expect(members.data).toBeDefined();
  });
});
```

## Best Practices

1. **Module Independence**: Each module should be independent and not directly import from other module's internal components
2. **Clear APIs**: Only expose necessary components, services, and hooks through module's index.js
3. **Consistent Naming**: Follow naming conventions across all modules
4. **Reusable Components**: Share common components through core module
5. **Error Handling**: Use consistent error handling across all API calls
6. **State Management**: Use Redux for global state, local state for component-specific data
7. **Type Safety**: Use JSDoc or TypeScript for better type checking
8. **Testing**: Each module should have unit and integration tests

---

**Next Steps:**
1. Reorganize existing React components into module structure
2. Implement API service layer for each module
3. Set up Redux slices for each module
4. Create shared hooks and utilities in core module
5. Update route configuration to match new structure
