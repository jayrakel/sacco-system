import re
import glob

# Files with issues and their fixes
fixes = {
    'src/main/java/com/sacco/sacco_system/modules/auth/service/AuthService.java': [
        (r'import com\.sacco\.sacco_system\.annotation\.Loggable;', ''),
        (r'import com\.sacco\.sacco_system\.security\.JwtService;', 'import com.sacco.sacco_system.modules.auth.service.JwtService;\n// Custom JWT service - create if missing'),
        (r'import com\.sacco\.sacco_system\.service\.EmailService;', 'import com.sacco.sacco_system.modules.notification.domain.service.EmailService;'),
        (r'@Loggable\(action = "REGISTER_USER", category = "AUTH"\)', '// @Loggable removed - create separate audit service'),
    ],
    'src/main/java/com/sacco/sacco_system/modules/loan/api/controller/LoanController.java': [
        (r'import com\.sacco\.sacco_system\.modules\.LoanDTO;', 'import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;'),
    ],
    'src/main/java/com/sacco/sacco_system/modules/loan/domain/service/LoanService.java': [
        (r'import com\.sacco\.sacco_system\.modules\.LoanDTO;', 'import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;'),
    ],
    'src/main/java/com/sacco/sacco_system/modules/notification/domain/service/NotificationService.java': [
        (r'import com\.sacco\.sacco_system\.entity\.Notification;', 'import com.sacco.sacco_system.modules.notification.domain.entity.Notification;'),
    ],
}

fixed_count = 0
for file_path, replacements in fixes.items():
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        for pattern, replacement in replacements:
            content = re.sub(pattern, replacement, content)
        
        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            fixed_count += 1
            print(f"Fixed: {file_path}")
    except FileNotFoundError:
        print(f"File not found: {file_path}")
    except Exception as e:
        print(f"Error processing {file_path}: {e}")

print(f"\nTotal files fixed: {fixed_count}")
