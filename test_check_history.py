import os
for f in os.listdir("."):
    if f.endswith(".py") or f.endswith(".sh"):
        try:
            with open(f, "r", errors="ignore") as fh:
                c = fh.read()
                if "kapterka-pro-v3.4.3" in c or "17" in c:
                    print(f, "mentions target")
        except:
            pass
