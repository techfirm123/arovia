/*
 * Public entry point for the Identity feature.
 *
 * Next.js route files should import from this file rather than
 * reaching directly into Identity's internal component structure.
 */

export {
  default as IdentityBoundary,
} from './components/auth/IdentityBoundary';

export {
  default as LoginForm,
} from './components/auth/LoginForm';

export {
  default as ForgotPasswordForm,
} from './components/auth/ForgotPasswordForm';

export {
  default as ResetPasswordClient,
} from './components/ResetPasswordClient';

export {
  default as VerifyEmailClient,
} from './components/VerifyEmailClient';