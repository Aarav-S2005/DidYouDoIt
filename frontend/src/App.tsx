import { useEffect, useState } from 'react';
import {
  Download,
  Terminal,
  ShieldCheck,
  Zap,
  BellRing,
  Sliders,
  CheckCircle2,
  RefreshCw,
  Monitor,
  Timer
} from 'lucide-react';

interface ReleaseAsset {
  name: string;
  browser_download_url: string;
  size: number;
}

type Personality = 'gentle' | 'strict' | 'sarcastic' | 'aggressive';

interface QuoteSample {
  headline: string;
  body: string;
}

const TONE_SAMPLES: Record<Personality, QuoteSample[]> = {
  gentle: [
    {
      headline: "Time to focus: Read Chapter 5",
      body: "You planned this for now. Take a deep breath and begin!"
    },
    {
      headline: "Gentle Reminder: Drink 2L Water",
      body: "Overdue by 15 minutes. Even 5 minutes of focus will help brighten your day."
    },
    {
      headline: "Needs your attention: Submit Expense Report",
      body: "Be kind to future you and complete it now. You've got this!"
    }
  ],
  strict: [
    {
      headline: "Due Now: Finish Quarterly Taxes",
      body: "Scheduled deadline reached. Operational efficiency begins with punctual execution."
    },
    {
      headline: "Overdue: Review Pull Request #42",
      body: "Past deadline by 20 minutes. Stop delaying and complete the objective."
    },
    {
      headline: "CRITICAL DEADLINE: Deploy Security Patch",
      body: "Discipline over motivation. Complete this task without further postponement."
    }
  ],
  sarcastic: [
    {
      headline: "Look who's due: Complete Morning Routine",
      body: "Are you actually going to do this, or just admire this notification?"
    },
    {
      headline: "Still ignoring: File Expense Receipts?",
      body: "Overdue by 25m. Bold strategy. Let's see if magic finishes it."
    },
    {
      headline: "RIP Productivity: Write Documentation",
      body: "Overdue by 60m. Did you think this was going to complete itself? Do it now!"
    }
  ],
  aggressive: [
    {
      headline: "DO IT NOW: Gym Workout",
      body: "Close the tabs and put down distractions. Start right now!"
    },
    {
      headline: "MOVE IT: Finalize Client Proposal",
      body: "You are 30 minutes late! What happened to your discipline?!"
    },
    {
      headline: "DROP EVERYTHING AND FINISH: Study for Exam",
      body: "EMERGENCY LEVEL OVERDUE! Stop everything else and finish this right now!"
    }
  ]
};

function GitHubIcon({ size = 16 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4" />
      <path d="M9 18c-4.51 2-5-2-7-2" />
    </svg>
  );
}

