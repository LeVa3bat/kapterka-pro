from compare_packages import get_dex_strings
s21 = get_dex_strings("docs/kapterka-pro-v3.4.3-b21.apk")
s22 = get_dex_strings("docs/kapterka-pro-v3.4.4-b22.apk")

diff = s22 - s21
compose_diff = [c for c in diff if "compose" in c]
for c in compose_diff[:25]:
    print(c)
