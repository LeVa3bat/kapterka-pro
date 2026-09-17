with open("docs/index.html", "r", encoding="utf-8") as f:
    text = f.read()

old_card = """        <!-- Блок "Что нового в версии 3.4.3" -->
        <div style="max-width: 780px; margin: 32px auto 0; background: var(--bg-card); border: 1px solid rgba(141,170,89,0.25); border-radius: 12px; padding: 24px; box-shadow: 0 8px 32px rgba(0,0,0,0.35);">
          <div style="display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:10px; margin-bottom:16px; border-bottom:1px solid rgba(255,255,255,0.08); padding-bottom:12px;">
            <div style="display:flex; align-items:center; gap:10px;">
              <span style="font-size:1.4rem;">📦</span>
              <h3 style="font-size:1.25rem; margin:0; color:var(--text-primary); font-weight:700;">Что нового в версии 3.4.3 (Сборка 21)</h3>
            </div>
            <span style="background:rgba(141,170,89,0.15); color:var(--primary-bright); font-size:0.8rem; padding:4px 10px; border-radius:6px; font-weight:700; border:1px solid rgba(141,170,89,0.3);">
              АКТУАЛЬНЫЙ РЕЛИЗ
            </span>
          </div>"""

new_card = """        <!-- Блок "Что нового в версии 3.4.4" -->
        <div style="max-width: 780px; margin: 32px auto 0; background: var(--bg-card); border: 1px solid rgba(141,170,89,0.25); border-radius: 12px; padding: 24px; box-shadow: 0 8px 32px rgba(0,0,0,0.35);">
          <div style="display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:10px; margin-bottom:16px; border-bottom:1px solid rgba(255,255,255,0.08); padding-bottom:12px;">
            <div style="display:flex; align-items:center; gap:10px;">
              <span style="font-size:1.4rem;">⚡</span>
              <h3 style="font-size:1.25rem; margin:0; color:var(--text-primary); font-weight:700;">Что нового в версии 3.4.4 (Сборка 22)</h3>
            </div>
            <span style="background:rgba(141,170,89,0.15); color:var(--primary-bright); font-size:0.8rem; padding:4px 10px; border-radius:6px; font-weight:700; border:1px solid rgba(141,170,89,0.3);">
              АКТУАЛЬНЫЙ РЕЛИЗ (17 СЕНТЯБРЯ 2026)
            </span>
          </div>

          <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(280px, 1fr)); gap:16px;">
            <div style="background:rgba(255,255,255,0.02); border:1px solid rgba(255,255,255,0.06); border-radius:8px; padding:14px;">
              <div style="font-weight:700; color:var(--primary-bright); font-size:0.92rem; margin-bottom:6px; display:flex; align-items:center; gap:6px;">
                🗑️ Мгновенное удаление точек учёта
              </div>
              <div style="font-size:0.85rem; color:var(--text-secondary); line-height:1.45;">
                При удалении передовой точки или склада на одном смартфоне она мгновенно исчезает у всех бойцов роты в режиме реального времени.
              </div>
            </div>

            <div style="background:rgba(255,255,255,0.02); border:1px solid rgba(255,255,255,0.06); border-radius:8px; padding:14px;">
              <div style="font-weight:700; color:var(--accent-gold); font-size:0.92rem; margin-bottom:6px; display:flex; align-items:center; gap:6px;">
                📦 Полная зачистка облачных остатков
              </div>
              <div style="font-size:0.85rem; color:var(--text-secondary); line-height:1.45;">
                Остатки удалённой точки безвозвратно удаляются как из локальной базы Room, так и из облака Firestore. Никаких «зависших» килограммов и штук.
              </div>
            </div>

            <div style="background:rgba(255,255,255,0.02); border:1px solid rgba(255,255,255,0.06); border-radius:8px; padding:14px;">
              <div style="font-weight:700; color:#5bc0be; font-size:0.92rem; margin-bottom:6px; display:flex; align-items:center; gap:6px;">
                🔄 Исправлена кнопка синхронизации
              </div>
              <div style="font-size:0.85rem; color:var(--text-secondary); line-height:1.45;">
                Удалённые точки больше никогда не «воскресают» и не появляются заново при ручной сверке базы. Корневой базовый склад надёжно защищён.
              </div>
            </div>

            <div style="background:rgba(255,255,255,0.02); border:1px solid rgba(255,255,255,0.06); border-radius:8px; padding:14px;">
              <div style="font-weight:700; color:#29b6f6; font-size:0.92rem; margin-bottom:6px; display:flex; align-items:center; gap:6px;">
                📡 Оффлайн-сейф и автосинхронизация
              </div>
              <div style="font-size:0.85rem; color:var(--text-secondary); line-height:1.45;">
                Ключ подразделения сохраняется в постоянном защищённом сейфе устройства — при очистке кэша или сбоях связи доступ к складу не теряется.
              </div>
            </div>
          </div>"""

# Replace the whole release section
import re
pattern = r'<!-- Блок "Что нового в версии 3\.4\.3" -->.*?<!-- Footer -->'
# Let's match from <!-- Блок "Что нового в версии 3.4.3" --> up to </div>\s*</div>\s*</section>
match = re.search(r'<!-- Блок "Что нового в версии 3\.4\.3" -->.*?</div>\s*</div>\s*</section>', text, re.DOTALL)
if match:
    replacement = new_card + "\n        </div>\n      </div>\n    </section>"
    text = text[:match.start()] + replacement + text[match.end():]
    with open("docs/index.html", "w", encoding="utf-8") as f:
        f.write(text)
    print("Release section successfully patched in docs/index.html!")
else:
    print("Pattern match not found!")