export function App() {
  const [selectedTone, setSelectedTone] = useState<Personality>('sarcastic');
  const [quoteIndex, setQuoteIndex] = useState(0);
  const [releaseVersion, setReleaseVersion] = useState<string>('v1.1.4');
  const [assets, setAssets] = useState<Record<string, string>>({});
  const [isFetched, setIsFetched] = useState<boolean>(false);

  useEffect(() => {
    // Fetch latest release assets using native fetch() without any external HTTP libraries
    fetch('https://api.github.com/repos/Aarav-S2005/DidYouDoIt/releases/latest')
      .then((res) => {
        if (!res.ok) throw new Error('Network response not ok');
        return res.json();
      })
      .then((data) => {
        if (data.tag_name) {
          setReleaseVersion(data.tag_name);
        }
        if (Array.isArray(data.assets) && data.assets.length > 0) {
          const map: Record<string, string> = {};
          data.assets.forEach((asset: ReleaseAsset) => {
            const name = asset.name.toLowerCase();
            if (name.endsWith('.msi')) map['msi'] = asset.browser_download_url;
            else if (name.endsWith('.exe')) map['exe'] = asset.browser_download_url;
            else if (name.endsWith('.deb')) map['deb'] = asset.browser_download_url;
            else if (name.endsWith('.rpm')) map['rpm'] = asset.browser_download_url;
            else if (name.endsWith('.tar.gz')) map['gz'] = asset.browser_download_url;
            else if (name.endsWith('.zip')) map['zip'] = asset.browser_download_url;
          });
          setAssets(map);
          setIsFetched(true);
        }
      })
      .catch(() => {
        // Fallback gracefully to default repo releases URL if offline or unreleased
      });
  }, []);

  const samples = TONE_SAMPLES[selectedTone];
  const currentSample = samples[quoteIndex % samples.length];

  const handleToneChange = (tone: Personality) => {
    setSelectedTone(tone);
    setQuoteIndex(0);
  };

  const handleNextQuote = () => {
    setQuoteIndex((prev) => prev + 1);
  };

  const scrollToSection = (id: string) => {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const getDownloadUrl = (ext: string) => {
    return assets[ext] || `https://github.com/Aarav-S2005/DidYouDoIt/releases`;
  };

  return (
    <div>
      {/* Site Header */}
      <header className="site-header">
        <div className="container header-inner">
          <div className="brand">
            <svg className="brand-icon" viewBox="0 0 256 256" fill="none">
              <rect x="24" y="24" width="208" height="208" rx="54" fill="#E27D60" />
              <path d="M74 136 L112 174 L184 94" stroke="#FFFFFF" strokeWidth="26" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            <span>DidYouDoIt?</span>
          </div>

          <nav className="nav-links">
            <button onClick={() => scrollToSection('features')} className="nav-link">
              Features
            </button>
            <button onClick={() => scrollToSection('preview')} className="nav-link">
              Tone Simulator
            </button>
            <a
              href="https://github.com/Aarav-S2005/DidYouDoIt"
              target="_blank"
              rel="noreferrer"
              className="nav-link"
              style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
            >
              <GitHubIcon size={16} /> GitHub
            </a>
            <button onClick={() => scrollToSection('downloads')} className="btn-header">
              Download
            </button>
          </nav>
        </div>
      </header>

      {/* Hero Section */}
      <section className="hero">
        <div className="container">
          <div className="hero-pill">
            <CheckCircle2 size={14} color="#E27D60" /> Free & Open Source Desktop App
          </div>

          <h1 className="hero-title">
            The task app that actually <br />
            <span className="hero-highlight">makes sure you do them.</span>
          </h1>

          <p className="hero-subtitle">
            Most to-do lists just sit there quietly while deadlines slip away. DidYouDoIt? runs in
            your system tray, watches your clock, and nags you with escalating accountability until you finish.
          </p>

          <div className="hero-actions">
            <button onClick={() => scrollToSection('downloads')} className="btn-primary">
              <Download size={18} /> Download for Windows & Linux
            </button>
            <a
              href="https://github.com/Aarav-S2005/DidYouDoIt"
              target="_blank"
              rel="noreferrer"
              className="btn-secondary"
            >
              <GitHubIcon size={18} /> View Source Code
            </a>
          </div>
        </div>
      </section>

      {/* Interactive Tone Preview */}
      <section id="preview" className="preview-section">
        <div className="container">
          <div className="preview-card">
            <div className="preview-header">
              <span className="preview-title">Live Nagging Tone Simulator</span>

              <div className="tone-selector">
                {(['gentle', 'strict', 'sarcastic', 'aggressive'] as Personality[]).map((t) => (
                  <button
                    key={t}
                    onClick={() => handleToneChange(t)}
                    className={`tone-btn ${selectedTone === t ? 'active' : ''}`}
                  >
                    {t.charAt(0).toUpperCase() + t.slice(1)}
                  </button>
                ))}
              </div>
            </div>

            <div className="toast-simulator">
              <div className="toast-content">
                <div className="toast-headline">{currentSample.headline}</div>
                <div className="toast-body">{currentSample.body}</div>
              </div>

              <div className="toast-actions">
                <button className="toast-btn toast-btn-done">Done</button>
                <button className="toast-btn">Snooze 15m</button>
                <button
                  onClick={handleNextQuote}
                  title="Randomize Quote"
                  className="toast-btn"
                  style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}
                >
                  <RefreshCw size={12} /> Next
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Core Features */}
      <section id="features" className="features-section">
        <div className="container">
          <div className="section-label">Designed for Real Accountability</div>
          <h2 className="section-heading">How DidYouDoIt Keeps You On Track</h2>

          <div className="features-grid">
            <div className="feature-box">
              <div className="feature-icon-wrapper">
                <BellRing size={20} />
              </div>
              <h3 className="feature-title">Persistent Nagging Daemon</h3>
              <p className="feature-desc">
                Monitors due dates in the background even when the main window is closed. Escalates from gentle nudges to urgent warnings.
              </p>
            </div>

            <div className="feature-box">
              <div className="feature-icon-wrapper">
                <Sliders size={20} />
              </div>
              <h3 className="feature-title">4 Personality Styles</h3>
              <p className="feature-desc">
                Choose how your app communicates: Gentle mindfulness, Strict military discipline, Sarcastic reality checks, or Aggressive pressure.
              </p>
            </div>

            <div className="feature-box">
              <div className="feature-icon-wrapper">
                <Timer size={20} />
              </div>
              <h3 className="feature-title">Variable Focus Timers</h3>
              <p className="feature-desc">
                Set custom study or work durations (15m, 1h, custom hours & mins) with live progress tracking and alarm chimes upon completion.
              </p>
            </div>

            <div className="feature-box">
              <div className="feature-icon-wrapper">
                <ShieldCheck size={20} />
              </div>
              <h3 className="feature-title">100% Offline & Private</h3>
              <p className="feature-desc">
                No accounts, no logins, no cloud tracking. Everything is persisted locally in an SQLite database on your device.
              </p>
            </div>

            <div className="feature-box">
              <div className="feature-icon-wrapper">
                <Zap size={20} />
              </div>
              <h3 className="feature-title">System Tray & Auto-Start</h3>
              <p className="feature-desc">
                Minimizes cleanly to the Windows taskbar or Linux system tray. Supports instant snooze, quiet hours, and auto-start on login.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Downloads Section */}
      <section id="downloads" className="downloads-section">
        <div className="container">
          <div className="section-label">
            Installation Packages • {releaseVersion} {isFetched ? '(GitHub Releases Active)' : '(Official Mirror)'}
          </div>
          <h2 className="section-heading">Download DidYouDoIt</h2>

          <div className="downloads-grid">
            {/* Windows Card */}
            <div className="download-card">
              <div>
                <div className="download-card-header">
                  <Monitor className="os-icon" />
                  <div>
                    <div className="download-os-title">Windows</div>
                    <div className="download-os-meta">Windows 10 / 11 (64-bit)</div>
                  </div>
                </div>

                <div className="download-options">
                  <a href={getDownloadUrl('msi')} target="_blank" rel="noreferrer" className="download-btn-item">
                    <span>Windows Installer Package</span>
                    <span className="download-ext">.msi</span>
                  </a>
                  <a href={getDownloadUrl('zip')} target="_blank" rel="noreferrer" className="download-btn-item">
                    <span>Portable Bundle (Includes DidYouDoIt.exe)</span>
                    <span className="download-ext">.zip</span>
                  </a>
                </div>
              </div>

              <div style={{ fontSize: 13, color: '#6C757D', marginTop: 12 }}>
                Includes bundled Java runtime. Install via <code>.msi</code> for desktop & start menu shortcuts, or extract <code>.zip</code> to run portably.
              </div>
            </div>

            {/* Linux Card */}
            <div className="download-card">
              <div>
                <div className="download-card-header">
                  <Terminal className="os-icon" />
                  <div>
                    <div className="download-os-title">Linux</div>
                    <div className="download-os-meta">Compatible with all major distributions</div>
                  </div>
                </div>

                <div className="download-options">
                  <a href={getDownloadUrl('deb')} target="_blank" rel="noreferrer" className="download-btn-item">
                    <span>Debian / Ubuntu / Mint</span>
                    <span className="download-ext">.deb</span>
                  </a>
                  <a href={getDownloadUrl('gz')} target="_blank" rel="noreferrer" className="download-btn-item">
                    <span>Universal Portable Archive (Fedora, Arch, etc.)</span>
                    <span className="download-ext">.tar.gz</span>
                  </a>
                </div>
              </div>

              <div className="install-command">
                sudo apt install ./didyoudoit.deb
              </div>
            </div>
          </div>

          <div style={{ textAlign: 'center', fontSize: 13.5, color: '#6C757D' }}>
            Looking to build from source? Clone the repository and run <code>./mvnw javafx:run</code> inside the <code>app/</code> directory.
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="site-footer">
        <div className="container">
          <div className="footer-links">
            <a href="https://github.com/Aarav-S2005/DidYouDoIt" target="_blank" rel="noreferrer" className="footer-link">
              GitHub Repository
            </a>
            <span>•</span>
            <button onClick={() => scrollToSection('features')} className="footer-link">
              Features
            </button>
            <span>•</span>
            <button onClick={() => scrollToSection('downloads')} className="footer-link">
              Downloads
            </button>
          </div>
          <div>
            DidYouDoIt? — Free, Open Source Personal Accountability Desktop System.
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;
