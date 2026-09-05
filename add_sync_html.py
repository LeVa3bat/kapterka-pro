import re

with open('docs/index.html', 'r', encoding='utf-8') as f:
    content = f.read()

sync_block = """    <!-- Sync Mechanism Section -->
    <section class="section sync-mech-section" id="sync-mech" style="background-color: var(--color-bg); padding: 0 0 40px 0;">
      <div class="container">
        <details style="background: rgba(0,0,0,0.2); border: 1px solid var(--color-border); border-radius: 8px; overflow: hidden;">
          <summary style="cursor: pointer; padding: 18px 24px; font-weight: bold; font-size: 1.1rem; color: #fff; display: flex; align-items: center; background: rgba(255,255,255,0.03);">
            <span style="margin-right: 12px; font-size: 1.4rem;">🔗</span> Механизм облачной синхронизации и Ключ подразделения
          </summary>
          <div style="padding: 24px; border-top: 1px solid var(--color-border);">
            <div style="margin-bottom: 24px;">
              <h3 style="color: var(--color-primary-gold); margin-bottom: 12px; font-size: 1.2rem;">Как работает единая сеть (P2P-хаб)?</h3>
              <p style="color: var(--color-text-secondary); line-height: 1.6; font-size: 0.95rem;">Ядро системы — это уникальный <strong>Ключ подразделения</strong>. Он работает как пароль от изолированного облачного контейнера, в котором хранятся данные только вашей роты, взвода или батальона.</p>
            </div>
            
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px;">
              <div style="background: var(--color-surface); padding: 24px; border-radius: 8px; border-top: 3px solid var(--color-primary);">
                <div style="font-weight: bold; margin-bottom: 12px; color: #fff; font-size: 1.05rem;">1. Создание ключа (Командир)</div>
                <p style="color: #aaa; font-size: 0.9rem; line-height: 1.6; margin:0;">При первой регистрации программа автоматически генерирует уникальный 6-значный код (например, <code>kapt_b5e9f1</code>). Командир может заменить его на свой (например, <code>ROTA_1_VKS</code>). Этот ключ навсегда привязывается к профилю в настройках.</p>
              </div>

              <div style="background: var(--color-surface); padding: 24px; border-radius: 8px; border-top: 3px solid var(--color-primary);">
                <div style="font-weight: bold; margin-bottom: 12px; color: #fff; font-size: 1.05rem;">2. Подключение бойцов</div>
                <p style="color: #aaa; font-size: 0.9rem; line-height: 1.6; margin:0;">Второй боец при установке приложения выбирает вкладку «Войти в подразделение» и вписывает <strong>точно такой же ключ</strong>. Программа валидирует ключ и связывает устройства в единую защищенную сеть.</p>
              </div>

              <div style="background: var(--color-surface); padding: 24px; border-radius: 8px; border-top: 3px solid var(--color-primary);">
                <div style="font-weight: bold; margin-bottom: 12px; color: #fff; font-size: 1.05rem;">3. Облачный хаб (Firebase)</div>
                <p style="color: #aaa; font-size: 0.9rem; line-height: 1.6; margin:0;">При наличии связи приложение обращается к базе данных Google Firebase по вашему ключу. Оно скачивает измененные остатки/заявки от других бойцов и загружает ваши проведенные операции. Вся группа видит общую картину.</p>
              </div>

              <div style="background: var(--color-surface); padding: 24px; border-radius: 8px; border-top: 3px solid var(--color-primary);">
                <div style="font-weight: bold; margin-bottom: 12px; color: #fff; font-size: 1.05rem;">4. Локальный Offline-режим</div>
                <p style="color: #aaa; font-size: 0.9rem; line-height: 1.6; margin:0;">Если интернета нет из-за работы РЭБ или нахождения в бункере, система переключается на внутреннюю базу SQLite (Room DB). Вы продолжаете выдавать имущество, а программа выгрузит данные в облако, как только появится сеть.</p>
              </div>
            </div>
          </div>
        </details>
      </div>
    </section>

"""

if "Механизм облачной синхронизации и Ключ подразделения" not in content:
    target_str = "<!-- Tactical 3D Screenshots Showcase (100% Real Android Screens) -->"
    content = content.replace(target_str, sync_block + target_str)
    
    with open('docs/index.html', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Added sync block successfully.")
else:
    print("Sync block already exists.")
