import re

with open('docs/style.css', 'r', encoding='utf-8') as f:
    content = f.read()

old_css = """  .footer-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  .footer-bottom {
    flex-direction: column;
    text-align: center;
  }"""

new_css = """  .footer-grid {
    grid-template-columns: 1fr;
    gap: 32px;
    text-align: center;
  }
  .footer-col {
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  .footer-col h4 {
    margin-bottom: 12px;
  }
  .footer-links {
    align-items: center;
  }
  .footer-legal-badge {
    text-align: center;
    width: 100%;
  }
  .logo-brand {
    justify-content: center;
  }
  .footer-bottom {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }"""

content = content.replace(old_css, new_css)

with open('docs/style.css', 'w', encoding='utf-8') as f:
    f.write(content)
