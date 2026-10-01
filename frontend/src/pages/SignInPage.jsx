import { SignIn } from '@clerk/clerk-react';

export default function SignInPage() {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', marginTop: '4rem' }}>
      <SignIn signUpUrl="/sign-up" afterSignInUrl="/admin" />
    </div>
  );
}
