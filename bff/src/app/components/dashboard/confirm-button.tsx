'use client';

import {useState} from 'react';
import dashboardStyles from './dashboard.module.css';

interface ConfirmButtonProps {
  label: string;
  confirmLabel: string;
  disabled?: boolean;
  onConfirm: () => void;
}

// A secondary button that asks for a second click before a destructive action.
export default function ConfirmButton({label, confirmLabel, disabled, onConfirm}: ConfirmButtonProps) {
  const [confirming, setConfirming] = useState(false);

  if (!confirming) {
    return (
      <button type='button' className={dashboardStyles.buttonSecondary} disabled={disabled}
              onClick={() => setConfirming(true)}>
        {label}
      </button>
    );
  }
  return (
    <>
      <button type='button' className={dashboardStyles.buttonDanger} disabled={disabled}
              onClick={() => {
                setConfirming(false);
                onConfirm();
              }}>
        {confirmLabel}
      </button>
      <button type='button' className={dashboardStyles.buttonSecondary} disabled={disabled}
              onClick={() => setConfirming(false)}>
        Cancel
      </button>
    </>
  );
}
