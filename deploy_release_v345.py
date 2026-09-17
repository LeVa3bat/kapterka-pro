import shutil
import os
import re

apk_src = "app/build/outputs/apk/release/app-release.apk"
if not os.path.exists(apk_src):
    print("Error: apk not found at", apk_src)
    exit(1)

# Destination paths
dest_versioned = "docs/kapterka-pro-v3.4.5-b23.apk"
dest_main = "docs/kapterka-pro.apk"
dest_release = "docs/kapterka-release.apk"
dest_kapterka = "docs/kapterka.apk"

shutil.copy2(apk_src, dest_versioned)
shutil.copy2(apk_src, dest_main)
shutil.copy2(apk_src, dest_release)
shutil.copy2(apk_src, dest_kapterka)
shutil.copy2(apk_src, "Kapterka-debug.apk")

file_size = os.path.getsize(dest_versioned)
size_mb = file_size / (1024 * 1024)
print(f"APK files copied successfully! Size: {size_mb:.2f} MB ({file_size} bytes)")

# Update docs/index.html
with open("docs/index.html", "r", encoding="utf-8") as f:
    html = f.read()

# Replace versions
html = html.replace("v=3.4.4.22", "v=3.4.5.23")
html = html.replace("Версия: 3.4.4 PRO", "Версия: 3.4.5 PRO")
html = html.replace("kapterka-pro-v3.4.4-b22.apk", "kapterka-pro-v3.4.5-b23.apk")

# Card replacement
old_card_pattern = r'<!-- Блок "Что нового в версии 3\.4\.[0-9]" -->.*?</div>\s*</div>\s*</section>'

new_card = """<!-- Блок "Что нового в версии 3.4.5" -->
        <div style="max-width: 780px; margin: 32px auto 0; background: var(--bg-card); border: 1px solid rgba(141,170,89,0.25); border-radius: 12px; padding: 24px; box-shadow: 0 8px 32px rgba(0,0,0,0.35);">
          <div style="display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:10px; margin-bottom:16px; border-bottom:1px solid rgba(255,255,255,0.08); padding-bottom:12px;">
            <div style="display:flex; align-items:center; gap:10px;">
              <span style="font-size:1.4rem;">⚡</span>
              <h3 style="font-size:1.25rem; margin:0; color:var(--text-primary); font-weight:700;">Что нового в версии 3.4.5 (Сборка 23)</h3>
            </div>
            <span style="background:rgba(141,170,89,0.15); color:var(--primary-bright); font-size:0.8rem; padding:4px 10px; border-radius:6px; font-weight:700; border:1px solid rgba(141,170,89,0.3);">
              АКТУАЛЬНЫЙ РЕЛИЗ (17 СЕНТЯБРЯ 2026)
            </span>
          </div>

          <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(280px, 1fr)); gap:16px;">
            <div style="background:rgba(255,255,255,0.02); border:1px solid rgba(255,255,255,0.06); border-radius:8px; padding:14px;">
              <div style="font-weight:700; color:var(--primary-bright); font-size:0.92rem; margin-bottom:6px; display:flex; align-items:center; gap:6px;">
                🔄 Мгновенная синхронизация остатков
              </div>
              <div style="font-size:0.85rem; color:var(--text-secondary); line-height:1.45;">
                При проведении прихода, выдачи или списания на одном телефоне остатки мгновенно и без задержек обновляются на всех подключённых смартфонах подразделения.
              </div>
            </div>

            <div style="background:rgba(255,255,255,0.02); border:1px solid rgba(255,255,255,0.06); border-radius:8px; padding:14px;">
              <div style="font-weight:700; color:var(--accent-gold); font-size:0.92rem; margin-bottom:6px; display:flex; align-items:center; gap:6px;">
                📋 Отображение имущества на складах
              </div>
              <div style="font-size:0.85rem; color:var(--text-secondary); line-height:1.45;">
                Устранена скрытая фильтрация: новые позиции из облака автоматически распознаются локальной базой и сразу выводятся в списки складов и карточки остатков.
              </div>
            </div>

            <div style="background:rgba(255,255,255,0.02); border:1px solid rgba(255,255,255,0.06); border-radius:8px; padding:14px;">
              <div style="font-weight:700; color:#5bc0be; font-size:0.92rem; margin-bottom:6px; display:flex; align-items:center; gap:6px;">
                📦 Компактная чистая сборка 17.9 МБ
              </div>
              <div style="font-size:0.85rem; color:var(--text-secondary); line-height:1.45;">
                Размер APK оптимизирован до 17.9 МБ — исключены отладочные инструменты, обеспечена высокая скорость скачивания даже при слабом фронтовом интернете.
              </div>
            </div>

            <div style="background:rgba(255,255,255,0.02); border:1px solid rgba(255,255,255,0.06); border-radius:8px; padding:14px;">
              <div style="font-weight:700; color:#29b6f6; font-size:0.92rem; margin-bottom:6px; display:flex; align-items:center; gap:6px;">
                🗑️ Каскадная зачистка и защита базы
              </div>
              <div style="font-size:0.85rem; color:var(--text-secondary); line-height:1.45;">
                Синхронное удаление точек учёта у всех бойцов, зачистка облачных остатков и сохранение ключа подразделения в надёжном защищённом сейфе.
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>"""

match = re.search(old_card_pattern, html, re.DOTALL)
if match:
    html = html[:match.start()] + new_card + html[match.end():]
    print("Release card replaced successfully!")
else:
    print("Warning: Card pattern not matched, looking for custom replacement...")

# Also update admin panel subject and text
admin_subject_old = r'value="\[Каптёрка Про\] Вышло обновление v3\.4\.[0-9].*?"'
admin_subject_new = 'value="[Каптёрка Про] Вышло обновление v3.4.5 (Сборка 23): Полная синхронизация остатков и данных"'
html = re.sub(admin_subject_old, admin_subject_new, html)

# Write updated HTML
with open("docs/index.html", "w", encoding="utf-8") as f:
    f.write(html)

print("docs/index.html successfully updated to v3.4.5 (Build 23)!")
