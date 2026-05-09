import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import * as Label from '@radix-ui/react-label';
import { useAuth } from '../../hooks/useAuth.js';
import { registerSchema as schema } from '../../schemas/auth.js';
import styles from './RegisterPage.module.css';

export default function RegisterPage() {
  const { register: registerUser } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(schema) });

  const onSubmit = async (values) => {
    setServerError('');
    try {
      await registerUser(values);
      navigate('/login', { state: { registered: true } });
    } catch (err) {
      setServerError(err.response?.data?.message ?? 'Registration failed. Please try again.');
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.title}>Create an account</h1>
        <p className={styles.subtitle}>Join GenerosityWell and start making an impact.</p>

        <form onSubmit={handleSubmit(onSubmit)} noValidate className={styles.form}>
          <div className={styles.field}>
            <Label.Root htmlFor="name" className={styles.label}>
              Full name
            </Label.Root>
            <input
              id="name"
              type="text"
              autoComplete="name"
              className={`${styles.input} ${errors.name ? styles.inputError : ''}`}
              {...register('name')}
            />
            {errors.name && <span className={styles.error}>{errors.name.message}</span>}
          </div>

          <div className={styles.field}>
            <Label.Root htmlFor="email" className={styles.label}>
              Email
            </Label.Root>
            <input
              id="email"
              type="email"
              autoComplete="email"
              className={`${styles.input} ${errors.email ? styles.inputError : ''}`}
              {...register('email')}
            />
            {errors.email && <span className={styles.error}>{errors.email.message}</span>}
          </div>

          <div className={styles.field}>
            <Label.Root htmlFor="password" className={styles.label}>
              Password
            </Label.Root>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              className={`${styles.input} ${errors.password ? styles.inputError : ''}`}
              {...register('password')}
            />
            {errors.password && <span className={styles.error}>{errors.password.message}</span>}
          </div>

          {serverError && <p className={styles.serverError}>{serverError}</p>}

          <button type="submit" className={styles.submitBtn} disabled={isSubmitting}>
            {isSubmitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <p className={styles.footer}>
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </div>
    </div>
  );
}
