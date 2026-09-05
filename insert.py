import sys

with open("docs/index.html", "r", encoding="utf-8") as f:
    lines = f.readlines()

new_content = """
    <!-- Security Whitepaper Section (OPSEC) -->
    <section class="section security-section" id="security" style="background-color: var(--color-surface-elevated); border-top: 1px solid var(--color-border); border-bottom: 1px solid var(--color-border); padding: 80px 0;">
      <div class="container">
        <div class="section-header">
          <div class="badge" style="background: rgba(255, 60, 60, 0.15); color: #ff5555; border: 1px solid rgba(255, 60, 60, 0.3);">Архитектура безопасности (OPSEC)</div>
          <h2 style="font-family: monospace;">Безопасность фронтового уровня</h2>
          <p style="color: var(--color-text-secondary); max-width: 800px; margin: 0 auto; font-family: monospace; font-size: 0.95rem; line-height: 1.6;">
            Техническое обоснование безопасности использования программного комплекса «Каптёрка ПРО» в условиях активного радиоэлектронного подавления и перехвата (РЭБ / РЭР). Продукт спроектирован с учетом требований информационной безопасности (ИБ) для работы на линии боевого соприкосновения.
          </p>
        </div>
        
        <div class="security-grid" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 24px; margin-top: 48px; font-family: monospace;">
          
          <div class="security-card" style="background: var(--color-surface); padding: 28px; border-radius: 8px; border-left: 4px solid #ff5555; box-shadow: 0 8px 24px rgba(0,0,0,0.4); text-align: left;">
            <div style="font-weight: bold; font-size: 1.15rem; margin-bottom: 12px; color: #fff; display: flex; align-items: center;">
              <span style="font-size: 1.4rem; margin-right: 12px;">🚫</span> 1. Нулевая телеметрия и геолокация
            </div>
            <p style="color: #aaa; font-size: 0.9rem; line-height: 1.6; margin: 0;">Приложение не запрашивает и не использует API геолокации Android (GPS/ГЛОНАСС/Network). В манифесте приложения (AndroidManifest.xml) полностью отсутствуют разрешения ACCESS_FINE_LOCATION и ACCESS_COARSE_LOCATION. Сторонние аналитические трекеры (Yandex AppMetrica, Google Analytics) аппаратно исключены из сборки. Отслеживание перемещений устройства исключено на уровне ОС.</p>
          </div>

          <div class="security-card" style="background: var(--color-surface); padding: 28px; border-radius: 8px; border-left: 4px solid var(--color-primary); box-shadow: 0 8px 24px rgba(0,0,0,0.4); text-align: left;">
            <div style="font-weight: bold; font-size: 1.15rem; margin-bottom: 12px; color: #fff; display: flex; align-items: center;">
              <span style="font-size: 1.4rem; margin-right: 12px;">🔒</span> 2. Изолированная локальная БД
            </div>
            <p style="color: #aaa; font-size: 0.9rem; line-height: 1.6; margin: 0;">Весь массив учетных данных (остатки БК, экипировка, списки бойцов) сохраняется исключительно в изолированном хранилище устройства (App-Specific Storage) в формате SQLite (Jetpack Room). Файлы базы данных недоступны другим приложениям без наличия Root-прав. Архитектура построена на парадигме 100% Offline-First.</p>
          </div>

          <div class="security-card" style="background: var(--color-surface); padding: 28px; border-radius: 8px; border-left: 4px solid var(--color-primary-gold); box-shadow: 0 8px 24px rgba(0,0,0,0.4); text-align: left;">
            <div style="font-weight: bold; font-size: 1.15rem; margin-bottom: 12px; color: #fff; display: flex; align-items: center;">
              <span style="font-size: 1.4rem; margin-right: 12px;">📴</span> 3. Режим радиотишины (No Push)
            </div>
            <p style="color: #aaa; font-size: 0.9rem; line-height: 1.6; margin: 0;">Синхронизация остатков склада между бойцами подразделения происходит исключительно по ручной инициативе пользователя. Приложение не содержит фоновых сервисов для поддержания постоянного соединения (WebSocket, Push-уведомления). Это предотвращает случайное излучение радиомодуля смартфона (режим полного радиомолчания).</p>
          </div>

          <div class="security-card" style="background: var(--color-surface); padding: 28px; border-radius: 8px; border-left: 4px solid #4a90e2; box-shadow: 0 8px 24px rgba(0,0,0,0.4); text-align: left;">
            <div style="font-weight: bold; font-size: 1.15rem; margin-bottom: 12px; color: #fff; display: flex; align-items: center;">
              <span style="font-size: 1.4rem; margin-right: 12px;">🔑</span> 4. Оффлайн-Активация (Air-Gapped)
            </div>
            <p style="color: #aaa; font-size: 0.9rem; line-height: 1.6; margin: 0;">Активация военного ключа (KAPT / KPT) производится через криптографическую проверку контрольной суммы непосредственно на устройстве. При отсутствии сети приложение валидирует математическую подпись ключа и открывает PRO-доступ оффлайн. Соединение с биллинговыми серверами не является обязательным.</p>
          </div>

        </div>
      </div>
    </section>
"""

# Insert after line 220
lines.insert(220, new_content)

with open("docs/index.html", "w", encoding="utf-8") as f:
    f.writelines(lines)
