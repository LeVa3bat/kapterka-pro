package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenContainer
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalGoldText
import com.example.ui.theme.TacticalRedText
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceElevated
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTeal
import com.example.ui.theme.TacticalTealText
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary

data class GuideSection(
    val id: String,
    val title: String,
    val shortSubtitle: String,
    val icon: ImageVector,
    val badgeColor: Color,
    val forWho: String,
    val steps: List<GuideStep>,
    val proTip: String
)

data class GuideStep(
    val stepNumber: String,
    val stepTitle: String,
    val stepContent: String,
    val keyAction: String? = null
)

@Composable
fun UserManualDialog(
    onDismiss: () -> Unit,
    onOpenSyncDialog: (() -> Unit)? = null,
    profileMode: com.example.data.model.WorkspaceProfileMode = com.example.data.model.WorkspaceProfileMode.MILITARY
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val isMil = profileMode.isMilitary

    val sageBright = SageGreenBright
    val tealText = TacticalTealText
    val goldText = TacticalGoldText

    val guideSections = remember(sageBright, tealText, goldText, isMil) {
        if (!isMil) {
            listOf(
                GuideSection(
                    id = "quick_start",
                    title = "1. Первый запуск",
                    shortSubtitle = "Регистрация, почта, код общего склада",
                    icon = Icons.Default.Key,
                    badgeColor = sageBright,
                    forWho = "Все пользователи",
                    steps = listOf(
                        GuideStep("Шаг 1", "Регистрация", "Введите имя (или ФИО), название склада или мастерской и почту. Почта нужна для кода подключения и восстановления лицензии PRO.", "Нажмите «Создать учёт»"),
                        GuideStep("Шаг 2", "Код из письма", "На почту придёт 6-значный код подтверждения. Введите его в поля на экране. Если письма нет 2 минуты — проверьте папку «Спам».", "Код действует 10 минут"),
                        GuideStep("Шаг 3", "Код общего склада", "После регистрации создаётся код вида kapt_xxxx — он виден вверху главного экрана. По нему можно подключить второй телефон.", "Храните код в надёжном месте")
                    ),
                    proTip = "Если письма нет сразу, можно продолжить работу офлайн и подтвердить почту позже."
                ),
                GuideSection(
                    id = "warehouses_stock",
                    title = "2. Главный экран: склады и остатки",
                    shortSubtitle = "Позиции по единицам, поиск, категории",
                    icon = Icons.Default.Warehouse,
                    badgeColor = tealText,
                    forWho = "Кладовщики, мастера, руководители",
                    steps = listOf(
                        GuideStep("1", "Живая сводка сверху", "Показывает количество активных позиций в наличии и точный баланс раздельно по единицам измерения (шт., л., кг., компл.) без смешивания разных единиц.", null),
                        GuideStep("2", "Склады списком", "Нажмите на карточку склада — откроется таблица: наименование, приход, расход и остаток. Повторное нажатие свернёт список.", "Долгое нажатие — изменить порядок или удалить"),
                        GuideStep("3", "Поиск и категории", "Строка поиска ищет по названию и артикулу сразу по всем складам. Кнопки категорий быстро фильтруют список.", null),
                        GuideStep("4", "Инвентаризация", "Нажмите на строку в таблице остатков, чтобы скорректировать фактическое количество — правка зафиксируется в журнале.", null)
                    ),
                    proTip = "Базовый склад отмечен золотым значком — на него удобнее принимать новые поставки."
                ),
                GuideSection(
                    id = "operations",
                    title = "3. Операции",
                    shortSubtitle = "Приход, выдача, возврат, списание",
                    icon = Icons.Default.TrendingUp,
                    badgeColor = goldText,
                    forWho = "Все, кто ведёт склад",
                    steps = listOf(
                        GuideStep("Приход", "Принять товар / инструмент", "Кнопка «Приход» → выберите склад, поставщика и позиции, укажите количество кнопками − / +.", "Остаток обновится мгновенно"),
                        GuideStep("Выдача", "Выдать сотруднику или в цех", "Кнопка «Выдача» → укажите получателя и позиции. Приложение контролирует наличие и предупреждает о нехватке.", null),
                        GuideStep("Возврат", "Вернуть инструмент на склад", "Кнопка «Возврат» → выберите, кто сдаёт инструмент и на какой склад. Возврат отразится в журнале и ведомости МБ-7.", null),
                        GuideStep("Списание", "Расход, износ или поломка", "Кнопка «Списание» → укажите причину. Запись попадёт в Акт списания ТМЦ (МБ-8 / ТОРГ-16).", "Доступно в демо и PRO"),
                        GuideStep("Журнал", "Вкладка «Операции»", "Вся история движения по дням с поиском и фильтром по типам и категориям.", null)
                    ),
                    proTip = "Если ошиблись в количестве прихода — откройте остаток на складе и скорректируйте инвентаризацией."
                ),
                GuideSection(
                    id = "requests",
                    title = "4. Заявки",
                    shortSubtitle = "Запросы на комплектацию и выдачу",
                    icon = Icons.Default.Assignment,
                    badgeColor = sageBright,
                    forWho = "Мастера, прорабы, снабженцы",
                    steps = listOf(
                        GuideStep("1", "Создать заявку", "Вкладка «Заявки» → кнопка «+ Заявка» сверху: выберите склад, получателя и позиции. Если позиции нет в наличии, программа сразу предупредит.", null),
                        GuideStep("2", "Этапы работы", "Новые → Собираются → Собраны → Выданы. При финальной выдаче остаток автоматически списывается со склада.", null)
                    ),
                    proTip = "Заявки синхронизируются между всеми подключёнными телефонами команды."
                ),
                GuideSection(
                    id = "sync",
                    title = "5. Второй телефон и синхронизация",
                    shortSubtitle = "Общий склад на нескольких устройствах",
                    icon = Icons.Default.Sync,
                    badgeColor = tealText,
                    forWho = "Команды и сменная работа",
                    steps = listOf(
                        GuideStep("Шаг 1", "Подключить 2-й телефон", "Откройте «QR-код» на главном экране или в разделе «Ещё → Общий склад». На втором телефоне выберите «Подключиться» и отсканируйте QR.", null),
                        GuideStep("Шаг 2", "Работа офлайн", "Все операции работают без интернета в локальной базе. Как только появится сеть, изменения синхронизируются.", null),
                        GuideStep("Шаг 3", "Сверка при расхождении", "В окне «Общий склад» можно сделать один телефон эталоном или загрузить актуальное состояние из облака.", null)
                    ),
                    proTip = "Обязательно сохраняйте резервную копию в файл перед крупной инвентаризацией."
                ),
                GuideSection(
                    id = "reports_excel",
                    title = "6. Отчёты и бланки Excel",
                    shortSubtitle = "Оборотная ТМЦ, М-11, МБ-7, МБ-8",
                    icon = Icons.Default.TableChart,
                    badgeColor = goldText,
                    forWho = "Бухгалтерия, склад, руководитель",
                    steps = listOf(
                        GuideStep("1", "Оборотная ТМЦ и остатки", "Полный отчёт по всем складам и отдельно по каждому складу с приходом, расходом и остатком.", null),
                        GuideStep("2", "Накладная М-11 / ТОРГ-13", "Оформленные перемещения ТМЦ между складами и участками.", null),
                        GuideStep("3", "Ведомость МБ-7", "Выдача инструмента и спецодежды на руки сотрудникам с автоматическим учётом возвратов.", null),
                        GuideStep("4", "Акт списания МБ-8", "Акт на списание пришедшего в негодность инструмента и расходников.", "Выгрузка в .xlsx и CSV")
                    ),
                    proTip = "Файлы .xlsx готовы к печати на листе А4 в Excel, LibreOffice и Google Таблицах."
                ),
                GuideSection(
                    id = "license",
                    title = "7. Базовый режим, демо и PRO",
                    shortSubtitle = "3 дня безлимита, далее базовый режим или PRO",
                    icon = Icons.Default.Star,
                    badgeColor = sageBright,
                    forWho = "Все пользователи",
                    steps = listOf(
                        GuideStep("Демо", "3 дня полного доступа", "После регистрации все функции открыты на 72 часа. Далее доступен бесплатный локальный режим: 1 основной склад и до 15 позиций.", null),
                        GuideStep("Тарифы PRO", "30, 90 или 365 дней", "Откройте «Лицензия PRO» → выберите срок (490 ₽ / 1 190 ₽ / 3 490 ₽). Оплата через ЮKassa (СБП / МИР), ключ также придёт на Email.", null),
                        GuideStep("Восстановить", "При смене телефона", "«Ещё» → «Восстановить оплаченную лицензию по Email» — сервер автоматически найдёт вашу активную подписку.", null)
                    ),
                    proTip = "Лицензия хранится на сервере и не пропадает при переустановке приложения."
                ),
                GuideSection(
                    id = "faq",
                    title = "8. Частые вопросы",
                    shortSubtitle = "Письма, синхронизация, обновление",
                    icon = Icons.Default.HelpOutline,
                    badgeColor = tealText,
                    forWho = "Все пользователи",
                    steps = listOf(
                        GuideStep("?", "Не приходит письмо с кодом", "Проверьте «Спам» и «Промоакции» или нажмите «Отправить код снова». При трудностях напишите в бот поддержки @kapterka_help_bot.", null),
                        GuideStep("?", "Пропадут ли данные при обновлении?", "Нет. Все склады, остатки, журнал и лицензия сохраняются.", null),
                        GuideStep("?", "Где задать вопрос?", "Telegram-бот поддержки @kapterka_help_bot, новости — @kapterka_pro, сайт kapterka-pro.ru.", null)
                    ),
                    proTip = "Быстрые ссылки на сайт и поддержку доступны внизу этой инструкции и в разделе «Ещё»."
                )
            )
        } else listOf(
            GuideSection(
                id = "quick_start",
                title = "1. Первый запуск",
                shortSubtitle = "Регистрация, почта, ключ подразделения",
                icon = Icons.Default.Key,
                badgeColor = sageBright,
                forWho = "Все пользователи",
                steps = listOf(
                    GuideStep("Шаг 1", "Регистрация", "Введите позывной (или ФИО), название подразделения и почту. Почта нужна, чтобы прислать ключ подразделения и восстановить лицензию.", "Нажмите «Создать подразделение»"),
                    GuideStep("Шаг 2", "Код из письма", "На почту придёт письмо «Код подтверждения» с 6 цифрами (код есть и в теме письма). Введите их в шесть клеточек. Письма нет 2 минуты — проверьте «Спам» или нажмите «Отправить код снова».", "Код действует 10 минут"),
                    GuideStep("Шаг 3", "Ключ подразделения", "Сразу после регистрации у вас есть ключ вида kapt_xxxx — он виден вверху главного экрана и приходит в приветственном письме. По нему другие телефоны подключаются к вашим данным.", "Не публикуйте ключ в открытых чатах")
                ),
                proTip = "Если письмо долго не приходит, можно продолжить без подтверждения и подтвердить почту позже — учёт работает сразу."
            ),
            GuideSection(
                id = "warehouses_stock",
                title = "2. Главный экран: склады и остатки",
                shortSubtitle = "Таблица остатков, поиск, фильтр по службам",
                icon = Icons.Default.Warehouse,
                badgeColor = tealText,
                forWho = "Старшины, начальники складов",
                steps = listOf(
                    GuideStep("1", "Сводка сверху", "Три числа: сколько складов, сколько наименований на остатке и сколько единиц всего. Числа обновляются сами после каждой операции.", null),
                    GuideStep("2", "Склады списком", "Каждый склад — одна строка. Нажмите на неё, и раскроется таблица: наименование, приход, расход и остаток. Нажмите ещё раз — свернётся.", "Удерживайте карточку — выше, ниже, изменить, удалить"),
                    GuideStep("3", "Поиск и фильтр", "Строка поиска ищет по названию, коду и службе сразу во всех складах. Чипы «РАВ», «ГСМ», «Вещевая» и т. д. оставляют только нужную службу.", null),
                    GuideStep("4", "Исправить остаток", "В таблице нажмите на количество — можно поправить остаток после инвентаризации. Исправление попадает в журнал.", null)
                ),
                proTip = "Базовый склад отмечен золотым значком — на него удобнее всего принимать приход."
            ),
            GuideSection(
                id = "operations",
                title = "3. Операции",
                shortSubtitle = "Приход, выдача, перемещение, списание",
                icon = Icons.Default.TrendingUp,
                badgeColor = goldText,
                forWho = "Все, кто ведёт учёт",
                steps = listOf(
                    GuideStep("Приход", "Принять имущество", "Кнопка «Приход» → выберите склад, отправителя и позиции, количество — кнопками − / + или цифрами. Кнопка «Сохранить» всегда внизу окна.", "Остаток увеличится сразу"),
                    GuideStep("Выдача", "Выдать бойцу или подразделению", "Кнопка «Выдача» → кому выдаёте и что. Приложение не даст выдать больше, чем есть на складе.", null),
                    GuideStep("Перемещение", "Со склада на склад", "Кнопка «Перемещение» → откуда и куда. Общий остаток не меняется, меняется только место хранения.", null),
                    GuideStep("Списание", "Израсходовано или утрачено", "Кнопка «Списание» → укажите причину. Эти записи попадают в отчёт по форме 8.", "Доступно в демо и PRO"),
                    GuideStep("Журнал", "Вкладка «Операции»", "Все проводки по дням: кто, когда, откуда, куда и что. Есть поиск и фильтр по типу операции.", null)
                ),
                proTip = "Ошиблись в операции? Проведите обратную (например, приход на то же количество) — журнал останется честным."
            ),
            GuideSection(
                id = "requests",
                title = "4. Заявки",
                shortSubtitle = "Запрос имущества от подразделений",
                icon = Icons.Default.Assignment,
                badgeColor = sageBright,
                forWho = "Командиры взводов, старшины",
                steps = listOf(
                    GuideStep("1", "Создать заявку", "Вкладка «Заявки» → «Новая заявка»: склад, заявитель, позиции и комментарий.", null),
                    GuideStep("2", "Статусы", "Новая → Собрана → Выдана. Счётчик новых заявок виден на нижней панели.", "Нажмите на статус, чтобы перевести дальше")
                ),
                proTip = "Заявки видны всем телефонам подразделения — удобно собирать запросы от взводов."
            ),
            GuideSection(
                id = "sync",
                title = "5. Второй телефон и синхронизация",
                shortSubtitle = "Общие данные на нескольких телефонах",
                icon = Icons.Default.Sync,
                badgeColor = tealText,
                forWho = "Все, у кого больше одного телефона",
                steps = listOf(
                    GuideStep("Шаг 1", "Подключить телефон", "На втором телефоне при входе выберите «Вход по ключу» и вставьте ключ подразделения (kapt_…). Данные подтянутся автоматически.", null),
                    GuideStep("Шаг 2", "Проверить связь", "Вверху главного экрана кнопка «Подключить» показывает состояние: «Синхронизировано» — всё в порядке; красная надпись — нет связи с сервером.", null),
                    GuideStep("Шаг 3", "Если данные разошлись", "В окне «Подключить»: на телефоне с правильными данными нажмите «Этот телефон — эталон». На остальных — «Загрузить всё из облака».", "Сначала эталон, потом загрузка")
                ),
                proTip = "Если синхронизация работает только с VPN, значит ваш оператор ограничивает доступ к серверам Google. Учёт на телефоне при этом работает, данные отправятся, когда связь появится."
            ),
            GuideSection(
                id = "reports_excel",
                title = "6. Отчёты и формы",
                shortSubtitle = "Сводная ведомость, форма 8, форма 18",
                icon = Icons.Default.TableChart,
                badgeColor = goldText,
                forWho = "Старшины, делопроизводители",
                steps = listOf(
                    GuideStep("1", "Где найти", "Кнопка «Отчёт» на главном экране или раздел «Ещё» → «Отчёты». Вверху выберите вкладку нужного документа.", null),
                    GuideStep("2", "Форма № 8", "Раздаточная (сдаточная) ведомость: кому и что выдано, количество и места для подписей получателей.", null),
                    GuideStep("3", "Форма № 18", "Книга учёта: приход, расход и остаток по каждому наименованию за период.", null),
                    GuideStep("4", "Отправить", "Кнопка «Поделиться» сохраняет таблицу для Excel и отправляет в мессенджер или на почту.", "Открывается в Excel и Google Таблицах")
                ),
                proTip = "Перед печатью проверьте реквизиты воинской части и фамилии должностных лиц в шапке."
            ),
            GuideSection(
                id = "license",
                title = "7. Базовый режим, демо и PRO",
                shortSubtitle = "3 дня безлимита, далее базовый режим или PRO",
                icon = Icons.Default.Star,
                badgeColor = sageBright,
                forWho = "Все пользователи",
                steps = listOf(
                    GuideStep("Демо", "3 дня полного доступа", "После первой регистрации все функции открыты на 72 часа. Затем приложение переходит в бесплатный локальный режим: 1 основной склад, до 15 активных позиций и до 15 своих карточек, приход, выдача на руки и локальная копия в файл.", null),
                    GuideStep("Тарифы PRO", "30, 90 или 365 дней", "Нажмите на плашку «Демо / Базовый» → выберите срок: 30 дней (490 ₽), 90 дней (1 190 ₽) или 365 дней (3 490 ₽). Оплата через ЮKassa (СБП, карта МИР), ключ также придёт на почту.", null),
                    GuideStep("Восстановить", "Сменили телефон", "«Ещё» → «Восстановить оплаченную лицензию» — сервер найдёт её по вашей почте.", null)
                ),
                proTip = "Лицензия и срок демо привязываются на сервере, поэтому не теряются при смене телефона."
            ),
            GuideSection(
                id = "faq",
                title = "8. Частые вопросы",
                shortSubtitle = "Письма, синхронизация, данные",
                icon = Icons.Default.HelpOutline,
                badgeColor = tealText,
                forWho = "Все пользователи",
                steps = listOf(
                    GuideStep("?", "Не приходит письмо с кодом", "Подождите минуту и проверьте «Спам» и «Промоакции». Нажмите «Отправить код снова». Если пишет «Письмо не удалось отправить» — продолжите без подтверждения и напишите нам в бот поддержки.", null),
                    GuideStep("?", "Разные данные на телефонах", "Проверьте, что ключи подразделения совпадают, затем сделайте «Эталон» на правильном телефоне и «Загрузить из облака» на остальных.", null),
                    GuideStep("?", "Потеряются ли данные при обновлении", "Нет. Обновление ставится поверх, склад, журнал и лицензия сохраняются.", null),
                    GuideStep("?", "Где задать вопрос", "Telegram-бот поддержки @kapterka_help_bot, новости — в канале @kapterka_pro, сайт kapterka-pro.ru.", null)
                ),
                proTip = "Ссылки на сайт и канал есть внизу этой инструкции и в разделе «Ещё»."
            )
        )
    }

    val categoriesList = listOf("Все темы", "Старт", "Склад", "Операции", "Заявки", "Синхронизация", "Отчёты", "Лицензия", "Вопросы")

    val filteredSections = remember(searchQuery, selectedCategoryIndex) {
        guideSections.filterIndexed { index, section ->
            val matchesCategory = selectedCategoryIndex == 0 || index == selectedCategoryIndex - 1

            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                section.title.lowercase().contains(q) ||
                section.shortSubtitle.lowercase().contains(q) ||
                section.forWho.lowercase().contains(q) ||
                section.proTip.lowercase().contains(q) ||
                section.steps.any { it.stepTitle.lowercase().contains(q) || it.stepContent.lowercase().contains(q) }
            }

            matchesCategory && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TacticalBg.copy(alpha = 0.86f))
                .padding(horizontal = 12.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Animated3DDialogContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f),
                cornerRadius = 24.dp,
                accentBorder = SageGreenPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // DIALOG TOP HEADER WITH 3D BADGE & CLOSE ORB
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(SageGreenPrimary, TacticalTeal)
                                        )
                                    )
                                    .border(0.9.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Руководство",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "РУКОВОДСТВО ПОЛЬЗОВАТЕЛЯ",
                                    color = SageGreenBright,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.4.sp
                                )
                                Text(
                                    text = if (isMil) "Пошаговая инструкция по воинскому учёту" else "Пошаговая инструкция по учёту склада",
                                    color = TacticalTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconOrb3DButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = TacticalTextPrimary,
                            onClick = onDismiss,
                            size = 36.dp,
                            modifier = Modifier.testTag("close_guide_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // MODERN SEARCH BOX
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("guide_search_input"),
                        placeholder = {
                            Text(
                                "Введите название",
                                color = TacticalTextMuted,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = SageGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Очистить",
                                        tint = TacticalTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SageGreenPrimary,
                            unfocusedBorderColor = TacticalBorder,
                            focusedTextColor = TacticalTextPrimary,
                            unfocusedTextColor = TacticalTextPrimary,
                            cursorColor = SageGreenBright
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // HORIZONTAL CATEGORY PILLS (YOUTHFUL CHIP NAVIGATION)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoriesList.forEachIndexed { idx, catName ->
                            val isSelected = selectedCategoryIndex == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SageGreenPrimary else TacticalSurfaceLight)
                                    .border(
                                        1.dp,
                                        if (isSelected) SageGreenBright else TacticalBorderSubtle,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedCategoryIndex = idx }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = catName,
                                    color = if (isSelected) Color(0xFF0F1C13) else TacticalTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // MAIN SCROLLABLE ACCORDION LIST
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (filteredSections.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = TacticalTextMuted,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "По запросу «$searchQuery» ничего не найдено",
                                            color = TacticalTextMuted,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        itemsIndexed(filteredSections) { index, section ->
                            GuideSectionCard(
                                section = section,
                                defaultExpanded = index == 0 || searchQuery.isNotBlank()
                            )
                        }

                        item { CommunityLinksCard() }

                        item {
                            // BOTTOM CALL-TO-ACTION CARD
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF14241B), Color(0xFF182D22))
                                        )
                                    )
                                    .border(1.dp, SageGreenPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SageGreenBright,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Готовы к работе?",
                                            color = TacticalTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Вы можете начать с добавления прихода или создания заявки прямо сейчас.",
                                        color = TacticalTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SageGreenPrimary,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Понятно, перейти в программу", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Разработчик: Васев Алексей Евгеньевич • Каптёрка PRO v${com.example.BuildConfig.VERSION_NAME}",
                                color = TacticalTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSectionCard(
    section: GuideSection,
    defaultExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isExpanded) section.badgeColor.copy(alpha = 0.5f) else TacticalBorder
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // HEADER BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TacticalSurfaceLight)
                            .border(1.dp, section.badgeColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = null,
                            tint = section.badgeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = section.title,
                            color = TacticalTextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = section.shortSubtitle,
                                color = TacticalTextMuted,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(TacticalSurfaceLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = if (isExpanded) section.badgeColor else TacticalTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // EXPANDED DETAILED STEPS
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TacticalBg.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Audience Tag
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TacticalSurfaceLight)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Кому подходит: ",
                            color = TacticalTextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = section.forWho,
                            color = SageGreenBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Steps
                    section.steps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TacticalSurface)
                                .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1B3828))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = step.stepNumber,
                                    color = SageGreenBright,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.stepTitle,
                                    color = TacticalTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = step.stepContent,
                                    color = TacticalTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                                step.keyAction?.let { action ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "💡 $action",
                                        color = TacticalGoldText,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Pro Tip Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF132219))
                            .border(1.dp, SageGreenPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = TacticalGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = section.proTip,
                            color = TacticalTextSecondary,
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
