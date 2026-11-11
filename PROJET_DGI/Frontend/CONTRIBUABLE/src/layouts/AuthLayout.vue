<template>
  <div class="login-page">
    <!-- ========== Panneau gauche : identité DGI + bâtiment (nuit) ========== -->
    <section class="visual-panel">
      <div class="glow" aria-hidden="true"></div>

      <div class="dgi-brand">
        <div class="logo-box">DGI</div>
        <div class="brand-title">
          <span class="main-title">DIRECTION GÉNÉRALE</span>
          <span class="sub-title">DES IMPÔTS</span>
        </div>
      </div>

      <div class="hero-text">
        <!-- Contenu dynamique (Textes et puces) -->
        <slot name="hero"></slot>
      </div>

      <!-- Illustration : bâtiment DGI + drapeau de Madagascar -->
      <svg class="building" viewBox="0 0 560 320" preserveAspectRatio="xMidYMax meet" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <pattern id="dgi-windows" width="40" height="52" patternUnits="userSpaceOnUse">
            <rect x="9" y="12" width="22" height="30" rx="2" fill="#bcd8ff"/>
          </pattern>
        </defs>
        <rect x="-3000" y="262" width="6560" height="58" fill="#0a2347"/>
        <rect x="40" y="156" width="160" height="104" fill="#17407a"/>
        <rect x="40" y="156" width="160" height="104" fill="url(#dgi-windows)"/>
        <rect x="360" y="156" width="160" height="104" fill="#17407a"/>
        <rect x="360" y="156" width="160" height="104" fill="url(#dgi-windows)"/>
        <rect x="34" y="146" width="172" height="10" fill="#2a5ba0"/>
        <rect x="354" y="146" width="172" height="10" fill="#2a5ba0"/>
        <rect x="200" y="110" width="160" height="150" fill="#1f4d8f"/>
        <polygon points="188,110 372,110 280,60" fill="#2a5ba0"/>
        <rect x="188" y="110" width="184" height="10" fill="#3a6fb8"/>
        <circle cx="280" cy="92" r="9" fill="#cfe3ff"/>
        <rect x="212" y="120" width="12" height="130" fill="#9cc4ff"/>
        <rect x="240" y="120" width="12" height="130" fill="#9cc4ff"/>
        <rect x="308" y="120" width="12" height="130" fill="#9cc4ff"/>
        <rect x="336" y="120" width="12" height="130" fill="#9cc4ff"/>
        <rect x="266" y="196" width="28" height="54" rx="2" fill="#071a33"/>
        <rect x="176" y="250" width="208" height="12" fill="#12336a"/>
        <line x1="280" y1="60" x2="280" y2="14" stroke="#6b7a8c" stroke-width="2"/>
        <rect x="282" y="14" width="12" height="28" fill="#ffffff"/>
        <rect x="294" y="14" width="32" height="14" fill="#fc3d32"/>
        <rect x="294" y="28" width="32" height="14" fill="#007e3a"/>
        <rect x="21" y="238" width="6" height="26" fill="#12336a"/>
        <circle cx="24" cy="226" r="24" fill="#1a4a85"/>
        <rect x="533" y="238" width="6" height="26" fill="#12336a"/>
        <circle cx="536" cy="226" r="24" fill="#1a4a85"/>
      </svg>

      <!-- Bord courbe vers le panneau blanc -->
      <svg class="edge" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
        <path d="M0 0 C 55 28, 55 72, 0 100 L100 100 L100 0 Z" fill="#ffffff"/>
        <path d="M0 0 C 55 28, 55 72, 0 100" fill="none" stroke="#3a6fb8" stroke-width="2" vector-effect="non-scaling-stroke"/>
      </svg>
    </section>

    <!-- ========== Panneau droit : formulaire dynamique ========== -->
    <section class="form-panel">
      <div class="form-inner">
        <!-- Formulaire de login ou d'inscription -->
        <slot></slot>
      </div>
    </section>
  </div>
</template>

