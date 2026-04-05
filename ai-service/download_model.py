import os
import zipfile
import shutil
import gdown
import joblib

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(BASE_DIR, "models")

os.makedirs(MODELS_DIR, exist_ok=True)

FILES = {
    "category_encoder.pkl": "https://drive.google.com/uc?id=1bMxnKdwFKNRKgiImWNXQ3gEINDiu6n0W",
    "type_encoder.pkl": "https://drive.google.com/uc?id=1nxMYIImttsTdKVsxeuNQuTrrxVacFuuZ",
    "finance_nlu_metadata.json": "https://drive.google.com/uc?id=1kY5HjJPdsf9lWoXTv83HlxFvtS-i7OCI",
    "phobert_type_model.zip": "https://drive.google.com/uc?id=1bqwAQPdcc5Bja7xSEHGByoBkOknqBAV4",
    "phobert_category_model.zip": "https://drive.google.com/uc?id=1WkSWrpYmQfkPh9lJGHeZp5d5YvRtNZc6",
}

def validate_pickle(path: str):
    try:
        obj = joblib.load(path)
        print(f"[OK] Valid pickle: {os.path.basename(path)} -> {type(obj)}")
    except Exception as e:
        raise ValueError(
            f"{os.path.basename(path)} tải về xong nhưng joblib.load không đọc được: {e}"
        )

def download_file(name, url):
    output_path = os.path.join(MODELS_DIR, name)

    if os.path.exists(output_path):
        print(f"[SKIP] {name} already exists")
        return output_path

    print(f"[DOWNLOADING] {name}")
    gdown.download(url, output_path, quiet=False)

    if name.endswith(".pkl"):
        validate_pickle(output_path)

    return output_path

def unzip_if_needed(file_path):
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
            shutil.move(os.path.join(nested_path, item), os.path.join(extract_path, item))
        os.rmdir(nested_path)

def main():
    print("=== Downloading AI model files ===")
    for name, url in FILES.items():
        downloaded_path = download_file(name, url)
        unzip_if_needed(downloaded_path)
    print("=== DONE ===")

if __name__ == "__main__":
    main()