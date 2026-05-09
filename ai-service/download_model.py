import os
import json
import time
import zipfile
import shutil
import gdown
import joblib

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(BASE_DIR, "models")
os.makedirs(MODELS_DIR, exist_ok=True)

# Chỉ lưu file ID cho dễ quản lý
FILES = {
    "category_encoder.pkl": "1bMxnKdwFKNRKgiImWNXQ3gEINDiu6n0W",
    "type_encoder.pkl": "1nxMYIImttsTdKVsxeuNQuTrrxVacFuuZ",
    #"finance_nlu_metadata.json": "1kY5HjJPdsf9lWoXTv83HlxFvtS-i7OCI",
    "phobert_type_model.zip": "1bqwAQPdcc5Bja7xSEHGByoBkOknqBAV4",
    "phobert_category_model.zip": "1WkSWrpYmQfkPh9lJGHeZp5d5YvRtNZc6",
}

def build_gdrive_url(file_id: str) -> str:
    return f"https://drive.google.com/file/d/{file_id}/view?usp=sharing"

def validate_pickle(path: str):
    obj = joblib.load(path)
    print(f"[OK] Valid pickle: {os.path.basename(path)} -> {type(obj)}")

def validate_json(path: str):
    with open(path, "r", encoding="utf-8") as f:
        json.load(f)
    print(f"[OK] Valid json: {os.path.basename(path)}")

def validate_zip(path: str):
    with zipfile.ZipFile(path, "r") as zf:
        bad_file = zf.testzip()
        if bad_file is not None:
            raise ValueError(f"Zip bị lỗi tại file: {bad_file}")
    print(f"[OK] Valid zip: {os.path.basename(path)}")

def validate_file(path: str):
    if path.endswith(".pkl"):
        validate_pickle(path)
    elif path.endswith(".json"):
        validate_json(path)
    elif path.endswith(".zip"):
        validate_zip(path)

def is_valid_existing_file(path: str) -> bool:
    if not os.path.exists(path):
        return False
    try:
        validate_file(path)
        return True
    except Exception as e:
        print(f"[WARN] Existing file invalid: {os.path.basename(path)} -> {e}")
        return False

def download_file(name: str, file_id: str, max_retries: int = 3):
    output_path = os.path.join(MODELS_DIR, name)
    url = build_gdrive_url(file_id)

    # Nếu file đã có nhưng hợp lệ thì bỏ qua
    if is_valid_existing_file(output_path):
        print(f"[SKIP] {name} already exists and is valid")
        return output_path

    # Nếu file cũ tồn tại nhưng hỏng thì xóa
    if os.path.exists(output_path):
        print(f"[DELETE] Removing invalid file: {name}")
        os.remove(output_path)

    last_error = None

    for attempt in range(1, max_retries + 1):
        try:
            print(f"[DOWNLOADING] {name} (attempt {attempt}/{max_retries})")
            gdown.download(url, output_path, quiet=False)

            if not os.path.exists(output_path):
                raise FileNotFoundError(f"{name} không được tạo sau khi download")

            validate_file(output_path)
            return output_path

        except Exception as e:
            last_error = e
            print(f"[ERROR] Download failed for {name}: {e}")

            if os.path.exists(output_path):
                try:
                    os.remove(output_path)
                except Exception:
                    pass

            if attempt < max_retries:
                time.sleep(2)

    raise RuntimeError(f"Tải thất bại file {name} sau {max_retries} lần. Lỗi cuối: {last_error}")

def unzip_if_needed(file_path: str):
    if not file_path.endswith(".zip"):
        return

    extract_name = os.path.splitext(os.path.basename(file_path))[0]
    extract_path = os.path.join(MODELS_DIR, extract_name)

    if os.path.exists(extract_path) and os.listdir(extract_path):
        print(f"[SKIP] {extract_name} already extracted")
        return

    os.makedirs(extract_path, exist_ok=True)

    print(f"[EXTRACTING] {os.path.basename(file_path)}")
    with zipfile.ZipFile(file_path, "r") as zip_ref:
        zip_ref.extractall(extract_path)

    nested_path = os.path.join(extract_path, extract_name)
    if os.path.isdir(nested_path):
        for item in os.listdir(nested_path):
            shutil.move(
                os.path.join(nested_path, item),
                os.path.join(extract_path, item)
            )
        os.rmdir(nested_path)

def main():
    print("=== Downloading AI model files ===")
    failed_files = []

    for name, file_id in FILES.items():
        try:
            downloaded_path = download_file(name, file_id)
            unzip_if_needed(downloaded_path)
        except Exception as e:
            failed_files.append((name, str(e)))
            print(f"[FAILED] {name}: {e}")

    if failed_files:
        print("\n=== SOME FILES FAILED ===")
        for name, error in failed_files:
            print(f"- {name}: {error}")
    else:
        print("=== DONE ===")

if __name__ == "__main__":
    main()