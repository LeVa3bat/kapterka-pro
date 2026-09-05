import re
import os

# Update docs/index.html
with open('docs/index.html', 'r', encoding='utf-8') as f:
    html_content = f.read()

html_content = html_content.replace('support@kapterka-pro.ru', 'alex.666.881@gmail.com')

with open('docs/index.html', 'w', encoding='utf-8') as f:
    f.write(html_content)
    
print("Updated docs/index.html")

# Update EmailDeliveryService.kt
email_service_path = "app/src/main/java/com/example/data/notification/EmailDeliveryService.kt"
if os.path.exists(email_service_path):
    with open(email_service_path, 'r', encoding='utf-8') as f:
        kt_content = f.read()
    
    kt_content = kt_content.replace('support@kapterka-pro.ru', 'alex.666.881@gmail.com')
    
    with open(email_service_path, 'w', encoding='utf-8') as f:
        f.write(kt_content)
        
    print("Updated EmailDeliveryService.kt")
