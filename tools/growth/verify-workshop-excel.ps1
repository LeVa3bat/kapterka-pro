param(
    [string]$Repository = 'C:\Projects\kapterka-pro',
    [string]$TemporaryDirectory = "$env:TEMP\kapterka-workshop-kit-20261013"
)
$ErrorActionPreference = 'Stop'
$kitUrl = 'https://kapterka-pro.ru/uchet-instrumenta-android.html#workshop-kit'
$results = [System.Collections.Generic.List[object]]::new()
$excel = $null
$book = $null
$qaBook = $null
function Assert-Cell($Sheet, [string]$Cell, $Expected, [string]$Label) {
    $value = $Sheet.Range($Cell).Value2
    if ($Expected -eq '') { $pass = ($null -eq $value -or $value -eq '') }
    elseif ($Expected -is [string]) { $pass = ($value -ceq $Expected) }
    else { $pass = [Math]::Abs([double]$value - [double]$Expected) -lt 0.00000001 }
    if (-not $pass) { throw "$Label [$($Sheet.Name)!$Cell]: expected '$Expected', got '$value'" }
    $results.Add([PSCustomObject]@{label=$Label; cell="$($Sheet.Name)!$Cell"; expected=$Expected; actual=$value; passed=$true})
}
try {
    $excel = New-Object -ComObject Excel.Application
    $excel.Visible = $false
    $excel.DisplayAlerts = $false
    $excel.EnableEvents = $false
    $excel.AutomationSecurity = 3
    foreach ($file in @('workshop-blank.xlsx', 'workshop-example.xlsx')) {
        Write-Output "Opening $file"
        $absolute = Join-Path $Repository "docs\downloads\$file"
        $book = $excel.Workbooks.Open($absolute, 0, $false)
        $excel.CalculateFullRebuild()
        $stock = $book.Worksheets.Item('Остатки')
        $journal = $book.Worksheets.Item('Журнал')
        $guide = $book.Worksheets.Item('Инструкция')
        if ($file -eq 'workshop-example.xlsx') {
            $expected = @(52,7,7,2,5.5,40)
            for ($i=0; $i -lt 6; $i++) { Assert-Cell $stock "H$($i+8)" $expected[$i] 'Excel: остаток примера' }
            Assert-Cell $journal 'J20' 'Да' 'Excel: строка учтена'
            if ($journal.Range('A8').Text -cne '01.10.2026') { throw "Example date display: '$($journal.Range('A8').Text)', value '$($journal.Range('A8').Value2)', format '$($journal.Range('A8').NumberFormat)', local '$($journal.Range('A8').NumberFormatLocal)', width '$($journal.Range('A8').ColumnWidth)'" }
            $results.Add([PSCustomObject]@{label='Excel: typed date display'; cell='Журнал!A8'; actual=$journal.Range('A8').Text; passed=$true})
        } else {
            Assert-Cell $stock 'H8' '' 'Excel: пустая позиция'
            Assert-Cell $journal 'J307' '' 'Excel: пустая строка 300'
        }
        # The artifact-tool creates the cells, formulas, styles, validations and
        # image. Excel supplies the unsupported native hyperlink/blank setting
        # and refreshes formula caches with its own calculation engine.
        if ($guide.Hyperlinks.Count -gt 0) { $guide.Hyperlinks.Delete() }
        $null = $guide.Hyperlinks.Add($guide.Range('B28'), $kitUrl)
        foreach ($range in @('C8:C37','D8:D37')) { $stock.Range($range).Validation.IgnoreBlank = $true }
        foreach ($range in @('A8:A307','B8:B307','C8:C307','D8:D307')) { $journal.Range($range).Validation.IgnoreBlank = $true }
        foreach ($range in @('B8:B307','C8:C307','D8:D307')) {
            $validation = $journal.Range($range).Validation
            if ($validation.AlertStyle -ne 1 -or -not $validation.ShowError) { throw "Stop validation missing: $file $range" }
        }
        $book.Save()
        $results.Add([PSCustomObject]@{label='Excel: saved native features'; file=$file; validations=6; hyperlinks=$guide.Hyperlinks.Count; passed=$true})
        $book.Close($false)
        $book = $null
    }
    $disposable = Join-Path $TemporaryDirectory 'workshop-example-native-qa.xlsx'
    Copy-Item -LiteralPath (Join-Path $Repository 'docs\downloads\workshop-example.xlsx') -Destination $disposable -Force
    $qaBook = $excel.Workbooks.Open($disposable, 0, $false)
    $stock = $qaBook.Worksheets.Item('Остатки')
    $journal = $qaBook.Worksheets.Item('Журнал')
    $journal.Range('D8').Value2 = 41
    $excel.CalculateFullRebuild()
    Assert-Cell $stock 'H8' 53 'Excel: изменение прихода'
    $journal.Range('D8').Value2 = 40
    $journal.Range('D9').Value2 = 20
    $excel.CalculateFullRebuild()
    Assert-Cell $journal 'H9' -10 'Excel: отрицательный остаток после выдачи'
    Assert-Cell $stock 'H9' -9 'Excel: отрицательный итог'
    Assert-Cell $journal 'I9' 'Отрицательный остаток' 'Excel: предупреждение'
    $journal.Range('D9').Value2 = 4
    $journal.Range('D8').Value2 = 0
    $excel.CalculateFullRebuild()
    Assert-Cell $journal 'J8' 'Нет' 'Excel: нулевая операция исключена'
    Assert-Cell $stock 'H8' 12 'Excel: нулевая операция не добавила приход'
    $journal.Range('D8').Value2 = 40
    $journal.Range('B8').Value2 = 'UNKNOWN'
    $excel.CalculateFullRebuild()
    Assert-Cell $journal 'J8' 'Нет' 'Excel: неизвестный код'
    $journal.Range('B8').Value2 = 'W001'
    $stock.Range('D8').ClearContents()
    $excel.CalculateFullRebuild()
    Assert-Cell $stock 'I8' 'Укажите начальный остаток' 'Excel: пустое поле'
    Assert-Cell $journal 'J8' 'Нет' 'Excel: пустой остаток не принят как ноль'
    $stock.Range('D8').Value2 = 0
    $excel.CalculateFullRebuild()
    Assert-Cell $stock 'H8' 32 'Excel: начальный ноль допустим'
    $stock.Range('D8').Value2 = 20
    $journal.Range('D13').Value2 = 5
    $excel.CalculateFullRebuild()
    Assert-Cell $stock 'I9' 'Проверьте возврат' 'Excel: возврат больше выдачи'
    $journal.Range('D13').Value2 = 1
    $stock.Range('A9').Value2 = 'W001'
    $excel.CalculateFullRebuild()
    Assert-Cell $stock 'I8' 'Повтор кода' 'Excel: дублированный код'
    Assert-Cell $journal 'J8' 'Нет' 'Excel: дублированный код исключён'
    $stock.Range('A9').Value2 = 'W002'
    $journal.Range('A307').Value2 = 46301
    $journal.Range('B307').Value2 = 'W001'
    $journal.Range('C307').Value2 = 'Возврат'
    $journal.Range('D307').Value2 = 1
    $excel.CalculateFullRebuild()
    Assert-Cell $stock 'H8' 53 'Excel: запись 300 учтена'
    $qaBook.Close($false)
    $qaBook = $null
    $report = [PSCustomObject]@{engine='Microsoft Excel'; version=$excel.Version; source=$kitUrl; checks=$results}
    $report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $Repository 'growth\qa\workshop-native-excel-checks.json') -Encoding utf8
    Write-Output "Excel checks passed: $($results.Count)"
} finally {
    if ($qaBook) { $qaBook.Close($false) }
    if ($book) { $book.Close($false) }
    if ($excel) { $excel.Quit(); [void][System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($excel) }
}
