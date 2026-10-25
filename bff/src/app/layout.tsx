import type {Metadata} from 'next';
import {Geist, Geist_Mono, Instrument_Serif} from 'next/font/google';
import Header from './header';
import './globals.css';

const geistSans = Geist({
  variable: '--font-geist-sans',
  subsets: ['latin'],
});

const geistMono = Geist_Mono({
  variable: '--font-geist-mono',
  subsets: ['latin'],
});

const instrumentSerif = Instrument_Serif({
  variable: '--font-instrument-serif',
  weight: '400',
  subsets: ['latin'],
});

export const metadata: Metadata = {
  title: 'Meeting Planner',
  description: 'Plan and manage meetings with your team.',
  icons: {
    icon: [{url: '/logo-mp.svg', type: 'image/svg+xml', sizes: 'any'}],
  },
};

export default function RootLayout({children}: LayoutProps<'/'>) {
  return (
    <html lang='en' className={`${geistSans.variable} ${geistMono.variable} ${instrumentSerif.variable}`}>
    <body>
    <Header/>
    {children}
    </body>
    </html>
  );
}
