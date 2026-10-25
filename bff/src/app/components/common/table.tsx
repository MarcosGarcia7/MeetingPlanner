'use client';

import {clsx} from 'clsx';
import styles from './table.module.css';

export type ColumnType = 'string' | 'number' | 'date' | 'currency' | 'boolean';

export interface Column<T> {
  key: keyof T & string;
  header: string;
  type?: ColumnType;
  render?: (value: T[keyof T], row: T) => React.ReactNode;
}

export interface TableProps<T> {
  title: string;
  columns: Column<T>[];
  data: T[];
  onRowClick?: (row: T) => void;
}

function formatValue(value: unknown, type: ColumnType): React.ReactNode {
  if (value === null || value === undefined) return '—';

  switch (type) {
    case 'number':
      return typeof value === 'number' ? value.toLocaleString() : String(value);
    case 'currency': {
      const num = typeof value === 'number' ? value : Number(value);
      return isNaN(num) ? String(value) : `$${num.toLocaleString(undefined, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })}`;
    }
    case 'date': {
      const date = value instanceof Date ? value : new Date(String(value));
      return isNaN(date.getTime()) ? String(value) : date.toLocaleDateString();
    }
    case 'boolean':
      return value ? 'Yes' : 'No';
    case 'string':
    default:
      return String(value);
  }
}

export default function Table<T extends object>({
                                                  title,
                                                  columns,
                                                  data,
                                                  onRowClick,
                                                }: TableProps<T>) {
  return (
    <div className={styles.container}>
      <h2 className={styles.title}>{title}</h2>
      <table className={styles.table}>
        <thead>
        <tr>
          {columns.map((col) => (
            <th key={col.key} className={styles.th}>
              {col.header}
            </th>
          ))}
        </tr>
        </thead>
        <tbody>
        {data.length === 0 ? (
          <tr>
            <td colSpan={columns.length} className={styles.empty}>
              No data available
            </td>
          </tr>
        ) : (
          data.map((row, rowIndex) => (
            <tr
              key={rowIndex}
              className={clsx(styles.tr, {[styles.clickable]: !!onRowClick})}
              onClick={() => onRowClick?.(row)}
            >
              {columns.map((col) => (
                <td key={col.key} className={styles.td}>
                  {col.render
                    ? col.render(row[col.key], row)
                    : formatValue(row[col.key], col.type ?? 'string')}
                </td>
              ))}
            </tr>
          ))
        )}
        </tbody>
      </table>
    </div>
  );
}
