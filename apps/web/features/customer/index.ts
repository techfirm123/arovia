export { default as CustomersWorkspace } from './components/CustomersWorkspace';

export {
  CustomerApiError,
  fetchCustomers,
  fetchCustomer,
  createCustomer,
  updateCustomer,
  archiveCustomer,
} from './api/customer-api';

export type {
  Customer,
  CustomerPage,
  CustomerPayload,
  CustomerType,
  CustomerStatus,
} from './api/customer-api';