<style>
/* Les sélecteurs préfixés par .login-page l'emportent sur Bootstrap (.card-title, .card-footer, .form-label...) */
/* CSS Commun - Ne pas mettre "scoped" pour qu'il s'applique aux composants enfants */
.login-page { display: flex; min-height: 100vh; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #ffffff; }
.visual-panel { position: relative; flex: 1 1 58%; min-width: 0; overflow: hidden; background: radial-gradient(ellipse at 30% 0%, #0f3f7f 0%, #0a2a55 50%, #06152f 100%); }
.visual-panel::before { content: ""; position: absolute; inset: 0; background-image: radial-gradient(rgba(156, 196, 255, 0.16) 1px, transparent 1.4px); background-size: 26px 26px; -webkit-mask-image: linear-gradient(180deg, #000 0%, transparent 70%); mask-image: linear-gradient(180deg, #000 0%, transparent 70%); }
.glow { position: absolute; width: 520px; height: 520px; top: -200px; right: -140px; border-radius: 50%; background: radial-gradient(circle, rgba(59, 130, 246, 0.35) 0%, transparent 65%); pointer-events: none; }
.dgi-brand { position: absolute; top: 40px; left: 48px; z-index: 2; display: flex; align-items: center; gap: 12px; }
.logo-box { background: #1f5fd6; color: #ffffff; font-weight: 800; padding: 10px 15px; border-radius: 10px; font-size: 18px; letter-spacing: 0.5px; box-shadow: 0 6px 18px rgba(31, 95, 214, 0.45); }
.brand-title { display: flex; flex-direction: column; color: #ffffff; font-size: 13px; font-weight: 800; line-height: 1.25; }
.main-title { font-size: 14px; letter-spacing: 0.5px; }
.sub-title { color: #9cc4ff; }
.hero-text { position: absolute; top: 138px; left: 48px; right: 80px; z-index: 2; }
.eyebrow { display: inline-block; margin-bottom: 14px; padding: 5px 12px; border: 1px solid rgba(156, 196, 255, 0.4); border-radius: 999px; font-size: 11px; font-weight: 700; letter-spacing: 2px; color: #9cc4ff; }
.hero-title { margin: 0 0 12px 0; font-size: 34px; line-height: 1.15; font-weight: 700; color: #ffffff; }
.hero-title em { font-style: normal; color: #9cc4ff; }
.hero-subtitle { margin: 0; max-width: 420px; font-size: 15px; line-height: 1.55; color: #b8cdea; }
.chips { list-style: none; margin: 24px 0 0; padding: 0; display: flex; flex-wrap: wrap; gap: 10px; }
.chips li { display: inline-flex; align-items: center; gap: 8px; padding: 8px 14px; border-radius: 999px; font-size: 13px; font-weight: 600; color: #e6f0ff; background: rgba(255, 255, 255, 0.08); border: 1px solid rgba(156, 196, 255, 0.22); backdrop-filter: blur(4px); }
.chips svg { color: #7fb0ff; }
.building { position: absolute; left: 0; bottom: 0; width: 100%; height: 42%; overflow: visible; }
.edge { position: absolute; top: 0; right: -1px; width: 72px; height: 100%; z-index: 3; display: block; }
.form-panel { flex: 1 1 42%; min-width: 0; display: flex; align-items: center; justify-content: center; padding: 40px 32px; box-sizing: border-box; background: #ffffff; }
.form-inner { width: 100%; max-width: 440px; }
.login-page .card-title { font-size: 30px; color: #0f172a; margin: 0 0 24px 0; font-weight: 700; }
.error-alert { background: #fef2f2; color: #dc2626; padding: 10px 14px; border-radius: 8px; font-size: 13px; margin-bottom: 18px; border: 1px solid #fecaca; }
.login-page .form-group { margin-bottom: 18px; }
.login-page .form-label { display: block; font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 8px; }
.form-input { width: 100%; padding: 14px 16px; border: 1px solid #cbd5e1; border-radius: 10px; font-size: 15px; outline: none; transition: border-color 0.2s, box-shadow 0.2s; box-sizing: border-box; }
.form-input:focus { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12); }
.password-wrapper { position: relative; }
.form-input-password { padding-right: 48px; }
.toggle-password { position: absolute; top: 50%; right: 4px; transform: translateY(-50%); width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; border: none; border-radius: 8px; background: transparent; color: #64748b; cursor: pointer; transition: color 0.2s, background 0.2s; }
.toggle-password:hover { color: #2563eb; background: #eff6ff; }
.toggle-password:focus-visible { outline: 2px solid #2563eb; outline-offset: 1px; }
.forgot-wrapper { text-align: right; margin-bottom: 22px; }
.forgot-link { font-size: 13px; color: #2563eb; text-decoration: none; font-weight: 600; }
.forgot-link:hover { text-decoration: underline; }
.submit-button { width: 100%; padding: 15px; background: #2563eb; color: #ffffff; border: none; border-radius: 10px; font-size: 16px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
.submit-button:hover { background: #1d4ed8; }
.submit-button:disabled { opacity: 0.7; cursor: not-allowed; }
.register-wrapper { margin-top: 22px; display: flex; flex-direction: column; gap: 14px; }
.register-divider { display: flex; align-items: center; gap: 12px; font-size: 13px; color: #64748b; }
.register-divider::before, .register-divider::after { content: ""; flex: 1; height: 1px; background: #e2e8f0; }
.register-button { display: block; box-sizing: border-box; width: 100%; padding: 12px; text-align: center; font-size: 14px; font-weight: 600; color: #2563eb; text-decoration: none; background: #ffffff; border: 1.5px solid #2563eb; border-radius: 10px; transition: background 0.2s; }
.register-button:hover { background: #eff6ff; }
.login-page .card-footer { background: transparent; margin-top: 22px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #f1f5f9; padding-top: 16px; }

@media (max-width: 900px) {
  .login-page { flex-direction: column; }
  .visual-panel { flex: none; padding: 28px 24px 28px; }
  .dgi-brand, .hero-text { position: static; }
  .hero-text { margin-top: 20px; }
  .hero-title { font-size: 24px; }
  .chips, .building, .edge, .glow { display: none; }
  .form-panel { padding: 28px 20px 40px; }
}
</style>