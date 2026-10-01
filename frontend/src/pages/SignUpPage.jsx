import { SignUp } from '@clerk/clerk-react';

export default function SignUpPage() {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', marginTop: '4rem' }}>
      <SignUp signInUrl="/sign-in" afterSignUpUrl="/admin" />
    </div>
  );
}
