import re

with open('docs/index.html', 'r', encoding='utf-8') as f:
    content = f.read()

old_security = """<div class="security-grid" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 24px; margin-top: 48px; font-family: monospace;">"""

new_security = """<details style="margin-top: 24px; background: rgba(0,0,0,0.2); border: 1px solid var(--color-border); border-radius: 8px; overflow: hidden; font-family: monospace;">
          <summary style="cursor: pointer; padding: 18px 24px; font-weight: bold; font-size: 1.1rem; color: #fff; display: flex; align-items: center; background: rgba(255,255,255,0.03);">
            <span style="margin-right: 12px; font-size: 1.4rem;">🛡️</span> Развернуть техническое описание OPSEC
          </summary>
          <div style="padding: 24px; border-top: 1px solid var(--color-border);">
            <div class="security-grid" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px;">"""

content = content.replace(old_security, new_security)
content = content.replace("          </div>\n\n        </div>\n      </div>\n    </section>", "          </div>\n\n        </div>\n          </div>\n        </details>\n      </div>\n    </section>")

with open('docs/index.html', 'w', encoding='utf-8') as f:
    f.write(content)
