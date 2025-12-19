import os
import glob

# Find all Java files in modules directory
java_files = glob.glob(r'src/main/java/com/sacco/sacco_system/modules/**/*.java', recursive=True)

fixed_count = 0
for file_path in java_files:
    try:
        with open(file_path, 'rb') as f:
            content = f.read()
        
        # Check for BOM
        if content.startswith(b'\xef\xbb\xbf'):
            # Remove BOM
            clean_content = content[3:]
            with open(file_path, 'wb') as f:
                f.write(clean_content)
            fixed_count += 1
            print(f"Fixed BOM: {file_path}")
    except Exception as e:
        print(f"Error processing {file_path}: {e}")

print(f"\nTotal files fixed: {fixed_count}")
