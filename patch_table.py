import re

with open('app/src/main/java/com/example/ui/components/ExcelReportPreviewDialog.kt', 'r') as f:
    content = f.read()

target = """                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = reportText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                        }
                    }"""

replacement = """                    val lines = reportText.trim().split("\\n")
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp)) {
                        items(lines.size) { index ->
                            val line = lines[index]
                            if (line.isBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                            } else if (!line.contains("\\t")) {
                                Text(
                                    text = line,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                val cols = line.split("\\t")
                                val isHeader = line.startsWith("№\\t") || line.startsWith("Дата\\t")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.LightGray)
                                        .background(if (isHeader) Color(0xFFEEEEEE) else Color.Transparent)
                                        .padding(4.dp)
                                ) {
                                    cols.forEach { col ->
                                        Text(
                                            text = col,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 2.dp),
                                            fontSize = 9.sp,
                                            color = Color.Black,
                                            maxLines = 4,
                                            lineHeight = 11.sp,
                                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ui/components/ExcelReportPreviewDialog.kt', 'w') as f:
    f.write(content)
