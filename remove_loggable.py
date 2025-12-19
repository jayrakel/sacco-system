import re
import glob

# Find all Java files with @Loggable annotations
java_files = glob.glob(r'src/main/java/com/sacco/sacco_system/modules/**/*.java', recursive=True)

fixed_count = 0
for file_path in java_files:
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Remove @Loggable annotations and their parameters
        original = content
        # Pattern to match @Loggable(...) on a line before a method
        content = re.sub(r'\s*@Loggable\([^)]*\)\s*\n', '\n', content)
        
        if content != original:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            fixed_count += 1
            print(f"Removed @Loggable: {file_path}")
    except Exception as e:
        print(f"Error processing {file_path}: {e}")

print(f"\nTotal files fixed: {fixed_count}")
