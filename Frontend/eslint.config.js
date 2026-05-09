import js from '@eslint/js';
import globals from 'globals';
import reactPlugin from 'eslint-plugin-react';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import prettier from 'eslint-config-prettier';

export default [
  { ignores: ['dist', 'node_modules'] },

  // Base JS + React rules
  {
    files: ['**/*.{js,jsx}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: globals.browser,
      parserOptions: {
        ecmaFeatures: { jsx: true },
      },
    },
    settings: {
      react: { version: '18.3' },
    },
    plugins: {
      react: reactPlugin,
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      // ESLint recommended
      ...js.configs.recommended.rules,

      // React recommended (no manual React import needed with new JSX transform)
      ...reactPlugin.configs.recommended.rules,
      ...reactPlugin.configs['jsx-runtime'].rules,

      // Hooks lint rules (exhaustive-deps catches stale closure bugs)
      ...reactHooks.configs.recommended.rules,

      // Warn when a file exported by react-refresh isn't a component — prevents
      // HMR breaking silently in dev.
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],

      // Prefer const; catch unused vars (prefix _ to opt out)
      'prefer-const': 'error',
      'no-unused-vars': ['error', { varsIgnorePattern: '^_', argsIgnorePattern: '^_' }],

      // Guard against accidentally rendering objects as JSX children
      'react/jsx-no-useless-fragment': 'warn',

      // Prop-types are not used — the codebase relies on JSDoc today and will
      // migrate to TypeScript (Phase 5). Keeping this rule on would generate
      // noise without safety benefit.
      'react/prop-types': 'off',
    },
  },

  // Disable all ESLint formatting rules so Prettier owns them
  prettier,
];
