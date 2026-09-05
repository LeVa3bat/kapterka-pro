import re

with open('docs/index.html', 'r', encoding='utf-8') as f:
    content = f.read()

old_features = """        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon-box">📦</div>
            <h3>Многоскладской учет и точки</h3>
            <p>Неограниченное число точек хранения: Базовый склад, Рота, ВОП «Север», СП «Высота», Автопарк, склад РАВ и Вещевой службы.</p>
            <span class="feature-tag">Батальон / Рота / Взвод</span>
          </div>"""

new_features = """        <details style="background: rgba(0,0,0,0.2); border: 1px solid var(--color-border); border-radius: 8px; overflow: hidden; margin-top: 32px;">
          <summary style="cursor: pointer; padding: 18px 24px; font-weight: bold; font-size: 1.1rem; color: #fff; display: flex; align-items: center; background: rgba(255,255,255,0.03);">
            <span style="margin-right: 12px; font-size: 1.4rem;">⚙️</span> Развернуть список функций (Функционал МТО)
          </summary>
          <div style="padding: 24px; border-top: 1px solid var(--color-border);">
            <div class="features-grid">
              <div class="feature-card">
                <div class="feature-icon-box">📦</div>
                <h3>Многоскладской учет и точки</h3>
                <p>Неограниченное число точек хранения: Базовый склад, Рота, ВОП «Север», СП «Высота», Автопарк, склад РАВ и Вещевой службы.</p>
                <span class="feature-tag">Батальон / Рота / Взвод</span>
              </div>"""

content = content.replace(old_features, new_features)
content = content.replace("          </div>\n        </div>\n      </div>\n    </section>\n\n    <!-- Security Whitepaper Section (OPSEC) -->", "          </div>\n            </div>\n          </div>\n        </details>\n      </div>\n    </section>\n\n    <!-- Security Whitepaper Section (OPSEC) -->")

with open('docs/index.html', 'w', encoding='utf-8') as f:
    f.write(content)
