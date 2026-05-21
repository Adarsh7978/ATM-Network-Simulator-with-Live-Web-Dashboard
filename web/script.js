'use strict';

const API_ATMS = '/api/atms';
const API_TRANSACTIONS = '/api/transactions';
const REFRESH_INTERVAL = 3; // seconds

let countdownVal = REFRESH_INTERVAL;
let countdownTimer = null;
let refreshTimer = null;
let previousTxTimestamps = new Set();

// ── Helpers ────────────────────────────────────────────────────────────────

function formatRupees(amount) {
  return '₹' + Number(amount).toLocaleString('en-IN');
}

function cashLevel(balance) {
  if (balance > 30000)  return 'green';
  if (balance >= 10000) return 'orange';
  return 'red';
}

function cashStatusLabel(level) {
  return level === 'green' ? 'Adequate' : level === 'orange' ? 'Low' : 'Critical';
}

function cashPercent(balance) {
  const max = 50000;
  return Math.max(2, Math.min(100, Math.round((balance / max) * 100)));
}

function formatTimestamp(ts) {
  // ts like "2024-01-01 12:34:56"
  const parts = ts.split(' ');
  if (parts.length === 2) return `<span>${parts[0]}</span> <span style="color:var(--text)">${parts[1]}</span>`;
  return ts;
}

function typeLabel(type) {
  const labels = {
    WITHDRAW: 'Withdraw',
    BALANCE_CHECK: 'Balance Check',
    PIN_CHANGE: 'PIN Change',
    FAILED_PIN_ATTEMPT: 'Failed PIN'
  };
  return labels[type] || type;
}

// ── Render ATM Cards ───────────────────────────────────────────────────────

function renderATMs(atms) {
  const grid = document.getElementById('atm-grid');
  const alerts = [];

  // Build cards
  const cards = atms.map(atm => {
    const level = cashLevel(atm.cashBalance);
    const pct   = cashPercent(atm.cashBalance);
    const isSus = atm.suspicious;

    if (isSus) {
      alerts.push(atm);
    }

    return `
      <div class="atm-card ${isSus ? 'suspicious' : ''}">
        <div class="card-top">
          <div>
            <div class="card-id">${atm.id}</div>
            <div class="card-location">${atm.location}</div>
          </div>
          ${isSus ? `<div class="suspicious-badge">⚠ Suspicious</div>` : ''}
        </div>

        <div class="card-balance-section">
          <div class="balance-label">Cash Balance</div>
          <div class="balance-amount">
            <span class="balance-symbol">₹</span>${Number(atm.cashBalance).toLocaleString('en-IN')}
          </div>
          <div class="cash-bar-wrap">
            <div class="cash-bar-meta">
              <span>${cashStatusLabel(level)}</span>
              <span>${pct}% of max</span>
            </div>
            <div class="cash-bar-track">
              <div class="cash-bar-fill ${level}" style="width:${pct}%"></div>
            </div>
            <span class="cash-status-chip ${level}">${cashStatusLabel(level)} Cash Level</span>
          </div>
        </div>

        <div class="card-footer">
          <div class="failed-pin">
            Failed PINs: <span>${atm.failedPinAttempts}</span>
          </div>
          <div style="font-size:0.62rem;color:var(--text-dim);">
            ${isSus ? '🔴 Under Investigation' : '🟢 Operational'}
          </div>
        </div>
      </div>
    `;
  });

  grid.innerHTML = cards.join('');

  // Update alert section
  const alertSection = document.getElementById('alert-section');
  const alertList    = document.getElementById('alert-list');
  const statAlerts   = document.querySelector('#stat-alerts .stat-num');

  if (alerts.length > 0) {
    alertSection.style.display = 'block';
    alertList.innerHTML = alerts.map(a => `
      <div class="alert-item">
        ⚠ <strong>${a.id}</strong> (${a.location}) — ${a.failedPinAttempts} failed PIN attempts detected.
        Immediate investigation recommended.
      </div>
    `).join('');
  } else {
    alertSection.style.display = 'none';
  }

  statAlerts.textContent = alerts.length;
}

// ── Render Transactions ────────────────────────────────────────────────────

function renderTransactions(txs) {
  const tbody  = document.getElementById('tx-body');
  const badge  = document.getElementById('tx-count');
  const statTx = document.querySelector('#stat-total .stat-num');

  badge.textContent = `${txs.length} recent`;
  statTx.textContent = txs.length;

  const displayed = txs.slice(0, 20);
  const newSet = new Set(displayed.map(t => t.timestamp + t.atmId + t.type));

  const rows = displayed.map(tx => {
    const isNew = !previousTxTimestamps.has(tx.timestamp + tx.atmId + tx.type);
    const amtStr = tx.amount > 0
      ? `<span class="tx-amount">${formatRupees(tx.amount)}</span>`
      : `<span class="tx-amount zero">—</span>`;

    return `
      <tr class="${isNew ? 'new-row' : ''}">
        <td class="col-ts">${formatTimestamp(tx.timestamp)}</td>
        <td><span class="atm-chip">${tx.atmId}</span></td>
        <td><span class="tx-type ${tx.type}">${typeLabel(tx.type)}</span></td>
        <td>${amtStr}</td>
        <td><span class="tx-status ${tx.status}">${tx.status === 'SUCCESS' ? '✓' : '✗'} ${tx.status}</span></td>
      </tr>
    `;
  });

  tbody.innerHTML = rows.length > 0 ? rows.join('') : '<tr><td colspan="5" class="loading-row">No transactions yet…</td></tr>';
  previousTxTimestamps = newSet;
}

// ── Fetch & Refresh ────────────────────────────────────────────────────────

async function fetchData() {
  try {
    const [atmRes, txRes] = await Promise.all([
      fetch(API_ATMS),
      fetch(API_TRANSACTIONS)
    ]);

    if (!atmRes.ok || !txRes.ok) throw new Error('API error');

    const atms = await atmRes.json();
    const txs  = await txRes.json();

    renderATMs(atms);
    renderTransactions(txs);

    document.getElementById('last-update').textContent =
      'Last updated: ' + new Date().toLocaleTimeString('en-IN');

  } catch (err) {
    console.error('Fetch failed:', err);
    document.getElementById('last-update').textContent = '⚠ Connection error — retrying…';
  }
}

// ── Countdown timer ────────────────────────────────────────────────────────

function startCountdown() {
  const el = document.getElementById('countdown');
  countdownVal = REFRESH_INTERVAL;
  el.textContent = countdownVal + 's';

  clearInterval(countdownTimer);
  countdownTimer = setInterval(() => {
    countdownVal--;
    el.textContent = countdownVal + 's';
    if (countdownVal <= 0) {
      countdownVal = REFRESH_INTERVAL;
    }
  }, 1000);
}

// ── Init ───────────────────────────────────────────────────────────────────

async function init() {
  await fetchData();
  startCountdown();

  refreshTimer = setInterval(async () => {
    await fetchData();
    startCountdown();
  }, REFRESH_INTERVAL * 1000);
}

document.addEventListener('DOMContentLoaded', init);
