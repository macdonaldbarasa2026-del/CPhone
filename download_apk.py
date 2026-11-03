import os
import sys
import urllib.request

URL = "https://github.com/macdonaldbarasa2026-del/CPhone/releases/download/latest/app-debug.apk"
OUT_PATH = "/sdcard/Download/CPhone-debug.apk"

def main():
    print("Connecting to GitHub release...")
    req = urllib.request.Request(URL, headers={"User-Agent": "Mozilla/5.0"})
    partial_path = OUT_PATH + ".part"
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            total_size = int(response.headers.get("Content-Length", 0))
            if total_size <= 0:
                raise ValueError("Could not determine APK file size.")

            print(f"APK size: {total_size} bytes ({total_size / (1024*1024):.2f} MB)")
            downloaded = 0
            with open(partial_path, "wb") as apk:
                while True:
                    chunk = response.read(1024 * 1024)
                    if not chunk:
                        break
                    apk.write(chunk)
                    downloaded += len(chunk)

        if downloaded != total_size:
            raise IOError(f"Incomplete download: expected {total_size} bytes, got {downloaded}.")

        os.replace(partial_path, OUT_PATH)
        print(f"\nSUCCESS! Downloaded {downloaded} bytes.")
        print(f"File saved to: {OUT_PATH}")
        os.system(f"termux-open '{OUT_PATH}'")
    except Exception as e:
        if os.path.exists(partial_path):
            os.remove(partial_path)
        print(f"Download failed: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
