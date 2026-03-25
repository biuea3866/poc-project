import { spawn, type ChildProcess } from 'child_process';

let smeeProcess: ChildProcess | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let isShuttingDown = false;

export function startSmeeClient(smeeUrl: string, targetUrl: string): void {
  if (!smeeUrl) {
    console.log('[smee] SMEE_URL not set, skipping proxy');
    return;
  }

  isShuttingDown = false;
  connect(smeeUrl, targetUrl);

  // Graceful shutdown
  process.on('SIGINT', () => stopSmeeClient());
  process.on('SIGTERM', () => stopSmeeClient());
}

function connect(smeeUrl: string, targetUrl: string): void {
  if (isShuttingDown) return;

  console.log(`[smee] Connecting: ${smeeUrl} → ${targetUrl}`);

  smeeProcess = spawn('npx', ['smee', '-u', smeeUrl, '-t', targetUrl], {
    stdio: ['ignore', 'pipe', 'pipe'],
    shell: true,
  });

  smeeProcess.stdout?.on('data', (data: Buffer) => {
    const msg = data.toString().trim();
    if (msg) console.log(`[smee] ${msg}`);
  });

  smeeProcess.stderr?.on('data', (data: Buffer) => {
    const msg = data.toString().trim();
    if (msg) console.error(`[smee] ERROR: ${msg}`);
  });

  smeeProcess.on('close', (code) => {
    if (isShuttingDown) return;

    console.warn(`[smee] Process exited (code=${code}). Reconnecting in 5s...`);
    smeeProcess = null;

    reconnectTimer = setTimeout(() => {
      connect(smeeUrl, targetUrl);
    }, 5000);
  });

  smeeProcess.on('error', (err) => {
    console.error(`[smee] Spawn error:`, err.message);
  });
}

export function stopSmeeClient(): void {
  isShuttingDown = true;

  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }

  if (smeeProcess) {
    smeeProcess.kill();
    smeeProcess = null;
    console.log('[smee] Stopped');
  }
}
