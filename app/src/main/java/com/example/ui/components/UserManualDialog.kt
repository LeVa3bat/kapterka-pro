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
    onOpenSyncDialog: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    val guideSections = remember {
        listOf(
            GuideSection(
                id = "quick_start",
                title = "1. Быстрый старт и подключение",
                shortSubtitle = "Вход, код подразделения, синхронизация",
                icon = Icons.Default.Key,
                badgeColor = SageGreenBright,
                forWho = "Военные, гражданские, волонтеры",
                steps = listOf(
                    GuideStep(
                        stepNumber = "Шаг 1",
                        stepTitle = "Вход в систему и выбор роли",
                        stepContent = "При первом входе введите ваш позывной или ФИО (например: «Старшина роты», «Завскладом Иванов», «Логист Мария») и название подразделения/склада.",
                        keyAction = "Укажите имя и нажмите «Создать» или «Войти»"
                    ),
                    GuideStep(
                        stepNumber = "Шаг 2",
                        stepTitle = "Единый код подключения (Секретный код)",
                        stepContent = "Чтобы работать в единой базе нескольким людям, один пользователь создает подразделение и копирует секретный код (вверху экрана, например: kapt_59e13b).",
                        keyAction = "Код можно скопировать в 1 клик и передать коллегам"
                    ),
                    GuideStep(
                        stepNumber = "Шаг 3",
                        stepTitle = "Подключение других сотрудников / бойцов",
                        stepContent = "На других телефонах при первом открытии выберите «Вход в подразделение» и вставьте полученный код. База, склады и заявки объединятся автоматически.",
                        keyAction = "Все склады и остатки синхронизируются без проводов"
                    )
                ),
                proTip = "Приложение работает полностью офлайн в бункерах, складах и подвалах. При появлении интернета нажмите «Синхронизация базы» для обновления данных."
            ),
            GuideSection(
                id = "warehouses_stock",
                title = "2. Склады, точки и просмотр остатков",
                shortSubtitle = "Опорные пункты, цеха, фильтры по службам",
                icon = Icons.Default.Warehouse,
                badgeColor = TacticalTealText,
                forWho = "Кладовщики, старшины, командиры",
                steps = listOf(
                    GuideStep(
                        stepNumber = "Шаг 1",
                        stepTitle = "Переключение между складами и точками",
                        stepContent = "На главном экране вверху расположены карточки точек: «Базовый склад», «Опорный пункт Восток», «Огневая позиция 2» и др. Нажмите на нужную точку, чтобы увидеть ее остатки.",
                        keyAction = "Быстрый переключатель в один тап"
                    ),
                    GuideStep(
                        stepNumber = "Шаг 2",
                        stepTitle = "Добавление новых складов и локаций",
                        stepContent = "Нажмите значок «+» рядом с точками склада, чтобы добавить новый склад, цех, бокс или позицию с описанием.",
                        keyAction = "Кнопка «+ Точка» на главном экране"
                    ),
                    GuideStep(
                        stepNumber = "Шаг 3",
                        stepTitle = "Удобный фильтр по службам / категориям",
                        stepContent = "Используйте горизонтальные плашки («Служба РАВ», «БПЛА», «Связь», «СИБЗ», «Медицина», «ГСМ») и строку поиска для мгновенной фильтрации имущества.",
                        keyAction = "Поиск по артикулу, номенклатуре и категории"
                    )
                ),
                proTip = "Вы можете корректировать фактический остаток прямо из карточки позиции при проведении быстрой ревизии."
            ),
            GuideSection(
                id = "operations",
                title = "3. Операции: Приход, Расход, Выдача, Перемещение",
                shortSubtitle = "Акты списания, закрепление за бойцами",
                icon = Icons.Default.TrendingUp,
                badgeColor = TacticalGoldText,
                forWho = "Все пользователи",
                steps = listOf(
                    GuideStep(
                        stepNumber = "🟢 Приход",
                        stepTitle = "Поступление имущества на склад",
                        stepContent = "Нажмите зеленую кнопку «Приход». Выберите позицию из каталога (или добавьте новую), укажите склад назначения, количество и поставщика/источник.",
                        keyAction = "Остаток на складе увеличивается"
                    ),
                    GuideStep(
                        stepNumber = "🔵 Перемещение",
                        stepTitle = "Передача между складами и точками",
                        stepContent = "Нажмите синюю кнопку «Перемещение». Выберите склад-отправитель и склад-получатель. Количество спишется с одного и зачислится на другой.",
                        keyAction = "Исключает путаницу при перемещениях"
                    ),
                    GuideStep(
                        stepNumber = "🟡 Выдача",
                        stepTitle = "Закрепление за конкретным бойцом / сотрудником",
                        stepContent = "Нажмите кнопку «Выдача». Укажите позывной/ФИО получателя и основание выдачи (под роспись). В истории сохранится точная запись о выдаче.",
                        keyAction = "Фиксация ответственности в истории"
                    ),
                    GuideStep(
                        stepNumber = "🔴 Расход / Списание",
                        stepTitle = "Списание израсходованного имущества (Форма № 8)",
                        stepContent = "Нажмите красную кнопку «Расход». Выберите точку, количество и причину (например: «Боевая работа», «Учебные стрельбы», «Естественный износ»). Создается акт расхода.",
                        keyAction = "Автоматически попадает в отчет Формы № 8"
                    )
                ),
                proTip = "Все действия с точностью до секунды сохраняются во вкладке «История» с возможностью поиска по датам, типу и фамилиям."
            ),
            GuideSection(
                id = "requests",
                title = "4. Электронные заявки и потребности",
                shortSubtitle = "Подача потребностей, сборка, выдача",
                icon = Icons.Default.Assignment,
                badgeColor = SageGreenBright,
                forWho = "Командиры отделений, цеха, снабженцы",
                steps = listOf(
                    GuideStep(
                        stepNumber = "Шаг 1",
                        stepTitle = "Создание новой заявки",
                        stepContent = "Перейдите во вкладку «Заявки» и нажмите «+ Создать заявку». Выберите склад-назначение, позиции, нужное количество и срочность (Обычная / Срочная / Критическая).",
                        keyAction = "Заявка моментально появится у завскладом"
                    ),
                    GuideStep(
                        stepNumber = "Шаг 2",
                        stepTitle = "Обработка и сборка на складе",
                        stepContent = "Старшина или завскладом меняет статус заявки: «В обработке» ➔ «Собрана» ➔ «Выдана». Инициатор видит статус в реальном времени.",
                        keyAction = "Удобный контроль готовности заказа"
                    )
                ),
                proTip = "На иконке вкладки «Заявки» отображается яркий бейдж с количеством необработанных требований."
            ),
            GuideSection(
                id = "reports_excel",
                title = "5. Армейская и складская отчетность (Excel)",
                shortSubtitle = "Форма № 8, Форма № 18, инвентаризация",
                icon = Icons.Default.TableChart,
                badgeColor = TacticalTealText,
                forWho = "Старшины, бухгалтеры, начальники служб",
                steps = listOf(
                    GuideStep(
                        stepNumber = "Форма № 8",
                        stepTitle = "Акт списания (расхода) материальных ценностей",
                        stepContent = "Формирует официальный акт с перечнем израсходованного имущества, боеприпасов, ГСМ, основанием списания и графами для подписей комиссии.",
                        keyAction = "Готово для печати и отправки командованию"
                    ),
                    GuideStep(
                        stepNumber = "Форма № 18",
                        stepTitle = "Книга учета движения материальных средств",
                        stepContent = "Сводный журнал: остаток на начало периода, весь приход, весь расход/выдача и остаток на конец периода по каждому наименованию.",
                        keyAction = "Основной отчет для строевой части и склада"
                    ),
                    GuideStep(
                        stepNumber = "Экспорт в Excel",
                        stepTitle = "Выгрузка файлов и печать",
                        stepContent = "В шапке нажмите «Армейские отчеты Excel» или в меню «Ещё». Нажмите «Поделиться Excel / Печать» для отправки в мессенджер, почту или на принтер.",
                        keyAction = "Совместимо с Microsoft Excel, LibreOffice и МойОфис"
                    )
                ),
                proTip = "Перед экспортом можно просмотреть предпросмотр таблицы прямо на экране телефона."
            ),
            GuideSection(
                id = "civilian_tips",
                title = "6. Для гражданского персонала и волонтеров",
                shortSubtitle = "Учет гуманитарной помощи, складов и инструментов",
                icon = Icons.Default.People,
                badgeColor = SageGreenBright,
                forWho = "Волонтеры, гражданские склады, цеха",
                steps = listOf(
                    GuideStep(
                        stepNumber = "Совет 1",
                        stepTitle = "Учет гуманитарной помощи и снабжения",
                        stepContent = "Используйте «Каптёрку» как складскую программу: создавайте склады («Склад Москва», «Склад Ростов», «Сортировочный цех»), принимайте грузы и выдавайте адресатам.",
                        keyAction = "Прозрачный учет каждой коробки и позиции"
                    ),
                    GuideStep(
                        stepNumber = "Совет 2",
                        stepTitle = "Настройка собственных групп и служб",
                        stepContent = "В меню «Ещё» ➔ «Управление штатными группами» вы можете удалить армейские службы (РАВ, РХБЗ) и добавить свои (например: «Медикаменты», «Теплая одежда», «Инструмент», «Рации»).",
                        keyAction = "Полная кастомизация под гражданские задачи"
                    )
                ),
                proTip = "Интерфейс адаптирован как для сенсорных экранов смартфонов, так и для планшетов в горизонтальной ориентации."
            )
        )
    }

    val categoriesList = listOf("Все темы", "Быстрый старт", "Склады", "Операции", "Заявки", "Отчеты Excel", "Гражданским")

    val filteredSections = remember(searchQuery, selectedCategoryIndex) {
        guideSections.filterIndexed { index, section ->
            val matchesCategory = when (selectedCategoryIndex) {
                0 -> true
                1 -> section.id == "quick_start"
                2 -> section.id == "warehouses_stock"
                3 -> section.id == "operations"
                4 -> section.id == "requests"
                5 -> section.id == "reports_excel"
                6 -> section.id == "civilian_tips"
                else -> true
            }

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
                .background(TacticalBg.copy(alpha = 0.95f))
                .padding(horizontal = 12.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .clip(RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, SageGreenPrimary.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // DIALOG TOP HEADER WITH SLEEK GRADIENT & CLOSE BUTTON
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(SageGreenDark, Color(0xFF2C4A34))
                                        )
                                    )
                                    .border(1.dp, SageGreenBright, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Руководство",
                                    tint = SageGreenBright,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "РУКОВОДСТВО ПОЛЬЗОВАТЕЛЯ",
                                    color = SageGreenBright,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Инструкция для военных и гражданского персонала",
                                    color = TacticalTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(TacticalSurfaceLight)
                                .testTag("close_guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = TacticalTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
                                text = "Разработчик: Васев Алексей Евгеньевич • АСУ «Каптёрка» v2.9.4 PRO",
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
